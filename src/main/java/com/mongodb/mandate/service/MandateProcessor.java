package com.mongodb.mandate.service;

import com.mongodb.client.ClientSession;
import com.mongodb.client.TransactionBody;
import com.mongodb.mandate.model.*;
import com.mongodb.mandate.repository.MandateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class MandateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MandateProcessor.class);

    private final MandateRepository repository;
    private final MandateDiffService diffService;
    private final int batchSize;

    // Statistics
    private long totalProcessed = 0;
    private long insertCount = 0;
    private long updateCount = 0;
    private long skipCount = 0;
    private long errorCount = 0;
    private long newCreditors = 0;
    private long newDebtors = 0;

    public MandateProcessor(MandateRepository repository, int batchSize) {
        this.repository = repository;
        this.diffService = new MandateDiffService();
        this.batchSize = batchSize;
    }

    public void processFile(Path filePath) throws IOException {
        logger.info("Starting to process file: {}", filePath);
        long startTime = System.currentTimeMillis();
        String batchId = UUID.randomUUID().toString();

        resetStatistics();

        try (MandateFileReader reader = new MandateFileReader(filePath)) {
            List<MandateFileRecord> batch;

            while (!(batch = reader.readBatch(batchSize)).isEmpty()) {
                processBatch(batch, reader.getFileName(), batchId);
                totalProcessed += batch.size();

                if (totalProcessed % 1000 == 0) {
                    logger.info("Processed {} records...", totalProcessed);
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        logStatistics(duration);
    }

    private void processBatch(List<MandateFileRecord> records, String sourceFile, String batchId) {
        // Extract mandate IDs
        List<String> mandateIds = records.stream()
                .map(MandateFileRecord::getMandateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Get existing mandate dates
        Map<String, LocalDateTime> existingDates = repository.batchGetMandateUpdateDates(mandateIds);

        // Categorize records
        List<MandateFileRecord> toInsert = new ArrayList<>();
        List<String> toCheckForUpdate = new ArrayList<>();

        for (MandateFileRecord record : records) {
            String mandateId = record.getMandateId();

            if (!existingDates.containsKey(mandateId)) {
                toInsert.add(record);
            } else {
                LocalDateTime existingDate = existingDates.get(mandateId);
                LocalDateTime newDate = record.getLastUpdateDate();

                if (existingDate != null && newDate != null && existingDate.equals(newDate)) {
                    skipCount++;
                } else {
                    toCheckForUpdate.add(mandateId);
                }
            }
        }

        // Process inserts with transactions
        if (!toInsert.isEmpty()) {
            processInserts(toInsert, sourceFile, batchId);
        }

        // Process updates
        if (!toCheckForUpdate.isEmpty()) {
            processUpdates(records, toCheckForUpdate, sourceFile, batchId);
        }
    }

    private void processInserts(List<MandateFileRecord> records, String sourceFile, String batchId) {
        // Collect all creditor and debtor IDs
        Set<String> creditorIds = records.stream()
                .map(MandateFileRecord::getCreditorId)
                .collect(Collectors.toSet());

        Set<String> debtorIds = records.stream()
                .map(MandateFileRecord::generateDebtorId)
                .collect(Collectors.toSet());

        // Check which already exist
        Set<String> existingCreditors = repository.getExistingCreditorIds(creditorIds);
        Set<String> existingDebtors = repository.getExistingDebtorIds(debtorIds);

        List<MandateAudit> audits = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (MandateFileRecord record : records) {
            String debtorId = record.generateDebtorId();
            boolean insertCreditor = !existingCreditors.contains(record.getCreditorId());
            boolean insertDebtor = !existingDebtors.contains(debtorId);

            // Build entities
            Creditor creditor = insertCreditor ? buildCreditor(record) : null;
            Debtor debtor = insertDebtor ? buildDebtor(record, debtorId) : null;
            DirectDebitMandate mandate = buildMandate(record, debtorId);

            try {
                // Use transaction for insert
                try (ClientSession session = repository.getMongoClient().startSession()) {
                    session.withTransaction((TransactionBody<Void>) () -> {
                        repository.insertMandateWithTransaction(
                                session, mandate, creditor, debtor, insertCreditor, insertDebtor);
                        return null;
                    });
                }

                // Track new entities
                if (insertCreditor) {
                    existingCreditors.add(record.getCreditorId());
                    newCreditors++;
                }
                if (insertDebtor) {
                    existingDebtors.add(debtorId);
                    newDebtors++;
                }

                insertCount++;

                // Create audit
                audits.add(MandateAudit.builder()
                        .mandateId(record.getMandateId())
                        .changeType("INSERT")
                        .changeTimestamp(now)
                        .sourceFile(sourceFile)
                        .newUpdateDate(record.getLastUpdateDate())
                        .fieldChanges(Collections.emptyList())
                        .processedBy(System.getProperty("user.name", "system"))
                        .batchId(batchId)
                        .build());

            } catch (Exception e) {
                logger.error("Transaction failed for mandate {}: {}", record.getMandateId(), e.getMessage());
                errorCount++;
            }
        }

        // Batch insert audits
        if (!audits.isEmpty()) {
            repository.batchInsertAudits(audits);
        }
    }

    private void processUpdates(List<MandateFileRecord> allRecords,
                                List<String> mandateIdsToUpdate,
                                String sourceFile, String batchId) {

        Map<String, MandateFileRecord> recordsMap = allRecords.stream()
                .filter(r -> mandateIdsToUpdate.contains(r.getMandateId()))
                .collect(Collectors.toMap(MandateFileRecord::getMandateId, r -> r));

        Map<String, DirectDebitMandate> existingMandates = repository.batchGetMandates(mandateIdsToUpdate);

        List<MandateAudit> audits = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (String mandateId : mandateIdsToUpdate) {
            DirectDebitMandate existing = existingMandates.get(mandateId);
            MandateFileRecord record = recordsMap.get(mandateId);

            if (existing == null || record == null) {
                errorCount++;
                continue;
            }

            String debtorId = record.generateDebtorId();
            DirectDebitMandate updated = buildMandate(record, debtorId);

            // Diff the mandates
            List<FieldChange> changes = diffService.diff(existing, updated);

            if (!changes.isEmpty()) {
                // Preserve system fields
                updated.setId(existing.getId());
                updated.setCreatedAt(existing.getCreatedAt());
                updated.setVersion(existing.getVersion());

                try {
                    repository.updateMandate(updated);
                    updateCount++;

                    audits.add(MandateAudit.builder()
                            .mandateId(mandateId)
                            .changeType("UPDATE")
                            .changeTimestamp(now)
                            .sourceFile(sourceFile)
                            .previousUpdateDate(existing.getLastUpdateDate())
                            .newUpdateDate(record.getLastUpdateDate())
                            .fieldChanges(changes)
                            .processedBy(System.getProperty("user.name", "system"))
                            .batchId(batchId)
                            .build());

                } catch (Exception e) {
                    logger.error("Update failed for mandate {}: {}", mandateId, e.getMessage());
                    errorCount++;
                }
            } else {
                skipCount++;
            }
        }

        if (!audits.isEmpty()) {
            repository.batchInsertAudits(audits);
        }
    }

    private Creditor buildCreditor(MandateFileRecord record) {
        return Creditor.builder()
                .creditorId(record.getCreditorId())
                .creditorName(record.getCreditorName())
                .accountNumber(record.getCreditorAccountNumber())
                .sortCode(record.getCreditorSortCode())
                .iban(record.getCreditorIban())
                .bic(record.getCreditorBic())
                .build();
    }

    private Debtor buildDebtor(MandateFileRecord record, String debtorId) {
        return Debtor.builder()
                .debtorId(debtorId)
                .name(record.getDebtorName())
                .accountNumber(record.getDebtorAccountNumber())
                .sortCode(record.getDebtorSortCode())
                .iban(record.getDebtorIban())
                .bic(record.getDebtorBic())
                .email(record.getDebtorEmail())
                .phone(record.getDebtorPhone())
                .build();
    }

    private DirectDebitMandate buildMandate(MandateFileRecord record, String debtorId) {
        return DirectDebitMandate.builder()
                .mandateId(record.getMandateId())
                .lastUpdateDate(record.getLastUpdateDate())
                .creditorId(record.getCreditorId())
                .debtorId(debtorId)
                .mandateReference(record.getMandateReference())
                .mandateType(record.getMandateType())
                .frequency(record.getFrequency())
                .status(record.getStatus())
                .signatureDate(record.getSignatureDate())
                .effectiveDate(record.getEffectiveDate())
                .expiryDate(record.getExpiryDate())
                .maxAmountPerTransaction(record.getMaxAmountPerTransaction())
                .maxAmountPerMonth(record.getMaxAmountPerMonth())
                .maxTransactionsPerMonth(record.getMaxTransactionsPerMonth())
                .currency(record.getCurrency())
                .description(record.getDescription())
                .schemeType(record.getSchemeType())
                .build();
    }

    private void resetStatistics() {
        totalProcessed = 0;
        insertCount = 0;
        updateCount = 0;
        skipCount = 0;
        errorCount = 0;
        newCreditors = 0;
        newDebtors = 0;
    }

    private void logStatistics(long durationMs) {
        logger.info("========================================");
        logger.info("Processing Complete");
        logger.info("========================================");
        logger.info("Total Records Processed: {}", totalProcessed);
        logger.info("Inserted: {}", insertCount);
        logger.info("Updated: {}", updateCount);
        logger.info("Skipped (unchanged): {}", skipCount);
        logger.info("Errors: {}", errorCount);
        logger.info("New Creditors: {}", newCreditors);
        logger.info("New Debtors: {}", newDebtors);
        logger.info("Duration: {} ms", durationMs);
        logger.info("Throughput: {} records/sec",
                durationMs > 0 ? (totalProcessed * 1000 / durationMs) : 0);
        logger.info("========================================");
    }
}
