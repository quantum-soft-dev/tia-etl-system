package com.quantumsoft.tia.parsers.zte.asn1

import com.quantumsoft.tia.parsers.zte.core.model.DecodeResult
import com.quantumsoft.tia.parsers.zte.asn1.model.Asn1Element
import com.quantumsoft.tia.parsers.zte.asn1.model.ZteFieldTag

/**
 * Interface for ASN.1 BER/DER decoding operations following Interface Segregation Principle.
 * 
 * This interface focuses exclusively on low-level ASN.1 decoding operations,
 * separating ASN.1 structure parsing from ZTE-specific field interpretation.
 * This design allows for different ASN.1 decoder implementations while maintaining
 * a consistent interface for higher-level parsing components.
 * 
 * ## Design Rationale
 * 
 * - **Single Responsibility**: Only handles ASN.1 structure decoding
 * - **Interface Segregation**: Separate from field extraction and business logic
 * - **Open/Closed**: Extensible for different ASN.1 encoding variations
 * - **Dependency Inversion**: High-level parsers depend on this abstraction
 * 
 * ## ASN.1 Support Requirements
 * 
 * - BER (Basic Encoding Rules) decoding
 * - DER (Distinguished Encoding Rules) decoding  
 * - Primitive and constructed data types
 * - Definite and indefinite length encoding
 * - ZTE-specific tag extensions
 * 
 * ## Thread Safety
 * 
 * Implementations must be thread-safe for concurrent decoding operations.
 * Each method call should be independent and not rely on instance state
 * from previous operations.
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
interface Asn1Decoder {
    
    /**
     * Decodes ASN.1 encoded data into a structured element tree.
     * 
     * This method performs the core ASN.1 decoding operation, parsing the binary
     * ASN.1 data according to BER/DER rules and creating a hierarchical structure
     * of ASN.1 elements. The decoder handles both primitive and constructed types,
     * managing length encoding and nested structures appropriately.
     * 
     * ## Decoding Process
     * 
     * 1. Parse ASN.1 tag information (class, constructed flag, tag number)
     * 2. Decode length field (definite or indefinite length)
     * 3. Extract content octets based on length
     * 4. For constructed types, recursively decode child elements
     * 5. Build hierarchical Asn1Element structure
     * 
     * ## Error Handling
     * 
     * The method returns a DecodeResult that encapsulates either successful
     * decoding or detailed error information. Common error scenarios include:
     * 
     * - Malformed ASN.1 structure
     * - Invalid length encoding
     * - Truncated data
     * - Unsupported tag types
     * - Memory constraints exceeded
     * 
     * @param asn1Data Raw ASN.1 encoded binary data
     * @return DecodeResult containing either decoded Asn1Element tree or error details
     * 
     * @see Asn1Element for the decoded structure representation
     * @see DecodeResult for result type specifications
     */
    fun decodeAsn1Structure(asn1Data: ByteArray): DecodeResult&lt;Asn1Element&gt;
    
    /**
     * Extracts a specific field value by ZTE tag identifier.
     * 
     * This method provides direct access to specific fields within an already
     * decoded ASN.1 structure using ZTE-specific tag identifiers. It traverses
     * the ASN.1 element tree to locate the requested field and extract its
     * raw value for further processing.
     * 
     * ## Tag Resolution Process
     * 
     * 1. Traverse ASN.1 element tree structure
     * 2. Match ZTE field tag against element tags
     * 3. Handle context-specific tag variations
     * 4. Extract primitive value or constructed content
     * 5. Return raw bytes for field-specific interpretation
     * 
     * ## ZTE Tag Handling
     * 
     * ZTE CDR records use specific ASN.1 tag assignments for different field types.
     * The decoder must understand ZTE's tag encoding conventions and handle
     * both standard and proprietary tag extensions.
     * 
     * @param element Root ASN.1 element containing the field structure
     * @param fieldTag ZTE-specific field tag identifier to locate
     * @return DecodeResult containing field bytes or indication of field absence
     * 
     * @see ZteFieldTag for supported ZTE field tag definitions
     * @see Asn1Element for element tree structure
     */
    fun extractFieldValue(element: Asn1Element, fieldTag: ZteFieldTag): DecodeResult&lt;ByteArray&gt;
    
    /**
     * Validates ASN.1 structure integrity before processing.
     * 
     * This method performs structural validation on ASN.1 data to ensure it
     * conforms to basic ASN.1 encoding rules before attempting full decoding.
     * This allows for early detection of corrupted or malformed data.
     * 
     * ## Validation Checks
     * 
     * - Valid ASN.1 tag structure
     * - Consistent length encoding
     * - Complete data availability for declared lengths
     * - Nested structure integrity
     * - Basic format compliance
     * 
     * ## Performance Optimization
     * 
     * This validation is lightweight and designed for fast execution to avoid
     * performance impact on the main processing pipeline. It performs only
     * essential checks that prevent decoder errors or crashes.
     * 
     * @param asn1Data Raw ASN.1 data to validate
     * @return true if structure appears valid, false if corrupted or malformed
     */
    fun isValidAsn1Structure(asn1Data: ByteArray): Boolean
}