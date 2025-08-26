# Parser API Module

Core API module defining interfaces and contracts for all data parsers in the TIA ETL system.

## Overview

This module provides the foundation for the plugin architecture that allows the TIA ETL system to support different data formats and sources. It defines the contracts that all parser implementations must follow.

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

```kotlin
class ExampleCsvParser : DataParser, ParserLifecycle {
    override fun getMetadata(): ParserMetadata {
        return ParserMetadata(
            parserId = "csv-parser",
            name = "CSV Parser",
            version = "1.0.0",
            description = "Parses CSV files",
            supportedFormats = listOf("csv"),
            targetTable = "csv_data",
            schemaDefinition = TableSchema(
                tableName = "csv_data",
                columns = listOf(
                    ColumnDefinition("id", "UInt32"),
                    ColumnDefinition("name", "String")
                )
            )
        )
    }
    
    override fun process(context: ProcessingContext): ProcessingResult {
        // Implementation here
    }
}
```

## Thread Safety

All interface implementations must be thread-safe as multiple instances may be used concurrently.

## Error Handling

Parsers should:
- Never throw unchecked exceptions during processing
- Report errors through `ProcessingResult` rather than throwing exceptions
- Use the provided logger for all logging operations

## Testing

The module includes comprehensive unit tests and an example parser implementation demonstrating proper API usage.

Run tests with:
```bash
./gradlew :core:parser-api:test
```