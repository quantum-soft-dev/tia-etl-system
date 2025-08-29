package com.quantumsoft.tia.scanner.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.quantumsoft.tia.scanner.dto.jobs.CreateScanJobRequest
import com.quantumsoft.tia.scanner.dto.jobs.UpdateScanJobRequest
import com.quantumsoft.tia.scanner.entities.ScanIntervalType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.*

@AutoConfigureMockMvc
class ScanJobE2ETest : BaseE2ETest() {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @Test
    fun `should create scan job`() {
        val request = CreateScanJobRequest(
            name = "Test Job",
            description = "Test job description",
            sourceDirectory = "/test/path",
            filePattern = "*.asn1",
            scanIntervalType = ScanIntervalType.CRON,
            scanIntervalValue = "0 * * * * *",
            parserId = "test-parser",
            maxFileSizeMb = 100,
            recursiveScan = true,
            maxDepth = 5,
            priority = 1,
            isActive = true
        )
        
        mockMvc.perform(
            post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
        .andExpect(jsonPath("$.name").value("Test Job"))
        .andExpect(jsonPath("$.description").value("Test job description"))
        .andExpect(jsonPath("$.sourceDirectory").value("/test/path"))
        .andExpect(jsonPath("$.filePattern").value("*.asn1"))
        .andExpect(jsonPath("$.parserId").value("test-parser"))
        .andExpect(jsonPath("$.isActive").value(true))
        .andExpect(jsonPath("$.id").exists())
    }
    
    @Test
    fun `should get all scan jobs`() {
        // First create a job
        val request = CreateScanJobRequest(
            name = "List Test Job",
            description = "Job for listing test",
            sourceDirectory = "/list/test",
            filePattern = "*.csv",
            scanIntervalType = ScanIntervalType.FIXED,
            scanIntervalValue = "PT5M",
            parserId = "csv-parser",
            isActive = true
        )
        
        mockMvc.perform(
            post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
        
        // Then get all jobs
        mockMvc.perform(get("/api/v1/jobs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content[0].name").exists())
    }
    
    @Test
    fun `should get scan job by id`() {
        // Create a job
        val request = CreateScanJobRequest(
            name = "Get By ID Test Job",
            description = "Job for get by ID test",
            sourceDirectory = "/get/test",
            filePattern = "*.txt",
            scanIntervalType = ScanIntervalType.CRON,
            scanIntervalValue = "0 0 * * * *",
            parserId = "txt-parser",
            isActive = false
        )
        
        val createResult = mockMvc.perform(
            post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
        .andReturn()
        
        val response = objectMapper.readTree(createResult.response.contentAsString)
        val jobId = response.get("id").asText()
        
        // Get job by ID
        mockMvc.perform(get("/api/v1/jobs/$jobId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(jobId))
            .andExpect(jsonPath("$.name").value("Get By ID Test Job"))
            .andExpect(jsonPath("$.isActive").value(false))
    }
    
    @Test
    fun `should update scan job`() {
        // Create a job
        val createRequest = CreateScanJobRequest(
            name = "Update Test Job",
            description = "Original description",
            sourceDirectory = "/update/test",
            filePattern = "*.log",
            scanIntervalType = ScanIntervalType.FIXED,
            scanIntervalValue = "PT10M",
            parserId = "log-parser",
            isActive = true
        )
        
        val createResult = mockMvc.perform(
            post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
        )
        .andExpect(status().isCreated)
        .andReturn()
        
        val response = objectMapper.readTree(createResult.response.contentAsString)
        val jobId = response.get("id").asText()
        
        // Update the job
        val updateRequest = UpdateScanJobRequest(
            description = "Updated description",
            isActive = false,
            priority = 5
        )
        
        mockMvc.perform(
            put("/api/v1/jobs/$jobId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.description").value("Updated description"))
        .andExpect(jsonPath("$.isActive").value(false))
        .andExpect(jsonPath("$.priority").value(5))
    }
    
    @Test
    fun `should delete scan job`() {
        // Create a job
        val request = CreateScanJobRequest(
            name = "Delete Test Job",
            description = "Job to be deleted",
            sourceDirectory = "/delete/test",
            filePattern = "*.json",
            scanIntervalType = ScanIntervalType.CRON,
            scanIntervalValue = "0 30 * * * *",
            parserId = "json-parser",
            isActive = true
        )
        
        val createResult = mockMvc.perform(
            post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
        .andReturn()
        
        val response = objectMapper.readTree(createResult.response.contentAsString)
        val jobId = response.get("id").asText()
        
        // Delete the job
        mockMvc.perform(delete("/api/v1/jobs/$jobId"))
            .andExpect(status().isNoContent)
        
        // Verify job is deleted
        mockMvc.perform(get("/api/v1/jobs/$jobId"))
            .andExpect(status().isNotFound)
    }
    
    @Test
    fun `should get active scan jobs`() {
        // Create active job
        val activeRequest = CreateScanJobRequest(
            name = "Active Job",
            description = "This job is active",
            sourceDirectory = "/active",
            filePattern = "*.xml",
            scanIntervalType = ScanIntervalType.FIXED,
            scanIntervalValue = "PT15M",
            parserId = "xml-parser",
            isActive = true
        )
        
        mockMvc.perform(
            post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activeRequest))
        )
        .andExpect(status().isCreated)
        
        // Create inactive job
        val inactiveRequest = CreateScanJobRequest(
            name = "Inactive Job",
            description = "This job is inactive",
            sourceDirectory = "/inactive",
            filePattern = "*.yaml",
            scanIntervalType = ScanIntervalType.CRON,
            scanIntervalValue = "0 0 0 * * *",
            parserId = "yaml-parser",
            isActive = false
        )
        
        mockMvc.perform(
            post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inactiveRequest))
        )
        .andExpect(status().isCreated)
        
        // Get only active jobs
        mockMvc.perform(get("/api/v1/jobs/active"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[*].isActive", everyItem(equalTo(true))))
    }
    
    @Test
    fun `should validate scan job creation`() {
        // Test with invalid data
        val invalidRequest = CreateScanJobRequest(
            name = "", // Empty name should fail
            description = "Invalid job",
            sourceDirectory = "/test",
            filePattern = "*.txt",
            scanIntervalType = ScanIntervalType.CRON,
            scanIntervalValue = "invalid-cron", // Invalid cron expression
            parserId = "parser",
            isActive = true
        )
        
        mockMvc.perform(
            post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
        .andExpect(status().isBadRequest)
    }
}