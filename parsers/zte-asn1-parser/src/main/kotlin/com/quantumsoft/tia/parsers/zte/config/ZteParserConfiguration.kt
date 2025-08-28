package com.quantumsoft.tia.parsers.zte.config

import com.quantumsoft.tia.parsers.zte.asn1.Asn1Decoder
import com.quantumsoft.tia.parsers.zte.asn1.impl.BouncyCastleAsn1Decoder
import com.quantumsoft.tia.parsers.zte.core.ZteAsn1Parser
import com.quantumsoft.tia.parsers.zte.core.impl.DefaultZteAsn1Parser
import com.quantumsoft.tia.parsers.zte.database.DatabaseWriter
import com.quantumsoft.tia.parsers.zte.database.impl.ClickHouseDatabaseWriter
import com.quantumsoft.tia.parsers.zte.utils.MetricsCollector
import com.quantumsoft.tia.parsers.zte.utils.impl.MicrometerMetricsCollector
import com.quantumsoft.tia.parsers.zte.validation.DataValidator
import com.quantumsoft.tia.parsers.zte.validation.impl.DefaultDataValidator
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

/**
 * Spring Boot configuration for ZTE ASN.1 parser components.
 * 
 * This configuration class implements the Dependency Inversion Principle by
 * defining beans for interface implementations and managing their dependencies
 * through Spring's IoC container. All components depend on abstractions
 * rather than concrete implementations.
 * 
 * ## Design Principles Applied
 * 
 * - **Dependency Inversion**: High-level modules depend on abstractions
 * - **Single Responsibility**: Each configuration method has one concern
 * - **Open/Closed**: New implementations can be added without modification
 * - **Interface Segregation**: Components depend only on interfaces they use
 * 
 * ## Configuration Profiles
 * 
 * - **default**: Production configuration with all features enabled
 * - **test**: Test configuration with mocked external dependencies
 * - **dev**: Development configuration with enhanced debugging
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(ZteParserProperties::class)
class ZteParserConfiguration {

    /**
     * Configures the ASN.1 decoder implementation.
     * 
     * Creates a BouncyCastle-based ASN.1 decoder with ZTE-specific
     * configuration and error handling. The decoder is configured
     * with performance optimizations for high-volume processing.
     * 
     * ## Configuration Features
     * 
     * - ZTE tag mapping configuration
     * - Memory usage optimization for large structures
     * - Error handling for malformed ASN.1 data
     * - Performance monitoring and metrics integration
     * 
     * @param properties Configuration properties for customization
     * @param metricsCollector Metrics collector for performance monitoring
     * @return Configured ASN.1 decoder implementation
     */
    @Bean
    @Profile("!test")
    fun asn1Decoder(
        properties: ZteParserProperties,
        metricsCollector: MetricsCollector
    ): Asn1Decoder {
        return BouncyCastleAsn1Decoder(
            enableStrictValidation = properties.asn1.strictValidation,
            maxStructureDepth = properties.asn1.maxStructureDepth,
            maxFieldLength = properties.asn1.maxFieldLength,
            metricsCollector = metricsCollector
        )
    }

    /**
     * Configures the data validator implementation.
     * 
     * Creates a validator with comprehensive ZTE CDR field validation
     * rules, constraint checking, and configurable validation policies.
     * Validation rules are loaded from configuration and can be
     * customized per deployment environment.
     * 
     * ## Validation Features
     * 
     * - Field format validation (MSISDN, IMSI patterns)
     * - Business rule validation (temporal consistency)
     * - Configurable constraint checking
     * - Performance-optimized batch validation
     * 
     * @param properties Configuration properties with validation rules
     * @param metricsCollector Metrics collector for validation statistics
     * @return Configured data validator implementation
     */
    @Bean
    @Profile("!test")
    fun dataValidator(
        properties: ZteParserProperties,
        metricsCollector: MetricsCollector
    ): DataValidator {
        return DefaultDataValidator(
            validationRules = properties.validation.rules,
            strictMode = properties.validation.strictMode,
            enableCrossFieldValidation = properties.validation.enableCrossFieldValidation,
            metricsCollector = metricsCollector
        )
    }

    /**
     * Configures the database writer implementation.
     * 
     * Creates a ClickHouse database writer with connection pooling,
     * batch processing optimization, and transaction management.
     * The writer is configured with non-nullable schema enforcement
     * and performance monitoring.
     * 
     * ## Database Features
     * 
     * - HikariCP connection pooling for performance
     * - Batch insert optimization for high throughput
     * - Non-nullable schema validation and enforcement
     * - Transaction management with rollback support
     * - Connection health monitoring and recovery
     * 
     * @param dataSource ClickHouse data source with connection pooling
     * @param properties Configuration properties for database operations
     * @param metricsCollector Metrics collector for database performance
     * @return Configured database writer implementation
     */
    @Bean
    @Profile("!test")
    fun databaseWriter(
        dataSource: DataSource,
        properties: ZteParserProperties,
        metricsCollector: MetricsCollector
    ): DatabaseWriter {
        return ClickHouseDatabaseWriter(
            dataSource = dataSource,
            batchSize = properties.database.batchSize,
            connectionTimeout = properties.database.connectionTimeout,
            queryTimeout = properties.database.queryTimeout,
            enableSchemaValidation = properties.database.enableSchemaValidation,
            metricsCollector = metricsCollector
        )
    }

    /**
     * Configures the metrics collector implementation.
     * 
     * Creates a Micrometer-based metrics collector that integrates
     * with Spring Boot Actuator and can export metrics to various
     * monitoring systems (Prometheus, CloudWatch, etc.).
     * 
     * ## Metrics Features
     * 
     * - Performance timing metrics for all operations
     * - Error rate and categorization metrics
     * - Resource utilization monitoring
     * - Custom business metrics for ZTE CDR processing
     * 
     * @param meterRegistry Micrometer meter registry for metrics export
     * @param properties Configuration properties for metrics collection
     * @return Configured metrics collector implementation
     */
    @Bean
    fun metricsCollector(
        meterRegistry: MeterRegistry,
        properties: ZteParserProperties
    ): MetricsCollector {
        return MicrometerMetricsCollector(
            meterRegistry = meterRegistry,
            enableDetailedMetrics = properties.metrics.enableDetailedMetrics,
            metricPrefix = properties.metrics.prefix,
            commonTags = properties.metrics.commonTags
        )
    }

    /**
     * Configures the main ZTE ASN.1 parser implementation.
     * 
     * Creates the core parser that orchestrates ASN.1 decoding,
     * field extraction, data validation, and result aggregation.
     * This is the main entry point for ZTE CDR processing operations.
     * 
     * ## Parser Features
     * 
     * - Complete ZTE CDR field mapping and extraction
     * - Comprehensive error handling and recovery
     * - Performance monitoring and optimization
     * - Memory-efficient streaming processing
     * 
     * @param asn1Decoder ASN.1 decoder for structure parsing
     * @param dataValidator Data validator for field validation
     * @param metricsCollector Metrics collector for performance monitoring
     * @param properties Configuration properties for parser behavior
     * @return Configured ZTE ASN.1 parser implementation
     */
    @Bean
    fun zteAsn1Parser(
        asn1Decoder: Asn1Decoder,
        dataValidator: DataValidator,
        metricsCollector: MetricsCollector,
        properties: ZteParserProperties
    ): ZteAsn1Parser {
        return DefaultZteAsn1Parser(
            asn1Decoder = asn1Decoder,
            dataValidator = dataValidator,
            metricsCollector = metricsCollector,
            enableStrictParsing = properties.parser.strictParsing,
            enableFieldCaching = properties.parser.enableFieldCaching,
            maxRecordsInMemory = properties.parser.maxRecordsInMemory
        )
    }
}

/**
 * Test-specific configuration that provides mock implementations.
 * 
 * This configuration is active only during testing to provide
 * controlled, predictable behavior for unit and integration tests.
 * Mock implementations eliminate external dependencies and allow
 * for focused testing of business logic.
 */
@Configuration
@Profile("test")
class ZteParserTestConfiguration {

    /**
     * Provides a mock ASN.1 decoder for testing scenarios.
     * 
     * The mock decoder provides predictable responses for test cases
     * without requiring actual ASN.1 processing capabilities.
     */
    @Bean
    fun testAsn1Decoder(): Asn1Decoder {
        // Mock implementation will be created in test classes
        // This bean definition ensures Spring context loads correctly
        return MockAsn1Decoder()
    }

    /**
     * Provides a mock data validator for testing scenarios.
     * 
     * The mock validator allows tests to control validation
     * behavior without complex validation rule configuration.
     */
    @Bean
    fun testDataValidator(): DataValidator {
        return MockDataValidator()
    }

    /**
     * Provides a mock database writer for testing scenarios.
     * 
     * The mock database writer eliminates database dependencies
     * during testing while providing controllable behavior.
     */
    @Bean
    fun testDatabaseWriter(): DatabaseWriter {
        return MockDatabaseWriter()
    }
}

/**
 * Factory configuration for creating parser instances dynamically.
 * 
 * This configuration provides factory methods for creating parser
 * instances with different configurations or for different contexts
 * while maintaining dependency injection principles.
 */
@Configuration
class ZteParserFactoryConfiguration {

    /**
     * Factory method for creating parser instances with custom configuration.
     * 
     * This method allows runtime creation of parser instances with
     * different configuration parameters while maintaining proper
     * dependency injection and configuration management.
     * 
     * @param asn1Decoder ASN.1 decoder dependency
     * @param dataValidator Data validator dependency  
     * @param metricsCollector Metrics collector dependency
     * @return Factory function for creating configured parser instances
     */
    @Bean
    fun parserFactory(
        asn1Decoder: Asn1Decoder,
        dataValidator: DataValidator,
        metricsCollector: MetricsCollector
    ): (ZteParserProperties) -> ZteAsn1Parser {
        return { properties ->
            DefaultZteAsn1Parser(
                asn1Decoder = asn1Decoder,
                dataValidator = dataValidator,
                metricsCollector = metricsCollector,
                enableStrictParsing = properties.parser.strictParsing,
                enableFieldCaching = properties.parser.enableFieldCaching,
                maxRecordsInMemory = properties.parser.maxRecordsInMemory
            )
        }
    }
}