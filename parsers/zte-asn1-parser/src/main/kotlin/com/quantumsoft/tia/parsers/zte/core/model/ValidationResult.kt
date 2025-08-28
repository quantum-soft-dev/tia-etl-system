package com.quantumsoft.tia.parsers.zte.core.model

import java.time.Instant

/**
 * Sealed class representing the result of data validation operations.
 * 
 * This sealed class provides type-safe validation result handling with
 * comprehensive error reporting and context preservation. It supports
 * both individual field validation and complete record validation
 * with detailed error categorization and recovery suggestions.
 * 
 * ## Design Principles
 * 
 * - **Type Safety**: Compile-time validation result handling
 * - **Immutability**: Thread-safe immutable result instances  
 * - **Comprehensive Errors**: Detailed validation failure information
 * - **Functional Composition**: Chainable validation operations
 * - **Error Accumulation**: Multiple validation errors can be collected
 * 
 * ## Usage Pattern
 * 
 * ```kotlin
 * when (val result = validator.validateRecord(record)) {
 *     is ValidationResult.Valid -> {
 *         // Proceed with valid record
 *         databaseWriter.insert(record)
 *     }
 *     is ValidationResult.Invalid -> {
 *         // Handle validation failures
 *         result.violations.forEach { violation ->
 *             logger.warn("Validation failed: ${violation.message}")
 *         }
 *         metricsCollector.recordValidationFailure(result.violations.size)
 *     }
 * }
 * ```
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
sealed class ValidationResult {
    
    /**
     * Timestamp when the validation operation completed.
     */
    abstract val timestamp: Instant
    
    /**
     * Unique identifier for correlating this validation with logs and metrics.
     */
    abstract val correlationId: String
    
    /**
     * Number of fields or rules that were validated.
     */
    abstract val validationCount: Int
    
    /**
     * Represents a successful validation with all constraints satisfied.
     * 
     * Contains metadata about the validation process for monitoring
     * and performance tracking purposes.
     * 
     * @param validatedFields List of field names that were validated
     * @param appliedRules List of validation rules that were applied
     * @param timestamp When validation completed
     * @param correlationId Unique identifier for operation correlation
     * @param validationCount Number of validations performed
     */
    data class Valid(
        val validatedFields: List&lt;String&gt; = emptyList(),
        val appliedRules: List&lt;String&gt; = emptyList(),
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String,
        override val validationCount: Int = validatedFields.size
    ) : ValidationResult() {
        
        /**
         * Combines this validation result with another validation result.
         * 
         * This method enables accumulation of validation results from
         * multiple validation operations while preserving all context.
         * 
         * @param other Another validation result to combine
         * @return Combined validation result (Invalid if either failed)
         */
        fun combine(other: ValidationResult): ValidationResult {
            return when (other) {
                is Valid -&gt; Valid(
                    validatedFields = this.validatedFields + other.validatedFields,
                    appliedRules = this.appliedRules + other.appliedRules,
                    timestamp = if (other.timestamp.isAfter(this.timestamp)) other.timestamp else this.timestamp,
                    correlationId = this.correlationId,
                    validationCount = this.validationCount + other.validationCount
                )
                is Invalid -&gt; other
            }
        }
    }
    
    /**
     * Represents a failed validation with detailed violation information.
     * 
     * Contains comprehensive details about validation failures including
     * specific violations, severity levels, and recovery suggestions
     * for troubleshooting and data correction.
     * 
     * @param violations List of specific validation violations
     * @param failedFields Set of field names that failed validation
     * @param timestamp When validation failed
     * @param correlationId Unique identifier for operation correlation
     * @param validationCount Number of validations attempted before failure
     */
    data class Invalid(
        val violations: List&lt;ValidationViolation&gt;,
        val failedFields: Set&lt;String&gt; = violations.map { it.fieldName }.toSet(),
        override val timestamp: Instant = Instant.now(),
        override val correlationId: String,
        override val validationCount: Int = violations.size
    ) : ValidationResult() {
        
        /**
         * Gets violations filtered by severity level.
         * 
         * This method enables filtering of validation violations by their
         * severity to support different error handling strategies for
         * different types of validation failures.
         * 
         * @param severity Minimum severity level to include
         * @return List of violations at or above the specified severity
         */
        fun getViolationsBySeverity(severity: ValidationSeverity): List&lt;ValidationViolation&gt; {
            return violations.filter { it.severity.ordinal &gt;= severity.ordinal }
        }
        
        /**
         * Gets violations for a specific field name.
         * 
         * This method enables field-specific error handling and reporting
         * when processing validation results.
         * 
         * @param fieldName Name of the field to get violations for
         * @return List of violations for the specified field
         */
        fun getViolationsForField(fieldName: String): List&lt;ValidationViolation&gt; {
            return violations.filter { it.fieldName == fieldName }
        }
        
        /**
         * Checks if any violations are at CRITICAL severity level.
         * 
         * Critical violations typically indicate data that should not
         * be processed further as it may cause system errors or
         * data corruption.
         * 
         * @return true if any critical violations exist
         */
        fun hasCriticalViolations(): Boolean {
            return violations.any { it.severity == ValidationSeverity.CRITICAL }
        }
        
        /**
         * Gets a summary of all validation violations.
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
         * Combines this invalid result with another validation result.
         * 
         * This method accumulates validation violations from multiple
         * validation operations for comprehensive error reporting.
         * 
         * @param other Another validation result to combine
         * @return Combined invalid result with all violations
         */
        fun combine(other: ValidationResult): Invalid {
            return when (other) {
                is Valid -&gt; this // Invalid result dominates
                is Invalid -&gt; Invalid(
                    violations = this.violations + other.violations,
                    failedFields = this.failedFields + other.failedFields,
                    timestamp = if (other.timestamp.isAfter(this.timestamp)) other.timestamp else this.timestamp,
                    correlationId = this.correlationId,
                    validationCount = this.validationCount + other.validationCount
                )
            }
        }
    }
    
    /**
     * Checks if this result represents successful validation.
     * 
     * @return true if validation passed, false otherwise
     */
    fun isValid(): Boolean = this is Valid
    
    /**
     * Checks if this result represents failed validation.
     * 
     * @return true if validation failed, false otherwise
     */
    fun isInvalid(): Boolean = this is Invalid
    
    /**
     * Gets validation violations if this result is invalid.
     * 
     * @return List of violations if invalid, empty list if valid
     */
    fun getViolations(): List&lt;ValidationViolation&gt; = when (this) {
        is Valid -&gt; emptyList()
        is Invalid -&gt; violations
    }
    
    /**
     * Executes an action if validation was successful.
     * 
     * This method enables side effects on successful validation
     * without changing the result itself.
     * 
     * @param action Function to execute on successful validation
     * @return The same ValidationResult instance
     */
    fun onValid(action: () -&gt; Unit): ValidationResult {
        if (this is Valid) {
            action()
        }
        return this
    }
    
    /**
     * Executes an action if validation failed.
     * 
     * This method enables side effects on validation failures
     * without changing the result itself.
     * 
     * @param action Function to execute with violation details
     * @return The same ValidationResult instance
     */
    fun onInvalid(action: (List&lt;ValidationViolation&gt;) -&gt; Unit): ValidationResult {
        if (this is Invalid) {
            action(violations)
        }
        return this
    }
    
    /**
     * Combines this result with another validation result.
     * 
     * This method provides a general way to combine validation results
     * regardless of their success or failure status.
     * 
     * @param other Another validation result to combine
     * @return Combined validation result
     */
    fun combine(other: ValidationResult): ValidationResult = when (this) {
        is Valid -&gt; this.combine(other)
        is Invalid -&gt; this.combine(other)
    }
}

/**
 * Represents a specific validation constraint violation.
 * 
 * This data class provides detailed information about individual
 * validation failures to support debugging, error reporting,
 * and data correction efforts.
 * 
 * @param fieldName Name of the field that failed validation
 * @param violatedRule Name of the validation rule that was violated
 * @param message Human-readable description of the violation
 * @param severity Severity level of this validation violation
 * @param actualValue The actual value that failed validation
 * @param expectedValue Expected value or format description
 * @param suggestion Suggested correction or recovery action
 */
data class ValidationViolation(
    val fieldName: String,
    val violatedRule: String,
    val message: String,
    val severity: ValidationSeverity,
    val actualValue: Any? = null,
    val expectedValue: String? = null,
    val suggestion: String? = null
) {
    
    /**
     * Creates a formatted error message for logging and reporting.
     * 
     * @return Comprehensive error message including all violation details
     */
    fun toDetailedMessage(): String {
        val parts = mutableListOf&lt;String&gt;()
        parts.add("Field '$fieldName' violated rule '$violatedRule': $message")
        
        if (actualValue != null) {
            parts.add("Actual value: $actualValue")
        }
        
        if (expectedValue != null) {
            parts.add("Expected: $expectedValue")
        }
        
        if (suggestion != null) {
            parts.add("Suggestion: $suggestion")
        }
        
        return parts.joinToString(". ")
    }
}

/**
 * Enumeration of validation violation severity levels.
 * 
 * Severity levels help classify validation violations by their impact
 * and enable appropriate error handling and recovery strategies.
 */
enum class ValidationSeverity {
    /**
     * Informational issues that don't prevent processing.
     * Examples: Optional field missing, format preferences
     */
    INFO,
    
    /**
     * Warning-level issues that may indicate data quality problems.
     * Examples: Unusual value ranges, deprecated formats
     */
    WARNING,
    
    /**
     * Error-level issues that should prevent normal processing.
     * Examples: Invalid formats, constraint violations
     */
    ERROR,
    
    /**
     * Critical issues that may cause system errors or data corruption.
     * Examples: Required field missing, data type mismatches
     */
    CRITICAL
}