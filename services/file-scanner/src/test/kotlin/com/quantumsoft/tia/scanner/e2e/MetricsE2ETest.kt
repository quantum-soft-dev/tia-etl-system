package com.quantumsoft.tia.scanner.e2e

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.http.MediaType
import org.hamcrest.Matchers.*
import java.time.LocalDate

@AutoConfigureMockMvc
class MetricsE2ETest : BaseE2ETest() {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Test
    fun `should get scanner metrics`() {
        mockMvc.perform(get("/api/v1/metrics/scanner"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.filesScanned").exists())
            .andExpect(jsonPath("$.filesQueued").exists())
            .andExpect(jsonPath("$.filesProcessed").exists())
            .andExpect(jsonPath("$.filesFailed").exists())
            .andExpect(jsonPath("$.averageProcessingTimeMs").exists())
            .andExpect(jsonPath("$.queueDepth").exists())
            .andExpect(jsonPath("$.activeJobs").exists())
            .andExpect(jsonPath("$.lastScanTime").exists())
    }
    
    @Test
    fun `should get processing metrics by date range`() {
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()
        
        mockMvc.perform(get("/api/v1/metrics/processing")
            .param("startDate", startDate.toString())
            .param("endDate", endDate.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
    
    @Test
    fun `should get queue metrics`() {
        mockMvc.perform(get("/api/v1/metrics/queue"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentDepth").exists())
            .andExpect(jsonPath("$.processingRate").exists())
            .andExpect(jsonPath("$.averageWaitTimeMs").exists())
            .andExpect(jsonPath("$.depthByPriority").exists())
            .andExpect(jsonPath("$.deadLetterCount").exists())
    }
    
    @Test
    fun `should get parser metrics`() {
        mockMvc.perform(get("/api/v1/metrics/parsers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
    
    @Test
    fun `should get error metrics`() {
        mockMvc.perform(get("/api/v1/metrics/errors")
            .param("limit", "50"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
    
    @Test
    fun `should get performance metrics`() {
        mockMvc.perform(get("/api/v1/metrics/performance"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cpuUsage").exists())
            .andExpect(jsonPath("$.memoryUsage").exists())
            .andExpect(jsonPath("$.diskUsage").exists())
            .andExpect(jsonPath("$.threadCount").exists())
            .andExpect(jsonPath("$.gcCount").exists())
            .andExpect(jsonPath("$.gcTimeMs").exists())
    }
    
    @Test
    fun `should get hourly metrics`() {
        mockMvc.perform(get("/api/v1/metrics/hourly")
            .param("hours", "24"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
    
    @Test
    fun `should export metrics`() {
        mockMvc.perform(get("/api/v1/metrics/export")
            .param("format", "json"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }
    
    @Test
    fun `should reset metrics with authorization`() {
        // This should require admin authorization
        mockMvc.perform(post("/api/v1/metrics/reset"))
            .andExpect(status().isUnauthorized)
    }
}