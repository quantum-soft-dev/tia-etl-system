package com.quantumsoft.tia.parsers.zte.core

import com.quantumsoft.tia.parsers.zte.core.model.ParseResult
import com.quantumsoft.tia.parsers.zte.core.model.ValidationResult
import com.quantumsoft.tia.parsers.zte.core.model.ZteCdrRecord

/**
 * Core interface for ZTE ASN.1 CDR parsing following Single Responsibility Principle.
 * 
 * This interface defines the contract for parsing ZTE ASN.1 encoded Call Detail Records.
 * It focuses solely on the parsing responsibility, delegating validation and storage
 * to other specialized interfaces.
 * 
 * ## Design Principles
 * 
 * - **Single Responsibility**: Only handles ASN.1 parsing operations
 * - **Open/Closed**: Extensible for new ZTE CDR formats without modification
 * - **Liskov Substitution**: All implementations must be fully substitutable
 * - **Interface Segregation**: Clients depend only on parsing operations
 * - **Dependency Inversion**: Depends on abstractions, not concrete implementations
 * 
 * ## Thread Safety
 * 
 * Implementations must be thread-safe for concurrent processing of different records.
 * However, individual parsing operations are not required to be atomic across
 * multiple method calls.
 * 
 * ## Error Handling
 * 
 * All errors are communicated through the ParseResult sealed class rather than
 * exceptions. This provides explicit error handling and prevents unexpected
 * runtime failures.
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
interface ZteAsn1Parser {
    
    /**
     * Parses a single ZTE CDR record from ASN.1 encoded data.
     * 
     * This method decodes ASN.1 BER/DER encoded bytes into a structured ZTE CDR record.
     * The implementation should handle all ZTE-specific ASN.1 tags and field mappings
     * while providing detailed error information for malformed data.
     * 
     * ## Processing Flow
     * 
     * 1. Validate input ASN.1 data format
     * 2. Decode ASN.1 structure using BER/DER rules
     * 3. Extract ZTE-specific fields according to CDR specification
     * 4. Map fields to ZteCdrRecord structure
     * 5. Return parsing result with success/failure information
     * 
     * ## Performance Requirements
     * 
     * - Must handle individual records within 1ms on average
     * - Memory usage should not exceed 1MB per record
     * - Should be optimized for batch processing scenarios
     * 
     * @param asn1Data Raw ASN.1 encoded data as byte array
     * @return ParseResult containing either successful ZteCdrRecord or detailed error information
     * 
     * @see ParseResult for result type specifications
     * @see ZteCdrRecord for the target data structure
     */
    fun parseRecord(asn1Data: ByteArray): ParseResult&lt;ZteCdrRecord&gt;
    
    /**
     * Validates that a parsed ZTE CDR record meets structural requirements.
     * 
     * This method performs structure-level validation on an already parsed record,
     * checking for required fields, data consistency, and ZTE-specific business rules.
     * It does not perform deep field validation - that's handled by DataValidator.
     * 
     * ## Validation Scope
     * 
     * - Required field presence validation
     * - Cross-field consistency checks (e.g., start time &lt; end time)  
     * - ZTE CDR format compliance
     * - Record completeness verification
     * 
     * ## Separation of Concerns
     * 
     * This validation focuses on record structure and consistency. Individual field
     * format validation (MSISDN format, date ranges, etc.) is handled by the
     * DataValidator interface to maintain single responsibility.
     * 
     * @param record Parsed ZTE CDR record to validate
     * @return ValidationResult indicating success or specific validation failures
     * 
     * @see ValidationResult for validation result specifications
     * @see DataValidator for field-level validation
     */
    fun validateRecord(record: ZteCdrRecord): ValidationResult
    
    /**
     * Retrieves metadata about this parser's capabilities and configuration.
     * 
     * Returns static information about what this parser can handle, including
     * supported ZTE CDR versions, field mappings, and processing capabilities.
     * This metadata is used by the parser orchestrator for routing decisions.
     * 
     * ## Metadata Contents
     * 
     * - Supported ZTE CDR format versions
     * - Field mapping documentation
     * - Performance characteristics
     * - Parser version and build information
     * - Target ClickHouse schema information
     * 
     * @return ParserMetadata with complete parser capability information
     */
    fun getParserMetadata(): ParserMetadata
}