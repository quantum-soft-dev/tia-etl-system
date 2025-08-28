package com.quantumsoft.tia.parsers.zte.core.model

import java.time.Duration
import java.time.Instant

/**
 * Sealed class representing the result of database insertion operations.
 * 
 * This sealed class provides type-safe handling of ClickHouse insertion
 * results with comprehensive performance metrics and error reporting.
 * It supports both individual record and batch insertion scenarios
 * with detailed success/failure tracking and recovery options.
 * 
 * ## Design Principles
 * 
 * - **Type Safety**: Compile-time guarantees for result handling
 * - **Immutability**: Thread-safe immutable result instances
 * - **Performance Tracking**: Detailed metrics for optimization
 * - **Partial Success**: Support for partial batch insertion success
 * - **Error Recovery**: Rich error context for troubleshooting
 * 
 * ## Usage Pattern
 * 
 * ```kotlin
 * when (val result = databaseWriter.batchInsert(records)) {
 *     is InsertResult.Success -> {
 *         logger.info("Inserted ${result.recordsInserted} records in ${result.duration}")
 *         metricsCollector.recordInsertionTime(result.recordsInserted, result.duration)
 *     }
 *     is InsertResult.PartialSuccess -> {
 *         logger.warn("Partial insertion: ${result.recordsInserted}/${result.totalRecords}")
 *         result.failures.forEach { failure -> 
 *             logger.error("Failed record: ${failure.record.callId} - ${failure.reason}")
 *         }
 *     }
 *     is InsertResult.Failure -> {
 *         logger.error("Insertion failed: ${result.error.message}")
 *         // Handle complete insertion failure
 *     }
 * }
 * ```
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
sealed class InsertResult {
    
    /**
     * Timestamp when the insertion operation completed.
     */
    abstract val timestamp: Instant
    
    /**
     * Duration of the insertion operation.
     */
    abstract val duration: Duration
    
    /**
     * Unique identifier for correlating this result with logs and metrics.
     */
    abstract val correlationId: String
    
    /**
     * Total number of records that were attempted for insertion.
     */
    abstract val totalRecords: Int
    
    /**
     * Represents a completely successful batch insertion operation.
     * 
     * Contains performance metrics and transaction information for
     * monitoring and optimization purposes.
     * 
     * @param recordsInserted Number of records successfully inserted
     * @param totalRecords Total number of records attempted
     * @param batchSize Size of insertion batches used
     * @param transactionId Database transaction identifier
     * @param timestamp When the insertion completed
     * @param duration How long the insertion took
     * @param correlationId Unique identifier for operation correlation
     * @param performanceMetrics Detailed performance measurements
     */
    data class Success(
        val recordsInserted: Int,
        override val totalRecords: Int,
        val batchSize: Int,
        val transactionId: String? = null,
        override val timestamp: Instant = Instant.now(),
        override val duration: Duration,
        override val correlationId: String,
        val performanceMetrics: InsertionMetrics = InsertionMetrics()
    ) : InsertResult() {
        
        /**
         * Calculates the insertion throughput in records per second.
         * 
         * @return Throughput rate as records per second
         */
        fun getThroughput(): Double {
            return if (duration.toMillis() &gt; 0) {
                recordsInserted.toDouble() / (duration.toMillis() / 1000.0)
            } else {
                0.0
            }
        }
        
        /**
         * Checks if all attempted records were successfully inserted.
         * 
         * @return true if recordsInserted equals totalRecords
         */
        fun isCompleteSuccess(): Boolean = recordsInserted == totalRecords
    }
    
    /**
     * Represents a partially successful batch insertion operation.
     * 
     * Contains details about both successful insertions and failures,
     * enabling proper error handling while preserving successfully
     * inserted data.
     * 
     * @param recordsInserted Number of records successfully inserted
     * @param totalRecords Total number of records attempted
     * @param failures List of individual record insertion failures
     * @param transactionId Database transaction identifier (if applicable)
     * @param timestamp When the insertion completed
     * @param duration How long the insertion took
     * @param correlationId Unique identifier for operation correlation
     * @param performanceMetrics Detailed performance measurements
     */
    data class PartialSuccess(
        val recordsInserted: Int,
        override val totalRecords: Int,
        val failures: List&lt;RecordInsertionFailure&gt;,
        val transactionId: String? = null,
        override val timestamp: Instant = Instant.now(),
        override val duration: Duration,
        override val correlationId: String,
        val performanceMetrics: InsertionMetrics = InsertionMetrics()
    ) : InsertResult() {
        
        /**
         * Gets the success rate as a percentage.
         * 
         * @return Success rate from 0.0 to 1.0
         */
        fun getSuccessRate(): Double = recordsInserted.toDouble() / totalRecords
        
        /**
         * Gets failures grouped by error type for analysis.
         * 
         * @return Map of error types to lists of failures
         */
        fun getFailuresByType(): Map&lt;String, List&lt;RecordInsertionFailure&gt;&gt; {
            return failures.groupBy { it.errorType }
        }
        
        /**
         * Gets the number of records that failed insertion.
         * 
         * @return Number of failed record insertions
         */
        fun getFailureCount(): Int = failures.size
        
        /**
         * Calculates throughput including both successful and failed records.
         * 
         * @return Total processing throughput in records per second
         */
        fun getTotalThroughput(): Double {
            return if (duration.toMillis() &gt; 0) {
                totalRecords.toDouble() / (duration.toMillis() / 1000.0)
            } else {
                0.0
            }
        }
    }
    
    /**
     * Represents a completely failed insertion operation.
     * 
     * Contains detailed error information for troubleshooting and
     * recovery planning. No records were successfully inserted.
     * 
     * @param error Detailed error information
     * @param totalRecords Total number of records that failed to insert
     * @param failureType Category of insertion failure
     * @param transactionId Database transaction identifier (if applicable)
     * @param timestamp When the insertion failed
     * @param duration How long was spent before failure
     * @param correlationId Unique identifier for operation correlation
     * @param retryable Whether this operation can be safely retried
     */
    data class Failure(
        val error: DatabaseError,
        override val totalRecords: Int,
        val failureType: InsertionFailureType,
        val transactionId: String? = null,
        override val timestamp: Instant = Instant.now(),
        override val duration: Duration,
        override val correlationId: String,
        val retryable: Boolean = false
    ) : InsertResult() {
        
        /**
         * Attempts to create a retry strategy based on the failure type.
         * 
         * @return Suggested retry strategy or null if not retryable
         */
        fun getRetryStrategy(): RetryStrategy? {
            return if (retryable) {
                when (failureType) {
                    InsertionFailureType.CONNECTION_TIMEOUT -&gt; 
                        RetryStrategy.exponentialBackoff(maxRetries = 3)
                    InsertionFailureType.TRANSACTION_CONFLICT -&gt; 
                        RetryStrategy.linearBackoff(maxRetries = 5)
                    InsertionFailureType.TEMPORARY_UNAVAILABILITY -&gt; 
                        RetryStrategy.exponentialBackoff(maxRetries = 3)
                    else -&gt; null
                }
            } else {
                null
            }
        }
        
        /**
         * Checks if this failure indicates a systemic database problem.
         * 
         * @return true if the failure indicates broader system issues
         */
        fun isSystemicFailure(): Boolean {
            return when (failureType) {
                InsertionFailureType.DATABASE_UNAVAILABLE,
                InsertionFailureType.SCHEMA_MISMATCH,
                InsertionFailureType.PERMISSION_DENIED -&gt; true
                else -&gt; false
            }
        }
    }
    
    /**
     * Checks if this result represents a successful insertion (complete or partial).
     * 
     * @return true if any records were successfully inserted
     */
    fun hasSuccesses(): Boolean = when (this) {
        is Success -&gt; true
        is PartialSuccess -&gt; recordsInserted &gt; 0
        is Failure -&gt; false
    }
    
    /**
     * Checks if this result represents a complete failure.
     * 
     * @return true if no records were successfully inserted
     */
    fun isCompleteFailure(): Boolean = this is Failure
    
    /**
     * Gets the number of successfully inserted records.
     * 
     * @return Number of records inserted, 0 for complete failures
     */
    fun getInsertedCount(): Int = when (this) {
        is Success -&gt; recordsInserted
        is PartialSuccess -&gt; recordsInserted
        is Failure -&gt; 0
    }
    
    /**
     * Gets the overall success rate for this insertion operation.
     * 
     * @return Success rate from 0.0 to 1.0
     */
    fun getSuccessRate(): Double = when (this) {
        is Success -&gt; 1.0
        is PartialSuccess -&gt; recordsInserted.toDouble() / totalRecords
        is Failure -&gt; 0.0
    }
}

/**
 * Represents a failure to insert an individual record within a batch operation.
 * 
 * @param record The record that failed to insert
 * @param reason Human-readable description of why insertion failed
 * @param errorType Category of insertion error
 * @param errorCode Database-specific error code
 * @param retryable Whether insertion of this record can be retried
 */
data class RecordInsertionFailure(
    val record: ZteCdrRecord,
    val reason: String,
    val errorType: String,
    val errorCode: String? = null,
    val retryable: Boolean = false
)

/**
 * Performance metrics for insertion operations.
 * 
 * @param connectionAcquisitionTime Time spent acquiring database connection
 * @param statementPreparationTime Time spent preparing SQL statements
 * @param dataTransmissionTime Time spent transmitting data to database
 * @param transactionCommitTime Time spent committing the transaction
 * @param memoryUsed Peak memory usage during insertion
 */
data class InsertionMetrics(
    val connectionAcquisitionTime: Duration = Duration.ZERO,
    val statementPreparationTime: Duration = Duration.ZERO,
    val dataTransmissionTime: Duration = Duration.ZERO,
    val transactionCommitTime: Duration = Duration.ZERO,
    val memoryUsed: Long = 0L
) {
    
    /**
     * Calculates the total overhead time for the insertion operation.
     * 
     * @return Total overhead excluding actual data transmission
     */
    fun getOverheadTime(): Duration {
        return connectionAcquisitionTime
            .plus(statementPreparationTime)
            .plus(transactionCommitTime)
    }
}

/**
 * Categories of insertion failure types for error handling and recovery.
 */
enum class InsertionFailureType {
    /**
     * Database server is not available or cannot be reached.
     */
    DATABASE_UNAVAILABLE,
    
    /**
     * Database connection timed out during operation.
     */
    CONNECTION_TIMEOUT,
    
    /**
     * Transaction conflict with other concurrent operations.
     */
    TRANSACTION_CONFLICT,
    
    /**
     * Schema validation failed - data doesn't match table structure.
     */
    SCHEMA_MISMATCH,
    
    /**
     * Insufficient permissions to perform the insertion.
     */
    PERMISSION_DENIED,
    
    /**
     * Data constraint violation (unique key, foreign key, etc.).
     */
    CONSTRAINT_VIOLATION,
    
    /**
     * Temporary database unavailability or maintenance mode.
     */
    TEMPORARY_UNAVAILABILITY,
    
    /**
     * Unknown or unclassified insertion error.
     */
    UNKNOWN_ERROR
}

/**
 * Retry strategy configuration for failed insertion operations.
 * 
 * @param maxRetries Maximum number of retry attempts
 * @param baseDelay Initial delay between retry attempts
 * @param maxDelay Maximum delay between retry attempts
 * @param backoffMultiplier Multiplier for exponential backoff
 */
data class RetryStrategy(
    val maxRetries: Int,
    val baseDelay: Duration,
    val maxDelay: Duration,
    val backoffMultiplier: Double = 2.0
) {
    companion object {
        /**
         * Creates an exponential backoff retry strategy.
         */
        fun exponentialBackoff(maxRetries: Int): RetryStrategy {
            return RetryStrategy(
                maxRetries = maxRetries,
                baseDelay = Duration.ofSeconds(1),
                maxDelay = Duration.ofMinutes(5),
                backoffMultiplier = 2.0
            )
        }
        
        /**
         * Creates a linear backoff retry strategy.
         */
        fun linearBackoff(maxRetries: Int): RetryStrategy {
            return RetryStrategy(
                maxRetries = maxRetries,
                baseDelay = Duration.ofSeconds(2),
                maxDelay = Duration.ofSeconds(10),
                backoffMultiplier = 1.0
            )
        }
    }
}