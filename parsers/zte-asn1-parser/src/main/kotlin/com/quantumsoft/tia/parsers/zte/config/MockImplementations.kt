package com.quantumsoft.tia.parsers.zte.config

import com.quantumsoft.tia.parsers.zte.asn1.Asn1Decoder
import com.quantumsoft.tia.parsers.zte.asn1.model.Asn1Element
import com.quantumsoft.tia.parsers.zte.asn1.model.ZteFieldTag
import com.quantumsoft.tia.parsers.zte.core.model.*
import com.quantumsoft.tia.parsers.zte.database.DatabaseWriter
import com.quantumsoft.tia.parsers.zte.validation.DataValidator

/**
 * Mock implementations for testing purposes.
 * 
 * These implementations provide predictable behavior for unit and integration
 * tests without requiring external dependencies or complex setup.
 */

/**
 * Mock ASN.1 decoder for testing scenarios.
 */
class MockAsn1Decoder : Asn1Decoder {
    
    override fun decodeAsn1Structure(asn1Data: ByteArray): DecodeResult&lt;Asn1Element&gt; {
        return DecodeResult.Success(
            data = createMockAsn1Element(),
            duration = java.time.Duration.ofMillis(10),
            correlationId = "test-correlation"
        )
    }
    
    override fun extractFieldValue(element: Asn1Element, fieldTag: ZteFieldTag): DecodeResult&lt;ByteArray&gt; {
        return DecodeResult.Success(
            data = "test-value".toByteArray(),
            duration = java.time.Duration.ofMillis(1),
            correlationId = "test-correlation"
        )
    }
    
    override fun isValidAsn1Structure(asn1Data: ByteArray): Boolean = true
    
    private fun createMockAsn1Element(): Asn1Element {
        return Asn1Element(
            tag = com.quantumsoft.tia.parsers.zte.asn1.model.Asn1Tag.SEQUENCE,
            contentLength = 100L,
            children = emptyList(),
            offset = 0L
        )
    }
}

/**
 * Mock data validator for testing scenarios.
 */
class MockDataValidator : DataValidator {
    
    override fun validate(fieldName: String, value: Any?): ValidationResult {
        return ValidationResult.Valid(
            validatedFields = listOf(fieldName),
            correlationId = "test-correlation"
        )
    }
    
    override fun validateRecord(record: ZteCdrRecord): ValidationResult {
        return ValidationResult.Valid(
            validatedFields = listOf("callId", "msisdn"),
            correlationId = "test-correlation"
        )
    }
    
    override fun validateBatch(records: List&lt;ZteCdrRecord&gt;): List&lt;ValidationResult&gt; {
        return records.map { validateRecord(it) }
    }
    
    override fun isConfigurationValid(): Boolean = true
}

/**
 * Mock database writer for testing scenarios.
 */
class MockDatabaseWriter : DatabaseWriter {
    
    override suspend fun batchInsert(records: List&lt;ZteCdrRecord&gt;): InsertResult {
        return InsertResult.Success(
            recordsInserted = records.size,
            totalRecords = records.size,
            batchSize = records.size,
            duration = java.time.Duration.ofMillis(50),
            correlationId = "test-correlation"
        )
    }
    
    override fun validateSchema(record: ZteCdrRecord): SchemaValidationResult {
        return SchemaValidationResult.Valid(
            tableName = "zte_cdr_test",
            schemaVersion = "1.0.0",
            validatedFields = listOf("callId", "msisdn"),
            correlationId = "test-correlation"
        )
    }
    
    override fun validateSchemaBatch(records: List&lt;ZteCdrRecord&gt;): List&lt;SchemaValidationResult&gt; {
        return records.map { validateSchema(it) }
    }
    
    override fun isDatabaseHealthy(): Boolean = true
    
    override suspend fun cleanup() {
        // Mock cleanup - no action needed
    }
}