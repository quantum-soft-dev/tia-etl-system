package com.quantumsoft.tia.parsers.zte.core.model

import java.time.Instant
import java.time.Duration

/**
 * Sealed class representing the result of ASN.1 parsing operations.
 * 
 * This sealed class follows functional programming principles to provide
 * type-safe error handling and explicit result management. It eliminates
 * the need for exceptions during normal parsing operations and provides
 * rich error context for troubleshooting and debugging.
 * 
 * ## Design Principles
 * 
 * - **Type Safety**: Compile-time guarantees about result handling
 * - **Immutability**: All result instances are immutable and thread-safe
 * - **Explicit Error Handling**: No hidden exceptions or silent failures
 * - **Rich Context**: Detailed information for success and failure scenarios
 * - **Functional Style**: Supports functional composition and transformation
 * 
 * ## Usage Pattern
 * 
 * ```kotlin
 * when (val result = parser.parseRecord(asn1Data)) {
 *     is ParseResult.Success -> {
 *         // Process successful result
 *         val record = result.data
 *         logger.info("Parsed record with ID: ${record.callId}")
 *     }
 *     is ParseResult.Failure -> {
 *         // Handle parsing failure
 *         logger.error("Parse failed: ${result.error.message}")
 *         metricsCollector.recordError(result.error.type, result.error.category)
 *     }
 * }
 * ```
 * 
 * @param T The type of data contained in successful parsing results
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
sealed class ParseResult&lt;out T&gt; {
    
    /**
     * Timestamp when the parsing operation completed.
     */
    abstract val timestamp: Instant
    
    /**
     * Duration of the parsing operation.
     */
    abstract val duration: Duration
    
    /**
     * Unique identifier for correlating this result with logs and metrics.
     */
    abstract val correlationId: String
    
    /**
     * Represents a successful parsing operation with extracted data.
     * 
     * Contains the successfully parsed and validated data along with
     * metadata about the parsing operation for monitoring and debugging.
     * 
     * @param data The successfully parsed data
     * @param recordsProcessed Number of records processed (for batch operations)
     * @param memoryUsed Peak memory usage during parsing (bytes)
     * @param timestamp When the parsing completed
     * @param duration How long parsing took
     * @param correlationId Unique identifier for operation correlation
     */
    data class Success&lt;T&gt;(
        val data: T,
        val recordsProcessed: Int = 1,
        val memoryUsed: Long = 0L,
        override val timestamp: Instant = Instant.now(),
        override val duration: Duration,
        override val correlationId: String
    ) : ParseResult&lt;T&gt;() {
        
        /**
         * Transforms the successful result data using the provided function.
         * 
         * This method enables functional composition and transformation of
         * successful parse results while preserving metadata and context.
         * 
         * @param transform Function to transform the successful data
         * @return New ParseResult.Success with transformed data
         */
        fun &lt;R&gt; map(transform: (T) -&gt; R): Success&lt;R&gt; {
            return Success(
                data = transform(data),
                recordsProcessed = recordsProcessed,
                memoryUsed = memoryUsed,
                timestamp = timestamp,
                duration = duration,
                correlationId = correlationId
            )
        }
        
        /**
         * Applies a transformation function that returns another ParseResult.
         * 
         * This method enables chaining of parsing operations while maintaining
         * proper error propagation and context preservation.
         * 
         * @param transform Function that transforms data and returns ParseResult
         * @return Result of the transformation function
         */
        fun &lt;R&gt; flatMap(transform: (T) -&gt; ParseResult&lt;R&gt;): ParseResult&lt;R&gt; {
            return transform(data)
        }
    }
    
    /**
     * Represents a failed parsing operation with detailed error information.
     * 
     * Contains comprehensive error details for troubleshooting, including
     * error classification, context, and recovery suggestions where applicable.
     * 
     * @param error Detailed error information
     * @param partialData Any data that was successfully extracted before failure
     * @param inputSize Size of the input data that failed to parse
     * @param timestamp When the parsing failed
     * @param duration How long parsing took before failing
     * @param correlationId Unique identifier for operation correlation
     */
    data class Failure(
        val error: ParseError,
        val partialData: Any? = null,
        val inputSize: Long = 0L,
        override val timestamp: Instant = Instant.now(),
        override val duration: Duration,
        override val correlationId: String
    ) : ParseResult&lt;Nothing&gt;() {
        
        /**
         * Attempts to recover from the failure using a recovery function.
         * 
         * This method enables graceful error recovery and fallback strategies
         * while preserving the original error context for logging and monitoring.
         * 
         * @param recovery Function that attempts to create a successful result
         * @return Either the recovery result or the original failure
         */
        fun &lt;T&gt; recover(recovery: (ParseError) -&gt; ParseResult&lt;T&gt;): ParseResult&lt;T&gt; {
            return recovery(error)
        }
        
        /**
         * Provides a default value when parsing fails.
         * 
         * This method offers a simple way to provide fallback values
         * for non-critical parsing failures.
         * 
         * @param defaultValue Value to use as successful result
         * @return Success result with the default value
         */
        fun &lt;T&gt; withDefault(defaultValue: T): Success&lt;T&gt; {
            return Success(
                data = defaultValue,
                recordsProcessed = 0,
                memoryUsed = 0L,
                timestamp = timestamp,
                duration = duration,
                correlationId = correlationId
            )
        }
    }
    
    /**
     * Checks if this result represents a successful parsing operation.
     * 
     * @return true if this is a Success result, false otherwise
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Checks if this result represents a failed parsing operation.
     * 
     * @return true if this is a Failure result, false otherwise
     */
    fun isFailure(): Boolean = this is Failure
    
    /**
     * Gets the data from a successful result or null if failed.
     * 
     * This method provides safe access to successful parsing results
     * without having to cast or use when expressions.
     * 
     * @return The parsed data if successful, null otherwise
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
     * @return The parse error if failed, null otherwise
     */
    fun getErrorOrNull(): ParseError? = when (this) {
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
    fun &lt;R&gt; map(transform: (T) -&gt; R): ParseResult&lt;R&gt; = when (this) {
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
    fun onSuccess(action: (T) -&gt; Unit): ParseResult&lt;T&gt; {
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
    fun onFailure(action: (ParseError) -&gt; Unit): ParseResult&lt;T&gt; {
        if (this is Failure) {
            action(error)
        }
        return this
    }
}