package com.quantumsoft.tia.scanner.e2e

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@AutoConfigureMockMvc
class HealthE2ETest : BaseE2ETest() {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Test
    fun `should return health status`() {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").exists())
    }
    
    @Test
    fun `should return database health`() {
        mockMvc.perform(get("/health/db"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.database").value("PostgreSQL"))
    }
    
    @Test
    fun `should return redis health`() {
        mockMvc.perform(get("/health/redis"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.redis").exists())
    }
    
    @Test
    fun `should return liveness probe`() {
        mockMvc.perform(get("/health/liveness"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }
    
    @Test
    fun `should return readiness probe`() {
        mockMvc.perform(get("/health/readiness"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.database").value("UP"))
            .andExpect(jsonPath("$.redis").value("UP"))
            .andExpect(jsonPath("$.diskSpace").value("UP"))
    }
    
    @Test
    fun `should return actuator info`() {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk)
    }
    
    @Test
    fun `should return actuator health`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }
    
    @Test
    fun `should return actuator metrics`() {
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.names").isArray)
    }
    
    @Test
    fun `should return specific actuator metric`() {
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("jvm.memory.used"))
            .andExpect(jsonPath("$.measurements").isArray)
    }
    
    @Test
    fun `should return prometheus metrics`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes")))
    }
}