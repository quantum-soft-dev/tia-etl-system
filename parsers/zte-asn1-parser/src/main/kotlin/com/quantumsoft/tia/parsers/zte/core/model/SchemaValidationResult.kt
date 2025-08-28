package com.quantumsoft.tia.parsers.zte.core.model

import java.time.Instant

/**
 * Sealed class representing the result of database schema validation operations.
 * 
 * This sealed class provides type-safe handling of schema validation results
 * with detailed information about schema compliance, constraint violations,
 * and data type compatibility for ClickHouse insertion operations.
 * 
 * ## Design Principles
 * 
 * - **Type Safety**: Compile-time validation result handling
 * - **Immutability**: Thread-safe immutable result instances
 * - **Comprehensive Validation**: Detailed schema compliance checking
 * - **Performance Optimization**: Efficient validation for high-volume processing
 * - **Clear Error Reporting**: Specific information for troubleshooting
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
sealed class SchemaValidationResult {
    
    /**
     * Timestamp when the validation operation completed.
     */
    abstract val timestamp: Instant
    
    /**
     * Unique identifier for correlating this validation with logs and metrics.
     */
    abstract val correlationId: String
    
    /**
     * Number of fields that were validated.
     */
    abstract val fieldsValidated: Int
    
    /**
     * Represents successful schema validation with all constraints satisfied.
     * 
     * Contains metadata about the validation process and confirmed
     * compatibility with the target ClickHouse schema.
     * 
     * @param tableName Target ClickHouse table name
     * @param schemaVersion Schema version that was validated against
     * @param validatedFields List of field names that passed validation
     * @param timestamp When validation completed
     * @param correlationId Unique identifier for operation correlation
     * @param fieldsValidated Number of fields validated
     * @param validationMetrics Performance and quality metrics
     */
    data class Valid(
        val tableName: String,
        val schemaVersion: String,
        val validatedFields: List&lt;String&gt;,
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String,
        override val fieldsValidated: Int = validatedFields.size,
        val validationMetrics: SchemaValidationMetrics = SchemaValidationMetrics()
    ) : SchemaValidationResult() {
        
        /**
         * Combines this validation result with another schema validation result.
         * 
         * This method enables accumulation of validation results from multiple
         * validation operations while preserving all context and ensuring that
         * combined results remain valid only if all individual results are valid.
         * 
         * @param other Another schema validation result to combine
         * @return Combined validation result (Invalid if either failed)
         */
        fun combine(other: SchemaValidationResult): SchemaValidationResult {
            return when (other) {
                is Valid -&gt; Valid(
                    tableName = this.tableName,
                    schemaVersion = this.schemaVersion,
                    validatedFields = this.validatedFields + other.validatedFields,
                    timestamp = if (other.timestamp.isAfter(this.timestamp)) other.timestamp else this.timestamp,
                    correlationId = this.correlationId,
                    fieldsValidated = this.fieldsValidated + other.fieldsValidated,
                    validationMetrics = this.validationMetrics.combine(other.validationMetrics)
                )
                is Invalid -&gt; other
            }
        }
    }
    
    /**
     * Represents failed schema validation with detailed violation information.
     * 
     * Contains comprehensive details about schema validation failures including
     * specific violations, constraint mismatches, and data type incompatibilities
     * for effective troubleshooting and data correction.
     * 
     * @param tableName Target ClickHouse table name
     * @param schemaVersion Schema version that validation failed against
     * @param violations List of specific schema validation violations
     * @param failedFields Set of field names that failed validation
     * @param timestamp When validation failed
     * @param correlationId Unique identifier for operation correlation
     * @param fieldsValidated Number of fields attempted before failure
     */
    data class Invalid(
        val tableName: String,
        val schemaVersion: String,
        val violations: List&lt;SchemaViolation&gt;,
        val failedFields: Set&lt;String&gt; = violations.map { it.fieldName }.toSet(),
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String,
        override val fieldsValidated: Int = violations.size
    ) : SchemaValidationResult() {
        
        /**
         * Gets violations filtered by severity level.
         * 
         * This method enables filtering of schema violations by their severity
         * to support different error handling strategies for different types
         * of validation failures.
         * 
         * @param severity Minimum severity level to include
         * @return List of violations at or above the specified severity
         */
        fun getViolationsBySeverity(severity: SchemaSeverity): List&lt;SchemaViolation&gt; {
            return violations.filter { it.severity.ordinal &gt;= severity.ordinal }
        }
        
        /**
         * Gets violations for a specific field name.
         * 
         * This method enables field-specific error handling and reporting
         * when processing schema validation results.
         * 
         * @param fieldName Name of the field to get violations for
         * @return List of violations for the specified field
         */
        fun getViolationsForField(fieldName: String): List&lt;SchemaViolation&gt; {
            return violations.filter { it.fieldName == fieldName }
        }
        
        /**
         * Gets violations grouped by violation type for analysis.
         * 
         * @return Map of violation types to lists of violations
         */
        fun getViolationsByType(): Map&lt;SchemaViolationType, List&lt;SchemaViolation&gt;&gt; {
            return violations.groupBy { it.violationType }
        }
        
        /**
         * Checks if any violations are at CRITICAL severity level.
         * 
         * Critical violations typically indicate data that will cause insertion
         * errors or data corruption if processed.
         * 
         * @return true if any critical violations exist
         */
        fun hasCriticalViolations(): Boolean {
            return violations.any { it.severity == SchemaSeverity.CRITICAL }
        }
        
        /**
         * Checks if any violations are data type mismatches.
         * 
         * Type mismatches typically require data transformation or
         * schema updates to resolve.
         * 
         * @return true if any type mismatch violations exist
         */
        fun hasTypeMismatchViolations(): Boolean {
            return violations.any { it.violationType == SchemaViolationType.TYPE_MISMATCH }
        }
        
        /**
         * Gets a summary of all schema violations.
         * 
         * This method provides a concise summary suitable for logging
         * and monitoring systems.
         * 
         * @return Summary string describing all violations
         */
        fun getSummary(): String {
            val groupedBySeverity = violations.groupBy { it.severity }
            return groupedBySeverity.map { (severity, viols) -&gt;
                "${severity.name}: ${viols.size} violations"
            }.joinToString(", ")
        }
        
        /**
         * Combines this invalid result with another schema validation result.
         * 
         * This method accumulates schema violations from multiple validation
         * operations for comprehensive error reporting.
         * 
         * @param other Another schema validation result to combine
         * @return Combined invalid result with all violations
         */
        fun combine(other: SchemaValidationResult): Invalid {
            return when (other) {
                is Valid -&gt; this // Invalid result dominates
                is Invalid -&gt; Invalid(
                    tableName = this.tableName,
                    schemaVersion = this.schemaVersion,
                    violations = this.violations + other.violations,
                    failedFields = this.failedFields + other.failedFields,
                    timestamp = if (other.timestamp.isAfter(this.timestamp)) other.timestamp else this.timestamp,
                    correlationId = this.correlationId,
                    fieldsValidated = this.fieldsValidated + other.fieldsValidated
                )
            }
        }
    }
    
    /**
     * Checks if this result represents successful schema validation.
     * 
     * @return true if validation passed, false otherwise
     */
    fun isValid(): Boolean = this is Valid
    
    /**
     * Checks if this result represents failed schema validation.
     * 
     * @return true if validation failed, false otherwise
     */
    fun isInvalid(): Boolean = this is Invalid
    
    /**
     * Gets schema violations if this result is invalid.
     * 
     * @return List of violations if invalid, empty list if valid
     */
    fun getViolations(): List&lt;SchemaViolation&gt; = when (this) {
        is Valid -&gt; emptyList()
        is Invalid -&gt; violations
    }
    
    /**
     * Executes an action if schema validation was successful.
     * 
     * This method enables side effects on successful validation
     * without changing the result itself.
     * 
     * @param action Function to execute on successful validation
     * @return The same SchemaValidationResult instance
     */
    fun onValid(action: (Valid) -&gt; Unit): SchemaValidationResult {
        if (this is Valid) {
            action(this)
        }
        return this
    }
    
    /**
     * Executes an action if schema validation failed.
     * 
     * This method enables side effects on validation failures
     * without changing the result itself.
     * 
     * @param action Function to execute with violation details
     * @return The same SchemaValidationResult instance
     */
    fun onInvalid(action: (Invalid) -&gt; Unit): SchemaValidationResult {
        if (this is Invalid) {
            action(this)
        }
        return this
    }
    
    /**
     * Combines this result with another schema validation result.
     * 
     * This method provides a general way to combine validation results
     * regardless of their success or failure status.
     * 
     * @param other Another schema validation result to combine
     * @return Combined validation result
     */
    fun combine(other: SchemaValidationResult): SchemaValidationResult = when (this) {
        is Valid -&gt; this.combine(other)
        is Invalid -&gt; this.combine(other)
    }
}

/**
 * Represents a specific schema validation constraint violation.
 * 
 * This data class provides detailed information about individual
 * schema validation failures to support debugging, error reporting,
 * and data correction efforts.
 * 
 * @param fieldName Name of the field that failed schema validation
 * @param violationType Type of schema violation that occurred
 * @param message Human-readable description of the violation
 * @param severity Severity level of this schema violation
 * @param actualValue The actual value that failed validation
 * @param expectedType Expected ClickHouse column type
 * @param actualType Detected type of the actual value
 * @param constraint Schema constraint that was violated
 * @param suggestion Suggested correction or recovery action
 */
data class SchemaViolation(
    val fieldName: String,
    val violationType: SchemaViolationType,
    val message: String,
    val severity: SchemaSeverity,
    val actualValue: Any? = null,
    val expectedType: String? = null,
    val actualType: String? = null,
    val constraint: String? = null,
    val suggestion: String? = null
) {
    
    /**
     * Creates a formatted error message for logging and reporting.
     * 
     * @return Comprehensive error message including all violation details
     */
    fun toDetailedMessage(): String {
        val parts = mutableListOf&lt;String&gt;()
        parts.add("Field '$fieldName' has ${violationType.displayName}: $message")
        
        if (actualValue != null && actualType != null) {
            parts.add("Actual: $actualType = $actualValue")
        }
        
        if (expectedType != null) {
            parts.add("Expected: $expectedType")
        }
        
        if (constraint != null) {
            parts.add("Constraint: $constraint")
        }
        
        if (suggestion != null) {
            parts.add("Suggestion: $suggestion")
        }
        
        return parts.joinToString(". ")
    }
}

/**
 * Types of schema validation violations.
 */
enum class SchemaViolationType(val displayName: String) {
    /**
     * Data type doesn't match ClickHouse column type.
     */
    TYPE_MISMATCH("type mismatch"),
    
    /**
     * Required field is null in non-nullable schema.
     */
    NULL_CONSTRAINT_VIOLATION("null constraint violation"),
    
    /**
     * String field exceeds maximum length constraint.
     */
    LENGTH_CONSTRAINT_VIOLATION("length constraint violation"),
    
    /**
     * Numeric field violates range constraints.
     */
    RANGE_CONSTRAINT_VIOLATION("range constraint violation"),
    
    /**
     * Field value doesn't match expected format or pattern.
     */
    FORMAT_VIOLATION("format violation"),
    
    /**
     * Foreign key constraint violation.
     */
    FOREIGN_KEY_VIOLATION("foreign key violation"),
    
    /**
     * Unique constraint violation.
     */
    UNIQUE_CONSTRAINT_VIOLATION("unique constraint violation"),
    
    /**
     * Custom constraint violation defined by business rules.
     */
    CUSTOM_CONSTRAINT_VIOLATION("custom constraint violation"),
    
    /**
     * Unknown or unclassified schema violation.
     */
    UNKNOWN_VIOLATION("unknown violation")
}

/**
 * Enumeration of schema validation violation severity levels.
 * 
 * Severity levels help classify schema violations by their impact
 * and enable appropriate error handling and recovery strategies.
 */
enum class SchemaSeverity {
    /**
     * Warning-level issues that may indicate data quality problems.
     * Examples: Optional constraints, performance hints
     */
    WARNING,
    
    /**
     * Error-level issues that should prevent normal insertion.
     * Examples: Type mismatches, format violations
     */
    ERROR,
    
    /**
     * Critical issues that will cause insertion failures.
     * Examples: Null constraint violations, foreign key failures
     */
    CRITICAL
}

/**
 * Performance and quality metrics for schema validation operations.
 * 
 * @param validationTime Time spent on schema validation (milliseconds)
 * @param cacheHitRate Rate of cache hits for validation rules (0.0 to 1.0)
 * @param fieldsPerSecond Validation throughput in fields per second
 * @param memoryUsed Memory used during validation (bytes)
 */
data class SchemaValidationMetrics(
    val validationTime: Long = 0L,
    val cacheHitRate: Double = 0.0,
    val fieldsPerSecond: Double = 0.0,
    val memoryUsed: Long = 0L
) {
    
    /**
     * Combines this metrics instance with another metrics instance.
     * 
     * @param other Another metrics instance to combine
     * @return Combined metrics with aggregated values
     */
    fun combine(other: SchemaValidationMetrics): SchemaValidationMetrics {
        return SchemaValidationMetrics(
            validationTime = this.validationTime + other.validationTime,
            cacheHitRate = (this.cacheHitRate + other.cacheHitRate) / 2.0, // Average cache hit rate
            fieldsPerSecond = (this.fieldsPerSecond + other.fieldsPerSecond) / 2.0, // Average throughput
            memoryUsed = maxOf(this.memoryUsed, other.memoryUsed) // Peak memory usage
        )
    }
}