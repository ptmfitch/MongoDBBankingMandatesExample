# MongoDB Banking Mandates Example

Example MongoDB application demonstrating Direct Debit Mandate processing with change detection and audit trails.

## Features

- Batch processing of pipe-delimited mandate files
- Smart change detection using covering indexes
- Field-level audit trail for all modifications
- Realistic test data generation

## Prerequisites

- Java 21+
- Maven 3.9+
- MongoDB 6.0+

## Quick Start

````bash  
# Build the project  
mvn clean package  
  
# Generate 500k test records and process  
./buildAndRun.sh 500k  
  
# Modify 25% of records and reprocess  
./modifyAndReprocess.sh 25  
  
# Clear all data (keeps indexes)  
./clearCollections.sh  
````  
  
## Configuration  
  
Set environment variables before running:  
  
````bash  
export MONGODB_URI="mongodb://localhost:27017"  
export MONGODB_DATABASE="mandate_db"  
````  
  
## Scripts  
  
| Script | Description |  
|--------|-------------|  
| `buildAndRun.sh <count>` | Build, generate data, and process |  
| `modifyAndReprocess.sh <edit%> [file]` | Modify records and reprocess |  
| `clearCollections.sh` | Delete all documents, keep indexes |  
  
## Processing Logic  
  
1. **Missing mandateId** → Insert new document  
2. **Same lastUpdateDate** → Skip (no changes)  
3. **Different lastUpdateDate** → Diff fields, update, and create audit record  
  
## Collections  
  
- `mandates` - Direct debit mandate documents  
- `mandate_audits` - Change history with field-level diffs  
  
## License  
  
MIT
