package com.quantumsoft.tia.parsers.zte.validation

import com.quantumsoft.tia.parsers.zte.core.model.ValidationResult
import com.quantumsoft.tia.parsers.zte.core.model.ZteCdrRecord

/**
 * Interface for field-level data validation following Single Responsibility Principle.
 * 
 * This interface focuses exclusively on validating individual field values and
 * field combinations according to business rules and data integrity constraints.
 * It is separated from ASN.1 parsing and database operations to maintain
 * clear separation of concerns and enable independent testing and evolution.
 * 
 * ## Design Principles
 * 
 * - **Single Responsibility**: Only handles data validation logic
 * - **Interface Segregation**: Focused interface with minimal dependencies
 * - **Open/Closed**: Extensible for new validation rules without modification
 * - **Liskov Substitution**: All implementations fully interchangeable
 * - **Dependency Inversion**: Validation rules depend on configuration abstractions
 * 
 * ## Validation Scope
 * 
 * - Field format validation (MSISDN, IMSI patterns)
 * - Range validation (call duration, timestamps)
 * - Business rule validation (start time &lt; end time)
 * - Cross-field consistency checks
 * - Configurable constraint validation
 * 
 * ## Performance Requirements
 * 
 * - Individual field validation within 100μs
 * - Record validation within 1ms
 * - Optimized for batch validation scenarios
 * - Memory-efficient for large datasets
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
interface DataValidator {
    
    /**
     * Validates a single field value against its defined constraints.
     * 
     * This method performs comprehensive validation of individual field values,
     * including format validation, range checking, and constraint compliance.
     * It provides detailed error information to assist with data quality
     * troubleshooting and correction.
     * 
     * ## Validation Types
     * 
     * - **Format Validation**: Regex patterns, character sets, encoding
     * - **Range Validation**: Numeric ranges, string lengths, date boundaries  
     * - **Enum Validation**: Valid value lists, lookup table verification
     * - **Custom Rules**: Business-specific validation logic
     * 
     * ## Error Reporting
     * 
     * Validation failures include specific details about:
     * - Which constraint was violated
     * - Expected format or range
     * - Actual value that failed validation
     * - Suggested corrections where applicable
     * 
     * @param fieldName Identifier of the field being validated
     * @param value Field value to validate (type varies by field)
     * @return ValidationResult indicating success or specific validation failures
     * 
     * @see ValidationResult for detailed error reporting structure
     */
    fun validate(fieldName: String, value: Any?): ValidationResult
    
    /**
     * Validates an entire ZTE CDR record for consistency and completeness.
     * 
     * This method performs comprehensive record-level validation, including
     * individual field validation and cross-field business rule checking.
     * It orchestrates validation across all fields while maintaining performance
     * for high-volume processing scenarios.
     * 
     * ## Validation Layers
     * 
     * 1. **Null Checks**: Ensure required fields are present
     * 2. **Field Validation**: Validate each field using validate() method
     * 3. **Cross-Field Rules**: Validate relationships between fields
     * 4. **Business Logic**: Apply ZTE CDR specific business rules
     * 5. **Consistency Checks**: Verify overall record integrity
     * 
     * ## Cross-Field Validation Examples
     * 
     * - Call start time must be before end time
     * - Call duration must match calculated duration from timestamps
     * - Service type must be compatible with network type
     * - Billing amounts must be consistent with service usage
     * 
     * ## Performance Optimization
     * 
     * - Fail-fast approach for quick rejection of invalid records
     * - Batch validation optimizations for repeated constraint checks
     * - Caching of expensive validation operations where appropriate
     * 
     * @param record Complete ZTE CDR record to validate
     * @return ValidationResult with comprehensive validation status and error details
     * 
     * @see ZteCdrRecord for record structure definition
     * @see ValidationResult for validation outcome specifications
     */
    fun validateRecord(record: ZteCdrRecord): ValidationResult
    
    /**
     * Validates a batch of records efficiently.
     * 
     * This method provides optimized validation for multiple records simultaneously,
     * leveraging shared validation resources and constraints to improve performance
     * in high-volume processing scenarios. It maintains individual record
     * validation results while optimizing overall throughput.
     * 
     * ## Batch Optimizations
     * 
     * - Shared constraint loading and caching
     * - Parallel validation where thread-safe
     * - Early termination for critical validation failures
     * - Bulk constraint checking for similar values
     * 
     * ## Result Aggregation
     * 
     * Returns individual ValidationResult for each record plus aggregate
     * statistics about the batch validation process. This enables both
     * record-level error handling and batch-level monitoring.
     * 
     * @param records List of ZTE CDR records to validate
     * @return List of ValidationResult corresponding to input records
     */
    fun validateBatch(records: List&lt;ZteCdrRecord&gt;): List&lt;ValidationResult&gt;
    
    /**
     * Checks if validation rules are properly configured and operational.
     * 
     * This method verifies that all validation constraints, patterns, and rules
     * are properly loaded and configured. It can be used for health checks and
     * initialization verification to ensure the validator is ready for operation.
     * 
     * ## Configuration Checks
     * 
     * - Validation rule completeness for all ZTE CDR fields
     * - Pattern compilation and syntax validation
     * - Constraint range validity
     * - External dependency availability (lookup tables, etc.)
     * 
     * @return true if validator is properly configured and operational
     */
    fun isConfigurationValid(): Boolean
}