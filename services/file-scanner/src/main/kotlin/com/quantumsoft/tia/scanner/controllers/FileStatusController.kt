package com.quantumsoft.tia.scanner.controllers

import com.quantumsoft.tia.scanner.dto.FileStatusDto
import com.quantumsoft.tia.scanner.dto.FileStatusFilter
import com.quantumsoft.tia.scanner.entities.FileStatus
import com.quantumsoft.tia.scanner.services.FileStatusService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/scanner/files")
class FileStatusController(
    private val fileStatusService: FileStatusService
) {
    
    @GetMapping
    fun queryFiles(
        @RequestParam(required = false) jobId: UUID?,
        @RequestParam(required = false) status: List<FileStatus>?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @RequestParam(required = false) filePattern: String?,
        @RequestParam(required = false) minSize: Long?,
        @RequestParam(required = false) maxSize: Long?,
        @PageableDefault(size = 20, sort = ["discoveredAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<FileStatusDto>> {
        
        val filter = FileStatusFilter(
            jobId = jobId,
            statuses = status,
            fromDate = from,
            toDate = to,
            filePattern = filePattern,
            minSize = minSize,
            maxSize = maxSize
        )
        
        val files = fileStatusService.queryFiles(filter, pageable)
        return ResponseEntity.ok(files)
    }
    
    @GetMapping("/{fileId}")
    fun getFile(@PathVariable fileId: UUID): ResponseEntity<FileStatusDto> {
        val file = fileStatusService.getFile(fileId)
        return if (file != null) {
            ResponseEntity.ok(file)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/{fileId}/retry")
    fun retryFile(@PathVariable fileId: UUID): ResponseEntity<FileStatusDto> {
        val file = fileStatusService.retryFile(fileId)
        return if (file != null) {
            ResponseEntity.ok(file)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @DeleteMapping("/{fileId}")
    fun deleteFile(@PathVariable fileId: UUID): ResponseEntity<Void> {
        val deleted = fileStatusService.deleteFile(fileId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/statistics")
    fun getStatistics(
        @RequestParam(required = false) jobId: UUID?
    ): ResponseEntity<FileStatisticsDto> {
        val stats = fileStatusService.getStatistics(jobId)
        return ResponseEntity.ok(stats)
    }
    
    @PostMapping("/cleanup")
    fun cleanupOldFiles(
        @RequestParam daysToKeep: Int = 30
    ): ResponseEntity<CleanupResultDto> {
        val result = fileStatusService.cleanupOldFiles(daysToKeep)
        return ResponseEntity.ok(result)
    }
}

data class FileStatisticsDto(
    val totalFiles: Long,
    val statusDistribution: Map<FileStatus, Long>,
    val averageFileSize: Double,
    val totalSizeBytes: Long,
    val oldestFile: Instant?,
    val newestFile: Instant?
)

data class CleanupResultDto(
    val deletedCount: Int,
    val beforeCount: Long,
    val afterCount: Long,
    val message: String
)