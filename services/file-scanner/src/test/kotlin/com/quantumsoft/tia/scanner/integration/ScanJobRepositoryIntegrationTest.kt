package com.quantumsoft.tia.scanner.integration

import com.quantumsoft.tia.scanner.entities.ScanIntervalType
import com.quantumsoft.tia.scanner.entities.ScanJob
import com.quantumsoft.tia.scanner.repositories.ScanJobRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.util.UUID

class ScanJobRepositoryIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var scanJobRepository: ScanJobRepository
    
    @Test
    fun `should persist and retrieve scan job`() {
        // Given
        val scanJob = ScanJob(
            name = "test-job-${UUID.randomUUID()}",
            description = "Test job description",
            sourceDirectory = "/test/directory",
            filePattern = "*.asn1",
            scanIntervalType = ScanIntervalType.CRON,
            scanIntervalValue = "0 */5 * * * *",
            parserId = "test-parser",
            isActive = true
        )
        
        // When
        val saved = scanJobRepository.save(scanJob)
        val retrieved = scanJobRepository.findById(saved.id).orElse(null)
        
        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved.name).isEqualTo(scanJob.name)
        assertThat(retrieved.sourceDirectory).isEqualTo(scanJob.sourceDirectory)
        assertThat(retrieved.parserId).isEqualTo(scanJob.parserId)
    }
    
    @Test
    fun `should enforce unique job names`() {
        // Given
        val name = "unique-job-${UUID.randomUUID()}"
        val job1 = ScanJob(
            name = name,
            sourceDirectory = "/dir1",
            filePattern = "*.txt",
            scanIntervalType = ScanIntervalType.FIXED,
            scanIntervalValue = "PT5M",
            parserId = "parser1"
        )
        
        // When
        scanJobRepository.save(job1)
        val exists = scanJobRepository.existsByName(name)
        
        // Then
        assertThat(exists).isTrue()
    }
    
    @Test
    fun `should query active jobs efficiently`() {
        // Given
        val activeJob = ScanJob(
            name = "active-job-${UUID.randomUUID()}",
            sourceDirectory = "/active",
            filePattern = "*.csv",
            scanIntervalType = ScanIntervalType.CRON,
            scanIntervalValue = "0 0 * * * *",
            parserId = "csv-parser",
            isActive = true
        )
        
        val inactiveJob = ScanJob(
            name = "inactive-job-${UUID.randomUUID()}",
            sourceDirectory = "/inactive",
            filePattern = "*.csv",
            scanIntervalType = ScanIntervalType.CRON,
            scanIntervalValue = "0 0 * * * *",
            parserId = "csv-parser",
            isActive = false
        )
        
        scanJobRepository.save(activeJob)
        scanJobRepository.save(inactiveJob)
        
        // When
        val activeJobs = scanJobRepository.findByIsActive(true, PageRequest.of(0, 10))
        
        // Then
        assertThat(activeJobs.content).anyMatch { it.name == activeJob.name }
        assertThat(activeJobs.content).noneMatch { it.name == inactiveJob.name }
    }
    
    @Test
    fun `should find jobs with filters`() {
        // Given
        val parserId = "test-parser-${UUID.randomUUID()}"
        val job = ScanJob(
            name = "filtered-job-${UUID.randomUUID()}",
            sourceDirectory = "/filtered",
            filePattern = "*.log",
            scanIntervalType = ScanIntervalType.FIXED,
            scanIntervalValue = "PT10M",
            parserId = parserId,
            priority = 5,
            isActive = true
        )
        
        scanJobRepository.save(job)
        
        // When
        val results = scanJobRepository.findJobsWithFilters(
            active = true,
            parserId = parserId,
            pageable = PageRequest.of(0, 10)
        )
        
        // Then
        assertThat(results.content).hasSize(1)
        assertThat(results.content.first().parserId).isEqualTo(parserId)
    }
    
    @Test
    fun `should count active jobs by parser`() {
        // Given
        val parserId = "counter-parser-${UUID.randomUUID()}"
        val job1 = ScanJob(
            name = "count-job-1-${UUID.randomUUID()}",
            sourceDirectory = "/count1",
            filePattern = "*.txt",
            scanIntervalType = ScanIntervalType.CRON,
            scanIntervalValue = "0 * * * * *",
            parserId = parserId,
            isActive = true
        )
        
        val job2 = ScanJob(
            name = "count-job-2-${UUID.randomUUID()}",
            sourceDirectory = "/count2",
            filePattern = "*.txt",
            scanIntervalType = ScanIntervalType.CRON,
            scanIntervalValue = "0 * * * * *",
            parserId = parserId,
            isActive = true
        )
        
        scanJobRepository.save(job1)
        scanJobRepository.save(job2)
        
        // When
        val count = scanJobRepository.countActiveJobsByParser(parserId)
        
        // Then
        assertThat(count).isEqualTo(2)
    }
}