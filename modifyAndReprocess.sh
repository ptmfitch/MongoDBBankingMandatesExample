#!/bin/bash

# Configuration
EDIT_PERCENTAGE=${1:-25}  # Default 25% edit chance
INPUT_FILE=${2:-$(ls -t ./data/mandates_*.txt 2>/dev/null | grep -v modified | head -1)}
BATCH_SIZE=${3:-200}
DATA_DIR="./data"

export MONGODB_URI="${MONGODB_URI:-mongodb://localhost:27017}"
export MONGODB_DATABASE="${MONGODB_DATABASE:-mandate_db}"

# Check input file exists
if [[ -z "$INPUT_FILE" ]] || [[ ! -f "$INPUT_FILE" ]]; then
    echo "Error: No input file found"
    echo "Usage: $0 <edit-percentage> <input-file> [batch-size]"
    echo "Example: $0 25 ./data/mandates_2026-01-01_12-00-00_500.0K.txt 200"
    exit 1
fi

echo "╔════════════════════════════════════════════════════════════╗"
echo "║         Modify and Reprocess Mandate Data                  ║"
echo "╠════════════════════════════════════════════════════════════╣"
echo "║  Input file:      $(printf '%-39s' "$(basename "$INPUT_FILE")") ║"
echo "║  Edit percentage: $(printf '%-39s' "${EDIT_PERCENTAGE}%") ║"
echo "║  Batch size:      $(printf '%-39s' "$BATCH_SIZE") ║"
echo "║  MongoDB URI:     $(printf '%-39s' "$MONGODB_URI") ║"
echo "║  Database:        $(printf '%-39s' "$MONGODB_DATABASE") ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Step 1: Modify the file
echo "Step 1: Modifying records with ${EDIT_PERCENTAGE}% edit chance..."
echo ""
java -cp target/dd-mandate-processor-1.0.0-SNAPSHOT.jar \
    com.mongodb.mandate.generator.MandateDataModifier \
    "$INPUT_FILE" "$EDIT_PERCENTAGE" "$DATA_DIR"

# Find the modified file
MODIFIED_FILE=$(ls -t "$DATA_DIR"/*_modified_*.txt 2>/dev/null | head -1)

if [[ -z "$MODIFIED_FILE" ]]; then
    echo "Error: Modified file not found"
    exit 1
fi

echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""

# Step 2: Process the modified file
echo "Step 2: Processing modified file against MongoDB..."
echo ""
java -jar target/dd-mandate-processor-1.0.0-SNAPSHOT.jar "$MODIFIED_FILE" "$BATCH_SIZE"

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "✓ Complete!"
echo "  - Original file: $INPUT_FILE"
echo "  - Modified file: $MODIFIED_FILE"
