package com.mongodb.mandate.service;

import com.mongodb.mandate.model.DirectDebitMandate;
import com.mongodb.mandate.model.FieldChange;
import com.mongodb.mandate.model.MandateAudit;
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

    // Processing statistics
    private long totalProcessed = 0;
    private long insertCount = 0;
    private long updateCount = 0;
    private long skipCount = 0;
    private long errorCount = 0;

    public MandateProcessor(MandateRepository repository, int batchSize) {
        this.repository = repository;
        this.diffService = new MandateDiffService();
        this.batchSize = batchSize;
    }

    /**
     * Process a mandate file
     */
    public void processFile(Path filePath) throws IOException {
        logger.info("Starting to process file: {}", filePath);
        long startTime = System.currentTimeMillis();
        String batchId = UUID.randomUUID().toString();

        resetStatistics();

        try (MandateFileReader reader = new MandateFileReader(filePath)) {
            List<DirectDebitMandate> batch;

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

    /**
     * Process a batch of mandates
     */
    private void processBatch(List<DirectDebitMandate> mandates, String sourceFile, String batchId) {
        // Extract mandate IDs for batch lookup
        List<String> mandateIds = mandates.stream()
                .map(DirectDebitMandate::getMandateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Batch query for existing mandate dates (uses covering index)
        Map<String, LocalDateTime> existingDates = repository.batchGetMandateUpdateDates(mandateIds);

        // Categorize mandates
        List<DirectDebitMandate> toInsert = new ArrayList<>();
        List<String> toCheckForUpdate = new ArrayList<>();
        List<DirectDebitMandate> unchangedMandates = new ArrayList<>();

        for (DirectDebitMandate mandate : mandates) {
            String mandateId = mandate.getMandateId();

            if (!existingDates.containsKey(mandateId)) {
                // New mandate - needs insert
                toInsert.add(mandate);
            } else {
                LocalDateTime existingDate = existingDates.get(mandateId);
                LocalDateTime newDate = mandate.getLastUpdateDate();

                if (existingDate != null && newDate != null && existingDate.equals(newDate)) {
                    // Same update date - skip
                    unchangedMandates.add(mandate);
                } else {
                    // Different update date - needs comparison
                    toCheckForUpdate.add(mandateId);
                }
            }
        }

        // Process inserts
        if (!toInsert.isEmpty()) {
            processInserts(toInsert, sourceFile, batchId);
        }

        // Process potential updates (need to fetch full documents for comparison)
        if (!toCheckForUpdate.isEmpty()) {
            processUpdates(mandates, toCheckForUpdate, sourceFile, batchId);
        }

        // Count skipped
        skipCount += unchangedMandates.size();
    }

    /**
     * Process new mandates for insertion
     */
    private void processInserts(List<DirectDebitMandate> mandates, String sourceFile, String batchId) {
        try {
            // Batch insert mandates
            repository.batchInsertMandates(mandates);

            // Create audit records for inserts
            LocalDateTime now = LocalDateTime.now();
            List<MandateAudit> audits = mandates.stream()
                    .map(mandate -> MandateAudit.builder()
                            .mandateId(mandate.getMandateId())
                            .changeType("INSERT")
                            .changeTimestamp(now)
                            .sourceFile(sourceFile)
                            .newUpdateDate(mandate.getLastUpdateDate())
                            .fieldChanges(Collections.emptyList())
                            .processedBy(System.getProperty("user.name", "system"))
                            .batchId(batchId)
                            .build())
                    .collect(Collectors.toList());

            repository.batchInsertAudits(audits);
            insertCount += mandates.size();

            logger.debug("Inserted {} new mandates", mandates.size());
        } catch (Exception e) {
            logger.error("Error inserting mandates: {}", e.getMessage());
            errorCount += mandates.size();
        }
    }

    /**
     * Process mandates that need update comparison
     */
    private void processUpdates(List<DirectDebitMandate> allMandates,
                                List<String> mandateIdsToUpdate,
                                String sourceFile, String batchId) {

        // Create a map of new mandates by ID for quick lookup
        Map<String, DirectDebitMandate> newMandatesMap = allMandates.stream()
                .filter(m -> mandateIdsToUpdate.contains(m.getMandateId()))
                .collect(Collectors.toMap(DirectDebitMandate::getMandateId, m -> m));

        // Fetch existing mandates for comparison
        Map<String, DirectDebitMandate> existingMandates = repository.batchGetMandates(mandateIdsToUpdate);

        List<DirectDebitMandate> toUpdate = new ArrayList<>();
        List<MandateAudit> audits = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (String mandateId : mandateIdsToUpdate) {
            DirectDebitMandate existing = existingMandates.get(mandateId);
            DirectDebitMandate updated = newMandatesMap.get(mandateId);

            if (existing == null || updated == null) {
                logger.warn("Missing mandate for comparison: {}", mandateId);
                errorCount++;
                continue;
            }

            // Diff the two records
            List<FieldChange> changes = diffService.diff(existing, updated);

            if (!changes.isEmpty()) {
                // Apply changes while preserving system fields
                DirectDebitMandate mergedMandate = diffService.applyChanges(existing, updated);
                toUpdate.add(mergedMandate);

                // Create audit record
                MandateAudit audit = MandateAudit.builder()
                        .mandateId(mandateId)
                        .changeType("UPDATE")
                        .changeTimestamp(now)
                        .sourceFile(sourceFile)
                        .previousUpdateDate(existing.getLastUpdateDate())
                        .newUpdateDate(updated.getLastUpdateDate())
                        .fieldChanges(changes)
                        .processedBy(System.getProperty("user.name", "system"))
                        .batchId(batchId)
                        .build();

                audits.add(audit);

                logger.debug("Mandate {} has {} field changes", mandateId, changes.size());
            } else {
                // No actual changes despite different update dates
                skipCount++;
            }
        }

        // Batch update mandates
        if (!toUpdate.isEmpty()) {
            try {
                repository.batchUpdateMandates(toUpdate);
                repository.batchInsertAudits(audits);
                updateCount += toUpdate.size();

                logger.debug("Updated {} mandates", toUpdate.size());
            } catch (Exception e) {
                logger.error("Error updating mandates: {}", e.getMessage());
                errorCount += toUpdate.size();
            }
        }
    }

    private void resetStatistics() {
        totalProcessed = 0;
        insertCount = 0;
        updateCount = 0;
        skipCount = 0;
        errorCount = 0;
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
        logger.info("Duration: {} ms", durationMs);
        logger.info("Throughput: {} records/sec",
                durationMs > 0 ? (totalProcessed * 1000 / durationMs) : 0);
        logger.info("========================================");
    }

    // Getters for statistics
    public long getTotalProcessed() { return totalProcessed; }
    public long getInsertCount() { return insertCount; }
    public long getUpdateCount() { return updateCount; }
    public long getSkipCount() { return skipCount; }
    public long getErrorCount() { return errorCount; }
}
