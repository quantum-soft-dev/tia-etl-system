# ZTE ASN.1 CDR Parser

Parser library for processing ZTE ZXUN CG ASN.1 encoded CDR files.

## Overview

This library implements the `DataParser` interface from `parser-api` to process ZTE ASN.1 CDR files. It includes:

- Real ASN.1 binary parser from `pgw_r8_new` generated classes
- Support for PGW and SGW record types
- Clean Code and TDD implementation
- Non-nullable ClickHouse schema
- Batch processing capabilities

## Building

```bash
# Build the fat JAR
./gradlew :parsers:zte-asn1-parser:bootJar

# Run tests
./gradlew :parsers:zte-asn1-parser:test
```

## Deployment

The parser is packaged as a fat JAR that can be deployed to `/opt/tia/parsers/` for dynamic loading by the parser-orchestrator service.

```bash
# Copy JAR to deployment location
cp build/libs/zte-asn1-parser.jar /opt/tia/parsers/
```

## Usage

The parser is loaded dynamically by the parser-orchestrator service. Configuration is provided through the `ProcessingContext` when processing files.

## ClickHouse Schema

The parser writes to the `zte_cdr_records` table with the following non-nullable fields:

- `record_type` (UInt8)
- `record_sequence_number` (String)
- `processing_id` (UUID)
- `served_imsi` (String)
- `served_imei` (String)
- `served_msisdn` (String)
- `charging_id` (UInt32)
- `record_opening_time` (DateTime('UTC'))
- `duration` (UInt32)
- `uplink_volume` (UInt64)
- `downlink_volume` (UInt64)
- `total_volume` (UInt64)

## Dependencies

- jasn1 1.10.0 - ASN.1 BER encoding/decoding
- Spring Boot 3.3.5 - Framework support
- ClickHouse JDBC 0.7.1 - Database connectivity
- Kotlin 2.0.21 - Primary implementation language

## Version

1.0.0-SNAPSHOT