#!/bin/bash

MONGODB_URI="${MONGODB_URI:-mongodb://localhost:27017}"
MONGODB_DATABASE="${MONGODB_DATABASE:-mandate_db}"

echo "Clearing collections in $MONGODB_DATABASE..."
echo ""

mongosh "$MONGODB_URI/$MONGODB_DATABASE" --eval '
    print("Before deletion:");
    print("  mandates:       " + db.mandates.countDocuments({}) + " documents");
    print("  mandate_audits: " + db.mandate_audits.countDocuments({}) + " documents");
    print("  creditors:      " + db.creditors.countDocuments({}) + " documents");
    print("  debtors:        " + db.debtors.countDocuments({}) + " documents");
    print("");

    db.mandates.deleteMany({});
    db.mandate_audits.deleteMany({});
    db.creditors.deleteMany({});
    db.debtors.deleteMany({});

    print("✓ All collections cleared (indexes preserved)");
'
