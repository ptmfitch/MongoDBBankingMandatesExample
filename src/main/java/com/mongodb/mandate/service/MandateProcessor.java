package com.mongodb.mandate.service;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertManyOptions;
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

                if (totalProcessed % 10000 == 0) {
                    logger.info("Processed {} records...", totalProcessed);
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        logStatistics(duration);
    }

    private void processBatch(List<MandateFileRecord> records, String sourceFile, String batchId) {
        List<String> mandateIds = records.stream()
                .map(MandateFileRecord::getMandateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, LocalDateTime> existingDates = repository.batchGetMandateUpdateDates(mandateIds);

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

        if (!toInsert.isEmpty()) {
            processBatchInserts(toInsert, sourceFile, batchId);
        }

        if (!toCheckForUpdate.isEmpty()) {
            processUpdates(records, toCheckForUpdate, sourceFile, batchId);
        }
    }

    private void processBatchInserts(List<MandateFileRecord> records, String sourceFile, String batchId) {
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

        // Build batch lists
        List<Creditor> creditorsToInsert = new ArrayList<>();
        List<Debtor> debtorsToInsert = new ArrayList<>();
        List<DirectDebitMandate> mandatesToInsert = new ArrayList<>();
        List<MandateAudit> audits = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        Set<String> creditorsInBatch = new HashSet<>();
        Set<String> debtorsInBatch = new HashSet<>();

        for (MandateFileRecord record : records) {
            String debtorId = record.generateDebtorId();

            // Build creditor if new (and not already in this batch)
            if (!existingCreditors.contains(record.getCreditorId())
                    && !creditorsInBatch.contains(record.getCreditorId())) {
                Creditor creditor = buildCreditor(record);
                creditor.setCreatedAt(now);
                creditor.setUpdatedAt(now);
                creditorsToInsert.add(creditor);
                creditorsInBatch.add(record.getCreditorId());
            }

            // Build debtor if new (and not already in this batch)
            if (!existingDebtors.contains(debtorId)
                    && !debtorsInBatch.contains(debtorId)) {
                Debtor debtor = buildDebtor(record, debtorId);
                debtor.setCreatedAt(now);
                debtor.setUpdatedAt(now);
                debtorsToInsert.add(debtor);
                debtorsInBatch.add(debtorId);
            }

            // Build mandate
            DirectDebitMandate mandate = buildMandate(record, debtorId);
            mandate.setCreatedAt(now);
            mandate.setVersion(1);
            mandatesToInsert.add(mandate);

            // Build audit
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
        }

        // Execute batch insert in a single transaction
        try (ClientSession session = repository.getMongoClient().startSession()) {
            session.startTransaction();

            try {
                if (!creditorsToInsert.isEmpty()) {
                    repository.batchInsertCreditors(session, creditorsToInsert);
                    newCreditors += creditorsToInsert.size();
                }

                if (!debtorsToInsert.isEmpty()) {
                    repository.batchInsertDebtors(session, debtorsToInsert);
                    newDebtors += debtorsToInsert.size();
                }

                repository.batchInsertMandates(session, mandatesToInsert);
                repository.batchInsertAudits(session, audits);

                session.commitTransaction();
                insertCount += mandatesToInsert.size();

                logger.debug("Batch inserted {} mandates, {} creditors, {} debtors",
                        mandatesToInsert.size(), creditorsToInsert.size(), debtorsToInsert.size());

            } catch (Exception e) {
                session.abortTransaction();
                logger.error("Batch transaction failed: {}", e.getMessage());
                errorCount += records.size();
            }
        }
    }

    private void processUpdates(List<MandateFileRecord> allRecords,
                                List<String> mandateIdsToUpdate,
                                String sourceFile, String batchId) {

        Map<String, MandateFileRecord> recordsMap = allRecords.stream()
                .filter(r -> mandateIdsToUpdate.contains(r.getMandateId()))
                .collect(Collectors.toMap(MandateFileRecord::getMandateId, r -> r));

        Map<String, DirectDebitMandate> existingMandates = repository.batchGetMandates(mandateIdsToUpdate);

        List<DirectDebitMandate> mandatesToUpdate = new ArrayList<>();
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

            List<FieldChange> changes = diffService.diff(existing, updated);

            if (!changes.isEmpty()) {
                updated.setId(existing.getId());
                updated.setCreatedAt(existing.getCreatedAt());
                updated.setVersion(existing.getVersion() != null ? existing.getVersion() + 1 : 1);
                mandatesToUpdate.add(updated);

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
            } else {
                skipCount++;
            }
        }

        if (!mandatesToUpdate.isEmpty()) {
            try {
                repository.batchUpdateMandates(mandatesToUpdate);
                repository.batchInsertAudits(audits);
                updateCount += mandatesToUpdate.size();
            } catch (Exception e) {
                logger.error("Batch update failed: {}", e.getMessage());
                errorCount += mandatesToUpdate.size();
            }
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
