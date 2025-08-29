package com.quantumsoft.tia.scanner.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.quantumsoft.tia.scanner.dto.files.CleanupRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.hamcrest.Matchers.*

@AutoConfigureMockMvc
class FileOperationsE2ETest : BaseE2ETest() {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @Test
    fun `should get file statistics`() {
        mockMvc.perform(get("/api/v1/files/statistics"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalScanned").exists())
            .andExpect(jsonPath("$.totalProcessed").exists())
            .andExpect(jsonPath("$.totalFailed").exists())
            .andExpect(jsonPath("$.totalSizeBytes").exists())
            .andExpect(jsonPath("$.averageProcessingTimeMs").exists())
            .andExpect(jsonPath("$.lastScanTime").exists())
    }
    
    @Test
    fun `should perform cleanup with default settings`() {
        val request = CleanupRequest(
            daysToKeep = 30
        )
        
        mockMvc.perform(
            post("/api/v1/files/cleanup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.filesDeleted").exists())
        .andExpect(jsonPath("$.spaceSavedBytes").exists())
        .andExpect(jsonPath("$.filesDeleted").isNumber)
    }
    
    @Test
    fun `should get recent files`() {
        mockMvc.perform(get("/api/v1/files/recent")
            .param("limit", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
    
    @Test
    fun `should get files by status`() {
        mockMvc.perform(get("/api/v1/files")
            .param("status", "PROCESSED")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.pageable").exists())
            .andExpect(jsonPath("$.totalElements").exists())
            .andExpect(jsonPath("$.totalPages").exists())
    }
    
    @Test
    fun `should get file processing history`() {
        // This would require a file to exist first
        // For now, just test that the endpoint responds correctly
        val testFileId = "00000000-0000-0000-0000-000000000000"
        
        mockMvc.perform(get("/api/v1/files/$testFileId/history"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
    
    @Test
    fun `should get failed files`() {
        mockMvc.perform(get("/api/v1/files/failed")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.pageable.pageNumber").value(0))
            .andExpect(jsonPath("$.pageable.pageSize").value(10))
    }
    
    @Test
    fun `should get duplicate files`() {
        mockMvc.perform(get("/api/v1/files/duplicates"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
    
    @Test
    fun `should validate cleanup request`() {
        val invalidRequest = CleanupRequest(
            daysToKeep = 0 // Invalid: must be at least 1
        )
        
        mockMvc.perform(
            post("/api/v1/files/cleanup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
        .andExpect(status().isBadRequest)
    }
}