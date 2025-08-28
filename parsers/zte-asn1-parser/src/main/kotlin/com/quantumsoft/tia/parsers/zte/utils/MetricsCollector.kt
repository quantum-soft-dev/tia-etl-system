package com.quantumsoft.tia.parsers.zte.utils

import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Interface for performance metrics collection following Interface Segregation Principle.
 * 
 * This interface provides a focused contract for collecting and reporting performance
 * metrics during ZTE ASN.1 CDR processing. It separates metrics collection from
 * business logic while providing essential observability for production operations.
 * 
 * ## Design Principles
 * 
 * - **Single Responsibility**: Only handles metrics collection and reporting
 * - **Interface Segregation**: Minimal interface with essential metrics operations
 * - **Open/Closed**: Extensible for new metric types without interface changes
 * - **Dependency Inversion**: Abstracts metrics backend implementation details
 * 
 * ## Metrics Categories
 * 
 * - **Performance Metrics**: Processing times, throughput rates
 * - **Quality Metrics**: Success/failure rates, validation statistics
 * - **Resource Metrics**: Memory usage, connection pool utilization
 * - **Business Metrics**: Record counts, data volumes processed
 * 
 * ## Backend Integration
 * 
 * Implementations can integrate with various metrics backends:
 * - Micrometer for Spring Boot applications
 * - Prometheus for container environments
 * - CloudWatch for AWS deployments
 * - Custom logging for simple scenarios
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
interface MetricsCollector {
    
    /**
     * Records the time taken to process a single file.
     * 
     * This metric tracks end-to-end file processing duration from initial
     * ASN.1 parsing through final database insertion. It provides essential
     * performance monitoring for identifying processing bottlenecks and
     * tracking performance trends over time.
     * 
     * ## Metric Details
     * 
     * - **Name**: file.processing.duration
     * - **Type**: Timer/Histogram
     * - **Tags**: fileName, recordCount, fileSize
     * - **Unit**: Milliseconds
     * 
     * @param fileName Name of the processed file for correlation
     * @param duration Total processing time for the file
     * @param recordCount Number of records processed in the file
     */
    fun recordFileProcessingTime(fileName: String, duration: Duration, recordCount: Long)
    
    /**
     * Records the time taken to parse ASN.1 data for a batch of records.
     * 
     * This metric specifically tracks ASN.1 parsing performance, helping
     * identify parsing bottlenecks and optimize decoder performance for
     * different record types and volumes.
     * 
     * ## Metric Details
     * 
     * - **Name**: asn1.parsing.duration
     * - **Type**: Timer/Histogram  
     * - **Tags**: batchSize, parseSuccess
     * - **Unit**: Milliseconds
     * 
     * @param batchSize Number of records in the parsed batch
     * @param duration Time spent parsing the batch
     * @param successCount Number of successfully parsed records
     */
    fun recordParsingTime(batchSize: Int, duration: Duration, successCount: Int)
    
    /**
     * Records validation processing time and results.
     * 
     * This metric tracks data validation performance and quality metrics,
     * providing insights into validation overhead and data quality trends
     * across different data sources and time periods.
     * 
     * ## Metric Details
     * 
     * - **Name**: validation.processing.duration
     * - **Type**: Timer/Histogram
     * - **Tags**: recordCount, validationSuccess, validationFailures
     * - **Unit**: Milliseconds
     * 
     * @param recordCount Number of records validated
     * @param duration Time spent on validation
     * @param successCount Number of records that passed validation
     * @param failureCount Number of records that failed validation
     */
    fun recordValidationTime(recordCount: Int, duration: Duration, successCount: Int, failureCount: Int)
    
    /**
     * Records database insertion performance metrics.
     * 
     * This metric tracks ClickHouse insertion performance, including batch
     * processing efficiency and database operation success rates. It helps
     * optimize batch sizes and identify database performance issues.
     * 
     * ## Metric Details
     * 
     * - **Name**: database.insertion.duration  
     * - **Type**: Timer/Histogram
     * - **Tags**: batchSize, insertSuccess, recordCount
     * - **Unit**: Milliseconds
     * 
     * @param batchSize Number of records in the insertion batch
     * @param duration Time spent on database insertion
     * @param successCount Number of successfully inserted records
     */
    fun recordInsertionTime(batchSize: Int, duration: Duration, successCount: Int)
    
    /**
     * Records memory usage during processing operations.
     * 
     * This metric tracks memory consumption patterns during different phases
     * of processing, helping identify memory leaks, optimization opportunities,
     * and appropriate resource allocation for production deployments.
     * 
     * ## Metric Details
     * 
     * - **Name**: memory.usage.bytes
     * - **Type**: Gauge
     * - **Tags**: processingPhase, operationType  
     * - **Unit**: Bytes
     * 
     * @param phase Processing phase (parsing, validation, insertion)
     * @param memoryUsedBytes Current memory usage in bytes
     * @param maxMemoryBytes Maximum memory available
     */
    fun recordMemoryUsage(phase: String, memoryUsedBytes: Long, maxMemoryBytes: Long)
    
    /**
     * Records error occurrences with detailed categorization.
     * 
     * This metric tracks error frequencies and types, providing essential
     * data for troubleshooting, alerting, and system reliability monitoring.
     * Error categorization helps identify systemic issues vs. data quality problems.
     * 
     * ## Error Categories
     * 
     * - **ParseError**: ASN.1 parsing failures
     * - **ValidationError**: Data validation failures  
     * - **DatabaseError**: ClickHouse insertion failures
     * - **SystemError**: Infrastructure or configuration failures
     * 
     * ## Metric Details
     * 
     * - **Name**: processing.errors.total
     * - **Type**: Counter
     * - **Tags**: errorType, errorCategory, severity
     * 
     * @param errorType Specific error type identifier
     * @param errorCategory High-level error category
     * @param errorMessage Error message for correlation
     * @param severity Error severity level (LOW, MEDIUM, HIGH, CRITICAL)
     */
    fun recordError(errorType: String, errorCategory: String, errorMessage: String, severity: String)
    
    /**
     * Records processing throughput metrics.
     * 
     * This metric tracks overall processing throughput in records per second,
     * providing key performance indicators for system capacity planning and
     * performance optimization efforts.
     * 
     * ## Metric Details
     * 
     * - **Name**: processing.throughput.records_per_second
     * - **Type**: Gauge
     * - **Tags**: operationType, instanceId
     * - **Unit**: Records per second
     * 
     * @param recordsProcessed Total number of records processed
     * @param timeWindow Time window for the throughput calculation
     * @param operationType Type of processing operation
     */
    fun recordThroughput(recordsProcessed: Long, timeWindow: Duration, operationType: String)
    
    /**
     * Flushes any buffered metrics to the backend system.
     * 
     * This method ensures that all collected metrics are transmitted to the
     * metrics backend system, particularly important for batch-based metrics
     * systems or when graceful shutdown is required.
     */
    fun flush()
    
    /**
     * Checks if the metrics collection system is properly configured and operational.
     * 
     * This method verifies that the metrics collector is properly initialized
     * and can successfully communicate with the metrics backend. It can be used
     * for health checks and initialization verification.
     * 
     * @return true if metrics collection is operational and properly configured
     */
    fun isHealthy(): Boolean
}