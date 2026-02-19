#!/bin/bash

# Default values
COUNT=${1:-10k}
OUTPUT_DIR=${2:-.}

echo "Generating $COUNT mandate records..."
java -jar target/mandate-generator.jar "$COUNT" "$OUTPUT_DIR"
