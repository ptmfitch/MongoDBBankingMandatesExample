#!/bin/bash

MONGODB_URI="${MONGODB_URI:-mongodb://localhost:27017}"
MONGODB_DATABASE="${MONGODB_DATABASE:-mandate_db}"

echo "Clearing collections in $MONGODB_DATABASE..."
echo ""

mongosh "$MONGODB_URI/$MONGODB_DATABASE" --eval '
    const mandateCount = db.mandates.countDocuments({});
    const auditCount = db.mandate_audits.countDocuments({});

    print("Before deletion:");
    print("  mandates:       " + mandateCount + " documents");
    print("  mandate_audits: " + auditCount + " documents");
    print("");

    const mandateResult = db.mandates.deleteMany({});
    const auditResult = db.mandate_audits.deleteMany({});

    print("Deleted:");
    print("  mandates:       " + mandateResult.deletedCount + " documents");
    print("  mandate_audits: " + auditResult.deletedCount + " documents");
    print("");
    print("✓ Collections cleared (indexes preserved)");
'
