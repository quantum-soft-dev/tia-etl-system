package com.quantumsoft.tia.parsers.zte.core.model

import java.time.Instant

/**
 * Comprehensive error type hierarchy for ZTE ASN.1 parser operations.
 * 
 * This hierarchy provides specific, actionable error types that enable
 * proper error handling, recovery strategies, and debugging support.
 * All error types are designed to be serializable and contain sufficient
 * context for troubleshooting without exposing sensitive information.
 * 
 * ## Design Principles
 * 
 * - **Specific Error Types**: Each error type represents a distinct failure mode
 * - **Actionable Information**: Errors include context for resolution
 * - **No Sensitive Data**: Error messages are safe for logging and monitoring
 * - **Structured Context**: Consistent error context across all types
 * - **Recovery Guidance**: Errors include recovery suggestions where applicable
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */

/**
 * Base interface for all parser-related errors.
 * 
 * Provides common error properties and behavior across all error types
 * to ensure consistent error handling and reporting.
 */
interface ParserError {
    /**
     * Human-readable error message describing what went wrong.
     */
    val message: String
    
    /**
     * Error type identifier for categorization and metrics.
     */
    val type: String
    
    /**
     * Error category for high-level error grouping.
     */
    val category: String
    
    /**
     * Timestamp when the error occurred.
     */
    val timestamp: Instant
    
    /**
     * Unique correlation ID for tracking this error across logs.
     */
    val correlationId: String
    
    /**
     * Whether this error condition might be recoverable with retry.
     */
    val isRetryable: Boolean
    
    /**
     * Suggested recovery actions for this error.
     */
    val recoverySuggestions: List&lt;String&gt;
}

/**
 * Errors related to ASN.1 parsing and decoding operations.
 */
sealed class ParseError : ParserError {
    override val category: String = "PARSE_ERROR"
    
    /**
     * Invalid or corrupted ASN.1 structure that cannot be decoded.
     * 
     * @param details Specific details about the structural problem
     * @param position Byte position where the error was detected (if known)
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class InvalidAsn1Structure(
        val details: String,
        val position: Long? = null,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : ParseError() {
        override val message: String = "Invalid ASN.1 structure: $details${position?.let { " at position $it" } ?: ""}"
        override val type: String = "INVALID_ASN1_STRUCTURE"
        override val isRetryable: Boolean = false
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Verify the ASN.1 file is not corrupted",
            "Check if the file uses supported ASN.1 encoding (BER/DER)",
            "Validate the source system generating the ASN.1 data"
        )
    }
    
    /**
     * Unknown or unsupported ASN.1 tag encountered during parsing.
     * 
     * @param tag The unsupported tag value
     * @param context Context where the unknown tag was encountered
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class UnknownTag(
        val tag: String,
        val context: String,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : ParseError() {
        override val message: String = "Unknown ASN.1 tag '$tag' in context: $context"
        override val type: String = "UNKNOWN_TAG"
        override val isRetryable: Boolean = false
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Check if this is a new ZTE CDR format version",
            "Update parser to support additional tag types",
            "Review ZTE ASN.1 specification for tag definitions"
        )
    }
    
    /**
     * Required field is missing from the ASN.1 structure.
     * 
     * @param fieldName Name of the missing required field
     * @param expectedTag Expected ASN.1 tag for the missing field
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class MissingRequiredField(
        val fieldName: String,
        val expectedTag: String,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : ParseError() {
        override val message: String = "Required field '$fieldName' (tag: $expectedTag) is missing"
        override val type: String = "MISSING_REQUIRED_FIELD"
        override val isRetryable: Boolean = false
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Verify the ZTE CDR record is complete",
            "Check if the source system configuration is correct",
            "Review field mapping configuration for changes"
        )
    }
    
    /**
     * Field value cannot be decoded or converted to expected type.
     * 
     * @param fieldName Name of the field with decoding issues
     * @param expectedType Expected data type for the field
     * @param actualValue Raw value that couldn't be decoded
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class FieldDecodingError(
        val fieldName: String,
        val expectedType: String,
        val actualValue: String,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : ParseError() {
        override val message: String = "Cannot decode field '$fieldName' as $expectedType from value: $actualValue"
        override val type: String = "FIELD_DECODING_ERROR"
        override val isRetryable: Boolean = false
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Verify field encoding matches expected format",
            "Check if ZTE firmware version affects field encoding",
            "Update field decoder for this specific format variation"
        )
    }
    
    /**
     * ASN.1 data is truncated or incomplete.
     * 
     * @param expectedBytes Number of bytes expected
     * @param actualBytes Number of bytes actually available
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class TruncatedData(
        val expectedBytes: Long,
        val actualBytes: Long,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : ParseError() {
        override val message: String = "ASN.1 data is truncated: expected $expectedBytes bytes, got $actualBytes bytes"
        override val type: String = "TRUNCATED_DATA"
        override val isRetryable: Boolean = true
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Check if file transfer was interrupted",
            "Verify network connectivity during file retrieval",
            "Re-download or re-transfer the ASN.1 file"
        )
    }
}

/**
 * Errors related to database operations and schema validation.
 */
sealed class DatabaseError : ParserError {
    override val category: String = "DATABASE_ERROR"
    
    /**
     * Database connection could not be established or was lost.
     * 
     * @param details Specific connection error details
     * @param database Database system identifier (e.g., "ClickHouse")
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class ConnectionFailure(
        val details: String,
        val database: String = "ClickHouse",
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : DatabaseError() {
        override val message: String = "Database connection failed for $database: $details"
        override val type: String = "CONNECTION_FAILURE"
        override val isRetryable: Boolean = true
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Check database server availability",
            "Verify network connectivity to database",
            "Review database connection configuration",
            "Check if database credentials are valid"
        )
    }
    
    /**
     * Record data doesn't match the target database schema.
     * 
     * @param fieldName Name of the field with schema mismatch
     * @param expectedType Expected database column type
     * @param actualType Actual data type of the value
     * @param value The problematic value
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class SchemaMismatch(
        val fieldName: String,
        val expectedType: String,
        val actualType: String,
        val value: String,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : DatabaseError() {
        override val message: String = "Schema mismatch for field '$fieldName': expected $expectedType, got $actualType (value: $value)"
        override val type: String = "SCHEMA_MISMATCH"
        override val isRetryable: Boolean = false
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Update database schema to match data format",
            "Modify data transformation to match schema",
            "Review field mapping configuration",
            "Check if database schema version is current"
        )
    }
    
    /**
     * Database constraint violation during insertion.
     * 
     * @param constraintName Name of the violated constraint
     * @param constraintType Type of constraint (PRIMARY_KEY, UNIQUE, etc.)
     * @param affectedFields Fields involved in the constraint violation
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class ConstraintViolation(
        val constraintName: String,
        val constraintType: String,
        val affectedFields: List&lt;String&gt;,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : DatabaseError() {
        override val message: String = "Constraint violation: $constraintType constraint '$constraintName' on fields: ${affectedFields.joinToString(", ")}"
        override val type: String = "CONSTRAINT_VIOLATION"
        override val isRetryable: Boolean = false
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Check for duplicate records in source data",
            "Verify data uniqueness requirements",
            "Review constraint definitions for appropriateness",
            "Consider data deduplication before insertion"
        )
    }
    
    /**
     * Database operation timed out.
     * 
     * @param operation Type of operation that timed out
     * @param timeoutSeconds Timeout duration in seconds
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class OperationTimeout(
        val operation: String,
        val timeoutSeconds: Long,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : DatabaseError() {
        override val message: String = "Database operation '$operation' timed out after $timeoutSeconds seconds"
        override val type: String = "OPERATION_TIMEOUT"
        override val isRetryable: Boolean = true
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Increase database operation timeout",
            "Check database server performance",
            "Reduce batch size for large operations",
            "Verify database isn't under heavy load"
        )
    }
}

/**
 * Errors related to system resources and configuration.
 */
sealed class SystemError : ParserError {
    override val category: String = "SYSTEM_ERROR"
    
    /**
     * Insufficient memory to complete the operation.
     * 
     * @param requiredMemory Amount of memory required (bytes)
     * @param availableMemory Amount of memory available (bytes)
     * @param operation Operation that required additional memory
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class InsufficientMemory(
        val requiredMemory: Long,
        val availableMemory: Long,
        val operation: String,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : SystemError() {
        override val message: String = "Insufficient memory for $operation: required ${requiredMemory / 1024 / 1024}MB, available ${availableMemory / 1024 / 1024}MB"
        override val type: String = "INSUFFICIENT_MEMORY"
        override val isRetryable: Boolean = true
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Increase JVM heap size allocation",
            "Process files in smaller batches",
            "Optimize memory usage in parsing algorithms",
            "Consider processing during off-peak hours"
        )
    }
    
    /**
     * Configuration parameter is missing or invalid.
     * 
     * @param parameterName Name of the problematic configuration parameter
     * @param expectedValue Description of expected value format
     * @param actualValue Actual value that was found (if any)
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class ConfigurationError(
        val parameterName: String,
        val expectedValue: String,
        val actualValue: String?,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : SystemError() {
        override val message: String = "Configuration error for parameter '$parameterName': expected $expectedValue, got ${actualValue ?: "null"}"
        override val type: String = "CONFIGURATION_ERROR"
        override val isRetryable: Boolean = false
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Review configuration file for missing parameters",
            "Verify configuration parameter format and values",
            "Check environment variable settings",
            "Consult configuration documentation"
        )
    }
    
    /**
     * Required external dependency is not available.
     * 
     * @param dependency Name of the unavailable dependency
     * @param dependencyType Type of dependency (database, service, library)
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class DependencyUnavailable(
        val dependency: String,
        val dependencyType: String,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : SystemError() {
        override val message: String = "Required $dependencyType '$dependency' is not available"
        override val type: String = "DEPENDENCY_UNAVAILABLE"
        override val isRetryable: Boolean = true
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Check if $dependency service is running",
            "Verify network connectivity to $dependency",
            "Review dependency configuration and credentials",
            "Check service health status and logs"
        )
    }
}

/**
 * Errors related to file operations and I/O.
 */
sealed class FileError : ParserError {
    override val category: String = "FILE_ERROR"
    
    /**
     * File could not be found or accessed.
     * 
     * @param filePath Path to the file that couldn't be accessed
     * @param reason Specific reason for access failure
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class FileAccessError(
        val filePath: String,
        val reason: String,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : FileError() {
        override val message: String = "Cannot access file '$filePath': $reason"
        override val type: String = "FILE_ACCESS_ERROR"
        override val isRetryable: Boolean = true
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Verify file path is correct",
            "Check file system permissions",
            "Ensure file exists and is not locked by another process",
            "Check available disk space"
        )
    }
    
    /**
     * File format is not supported or recognized.
     * 
     * @param filePath Path to the unsupported file
     * @param detectedFormat Format that was detected (if any)
     * @param expectedFormats List of supported formats
     * @param timestamp When the error occurred
     * @param correlationId Correlation identifier for tracking
     */
    data class UnsupportedFileFormat(
        val filePath: String,
        val detectedFormat: String?,
        val expectedFormats: List&lt;String&gt;,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String
    ) : FileError() {
        override val message: String = "Unsupported file format for '$filePath': detected ${detectedFormat ?: "unknown"}, expected one of: ${expectedFormats.joinToString(", ")}"
        override val type: String = "UNSUPPORTED_FILE_FORMAT"
        override val isRetryable: Boolean = false
        override val recoverySuggestions: List&lt;String&gt; = listOf(
            "Verify file is in correct format",
            "Check if file was corrupted during transfer",
            "Ensure source system is generating correct format",
            "Update parser to support additional formats if needed"
        )
    }
}