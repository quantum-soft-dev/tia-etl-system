package com.quantumsoft.tia.scanner.controllers

import com.quantumsoft.tia.scanner.dto.*
import com.quantumsoft.tia.scanner.services.ScanJobService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/scanner/jobs")
class ScanJobController(
    private val scanJobService: ScanJobService
) {
    
    @GetMapping
    fun listJobs(
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) parserId: String?,
        @PageableDefault(size = 20, sort = ["priority"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<ScanJobDto>> {
        val jobs = scanJobService.findJobs(active, parserId, pageable)
        return ResponseEntity.ok(jobs)
    }
    
    @GetMapping("/{jobId}")
    fun getJob(@PathVariable jobId: UUID): ResponseEntity<ScanJobDetailDto> {
        val job = scanJobService.findById(jobId)
        return if (job != null) {
            ResponseEntity.ok(job)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping
    fun createJob(@Valid @RequestBody request: CreateScanJobRequest): ResponseEntity<ScanJobDto> {
        val job = scanJobService.createJob(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(job)
    }
    
    @PutMapping("/{jobId}")
    fun updateJob(
        @PathVariable jobId: UUID,
        @Valid @RequestBody request: UpdateScanJobRequest
    ): ResponseEntity<ScanJobDto> {
        val job = scanJobService.updateJob(jobId, request)
        return if (job != null) {
            ResponseEntity.ok(job)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @DeleteMapping("/{jobId}")
    fun deleteJob(@PathVariable jobId: UUID): ResponseEntity<Void> {
        val deleted = scanJobService.deleteJob(jobId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/{jobId}/scan")
    fun triggerScan(
        @PathVariable jobId: UUID,
        @RequestBody(required = false) request: TriggerScanRequest?
    ): ResponseEntity<ScanExecutionDto> {
        val force = request?.force ?: false
        val execution = scanJobService.triggerScan(jobId, force)
        
        return if (execution != null) {
            ResponseEntity.accepted().body(execution)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/{jobId}/executions")
    fun getJobExecutions(
        @PathVariable jobId: UUID,
        @PageableDefault(size = 20, sort = ["startedAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<ScanExecutionDto>> {
        val executions = scanJobService.getExecutions(jobId, pageable)
        return ResponseEntity.ok(executions)
    }
    
    @GetMapping("/{jobId}/statistics")
    fun getJobStatistics(@PathVariable jobId: UUID): ResponseEntity<JobStatisticsDto> {
        val stats = scanJobService.getJobStatistics(jobId)
        return if (stats != null) {
            ResponseEntity.ok(stats)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}