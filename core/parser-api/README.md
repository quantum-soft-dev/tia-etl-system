# TIA ETL Parser API

[![CI](https://github.com/quantum-soft/tia-etl-system/actions/workflows/parser-api-publish.yml/badge.svg)](https://github.com/quantum-soft/tia-etl-system/actions/workflows/parser-api-publish.yml)
[![Maven Package](https://img.shields.io/github/v/release/quantum-soft/tia-etl-system?filter=parser-api-v*&label=parser-api)](https://github.com/quantum-soft/tia-etl-system/packages)

Core API module defining interfaces and contracts for data parsers in the TIA ETL System.

## Overview

This module provides the foundation for the plugin architecture that allows the TIA ETL system to support different data formats and sources (ASN.1, CSV from Orange and MTN operators). It defines the contracts that all parser implementations must follow.

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/quantum-soft-dev/tia-etl-system")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    implementation("com.quantum.etl:parser-api:1.0.0")
}
```

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = "https://maven.pkg.github.com/quantum-soft-dev/tia-etl-system"
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    implementation 'com.quantum.etl:parser-api:1.0.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/quantum-soft-dev/tia-etl-system</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.quantum.etl</groupId>
        <artifactId>parser-api</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Authentication Setup

To access GitHub Packages, you need to authenticate:

1. Create a Personal Access Token (PAT) with `read:packages` permission
2. Add credentials to your project:

### Option 1: gradle.properties (local)
```properties
gpr.user=your-github-username
gpr.key=your-personal-access-token
```

### Option 2: Environment Variables
```bash
export USERNAME=your-github-username
export TOKEN=your-personal-access-token
```

### Option 3: GitHub Actions
```yaml
env:
  USERNAME: ${{ github.actor }}
  TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## Key Interfaces

- **`DataParser`**: Main interface for processing files
- **`ParserLifecycle`**: Optional interface for advanced lifecycle management

## Data Models

- **`ParserMetadata`**: Parser identification and capabilities
- **`ProcessingContext`**: Resources provided to parsers during execution
- **`ProcessingResult`**: Detailed results and statistics from processing
- **`JobConfiguration`**: Configuration for parsing jobs
- **`TableSchema`**: Target table structure definition
- **`ValidationResult`**: File validation outcomes

## Exception Types

- **`ParserException`**: Base exception for parser-related errors
- **`ValidationException`**: Specific exception for validation failures

## Usage Example

### Basic Parser Implementation

```kotlin
import com.tia.etl.parser.api.DataParser
import com.tia.etl.parser.api.ParserLifecycle
import com.tia.etl.parser.api.models.*

class ExampleCsvParser : DataParser, ParserLifecycle {
    
    override fun getMetadata(): ParserMetadata {
        return ParserMetadata(
            parserId = "csv-parser",
            name = "CSV Parser",
            version = "1.0.0",
            description = "Parses CSV files for telecommunications data",
            supportedFormats = listOf("csv"),
            targetTable = "csv_data",
            schemaDefinition = TableSchema(
                tableName = "csv_data",
                columns = listOf(
                    ColumnDefinition("id", "UInt32"),
                    ColumnDefinition("msisdn", "String"),
                    ColumnDefinition("call_duration", "UInt32"),
                    ColumnDefinition("timestamp", "DateTime")
                )
            ),
            author = "Your Name",
            configurationSchema = mapOf(
                "batchSize" to "integer",
                "delimiter" to "string",
                "hasHeader" to "boolean"
            )
        )
    }
    
    override suspend fun validate(context: ProcessingContext): ValidationResult {
        return try {
            val file = context.inputFile
            
            if (!file.exists()) {
                ValidationResult.invalid("File does not exist: ${file.absolutePath}")
            } else if (file.length() == 0L) {
                ValidationResult.invalid("File is empty: ${file.absolutePath}")
            } else {
                // Additional validation logic
                ValidationResult.valid()
            }
        } catch (e: Exception) {
            ValidationResult.invalid("Validation error: ${e.message}")
        }
    }
    
    override suspend fun process(context: ProcessingContext): ProcessingResult {
        return try {
            val file = context.inputFile
            val clickHouseConnection = context.clickHouseConnection
            val jobConfig = context.jobConfiguration
            
            var recordsProcessed = 0L
            var recordsInserted = 0L
            val batchSize = jobConfig.batchSize ?: 1000
            
            file.bufferedReader().useLines { lines ->
                val dataLines = if (jobConfig.hasHeader == true) lines.drop(1) else lines
                
                dataLines.chunked(batchSize).forEach { batch ->
                    val insertedCount = processBatch(batch, clickHouseConnection, context)
                    recordsProcessed += batch.size
                    recordsInserted += insertedCount
                }
            }
            
            ProcessingResult.success(
                recordsProcessed = recordsProcessed,
                recordsInserted = recordsInserted,
                duration = System.currentTimeMillis() - context.startTime
            )
            
        } catch (e: Exception) {
            context.logger.error("Processing failed", e)
            ProcessingResult.failure(
                error = e.message ?: "Unknown error",
                recordsProcessed = 0,
                duration = System.currentTimeMillis() - context.startTime,
                cause = e
            )
        }
    }
    
    override suspend fun initialize(config: Map<String, Any>) {
        // Initialize resources, connections, etc.
    }
    
    override suspend fun destroy() {
        // Clean up resources
    }
    
    private fun processBatch(batch: List<String>, connection: Connection, context: ProcessingContext): Int {
        // Implementation for processing a batch of CSV lines
        // Insert data into ClickHouse
        return batch.size
    }
}
```

### Creating a Parser Plugin JAR

Create a new Gradle project for your parser:

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
    `java-library`
}

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/quantum-soft-dev/tia-etl-system")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    implementation("com.quantum.etl:parser-api:1.0.0")
    
    // Add your specific dependencies
    implementation("com.opencsv:opencsv:5.9")
    implementation("com.clickhouse:clickhouse-jdbc:0.7.1")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.mockk:mockk:1.13.8")
}

// Build a fat JAR for deployment
tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "My Custom Parser",
            "Implementation-Version" to version,
            "Parser-Class" to "com.example.MyCustomParser"
        )
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

### Deployment

1. Build your parser JAR:
   ```bash
   ./gradlew jar
   ```

2. Copy the JAR to the TIA ETL system's parser directory:
   ```bash
   cp build/libs/my-parser.jar /opt/tia/parsers/
   ```

3. The system will automatically discover and load your parser

## Advanced Usage

### Error Handling Best Practices

```kotlin
override suspend fun process(context: ProcessingContext): ProcessingResult {
    return try {
        // Processing logic here
        ProcessingResult.success(/* ... */)
        
    } catch (e: SQLException) {
        context.logger.error("Database error during processing", e)
        ProcessingResult.failure(
            error = "Database connection failed: ${e.message}",
            recordsProcessed = 0,
            duration = System.currentTimeMillis() - context.startTime,
            cause = e
        )
    } catch (e: IOException) {
        context.logger.error("File I/O error", e)
        ProcessingResult.failure(
            error = "File processing failed: ${e.message}",
            recordsProcessed = 0,
            duration = System.currentTimeMillis() - context.startTime,
            cause = e
        )
    } catch (e: Exception) {
        context.logger.error("Unexpected error during processing", e)
        ProcessingResult.failure(
            error = "Unexpected error: ${e.message}",
            recordsProcessed = 0,
            duration = System.currentTimeMillis() - context.startTime,
            cause = e
        )
    }
}
```

### Performance Optimization with Coroutines

```kotlin
override suspend fun process(context: ProcessingContext): ProcessingResult = coroutineScope {
    val batchSize = context.jobConfiguration.batchSize ?: 1000
    var totalProcessed = 0L
    var totalInserted = 0L
    
    context.inputFile.bufferedReader().useLines { lines ->
        lines.chunked(batchSize)
            .map { batch -> 
                async(Dispatchers.IO) { 
                    processBatchAsync(batch, context) 
                } 
            }
            .forEach { deferred ->
                val result = deferred.await()
                totalProcessed += result.processed
                totalInserted += result.inserted
            }
    }
    
    ProcessingResult.success(
        recordsProcessed = totalProcessed,
        recordsInserted = totalInserted,
        duration = System.currentTimeMillis() - context.startTime
    )
}
```

## Testing

### Unit Testing Your Parser

```kotlin
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class MyCustomParserTest {
    
    @Test
    fun `should return correct metadata`() {
        val parser = MyCustomParser()
        val metadata = parser.getMetadata()
        
        assertEquals("my-custom-parser", metadata.parserId)
        assertEquals("1.0.0", metadata.version)
        assertFalse(metadata.supportedFormats.isEmpty())
    }
    
    @Test
    fun `should validate file successfully`() = runTest {
        val parser = MyCustomParser()
        val context = createTestContext(validTestFile)
        
        val result = parser.validate(context)
        
        assertTrue(result is ValidationResult.Valid)
    }
    
    @Test
    fun `should process file and return success`() = runTest {
        val parser = MyCustomParser()
        val context = createTestContext(testDataFile)
        
        val result = parser.process(context)
        
        assertTrue(result is ProcessingResult.Success)
        assertEquals(expectedRecordCount, result.recordsProcessed)
    }
    
    @Test
    fun `should handle file processing errors gracefully`() = runTest {
        val parser = MyCustomParser()
        val context = createTestContext(invalidTestFile)
        
        val result = parser.process(context)
        
        assertTrue(result is ProcessingResult.Failure)
        assertTrue(result.error.contains("processing failed"))
    }
    
    private fun createTestContext(file: File): ProcessingContext {
        return ProcessingContext(
            inputFile = file,
            jobConfiguration = JobConfiguration(/* test config */),
            clickHouseConnection = mockConnection,
            postgresConnection = mockConnection,
            logger = mockLogger,
            startTime = System.currentTimeMillis(),
            metadata = emptyMap()
        )
    }
}
```

## Thread Safety

All interface implementations must be thread-safe as multiple instances may be used concurrently.

## Error Handling Guidelines

Parsers should:
- Never throw unchecked exceptions during processing
- Report errors through `ProcessingResult` rather than throwing exceptions
- Use the provided logger for all logging operations
- Handle database connection failures gracefully
- Validate input files before processing

## Development Workflow

1. Implement the `DataParser` interface
2. Add proper error handling and logging
3. Write comprehensive unit tests
4. Build and test your parser JAR
5. Deploy to the TIA ETL system
6. Monitor processing through the dashboard

## Running Tests

Run the parser-api tests:

```bash
./gradlew :core:parser-api:test
```

Run tests with coverage:

```bash
./gradlew :core:parser-api:jacocoTestReport
```

## API Documentation

For detailed API documentation, see the KDoc comments in the source code or generate documentation:

```bash
./gradlew :core:parser-api:dokkaHtml
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/my-parser`)
3. Commit your changes (`git commit -am 'Add my parser'`)
4. Push to the branch (`git push origin feature/my-parser`)
5. Create a Pull Request

## Versioning

This library follows [Semantic Versioning](https://semver.org/):

- **MAJOR**: Breaking API changes
- **MINOR**: New features (backwards compatible)
- **PATCH**: Bug fixes (backwards compatible)

## License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.

## Support

- Create an issue for bug reports or feature requests
- Check the [main project documentation](../../docs/) for system overview
- Contact the development team at dev@quantum-soft.com

## Changelog

See releases on GitHub for version history and breaking changes.