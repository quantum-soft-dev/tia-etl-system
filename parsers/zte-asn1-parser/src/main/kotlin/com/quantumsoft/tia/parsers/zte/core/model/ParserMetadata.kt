package com.quantumsoft.tia.parsers.zte.core.model

import java.time.Instant

/**
 * Metadata information about the ZTE ASN.1 parser capabilities and configuration.
 * 
 * This data class provides comprehensive information about the parser's capabilities,
 * supported formats, performance characteristics, and operational parameters.
 * It is used by the parser orchestrator for routing decisions and monitoring.
 * 
 * ## Design Principles
 * 
 * - **Immutability**: Metadata is immutable once created
 * - **Completeness**: Contains all information needed for parser selection
 * - **Versioning**: Supports versioning for compatibility management
 * - **Self-Description**: Parser describes its own capabilities
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
data class ParserMetadata(
    
    // === Basic Parser Information ===
    
    /**
     * Unique identifier for this parser implementation.
     * Used for parser selection and routing by the orchestrator.
     */
    val parserId: String,
    
    /**
     * Human-readable parser name and description.
     * Used for monitoring dashboards and operational documentation.
     */
    val parserName: String,
    
    /**
     * Parser version following semantic versioning.
     * Format: MAJOR.MINOR.PATCH (e.g., "1.2.3")
     */
    val parserVersion: String,
    
    /**
     * Parser description explaining its purpose and capabilities.
     * Used for documentation and operational guidance.
     */
    val description: String,
    
    /**
     * Author or organization responsible for this parser.
     * Used for support and maintenance contact information.
     */
    val author: String = "TIA ETL Team",
    
    // === Supported Formats and Capabilities ===
    
    /**
     * List of supported ZTE CDR format versions.
     * Identifies which ZTE equipment versions this parser can handle.
     */
    val supportedCdrVersions: List&lt;String&gt;,
    
    /**
     * List of supported ASN.1 encoding types.
     * Typically includes BER, DER, and any ZTE-specific variants.
     */
    val supportedEncodings: List&lt;String&gt;,
    
    /**
     * List of supported service types.
     * Voice, SMS, Data, Video, etc. that this parser can process.
     */
    val supportedServiceTypes: List&lt;String&gt;,
    
    /**
     * Maximum file size this parser can handle (bytes).
     * Used for routing decisions to prevent memory issues.
     */
    val maxFileSizeBytes: Long,
    
    /**
     * Maximum number of records per file this parser can handle.
     * Used for capacity planning and load balancing.
     */
    val maxRecordsPerFile: Long,
    
    // === Performance Characteristics ===
    
    /**
     * Expected processing rate in records per second.
     * Used for capacity planning and SLA management.
     */
    val expectedThroughputRecordsPerSecond: Long,
    
    /**
     * Estimated memory usage per record (bytes).
     * Used for memory capacity planning and resource allocation.
     */
    val estimatedMemoryPerRecordBytes: Long,
    
    /**
     * Expected parsing time per record (milliseconds).
     * Used for timeout configuration and performance monitoring.
     */
    val expectedParsingTimePerRecordMs: Long,
    
    /**
     * Indicates if this parser supports parallel processing.
     * Used to determine optimal thread allocation.
     */
    val supportsParallelProcessing: Boolean,
    
    /**
     * Optimal thread count for parallel processing.
     * Recommended number of threads for best performance.
     */
    val optimalThreadCount: Int = 1,
    
    // === Field Mapping and Schema ===
    
    /**
     * Complete field mapping from ASN.1 tags to CDR fields.
     * Used for validation and documentation purposes.
     */
    val fieldMappings: Map&lt;String, FieldMappingInfo&gt;,
    
    /**
     * Target ClickHouse table schema information.
     * Used for schema validation and database routing.
     */
    val targetSchema: SchemaInfo,
    
    /**
     * List of required fields that must be present in every record.
     * Used for validation and data quality checks.
     */
    val requiredFields: List&lt;String&gt;,
    
    /**
     * List of optional fields that may be present in records.
     * Used for validation and completeness assessment.
     */
    val optionalFields: List&lt;String&gt;,
    
    // === Quality and Reliability ===
    
    /**
     * Parser reliability rating (0.0 to 1.0).
     * Based on testing results and production experience.
     */
    val reliabilityScore: Double,
    
    /**
     * Data quality score for parsed records (0.0 to 1.0).
     * Indicates expected accuracy and completeness.
     */
    val dataQualityScore: Double,
    
    /**
     * Test coverage percentage for this parser.
     * Indicates confidence level in parser behavior.
     */
    val testCoveragePercentage: Double,
    
    /**
     * List of known limitations or issues.
     * Used for operational awareness and planning.
     */
    val knownLimitations: List&lt;String&gt; = emptyList(),
    
    // === Operational Information ===
    
    /**
     * Creation timestamp of this metadata instance.
     * Used for version tracking and cache management.
     */
    val createdAt: Instant = Instant.now(),
    
    /**
     * Last update timestamp for this metadata.
     * Used for determining if metadata needs refresh.
     */
    val lastUpdated: Instant = Instant.now(),
    
    /**
     * Build information for this parser version.
     * Used for debugging and support purposes.
     */
    val buildInfo: BuildInfo,
    
    /**
     * Configuration parameters that affect parser behavior.
     * Used for operational tuning and troubleshooting.
     */
    val configurationParameters: Map&lt;String, Any&gt; = emptyMap(),
    
    /**
     * Health check endpoint information.
     * Used for monitoring and operational health checks.
     */
    val healthCheckInfo: HealthCheckInfo? = null
) {
    
    /**
     * Checks if this parser can handle the specified CDR version.
     * 
     * @param cdrVersion ZTE CDR format version to check
     * @return true if this parser supports the specified version
     */
    fun supportsCdrVersion(cdrVersion: String): Boolean {
        return supportedCdrVersions.contains(cdrVersion) || 
               supportedCdrVersions.any { it.startsWith(cdrVersion.substringBefore('.')) }
    }
    
    /**
     * Checks if this parser can handle the specified service type.
     * 
     * @param serviceType Service type to check (VOICE, SMS, DATA, etc.)
     * @return true if this parser supports the specified service type
     */
    fun supportsServiceType(serviceType: String): Boolean {
        return supportedServiceTypes.contains(serviceType.uppercase()) ||
               supportedServiceTypes.contains("ALL")
    }
    
    /**
     * Checks if a file is within this parser's processing capabilities.
     * 
     * @param fileSizeBytes Size of the file to process
     * @param estimatedRecordCount Estimated number of records in the file
     * @return true if the file is within processing limits
     */
    fun canHandle(fileSizeBytes: Long, estimatedRecordCount: Long): Boolean {
        return fileSizeBytes &lt;= maxFileSizeBytes && estimatedRecordCount &lt;= maxRecordsPerFile
    }
    
    /**
     * Calculates estimated processing time for a given workload.
     * 
     * @param recordCount Number of records to process
     * @return Estimated processing time in milliseconds
     */
    fun estimateProcessingTime(recordCount: Long): Long {
        return if (supportsParallelProcessing && optimalThreadCount > 1) {
            (recordCount * expectedParsingTimePerRecordMs) / optimalThreadCount
        } else {
            recordCount * expectedParsingTimePerRecordMs
        }
    }
    
    /**
     * Calculates estimated memory usage for a given workload.
     * 
     * @param recordCount Number of records to process
     * @return Estimated memory usage in bytes
     */
    fun estimateMemoryUsage(recordCount: Long): Long {
        return recordCount * estimatedMemoryPerRecordBytes
    }
    
    /**
     * Gets a compatibility score with another parser version.
     * 
     * @param otherVersion Version string to compare against
     * @return Compatibility score from 0.0 (incompatible) to 1.0 (fully compatible)
     */
    fun getCompatibilityScore(otherVersion: String): Double {
        val thisVersion = parserVersion.split(".")
        val otherVersionParts = otherVersion.split(".")
        
        if (thisVersion.isEmpty() || otherVersionParts.isEmpty()) return 0.0
        
        // Major version compatibility
        if (thisVersion[0] != otherVersionParts[0]) return 0.0
        
        // Minor version compatibility
        if (thisVersion.size > 1 && otherVersionParts.size > 1) {
            if (thisVersion[1] != otherVersionParts[1]) return 0.7
        }
        
        // Full compatibility for same major.minor version
        return 1.0
    }
    
    /**
     * Creates a summary string for logging and monitoring.
     * 
     * @return Concise metadata summary
     */
    fun toSummary(): String {
        return "Parser[$parserId v$parserVersion, CDR versions: ${supportedCdrVersions.joinToString()}, " +
               "throughput: ${expectedThroughputRecordsPerSecond}/sec, reliability: ${(reliabilityScore * 100).toInt()}%]"
    }
}

/**
 * Information about ASN.1 field mapping to CDR record fields.
 * 
 * @param asn1Tag ASN.1 tag identifier
 * @param fieldName Target field name in ZteCdrRecord
 * @param fieldType Expected data type for the field
 * @param isRequired Whether this field is required in every record
 * @param description Human-readable field description
 * @param validationRules Validation rules for this field
 */
data class FieldMappingInfo(
    val asn1Tag: String,
    val fieldName: String,
    val fieldType: String,
    val isRequired: Boolean,
    val description: String,
    val validationRules: List&lt;String&gt; = emptyList()
)

/**
 * Target database schema information.
 * 
 * @param tableName ClickHouse table name
 * @param schemaVersion Schema version for compatibility checking
 * @param partitionKey Partitioning configuration
 * @param indexes List of indexes for performance optimization
 * @param constraints Data constraints and validation rules
 */
data class SchemaInfo(
    val tableName: String,
    val schemaVersion: String,
    val partitionKey: String,
    val indexes: List&lt;String&gt; = emptyList(),
    val constraints: List&lt;String&gt; = emptyList()
)

/**
 * Build information for traceability and debugging.
 * 
 * @param buildNumber Unique build identifier
 * @param buildTimestamp When this build was created
 * @param gitCommit Git commit hash for source traceability
 * @param gitBranch Git branch used for the build
 * @param javaVersion Java/Kotlin version used for compilation
 * @param dependencies List of key dependencies and versions
 */
data class BuildInfo(
    val buildNumber: String,
    val buildTimestamp: Instant,
    val gitCommit: String,
    val gitBranch: String,
    val javaVersion: String,
    val dependencies: Map&lt;String, String&gt; = emptyMap()
)

/**
 * Health check configuration for operational monitoring.
 * 
 * @param endpoint Health check endpoint URL
 * @param intervalSeconds How frequently to perform health checks
 * @param timeoutSeconds Timeout for health check operations
 * @param criticalThresholds Thresholds for critical health indicators
 */
data class HealthCheckInfo(
    val endpoint: String,
    val intervalSeconds: Long = 30,
    val timeoutSeconds: Long = 10,
    val criticalThresholds: Map&lt;String, Double&gt; = emptyMap()
)