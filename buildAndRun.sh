#!/bin/bash

# Build the project
mvn clean package

# Configuration
BATCH_SIZE=200
RECORD_COUNT=${1:-10k}  # Default 10k records, or pass as first argument
DATA_DIR="./data"

export MONGODB_URI="${MONGODB_URI:-mongodb://localhost:27017}"
export MONGODB_DATABASE="${MONGODB_DATABASE:-mandate_db}"

# Create data directory
mkdir -p "$DATA_DIR"

# Generate test data
echo "Generating $RECORD_COUNT test records..."
java -cp target/dd-mandate-processor-1.0.0-SNAPSHOT.jar com.mongodb.mandate.generator.MandateDataGenerator "$RECORD_COUNT" "$DATA_DIR"

# Find the latest generated file
LATEST_FILE=$(ls -t "$DATA_DIR"/mandates_*.txt 2>/dev/null | head -1)

if [[ -z "$LATEST_FILE" ]]; then
    echo "Error: No generated file found"
    exit 1
fi

echo ""
echo "Processing file: $LATEST_FILE"
echo ""

# Process the generated file
java -jar target/dd-mandate-processor-1.0.0-SNAPSHOT.jar "$LATEST_FILE" "$BATCH_SIZE"
