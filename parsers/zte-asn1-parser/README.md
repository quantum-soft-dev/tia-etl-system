# ZTE ASN.1 Parser Library

A Kotlin library for parsing ZTE ZXUN CG ASN.1 encoded CDR (Call Detail Record) files. This library implements the TIA ETL System's parser API and can be used both as a standalone Gradle dependency and as a parser plugin in the TIA ETL System.

## Features

- **ASN.1 CDR Parsing**: Processes ZTE ZXUN CG ASN.1 encoded CDR files using the JASN.1 library
- **Multiple Record Types**: Supports PGW, SGW, and other ZTE record types from pgw_r8_new specification
- **ClickHouse Integration**: Optimized batch loading for high-performance data insertion
- **Comprehensive Validation**: Data validation and error handling with detailed reporting
- **Spring Boot Integration**: Built with Spring Boot for dependency injection and configuration
- **Metrics & Monitoring**: Prometheus metrics and health checks
- **Testcontainers Support**: Integration tests with real databases

## Usage as Gradle Dependency

### 1. Add Repository

Add the GitHub Packages repository to your `build.gradle.kts`:

```kotlin
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/quantum-soft-dev/tia-etl-system")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}
```

### 2. Add Dependency

```kotlin
dependencies {
    implementation("com.quantumsoft.tia.parsers:zte-asn1-parser:1.0.0")
    
    // Also needed: parser API interfaces
    implementation("com.quantumsoft.tia.parsers:parser-api:1.0.0")
}
```

### 3. Authentication Setup

Create a Personal Access Token on GitHub:
1. Go to GitHub → Settings → Developer settings → Personal access tokens
2. Create token with `read:packages` scope
3. Configure in your project:

**Option A: gradle.properties**
```properties
gpr.user=your-github-username
gpr.key=your-personal-access-token
```

**Option B: Environment Variables**
```bash
export USERNAME=your-github-username
export TOKEN=your-personal-access-token
```

### 4. Using the Parser

```kotlin
import com.quantumsoft.tia.parsers.api.DataParser
import com.quantumsoft.tia.parsers.api.ProcessingContext
import com.quantumsoft.tia.parsers.zte.ZteAsn1ParserImpl

class YourService {
    
    fun processZteCdrFile(file: File, context: ProcessingContext) {
        val parser: DataParser = ZteAsn1ParserImpl()
        
        val result = parser.process(context.copy(file = file))
        
        when (result.status) {
            ProcessingStatus.SUCCESS -> {
                println("Processed ${result.recordsProcessed} records")
            }
            ProcessingStatus.FAILED -> {
                println("Processing failed: ${result.errorMessage}")
            }
        }
    }
}
```

## Parser Plugin Deployment

For deploying as a parser plugin in the TIA ETL System:

### 1. Download Deployment Package

Download the deployment package from the [latest release](https://github.com/quantum-soft-dev/tia-etl-system/releases).

### 2. Extract to Parser Directory

```bash
# Extract to parser plugins directory
sudo mkdir -p /opt/tia/parsers/zte-asn1-parser/current
sudo tar -xzf zte-parser-deployment-v1.0.0.tar.gz -C /opt/tia/parsers/zte-asn1-parser/current

# Set permissions
sudo chown -R tia:tia /opt/tia/parsers/zte-asn1-parser
sudo chmod +x /opt/tia/parsers/zte-asn1-parser/current/zte-asn1-parser-executable.jar
```

### 3. Register Parser

The parser will be automatically discovered by the TIA ETL System using the `metadata.json` file:

```json
{
  "parserId": "zte-asn1-parser",
  "name": "ZTE ASN.1 CDR Parser",
  "version": "1.0.0",
  "supportedFormats": ["asn1", "ber", "cdr"],
  "targetTable": "zte_cdr_records",
  "jarPath": "/opt/tia/parsers/zte-asn1-parser/current/zte-asn1-parser-executable.jar"
}
```

## Development

### Building from Source

```bash
# Clone repository
git clone https://github.com/quantum-soft-dev/tia-etl-system.git
cd tia-etl-system

# Build library
./gradlew :parsers:zte-asn1-parser:build

# Run tests
./gradlew :parsers:zte-asn1-parser:test

# Run integration tests
./gradlew :parsers:zte-asn1-parser:integrationTest

# Create deployment package
./gradlew :parsers:zte-asn1-parser:deploymentPackage
```

### Testing

```bash
# Unit tests
./gradlew :parsers:zte-asn1-parser:test

# Integration tests (requires Docker)
./gradlew :parsers:zte-asn1-parser:integrationTest

# Coverage report
./gradlew :parsers:zte-asn1-parser:jacocoTestReport

# Quality checks (tests + coverage + security scan)
./gradlew :parsers:zte-asn1-parser:qualityCheck
```

## Configuration

### Application Properties

```yaml
# ClickHouse Configuration
clickhouse:
  url: jdbc:clickhouse://localhost:8123/tia
  username: default
  password: ""
  batch-size: 1000
  connection-timeout: 30000

# Parser Configuration
parser:
  zte-asn1:
    max-file-size: 1073741824  # 1GB
    batch-size: 1000
    validation-enabled: true
    target-table: zte_cdr_records

# Monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### Database Schema

The parser creates records in the `zte_cdr_records` table:

```sql
CREATE TABLE zte_cdr_records (
    id UUID DEFAULT generateUUIDv4(),
    record_type String,
    imsi String,
    msisdn String,
    start_time DateTime,
    end_time DateTime,
    duration UInt32,
    bytes_uplink UInt64,
    bytes_downlink UInt64,
    serving_node_address String,
    charging_id UInt32,
    processed_at DateTime DEFAULT now()
) ENGINE = MergeTree()
ORDER BY (record_type, start_time)
PARTITION BY toYYYYMM(start_time);
```

## Supported Record Types

The parser supports the following ZTE ASN.1 record types:

- **PGWRecord**: Packet Gateway records with data session information
- **SGWRecord**: Serving Gateway records with mobility data
- **MMERecord**: Mobility Management Entity records
- **HSS Record**: Home Subscriber Server authentication records

Each record type is mapped to specific fields in the target ClickHouse table with proper data type conversion and validation.

## Error Handling

The parser implements comprehensive error handling:

- **File Validation**: Size limits, format validation
- **ASN.1 Parsing Errors**: Invalid format, corrupted data
- **Database Errors**: Connection issues, constraint violations
- **Business Logic Errors**: Invalid field values, missing required data

All errors are logged with detailed context and metrics are updated accordingly.

## Performance Characteristics

- **Throughput**: Processes up to 100,000 records/minute on standard hardware
- **Memory Usage**: Maintains low memory footprint with streaming processing
- **Batch Processing**: Configurable batch sizes for optimal ClickHouse insertion
- **Concurrent Processing**: Thread-safe for parallel file processing

## Monitoring

The parser exposes metrics for monitoring:

```
# Processing metrics
parser_zte_asn1_files_processed_total
parser_zte_asn1_records_processed_total
parser_zte_asn1_processing_duration_seconds
parser_zte_asn1_errors_total

# Database metrics
clickhouse_batch_insert_duration_seconds
clickhouse_connection_pool_active
clickhouse_connection_pool_idle
```

## License

MIT License - see [LICENSE](../../LICENSE) for details.

## Support

For issues and questions:
- Create an issue in the [GitHub repository](https://github.com/quantum-soft-dev/tia-etl-system/issues)
- Tag with `zte-asn1-parser` label
- Include logs and error messages for faster resolution

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Run quality checks: `./gradlew :parsers:zte-asn1-parser:qualityCheck`
5. Submit a pull request

## Version History

- **v1.0.0**: Initial release with basic ASN.1 parsing support
- Support for PGW and SGW record types
- ClickHouse integration with batch loading
- Comprehensive test coverage and documentation