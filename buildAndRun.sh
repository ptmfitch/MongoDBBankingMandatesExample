#!/bin/bash

mvn clean package -q

RECORD_COUNT=${1:-10k}
BATCH_SIZE=${2:-1000}
DATA_DIR="./data"

export MONGODB_URI="${MONGODB_URI:-mongodb://localhost:27017}"
export MONGODB_DATABASE="${MONGODB_DATABASE:-mandate_db}"

mkdir -p "$DATA_DIR"

echo "Generating $RECORD_COUNT test records..."
java -cp target/dd-mandate-processor-1.0.0-SNAPSHOT.jar \
    com.mongodb.mandate.generator.MandateDataGenerator "$RECORD_COUNT" "$DATA_DIR"

LATEST_FILE=$(ls -t "$DATA_DIR"/mandates_*.txt 2>/dev/null | grep -v modified | head -1)

if [[ -z "$LATEST_FILE" ]]; then
    echo "Error: No generated file found"
    exit 1
fi

echo ""
echo "Processing file: $LATEST_FILE (batch size: $BATCH_SIZE)"
echo ""

java -jar target/dd-mandate-processor-1.0.0-SNAPSHOT.jar "$LATEST_FILE" "$BATCH_SIZE"
