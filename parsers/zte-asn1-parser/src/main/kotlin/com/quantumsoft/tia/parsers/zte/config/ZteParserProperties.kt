package com.quantumsoft.tia.parsers.zte.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.validation.annotation.Validated
import java.time.Duration
import jakarta.validation.Valid
import jakarta.validation.constraints.*

/**
 * Configuration properties for ZTE ASN.1 parser with comprehensive validation.
 * 
 * This configuration class provides externalized configuration for all aspects
 * of ZTE ASN.1 parser operation, including ASN.1 decoding, validation rules,
 * database operations, and performance tuning. All properties are validated
 * to ensure system reliability and proper configuration.
 * 
 * ## Configuration Structure
 * 
 * - **parser**: Core parser behavior and performance settings
 * - **asn1**: ASN.1 decoding configuration and limits
 * - **validation**: Data validation rules and policies
 * - **database**: ClickHouse connection and operation settings
 * - **metrics**: Performance monitoring and metrics collection
 * 
 * ## Property Validation
 * 
 * All properties include comprehensive validation constraints to prevent
 * configuration errors that could cause runtime failures or performance issues.
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "zte.parser")
@Validated
data class ZteParserProperties(
    
    /**
     * Core parser configuration settings.
     */
    @field:Valid
    @NestedConfigurationProperty
    val parser: ParserConfig = ParserConfig(),
    
    /**
     * ASN.1 decoding configuration and limits.
     */
    @field:Valid
    @NestedConfigurationProperty
    val asn1: Asn1Config = Asn1Config(),
    
    /**
     * Data validation rules and behavior.
     */
    @field:Valid
    @NestedConfigurationProperty
    val validation: ValidationConfig = ValidationConfig(),
    
    /**
     * ClickHouse database configuration.
     */
    @field:Valid
    @NestedConfigurationProperty
    val database: DatabaseConfig = DatabaseConfig(),
    
    /**
     * Metrics collection and monitoring configuration.
     */
    @field:Valid
    @NestedConfigurationProperty
    val metrics: MetricsConfig = MetricsConfig()
)

/**
 * Core parser behavior and performance configuration.
 * 
 * Controls fundamental parser operation including parsing strictness,
 * memory management, and performance optimizations.
 */
data class ParserConfig(
    
    /**
     * Enable strict parsing mode with comprehensive validation.
     * 
     * When enabled, the parser performs additional validation checks
     * that may impact performance but improve data quality assurance.
     */
    val strictParsing: Boolean = true,
    
    /**
     * Enable field value caching for repeated parsing operations.
     * 
     * Caching can improve performance for similar record structures
     * but increases memory usage. Disable for memory-constrained environments.
     */
    val enableFieldCaching: Boolean = true,
    
    /**
     * Maximum number of parsed records to keep in memory simultaneously.
     * 
     * Controls memory usage during batch processing. Lower values reduce
     * memory usage but may impact throughput for small record batches.
     */
    @field:Min(100)
    @field:Max(100000)
    val maxRecordsInMemory: Int = 10000,
    
    /**
     * Thread pool size for parallel processing operations.
     * 
     * Number of threads available for parallel parsing operations.
     * Should be tuned based on available CPU cores and I/O characteristics.
     */
    @field:Min(1)
    @field:Max(32)
    val threadPoolSize: Int = 4,
    
    /**
     * Maximum memory usage per parsing operation (MB).
     * 
     * Hard limit on memory usage to prevent OutOfMemoryError during
     * processing of unusually large ASN.1 structures.
     */
    @field:Min(64)
    @field:Max(2048)
    val maxMemoryMb: Long = 512
)

/**
 * ASN.1 decoding configuration and security limits.
 * 
 * Controls ASN.1 decoder behavior including validation strictness,
 * security limits, and error handling policies.
 */
data class Asn1Config(
    
    /**
     * Enable strict ASN.1 structure validation.
     * 
     * When enabled, the decoder performs comprehensive validation of
     * ASN.1 structure integrity before attempting to parse content.
     */
    val strictValidation: Boolean = true,
    
    /**
     * Maximum depth of nested ASN.1 structures.
     * 
     * Prevents stack overflow attacks and processing of maliciously
     * crafted ASN.1 data with excessive nesting depth.
     */
    @field:Min(10)
    @field:Max(100)
    val maxStructureDepth: Int = 50,
    
    /**
     * Maximum length of individual ASN.1 field values (bytes).
     * 
     * Prevents memory exhaustion from processing ASN.1 structures
     * with extremely large field values.
     */
    @field:Min(1024)
    @field:Max(10485760) // 10MB
    val maxFieldLength: Long = 1048576, // 1MB
    
    /**
     * Timeout for ASN.1 decoding operations (seconds).
     * 
     * Maximum time allowed for decoding a single ASN.1 structure
     * to prevent indefinite processing of malformed data.
     */
    @field:Min(1)
    @field:Max(300) // 5 minutes
    val decodingTimeoutSeconds: Long = 60,
    
    /**
     * Enable recovery from ASN.1 parsing errors.
     * 
     * When enabled, the decoder attempts to continue parsing after
     * encountering non-critical ASN.1 structure errors.
     */
    val enableErrorRecovery: Boolean = false
)

/**
 * Data validation configuration and business rules.
 * 
 * Controls validation behavior including rule enforcement,
 * error handling, and performance optimization settings.
 */
data class ValidationConfig(
    
    /**
     * Validation rules configuration mapped by field name.
     * 
     * Defines validation patterns, constraints, and business rules
     * for each ZTE CDR field type.
     */
    val rules: Map&lt;String, ValidationRuleConfig&gt; = mapOf(
        "msisdn" to ValidationRuleConfig(
            pattern = "^[0-9]{10,15}$",
            required = true,
            minLength = 10,
            maxLength = 15
        ),
        "imsi" to ValidationRuleConfig(
            pattern = "^[0-9]{15}$",
            required = true,
            minLength = 15,
            maxLength = 15
        ),
        "callDuration" to ValidationRuleConfig(
            required = true,
            minValue = 0,
            maxValue = 86400 // 24 hours in seconds
        )
    ),
    
    /**
     * Enable strict validation mode with comprehensive rule checking.
     * 
     * When enabled, validation failures result in record rejection.
     * When disabled, validation warnings are logged but processing continues.
     */
    val strictMode: Boolean = true,
    
    /**
     * Enable cross-field validation for business rules.
     * 
     * Validates relationships between fields (e.g., start time < end time)
     * in addition to individual field validation.
     */
    val enableCrossFieldValidation: Boolean = true,
    
    /**
     * Maximum number of validation errors to collect per record.
     * 
     * Limits memory usage and processing time for severely invalid records
     * by stopping validation after reaching the error limit.
     */
    @field:Min(1)
    @field:Max(100)
    val maxValidationErrors: Int = 10,
    
    /**
     * Timeout for validation operations (seconds).
     * 
     * Maximum time allowed for validating a single record to prevent
     * indefinite processing of complex validation scenarios.
     */
    @field:Min(1)
    @field:Max(60)
    val validationTimeoutSeconds: Long = 10
)

/**
 * Validation rule configuration for individual fields.
 * 
 * Defines validation constraints and patterns that can be applied
 * to specific CDR fields during data validation.
 */
data class ValidationRuleConfig(
    
    /**
     * Regular expression pattern for format validation.
     */
    val pattern: String? = null,
    
    /**
     * Whether this field is required and cannot be null/empty.
     */
    val required: Boolean = false,
    
    /**
     * Minimum string length or numeric value.
     */
    val minLength: Int? = null,
    val minValue: Long? = null,
    
    /**
     * Maximum string length or numeric value.
     */
    val maxLength: Int? = null,
    val maxValue: Long? = null,
    
    /**
     * List of valid enumeration values.
     */
    val validValues: List&lt;String&gt; = emptyList(),
    
    /**
     * Custom validation rule identifier for complex business logic.
     */
    val customRule: String? = null
)

/**
 * ClickHouse database configuration and connection settings.
 * 
 * Controls database connection pooling, transaction behavior,
 * and performance optimization for ClickHouse operations.
 */
data class DatabaseConfig(
    
    /**
     * Batch size for database insert operations.
     * 
     * Optimal batch size balances memory usage and insertion performance.
     * Larger batches improve throughput but increase memory requirements.
     */
    @field:Min(100)
    @field:Max(50000)
    val batchSize: Int = 10000,
    
    /**
     * Connection timeout for database operations (seconds).
     * 
     * Maximum time to wait for database connection establishment
     * before timing out and retrying or failing.
     */
    @field:Min(5)
    @field:Max(300)
    val connectionTimeout: Duration = Duration.ofSeconds(30),
    
    /**
     * Query timeout for database operations (seconds).
     * 
     * Maximum time to wait for query execution before timing out.
     * Should be set based on expected data volumes and system performance.
     */
    @field:Min(30)
    @field:Max(1800) // 30 minutes
    val queryTimeout: Duration = Duration.ofSeconds(300), // 5 minutes
    
    /**
     * Enable comprehensive schema validation before insertion.
     * 
     * When enabled, all records are validated against ClickHouse
     * table schema before insertion to prevent constraint violations.
     */
    val enableSchemaValidation: Boolean = true,
    
    /**
     * Maximum retry attempts for failed database operations.
     * 
     * Number of retry attempts for transient database failures
     * before giving up and reporting the operation as failed.
     */
    @field:Min(0)
    @field:Max(10)
    val maxRetryAttempts: Int = 3,
    
    /**
     * Retry delay between failed database operations (seconds).
     * 
     * Base delay between retry attempts, with exponential backoff
     * applied for subsequent retries.
     */
    @field:Min(1)
    @field:Max(60)
    val retryDelaySeconds: Long = 5
)

/**
 * Metrics collection and monitoring configuration.
 * 
 * Controls performance metrics collection, export configuration,
 * and monitoring behavior for observability and system optimization.
 */
data class MetricsConfig(
    
    /**
     * Enable detailed performance metrics collection.
     * 
     * When enabled, collects comprehensive metrics including timing
     * histograms, memory usage, and detailed error categorization.
     */
    val enableDetailedMetrics: Boolean = true,
    
    /**
     * Metric name prefix for all ZTE parser metrics.
     * 
     * Prepends all metric names to provide namespace isolation
     * and easier identification in monitoring systems.
     */
    @field:NotBlank
    @field:Size(min = 1, max = 50)
    val prefix: String = "zte.parser",
    
    /**
     * Common tags to apply to all metrics.
     * 
     * Key-value pairs that will be added as tags to all metrics
     * for filtering and aggregation in monitoring systems.
     */
    val commonTags: Map&lt;String, String&gt; = mapOf(
        "parser.type" to "zte-asn1",
        "parser.version" to "1.0.0"
    ),
    
    /**
     * Metrics collection interval (seconds).
     * 
     * How frequently to collect and export metrics to the monitoring
     * system. Lower values provide better resolution but higher overhead.
     */
    @field:Min(1)
    @field:Max(300)
    val collectionIntervalSeconds: Long = 30,
    
    /**
     * Enable JVM metrics collection.
     * 
     * Collects JVM-specific metrics including garbage collection,
     * memory pools, and thread statistics for system monitoring.
     */
    val enableJvmMetrics: Boolean = true,
    
    /**
     * Enable custom business metrics collection.
     * 
     * Collects ZTE CDR-specific business metrics including record
     * type distribution, processing quality metrics, and data patterns.
     */
    val enableBusinessMetrics: Boolean = true
)