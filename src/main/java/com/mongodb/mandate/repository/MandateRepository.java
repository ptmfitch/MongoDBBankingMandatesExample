package com.mongodb.mandate.repository;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.mandate.model.*;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
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
    private final MongoCollection<Creditor> creditorCollection;
    private final MongoCollection<Debtor> debtorCollection;

    public MandateRepository(String connectionString, String databaseName) {
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
        this.creditorCollection = database.getCollection("creditors", Creditor.class);
        this.debtorCollection = database.getCollection("debtors", Debtor.class);

        ensureIndexes();
    }

    private void ensureIndexes() {
        logger.info("Ensuring indexes exist...");

        mandateCollection.createIndex(
                Indexes.compoundIndex(
                        Indexes.ascending("mandateId"),
                        Indexes.ascending("lastUpdateDate")
                ),
                new IndexOptions().name("idx_mandate_lookup").unique(true)
        );

        auditCollection.createIndex(
                Indexes.ascending("mandateId"),
                new IndexOptions().name("idx_audit_mandateId")
        );
        auditCollection.createIndex(
                Indexes.descending("changeTimestamp"),
                new IndexOptions().name("idx_audit_timestamp")
        );

        creditorCollection.createIndex(
                Indexes.ascending("creditorId"),
                new IndexOptions().name("idx_creditor_id").unique(true)
        );

        debtorCollection.createIndex(
                Indexes.ascending("debtorId"),
                new IndexOptions().name("idx_debtor_id").unique(true)
        );

        logger.info("Indexes created successfully");
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    // Batch lookup for mandate dates
    public Map<String, LocalDateTime> batchGetMandateUpdateDates(List<String> mandateIds) {
        Map<String, LocalDateTime> result = new HashMap<>();
        if (mandateIds.isEmpty()) return result;

        mandateCollection.find(Filters.in("mandateId", mandateIds))
                .projection(Projections.fields(
                        Projections.include("mandateId", "lastUpdateDate"),
                        Projections.excludeId()
                ))
                .forEach(mandate -> result.put(mandate.getMandateId(), mandate.getLastUpdateDate()));

        return result;
    }

    // Batch get full mandates
    public Map<String, DirectDebitMandate> batchGetMandates(List<String> mandateIds) {
        Map<String, DirectDebitMandate> result = new HashMap<>();
        if (mandateIds.isEmpty()) return result;

        mandateCollection.find(Filters.in("mandateId", mandateIds))
                .forEach(mandate -> result.put(mandate.getMandateId(), mandate));

        return result;
    }

    // Check existing creditors
    public Set<String> getExistingCreditorIds(Set<String> creditorIds) {
        Set<String> existing = new HashSet<>();
        if (creditorIds.isEmpty()) return existing;

        creditorCollection.find(Filters.in("creditorId", creditorIds))
                .projection(Projections.include("creditorId"))
                .forEach(c -> existing.add(c.getCreditorId()));

        return existing;
    }

    // Check existing debtors
    public Set<String> getExistingDebtorIds(Set<String> debtorIds) {
        Set<String> existing = new HashSet<>();
        if (debtorIds.isEmpty()) return existing;

        debtorCollection.find(Filters.in("debtorId", debtorIds))
                .projection(Projections.include("debtorId"))
                .forEach(d -> existing.add(d.getDebtorId()));

        return existing;
    }

    // Batch insert creditors with session
    public void batchInsertCreditors(ClientSession session, List<Creditor> creditors) {
        if (creditors.isEmpty()) return;
        creditorCollection.insertMany(session, creditors, new InsertManyOptions().ordered(false));
    }

    // Batch insert debtors with session
    public void batchInsertDebtors(ClientSession session, List<Debtor> debtors) {
        if (debtors.isEmpty()) return;
        debtorCollection.insertMany(session, debtors, new InsertManyOptions().ordered(false));
    }

    // Batch insert mandates with session
    public void batchInsertMandates(ClientSession session, List<DirectDebitMandate> mandates) {
        if (mandates.isEmpty()) return;
        mandateCollection.insertMany(session, mandates, new InsertManyOptions().ordered(false));
    }

    // Batch insert audits with session
    public void batchInsertAudits(ClientSession session, List<MandateAudit> audits) {
        if (audits.isEmpty()) return;
        auditCollection.insertMany(session, audits, new InsertManyOptions().ordered(false));
    }

    // Batch insert audits without session (for updates)
    public void batchInsertAudits(List<MandateAudit> audits) {
        if (audits.isEmpty()) return;
        auditCollection.insertMany(audits, new InsertManyOptions().ordered(false));
    }

    // Batch update mandates using bulk write
    public void batchUpdateMandates(List<DirectDebitMandate> mandates) {
        if (mandates.isEmpty()) return;

        List<WriteModel<DirectDebitMandate>> updates = new ArrayList<>();

        for (DirectDebitMandate mandate : mandates) {
            updates.add(new ReplaceOneModel<>(
                    Filters.eq("mandateId", mandate.getMandateId()),
                    mandate
            ));
        }

        mandateCollection.bulkWrite(updates, new BulkWriteOptions().ordered(false));
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB connection closed");
        }
    }
}
