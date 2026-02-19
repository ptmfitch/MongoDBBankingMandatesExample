// Connect to the database
use mandate_db;

// Create covering index for efficient batch lookups
db.mandates.createIndex(
    { "mandateId": 1, "lastUpdateDate": 1 },
    {
        name: "idx_mandate_lookup",
        unique: true
    }
);

// Create index on audit collection for mandate lookups
db.mandate_audits.createIndex(
    { "mandateId": 1 },
    { name: "idx_audit_mandateId" }
);

// Create index on audit collection for time-based queries
db.mandate_audits.createIndex(
    { "changeTimestamp": -1 },
    { name: "idx_audit_timestamp" }
);

// Compound index for querying audits by mandate and time
db.mandate_audits.createIndex(
    { "mandateId": 1, "changeTimestamp": -1 },
    { name: "idx_audit_mandate_time" }
);

print("Indexes created successfully");
