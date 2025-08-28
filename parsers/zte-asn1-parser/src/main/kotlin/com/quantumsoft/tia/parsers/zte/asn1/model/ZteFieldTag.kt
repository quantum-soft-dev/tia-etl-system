package com.quantumsoft.tia.parsers.zte.asn1.model

/**
 * Enumeration of ZTE-specific ASN.1 field tags used in CDR records.
 * 
 * This enum defines the ASN.1 tag mappings for ZTE Call Detail Record fields
 * as specified in the ZTE ASN.1 CDR specification. Each enum value represents
 * a specific field in the CDR structure with its corresponding ASN.1 tag.
 * 
 * ## Tag Structure
 * 
 * ZTE uses context-specific tags for CDR fields, following the pattern:
 * - Context-specific tag class
 * - Sequential tag numbering starting from 0
 * - Both primitive and constructed types supported
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
enum class ZteFieldTag(
    /**
     * ASN.1 tag for this field in ZTE CDR structure.
     */
    val asn1Tag: Asn1Tag,
    
    /**
     * Target field name in ZteCdrRecord data class.
     */
    val fieldName: String,
    
    /**
     * Human-readable description of this field.
     */
    val description: String,
    
    /**
     * Whether this field is required in every CDR record.
     */
    val isRequired: Boolean = false,
    
    /**
     * Expected data type for this field.
     */
    val expectedType: String = "String"
) {
    
    // === Identity and Control Fields ===
    
    /**
     * Unique call identifier - primary key for call tracking.
     */
    CALL_ID(
        asn1Tag = Asn1Tag.zteContext(0),
        fieldName = "callId",
        description = "Unique call identifier",
        isRequired = true,
        expectedType = "String"
    ),
    
    /**
     * Global call identifier for multi-leg call correlation.
     */
    GLOBAL_CALL_ID(
        asn1Tag = Asn1Tag.zteContext(1),
        fieldName = "globalCallId",
        description = "Global unique call identifier",
        isRequired = false,
        expectedType = "String"
    ),
    
    /**
     * Sequence number for multi-record calls.
     */
    SEQUENCE_NUMBER(
        asn1Tag = Asn1Tag.zteContext(2),
        fieldName = "sequenceNumber",
        description = "Record sequence number",
        isRequired = false,
        expectedType = "Long"
    ),
    
    /**
     * CDR record type identifier.
     */
    RECORD_TYPE(
        asn1Tag = Asn1Tag.zteContext(3),
        fieldName = "recordType",
        description = "CDR record type",
        isRequired = true,
        expectedType = "String"
    ),
    
    // === Subscriber Information ===
    
    /**
     * Mobile Station ISDN number (calling party).
     */
    MSISDN(
        asn1Tag = Asn1Tag.zteContext(10),
        fieldName = "msisdn",
        description = "Calling party MSISDN",
        isRequired = true,
        expectedType = "String"
    ),
    
    /**
     * International Mobile Subscriber Identity.
     */
    IMSI(
        asn1Tag = Asn1Tag.zteContext(11),
        fieldName = "imsi",
        description = "Calling party IMSI",
        isRequired = true,
        expectedType = "String"
    ),
    
    /**
     * Called party number (destination).
     */
    CALLED_NUMBER(
        asn1Tag = Asn1Tag.zteContext(12),
        fieldName = "calledNumber",
        description = "Called party number",
        isRequired = true,
        expectedType = "String"
    ),
    
    /**
     * Called party MSISDN (for mobile-to-mobile calls).
     */
    CALLED_MSISDN(
        asn1Tag = Asn1Tag.zteContext(13),
        fieldName = "calledMsisdn",
        description = "Called party MSISDN",
        isRequired = false,
        expectedType = "String"
    ),
    
    /**
     * Called party IMSI (for mobile-to-mobile calls).
     */
    CALLED_IMSI(
        asn1Tag = Asn1Tag.zteContext(14),
        fieldName = "calledImsi",
        description = "Called party IMSI",
        isRequired = false,
        expectedType = "String"
    ),
    
    // === Network Information ===
    
    /**
     * Mobile Switching Center address.
     */
    MSC_ADDRESS(
        asn1Tag = Asn1Tag.zteContext(20),
        fieldName = "mscAddress",
        description = "MSC address handling the call",
        isRequired = true,
        expectedType = "String"
    ),
    
    /**
     * Visited Location Register number.
     */
    VLR_NUMBER(
        asn1Tag = Asn1Tag.zteContext(21),
        fieldName = "vlrNumber",
        description = "VLR number",
        isRequired = false,
        expectedType = "String"
    ),
    
    /**
     * Cell identifier for location information.
     */
    CELL_ID(
        asn1Tag = Asn1Tag.zteContext(22),
        fieldName = "cellId",
        description = "Originating cell identifier",
        isRequired = false,
        expectedType = "String"
    ),
    
    /**
     * Location Area Code.
     */
    LOCATION_AREA_CODE(
        asn1Tag = Asn1Tag.zteContext(23),
        fieldName = "locationAreaCode",
        description = "Location Area Code",
        isRequired = false,
        expectedType = "String"
    ),
    
    /**
     * Routing number for call setup.
     */
    ROUTING_NUMBER(
        asn1Tag = Asn1Tag.zteContext(24),
        fieldName = "routingNumber",
        description = "Call routing number",
        isRequired = false,
        expectedType = "String"
    ),
    
    // === Service Classification ===
    
    /**
     * Service type (VOICE, SMS, DATA, etc.).
     */
    SERVICE_TYPE(
        asn1Tag = Asn1Tag.zteContext(30),
        fieldName = "serviceType",
        description = "Type of service",
        isRequired = true,
        expectedType = "String"
    ),
    
    /**
     * Call type (MOC, MTC, etc.).
     */
    CALL_TYPE(
        asn1Tag = Asn1Tag.zteContext(31),
        fieldName = "callType",
        description = "Call type classification",
        isRequired = true,
        expectedType = "String"
    ),
    
    /**
     * Bearer service type.
     */
    BEARER_SERVICE(
        asn1Tag = Asn1Tag.zteContext(32),
        fieldName = "bearerService",
        description = "Bearer service type",
        isRequired = false,
        expectedType = "String"
    ),
    
    /**
     * Teleservice type.
     */
    TELE_SERVICE(
        asn1Tag = Asn1Tag.zteContext(33),
        fieldName = "teleService",
        description = "Teleservice type",
        isRequired = false,
        expectedType = "String"
    ),
    
    // === Timing Information ===
    
    /**
     * Call setup initiation time.
     */
    CALL_SETUP_TIME(
        asn1Tag = Asn1Tag.zteContext(40),
        fieldName = "callSetupTime",
        description = "Call setup timestamp",
        isRequired = true,
        expectedType = "Instant"
    ),
    
    /**
     * Call answer time (conversation start).
     */
    CALL_ANSWER_TIME(
        asn1Tag = Asn1Tag.zteContext(41),
        fieldName = "callAnswerTime",
        description = "Call answer timestamp",
        isRequired = false,
        expectedType = "Instant"
    ),
    
    /**
     * Call termination time.
     */
    CALL_END_TIME(
        asn1Tag = Asn1Tag.zteContext(42),
        fieldName = "callEndTime",
        description = "Call end timestamp",
        isRequired = true,
        expectedType = "Instant"
    ),
    
    /**
     * Total call duration in seconds.
     */
    CALL_DURATION(
        asn1Tag = Asn1Tag.zteContext(43),
        fieldName = "callDuration",
        description = "Call duration in seconds",
        isRequired = true,
        expectedType = "Long"
    ),
    
    /**
     * Call setup duration (setup to answer).
     */
    SETUP_DURATION(
        asn1Tag = Asn1Tag.zteContext(44),
        fieldName = "setupDuration",
        description = "Setup duration in seconds",
        isRequired = false,
        expectedType = "Long"
    ),
    
    // === Billing Information ===
    
    /**
     * Duration used for billing calculation.
     */
    CHARGED_DURATION(
        asn1Tag = Asn1Tag.zteContext(50),
        fieldName = "chargedDuration",
        description = "Charged duration in seconds",
        isRequired = false,
        expectedType = "Long"
    ),
    
    /**
     * Basic service charge amount.
     */
    BASIC_SERVICE_CHARGE(
        asn1Tag = Asn1Tag.zteContext(51),
        fieldName = "basicServiceCharge",
        description = "Basic service charge",
        isRequired = false,
        expectedType = "BigDecimal"
    ),
    
    /**
     * Additional service charges.
     */
    ADDITIONAL_SERVICE_CHARGE(
        asn1Tag = Asn1Tag.zteContext(52),
        fieldName = "additionalServiceCharge",
        description = "Additional service charges",
        isRequired = false,
        expectedType = "BigDecimal"
    ),
    
    /**
     * Total charge for the call.
     */
    TOTAL_CHARGE(
        asn1Tag = Asn1Tag.zteContext(53),
        fieldName = "totalCharge",
        description = "Total call charge",
        isRequired = false,
        expectedType = "BigDecimal"
    ),
    
    /**
     * Currency code for charges.
     */
    CHARGE_CURRENCY(
        asn1Tag = Asn1Tag.zteContext(54),
        fieldName = "chargeCurrency",
        description = "Charge currency code",
        isRequired = false,
        expectedType = "String"
    ),
    
    // === Quality and Status ===
    
    /**
     * Call completion status.
     */
    CALL_STATUS(
        asn1Tag = Asn1Tag.zteContext(60),
        fieldName = "callStatus",
        description = "Call completion status",
        isRequired = true,
        expectedType = "String"
    ),
    
    /**
     * Call termination cause.
     */
    TERMINATION_CAUSE(
        asn1Tag = Asn1Tag.zteContext(61),
        fieldName = "terminationCause",
        description = "Call termination cause",
        isRequired = false,
        expectedType = "String"
    ),
    
    /**
     * Quality of Service indicator.
     */
    QUALITY_OF_SERVICE(
        asn1Tag = Asn1Tag.zteContext(62),
        fieldName = "qualityOfService",
        description = "Quality of Service indicator",
        isRequired = false,
        expectedType = "String"
    ),
    
    /**
     * Signal strength measurement.
     */
    SIGNAL_STRENGTH(
        asn1Tag = Asn1Tag.zteContext(63),
        fieldName = "signalStrength",
        description = "Signal strength in dBm",
        isRequired = false,
        expectedType = "Int"
    ),
    
    // === Technical Information ===
    
    /**
     * Equipment identity (IMEI) of calling device.
     */
    EQUIPMENT_IDENTITY(
        asn1Tag = Asn1Tag.zteContext(70),
        fieldName = "equipmentIdentity",
        description = "Device equipment identity",
        isRequired = false,
        expectedType = "String"
    ),
    
    /**
     * Supplementary services used.
     */
    SUPPLEMENTARY_SERVICES(
        asn1Tag = Asn1Tag.zteContext(71),
        fieldName = "supplementaryServices",
        description = "Supplementary services",
        isRequired = false,
        expectedType = "String"
    ),
    
    /**
     * Network element software version.
     */
    NETWORK_ELEMENT_VERSION(
        asn1Tag = Asn1Tag.zteContext(72),
        fieldName = "networkElementVersion",
        description = "Network element software version",
        isRequired = false,
        expectedType = "String"
    ),
    
    // === Record Metadata ===
    
    /**
     * Record generation timestamp.
     */
    RECORD_GENERATION_TIME(
        asn1Tag = Asn1Tag.zteContext(80),
        fieldName = "recordGenerationTime",
        description = "Record generation timestamp",
        isRequired = true,
        expectedType = "Instant"
    );
    
    companion object {
        /**
         * Map of ASN.1 tags to ZTE field tags for efficient lookup.
         */
        private val tagMap: Map&lt;Asn1Tag, ZteFieldTag&gt; = values().associateBy { it.asn1Tag }
        
        /**
         * Map of field names to ZTE field tags for efficient lookup.
         */
        private val fieldNameMap: Map&lt;String, ZteFieldTag&gt; = values().associateBy { it.fieldName }
        
        /**
         * Gets the ZTE field tag for a given ASN.1 tag.
         * 
         * @param asn1Tag ASN.1 tag to look up
         * @return Corresponding ZTE field tag or null if not found
         */
        fun fromAsn1Tag(asn1Tag: Asn1Tag): ZteFieldTag? = tagMap[asn1Tag]
        
        /**
         * Gets the ZTE field tag for a given field name.
         * 
         * @param fieldName Field name to look up
         * @return Corresponding ZTE field tag or null if not found
         */
        fun fromFieldName(fieldName: String): ZteFieldTag? = fieldNameMap[fieldName]
        
        /**
         * Gets all required field tags.
         * 
         * @return List of field tags that are required in every CDR
         */
        fun getRequiredFields(): List&lt;ZteFieldTag&gt; = values().filter { it.isRequired }
        
        /**
         * Gets all optional field tags.
         * 
         * @return List of field tags that are optional in CDRs
         */
        fun getOptionalFields(): List&lt;ZteFieldTag&gt; = values().filter { !it.isRequired }
        
        /**
         * Gets field tags by expected data type.
         * 
         * @param expectedType Data type to filter by
         * @return List of field tags with the specified expected type
         */
        fun getFieldsByType(expectedType: String): List&lt;ZteFieldTag&gt; {
            return values().filter { it.expectedType == expectedType }
        }
    }
}