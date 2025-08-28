package com.quantumsoft.tia.parsers.zte.core.model

import java.time.Duration
import java.time.Instant

/**
 * Sealed class representing the result of ASN.1 decoding operations.
 * 
 * This sealed class follows functional programming principles to provide
 * type-safe error handling for ASN.1 decoding operations. It enables
 * explicit handling of decoding success and failure scenarios with
 * comprehensive error context.
 * 
 * ## Design Principles
 * 
 * - **Type Safety**: Compile-time guarantees about result handling
 * - **Immutability**: All result instances are immutable and thread-safe
 * - **Explicit Error Handling**: No hidden exceptions or silent failures
 * - **Rich Context**: Detailed information for debugging and monitoring
 * - **Functional Composition**: Supports functional transformation chains
 * 
 * @param T The type of data contained in successful decoding results
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
sealed class DecodeResult&lt;out T&gt; {
    
    /**
     * Timestamp when the decoding operation completed.
     */
    abstract val timestamp: Instant
    
    /**
     * Duration of the decoding operation.
     */
    abstract val duration: Duration
    
    /**
     * Unique identifier for correlating this result with logs and metrics.
     */
    abstract val correlationId: String
    
    /**
     * Represents a successful ASN.1 decoding operation.
     * 
     * Contains the successfully decoded data along with metadata about
     * the decoding process for performance monitoring and debugging.
     * 
     * @param data The successfully decoded data
     * @param bytesProcessed Number of bytes processed during decoding
     * @param elementsDecoded Number of ASN.1 elements decoded
     * @param timestamp When the decoding completed
     * @param duration How long decoding took
     * @param correlationId Unique identifier for operation correlation
     * @param memoryUsed Peak memory usage during decoding (bytes)
     */
    data class Success&lt;T&gt;(
        val data: T,
        val bytesProcessed: Long = 0L,
        val elementsDecoded: Int = 0,
        override val timestamp: Instant = Instant.now(),
        override val duration: Duration,
        override val correlationId: String,
        val memoryUsed: Long = 0L
    ) : DecodeResult&lt;T&gt;() {
        
        /**
         * Transforms the successful result data using the provided function.
         * 
         * This method enables functional composition and transformation of
         * successful decode results while preserving metadata and context.
         * 
         * @param transform Function to transform the successful data
         * @return New DecodeResult.Success with transformed data
         */
        fun &lt;R&gt; map(transform: (T) -&gt; R): Success&lt;R&gt; {
            return Success(
                data = transform(data),
                bytesProcessed = bytesProcessed,
                elementsDecoded = elementsDecoded,
                timestamp = timestamp,
                duration = duration,
                correlationId = correlationId,
                memoryUsed = memoryUsed
            )
        }
        
        /**
         * Applies a transformation function that returns another DecodeResult.
         * 
         * This method enables chaining of decoding operations while maintaining
         * proper error propagation and context preservation.
         * 
         * @param transform Function that transforms data and returns DecodeResult
         * @return Result of the transformation function
         */
        fun &lt;R&gt; flatMap(transform: (T) -&gt; DecodeResult&lt;R&gt;): DecodeResult&lt;R&gt; {
            return transform(data)
        }
        
        /**
         * Calculates the decoding throughput in bytes per second.
         * 
         * @return Throughput rate as bytes per second
         */
        fun getThroughput(): Double {
            return if (duration.toMillis() &gt; 0) {
                bytesProcessed.toDouble() / (duration.toMillis() / 1000.0)
            } else {
                0.0
            }
        }
    }
    
    /**
     * Represents a failed ASN.1 decoding operation.
     * 
     * Contains comprehensive error details for troubleshooting, including
     * error classification, context, and recovery suggestions where applicable.
     * 
     * @param error Detailed error information
     * @param partialData Any data that was successfully decoded before failure
     * @param bytesProcessed Number of bytes processed before failure
     * @param failurePosition Byte position where decoding failed
     * @param timestamp When the decoding failed
     * @param duration How long decoding took before failing
     * @param correlationId Unique identifier for operation correlation
     */
    data class Failure(
        val error: DecodeError,
        val partialData: Any? = null,
        val bytesProcessed: Long = 0L,
        val failurePosition: Long = 0L,
        override val timestamp: Instant = Instant.now(),
        override val duration: Duration,
        override val correlationId: String
    ) : DecodeResult&lt;Nothing&gt;() {
        
        /**
         * Attempts to recover from the failure using a recovery function.
         * 
         * This method enables graceful error recovery and fallback strategies
         * while preserving the original error context for logging and monitoring.
         * 
         * @param recovery Function that attempts to create a successful result
         * @return Either the recovery result or the original failure
         */
        fun &lt;T&gt; recover(recovery: (DecodeError) -&gt; DecodeResult&lt;T&gt;): DecodeResult&lt;T&gt; {
            return recovery(error)
        }
        
        /**
         * Provides a default value when decoding fails.
         * 
         * This method offers a simple way to provide fallback values
         * for non-critical decoding failures.
         * 
         * @param defaultValue Value to use as successful result
         * @return Success result with the default value
         */
        fun &lt;T&gt; withDefault(defaultValue: T): Success&lt;T&gt; {
            return Success(
                data = defaultValue,
                bytesProcessed = bytesProcessed,
                elementsDecoded = 0,
                timestamp = timestamp,
                duration = duration,
                correlationId = correlationId,
                memoryUsed = 0L
            )
        }
        
        /**
         * Creates a summary of the failure for logging and monitoring.
         * 
         * @return Concise failure summary
         */
        fun getFailureSummary(): String {
            return "Decode failed at position $failurePosition: ${error.message} " +
                   "(processed ${bytesProcessed} bytes in ${duration.toMillis()}ms)"
        }
    }
    
    /**
     * Checks if this result represents a successful decoding operation.
     * 
     * @return true if this is a Success result, false otherwise
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Checks if this result represents a failed decoding operation.
     * 
     * @return true if this is a Failure result, false otherwise
     */
    fun isFailure(): Boolean = this is Failure
    
    /**
     * Gets the data from a successful result or null if failed.
     * 
     * This method provides safe access to successful decoding results
     * without having to cast or use when expressions.
     * 
     * @return The decoded data if successful, null otherwise
     */
    fun getDataOrNull(): T? = when (this) {
        is Success -&gt; data
        is Failure -&gt; null
    }
    
    /**
     * Gets the error from a failed result or null if successful.
     * 
     * This method provides safe access to error information
     * without having to cast or use when expressions.
     * 
     * @return The decode error if failed, null otherwise
     */
    fun getErrorOrNull(): DecodeError? = when (this) {
        is Success -&gt; null
        is Failure -&gt; error
    }
    
    /**
     * Transforms the result data if successful, otherwise returns the same failure.
     * 
     * This method enables safe transformation of successful results while
     * preserving failure results unchanged.
     * 
     * @param transform Function to transform successful data
     * @return Transformed success or original failure
     */
    fun &lt;R&gt; map(transform: (T) -&gt; R): DecodeResult&lt;R&gt; = when (this) {
        is Success -&gt; map(transform)
        is Failure -&gt; this
    }
    
    /**
     * Applies a function to the result data if successful.
     * 
     * This method enables side effects (like logging or metrics) on successful
     * results without changing the result itself.
     * 
     * @param action Function to apply to successful data
     * @return The same result instance
     */
    fun onSuccess(action: (T) -&gt; Unit): DecodeResult&lt;T&gt; {
        if (this is Success) {
            action(data)
        }
        return this
    }
    
    /**
     * Applies a function to the error if this result failed.
     * 
     * This method enables side effects (like error logging or alerting) on
     * failed results without changing the result itself.
     * 
     * @param action Function to apply to failure error
     * @return The same result instance
     */
    fun onFailure(action: (DecodeError) -&gt; Unit): DecodeResult&lt;T&gt; {
        if (this is Failure) {
            action(error)
        }
        return this
    }
}

/**
 * Base interface for ASN.1 decoding errors.
 * 
 * Provides common error properties and behavior across all decode error types
 * to ensure consistent error handling and reporting.
 */
interface DecodeError {
    /**
     * Human-readable error message describing what went wrong.
     */
    val message: String
    
    /**
     * Error type identifier for categorization and metrics.
     */
    val type: String
    
    /**
     * Whether this error condition might be recoverable with retry.
     */
    val isRetryable: Boolean
    
    /**
     * Byte position where the error occurred (if applicable).
     */
    val position: Long?
    
    /**
     * Suggested recovery actions for this error.
     */
    val recoverySuggestions: List&lt;String&gt;
}

/**
 * Specific decode error for invalid ASN.1 structure.
 */
data class InvalidStructureError(
    val details: String,
    override val position: Long? = null,
    val expectedStructure: String? = null
) : DecodeError {
    override val message: String = "Invalid ASN.1 structure: $details${position?.let { " at position $it" } ?: ""}"
    override val type: String = "INVALID_STRUCTURE"
    override val isRetryable: Boolean = false
    override val recoverySuggestions: List&lt;String&gt; = listOf(
        "Verify ASN.1 data is not corrupted",
        "Check ASN.1 encoding format (BER vs DER)",
        "Validate source data generation process"
    )
}

/**
 * Decode error for truncated or incomplete ASN.1 data.
 */
data class TruncatedDataError(
    val expectedBytes: Long,
    val availableBytes: Long,
    override val position: Long
) : DecodeError {
    override val message: String = "Truncated ASN.1 data at position $position: expected $expectedBytes bytes, got $availableBytes"
    override val type: String = "TRUNCATED_DATA"
    override val isRetryable: Boolean = true
    override val recoverySuggestions: List&lt;String&gt; = listOf(
        "Check if data transfer was completed",
        "Verify network connectivity",
        "Re-download source data"
    )
}

/**
 * Decode error for unsupported ASN.1 tag types.
 */
data class UnsupportedTagError(
    val tagValue: String,
    val context: String,
    override val position: Long? = null
) : DecodeError {
    override val message: String = "Unsupported ASN.1 tag '$tagValue' in context: $context${position?.let { " at position $it" } ?: ""}"
    override val type: String = "UNSUPPORTED_TAG"
    override val isRetryable: Boolean = false
    override val recoverySuggestions: List&lt;String&gt; = listOf(
        "Check if this is a new ASN.1 format version",
        "Update decoder to support additional tag types",
        "Review ASN.1 specification for tag definitions"
    )
}