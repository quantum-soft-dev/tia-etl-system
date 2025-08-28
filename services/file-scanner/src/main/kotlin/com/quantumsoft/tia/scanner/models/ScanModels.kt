package com.quantumsoft.tia.scanner.models

import java.time.Duration
import java.time.Instant

data class ScanConfiguration(
    val sourceDirectory: String,
    val filePattern: String,
    val recursiveScan: Boolean = true,
    val maxDepth: Int = 10,
    val maxFileSizeMb: Int = 1024,
    val followSymlinks: Boolean = false,
    val scanTimeout: Duration = Duration.ofMinutes(5)
)

data class ScanResult(
    val filesDiscovered: Int = 0,
    val files: List<ScannedFile> = emptyList(),
    val errors: List<String> = emptyList(),
    val skippedDirectories: List<String> = emptyList(),
    val skippedFiles: List<SkippedFile> = emptyList(),
    val timedOut: Boolean = false,
    val scanDuration: Duration = Duration.ZERO,
    val timestamp: Instant = Instant.now()
)

data class ScannedFile(
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val lastModified: Instant,
    val fileHash: String? = null
)

data class SkippedFile(
    val filePath: String,
    val reason: String
)

/**
 * File format enumeration for supported file types
 */
enum class FileFormat {
    ASN1,
    CSV,
    UNKNOWN
}

/**
 * Result of file validation including format detection and accessibility
 */
data class ValidationResult(
    val isValid: Boolean,
    val isAccessible: Boolean = true,
    val detectedFormat: FileFormat,
    val fileHash: String? = null,
    val fileSizeBytes: Long = 0L,
    val errors: List<String> = emptyList()
)

/**
 * Queue status enumeration for file processing states
 */
enum class FileQueueStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    RETRY
}

/**
 * Represents a file queued for processing with metadata
 */
data class QueuedFile(
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val fileHash: String? = null,
    val priority: Int = 1,
    val jobId: String,
    val queuedAt: Instant = Instant.now(),
    val status: FileQueueStatus = FileQueueStatus.QUEUED,
    val retryCount: Int = 0
)

/**
 * Result of queuing operation
 */
data class QueueResult(
    val isSuccess: Boolean,
    val queuedFile: QueuedFile? = null,
    val error: String? = null
)

/**
 * Result of batch queuing operation
 */
data class BatchQueueResult(
    val successCount: Int,
    val failureCount: Int,
    val queuedFiles: List<QueuedFile> = emptyList(),
    val errors: List<String> = emptyList()
)

/**
 * Result of marking file as failed
 */
data class FailureResult(
    val isSuccess: Boolean,
    val retryScheduled: Boolean = false,
    val permanentFailure: Boolean = false,
    val error: String? = null
)

/**
 * Queue statistics for monitoring
 */
data class QueueStatistics(
    val totalQueued: Long,
    val totalProcessing: Long,
    val totalCompleted: Long,
    val totalFailed: Long,
    val queuesByPriority: Map<Int, Long> = emptyMap(),
    val averageWaitTime: Duration = Duration.ZERO,
    val throughputPerHour: Double = 0.0
)