package com.mongodb.mandate.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.mandate.model.DirectDebitMandate;
import com.mongodb.mandate.model.MandateAudit;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.LocalDateTime;
import java.util.*;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;

public class MandateRepository implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(MandateRepository.class);

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<DirectDebitMandate> mandateCollection;
    private final MongoCollection<MandateAudit> auditCollection;

    public MandateRepository(String connectionString, String databaseName) {
        // Configure POJO codec
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(
                        PojoCodecProvider.builder().automatic(true).build()
                )
        );

        this.mongoClient = MongoClients.create(connectionString);
        this.database = mongoClient.getDatabase(databaseName).withCodecRegistry(pojoCodecRegistry);
        this.mandateCollection = database.getCollection("mandates", DirectDebitMandate.class);
        this.auditCollection = database.getCollection("mandate_audits", MandateAudit.class);

        ensureIndexes();
    }

    private void ensureIndexes() {
        logger.info("Ensuring indexes exist...");

        // Create compound index on mandateId and lastUpdateDate for efficient lookups
        // This is a covering index for our batch queries
        mandateCollection.createIndex(
                Indexes.compoundIndex(
                        Indexes.ascending("mandateId"),
                        Indexes.ascending("lastUpdateDate")
                ),
                new IndexOptions().name("idx_mandate_lookup").unique(true)
        );

        // Index on audit collection for querying by mandateId
        auditCollection.createIndex(
                Indexes.ascending("mandateId"),
                new IndexOptions().name("idx_audit_mandateId")
        );

        // Index on audit collection for querying by timestamp
        auditCollection.createIndex(
                Indexes.descending("changeTimestamp"),
                new IndexOptions().name("idx_audit_timestamp")
        );

        logger.info("Indexes created successfully");
    }

    /**
     * Batch query to get mandateId and lastUpdateDate for a list of mandate IDs.
     * Uses projection to only return the fields we need (covered by index).
     */
    public Map<String, LocalDateTime> batchGetMandateUpdateDates(List<String> mandateIds) {
        Map<String, LocalDateTime> result = new HashMap<>();

        if (mandateIds.isEmpty()) {
            return result;
        }

        Bson filter = Filters.in("mandateId", mandateIds);

        // Project only the fields we need - this query is covered by our index
        Bson projection = Projections.fields(
                Projections.include("mandateId", "lastUpdateDate"),
                Projections.excludeId()
        );

        // Use hint to ensure we use our covering index
        mandateCollection.find(filter)
                .projection(projection)
                .hint(new Document("mandateId", 1).append("lastUpdateDate", 1))
                .forEach(mandate -> {
                    result.put(mandate.getMandateId(), mandate.getLastUpdateDate());
                });

        return result;
    }

    /**
     * Get full mandate document by mandateId for comparison
     */
    public Optional<DirectDebitMandate> getMandateById(String mandateId) {
        DirectDebitMandate mandate = mandateCollection.find(
                Filters.eq("mandateId", mandateId)
        ).first();
        return Optional.ofNullable(mandate);
    }

    /**
     * Batch get full mandate documents for comparison
     */
    public Map<String, DirectDebitMandate> batchGetMandates(List<String> mandateIds) {
        Map<String, DirectDebitMandate> result = new HashMap<>();

        if (mandateIds.isEmpty()) {
            return result;
        }

        mandateCollection.find(Filters.in("mandateId", mandateIds))
                .forEach(mandate -> result.put(mandate.getMandateId(), mandate));

        return result;
    }

    /**
     * Insert a new mandate
     */
    public void insertMandate(DirectDebitMandate mandate) {
        mandate.setCreatedAt(LocalDateTime.now());
        mandate.setVersion(1);
        mandateCollection.insertOne(mandate);
        logger.debug("Inserted mandate: {}", mandate.getMandateId());
    }

    /**
     * Batch insert mandates
     */
    public void batchInsertMandates(List<DirectDebitMandate> mandates) {
        if (mandates.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        mandates.forEach(m -> {
            m.setCreatedAt(now);
            m.setVersion(1);
        });

        mandateCollection.insertMany(mandates);
        logger.info("Batch inserted {} mandates", mandates.size());
    }

    /**
     * Update an existing mandate
     */
    public void updateMandate(DirectDebitMandate mandate) {
        mandate.setVersion(mandate.getVersion() != null ? mandate.getVersion() + 1 : 1);

        mandateCollection.replaceOne(
                Filters.eq("mandateId", mandate.getMandateId()),
                mandate
        );
        logger.debug("Updated mandate: {}", mandate.getMandateId());
    }

    /**
     * Batch update mandates using bulk write
     */
    public void batchUpdateMandates(List<DirectDebitMandate> mandates) {
        if (mandates.isEmpty()) {
            return;
        }

        List<WriteModel<DirectDebitMandate>> updates = new ArrayList<>();

        for (DirectDebitMandate mandate : mandates) {
            mandate.setVersion(mandate.getVersion() != null ? mandate.getVersion() + 1 : 1);

            updates.add(new ReplaceOneModel<>(
                    Filters.eq("mandateId", mandate.getMandateId()),
                    mandate
            ));
        }

        mandateCollection.bulkWrite(updates, new BulkWriteOptions().ordered(false));
        logger.info("Batch updated {} mandates", mandates.size());
    }

    /**
     * Insert audit record
     */
    public void insertAudit(MandateAudit audit) {
        auditCollection.insertOne(audit);
        logger.debug("Inserted audit for mandate: {}", audit.getMandateId());
    }

    /**
     * Batch insert audit records
     */
    public void batchInsertAudits(List<MandateAudit> audits) {
        if (audits.isEmpty()) {
            return;
        }

        auditCollection.insertMany(audits);
        logger.info("Batch inserted {} audit records", audits.size());
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB connection closed");
        }
    }
}
