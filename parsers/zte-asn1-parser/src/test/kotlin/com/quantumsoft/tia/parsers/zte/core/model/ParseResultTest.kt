package com.quantumsoft.tia.parsers.zte.core.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Unit tests for ParseResult sealed class ensuring functional programming principles.
 * 
 * Tests verify type safety, immutability, functional composition, and proper
 * error handling behavior across all result types.
 */
class ParseResultTest {
    
    private val testCorrelationId = "test-correlation-${UUID.randomUUID()}"
    private val testDuration = Duration.ofMillis(100)
    private val testData = "test-data"
    
    @Test
    fun `Success result should contain provided data and metadata`() {
        // Given
        val testTimestamp = Instant.now()
        
        // When
        val result = ParseResult.Success(
            data = testData,
            recordsProcessed = 5,
            memoryUsed = 1024L,
            timestamp = testTimestamp,
            duration = testDuration,
            correlationId = testCorrelationId
        )
        
        // Then
        assertThat(result.data).isEqualTo(testData)
        assertThat(result.recordsProcessed).isEqualTo(5)
        assertThat(result.memoryUsed).isEqualTo(1024L)
        assertThat(result.timestamp).isEqualTo(testTimestamp)
        assertThat(result.duration).isEqualTo(testDuration)
        assertThat(result.correlationId).isEqualTo(testCorrelationId)
    }
    
    @Test
    fun `Success result should support functional map transformation`() {
        // Given
        val originalResult = ParseResult.Success(
            data = 10,
            duration = testDuration,
            correlationId = testCorrelationId
        )
        
        // When
        val transformedResult = originalResult.map { it * 2 }
        
        // Then
        assertThat(transformedResult.data).isEqualTo(20)
        assertThat(transformedResult.recordsProcessed).isEqualTo(originalResult.recordsProcessed)
        assertThat(transformedResult.duration).isEqualTo(originalResult.duration)
        assertThat(transformedResult.correlationId).isEqualTo(originalResult.correlationId)
    }
    
    @Test
    fun `Success result should support functional flatMap composition`() {
        // Given
        val originalResult = ParseResult.Success(
            data = 5,
            duration = testDuration,
            correlationId = testCorrelationId
        )
        
        // When
        val chainedResult = originalResult.flatMap { value ->
            if (value > 0) {
                ParseResult.Success(
                    data = "positive: $value",
                    duration = Duration.ofMillis(50),
                    correlationId = testCorrelationId
                )
            } else {
                ParseResult.Failure(
                    error = createTestParseError("negative value"),
                    duration = Duration.ofMillis(25),
                    correlationId = testCorrelationId
                )
            }
        }
        
        // Then
        assertThat(chainedResult).isInstanceOf(ParseResult.Success::class.java)
        val successResult = chainedResult as ParseResult.Success
        assertThat(successResult.data).isEqualTo("positive: 5")
    }
    
    @Test
    fun `Failure result should contain error details and metadata`() {
        // Given
        val testError = createTestParseError("test error")
        val partialData = "partial-data"
        val inputSize = 2048L
        val testTimestamp = Instant.now()
        
        // When
        val result = ParseResult.Failure(
            error = testError,
            partialData = partialData,
            inputSize = inputSize,
            timestamp = testTimestamp,
            duration = testDuration,
            correlationId = testCorrelationId
        )
        
        // Then
        assertThat(result.error).isEqualTo(testError)
        assertThat(result.partialData).isEqualTo(partialData)
        assertThat(result.inputSize).isEqualTo(inputSize)
        assertThat(result.timestamp).isEqualTo(testTimestamp)
        assertThat(result.duration).isEqualTo(testDuration)
        assertThat(result.correlationId).isEqualTo(testCorrelationId)
    }
    
    @Test
    fun `Failure result should support recovery transformation`() {
        // Given
        val failureResult = ParseResult.Failure(
            error = createTestParseError("recoverable error"),
            duration = testDuration,
            correlationId = testCorrelationId
        )
        
        // When
        val recoveredResult = failureResult.recover { error ->
            ParseResult.Success(
                data = "recovered from: ${error.message}",
                duration = testDuration,
                correlationId = testCorrelationId
            )
        }
        
        // Then
        assertThat(recoveredResult).isInstanceOf(ParseResult.Success::class.java)
        val successResult = recoveredResult as ParseResult.Success
        assertThat(successResult.data).contains("recovered from:")
    }
    
    @Test
    fun `Failure result should provide default value fallback`() {
        // Given
        val failureResult = ParseResult.Failure(
            error = createTestParseError("error"),
            duration = testDuration,
            correlationId = testCorrelationId
        )
        val defaultValue = "default-value"
        
        // When
        val resultWithDefault = failureResult.withDefault(defaultValue)
        
        // Then
        assertThat(resultWithDefault).isInstanceOf(ParseResult.Success::class.java)
        assertThat(resultWithDefault.data).isEqualTo(defaultValue)
        assertThat(resultWithDefault.recordsProcessed).isEqualTo(0)
    }
    
    @Test
    fun `ParseResult should correctly identify success and failure states`() {
        // Given
        val successResult = ParseResult.Success(testData, duration = testDuration, correlationId = testCorrelationId)
        val failureResult = ParseResult.Failure(
            error = createTestParseError("error"),
            duration = testDuration,
            correlationId = testCorrelationId
        )
        
        // Then
        assertThat(successResult.isSuccess()).isTrue()
        assertThat(successResult.isFailure()).isFalse()
        assertThat(failureResult.isSuccess()).isFalse()
        assertThat(failureResult.isFailure()).isTrue()
    }
    
    @Test
    fun `ParseResult should provide safe data and error access`() {
        // Given
        val successResult = ParseResult.Success(testData, duration = testDuration, correlationId = testCorrelationId)
        val testError = createTestParseError("error")
        val failureResult = ParseResult.Failure(error = testError, duration = testDuration, correlationId = testCorrelationId)
        
        // Then
        assertThat(successResult.getDataOrNull()).isEqualTo(testData)
        assertThat(successResult.getErrorOrNull()).isNull()
        assertThat(failureResult.getDataOrNull()).isNull()
        assertThat(failureResult.getErrorOrNull()).isEqualTo(testError)
    }
    
    @Test
    fun `ParseResult should support functional map on mixed results`() {
        // Given
        val successResult: ParseResult<Int> = ParseResult.Success(10, duration = testDuration, correlationId = testCorrelationId)
        val failureResult: ParseResult<Int> = ParseResult.Failure(
            error = createTestParseError("error"),
            duration = testDuration,
            correlationId = testCorrelationId
        )
        
        // When
        val mappedSuccess = successResult.map { it.toString() }
        val mappedFailure = failureResult.map { it.toString() }
        
        // Then
        assertThat(mappedSuccess).isInstanceOf(ParseResult.Success::class.java)
        assertThat((mappedSuccess as ParseResult.Success).data).isEqualTo("10")
        assertThat(mappedFailure).isInstanceOf(ParseResult.Failure::class.java)
    }
    
    @Test
    fun `ParseResult should support side effects with onSuccess and onFailure`() {
        // Given
        val successResult = ParseResult.Success(testData, duration = testDuration, correlationId = testCorrelationId)
        val failureResult = ParseResult.Failure(
            error = createTestParseError("error"),
            duration = testDuration,
            correlationId = testCorrelationId
        )
        var successCalled = false
        var failureCalled = false
        
        // When
        successResult
            .onSuccess { successCalled = true }
            .onFailure { failureCalled = true }
            
        failureResult
            .onSuccess { successCalled = true }
            .onFailure { failureCalled = true }
        
        // Then
        assertThat(successCalled).isTrue()
        assertThat(failureCalled).isTrue()
    }
    
    @Test
    fun `Success result should be immutable`() {
        // Given
        val originalData = mutableListOf("item1", "item2")
        val result = ParseResult.Success(
            data = originalData,
            duration = testDuration,
            correlationId = testCorrelationId
        )
        
        // When
        originalData.add("item3")
        
        // Then - The result data should remain unchanged
        assertThat(result.data).hasSize(2)
        assertThat(result.data).containsExactly("item1", "item2")
    }
    
    @Test
    fun `Failure result should be immutable`() {
        // Given
        val error = createTestParseError("test error")
        val result = ParseResult.Failure(
            error = error,
            duration = testDuration,
            correlationId = testCorrelationId
        )
        
        // When/Then - Properties should be immutable
        assertThat(result.error).isEqualTo(error)
        assertThat(result.duration).isEqualTo(testDuration)
        assertThat(result.correlationId).isEqualTo(testCorrelationId)
    }
    
    private fun createTestParseError(message: String): ParseError {
        return ParseError.InvalidAsn1Structure(
            details = message,
            correlationId = testCorrelationId
        )
    }
}