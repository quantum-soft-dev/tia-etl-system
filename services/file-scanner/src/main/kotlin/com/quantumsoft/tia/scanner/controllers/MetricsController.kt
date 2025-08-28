package com.quantumsoft.tia.scanner.controllers

import com.quantumsoft.tia.scanner.components.QueueManager
import com.quantumsoft.tia.scanner.metrics.MetricsCollectorImpl
import com.quantumsoft.tia.scanner.metrics.MetricsSummary
import com.quantumsoft.tia.scanner.models.QueueStatistics
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration

@RestController
@RequestMapping("/api/v1/scanner/metrics")
class MetricsController(
    private val metricsCollector: MetricsCollectorImpl,
    private val queueManager: QueueManager
) {
    
    @GetMapping
    fun getMetrics(): ResponseEntity<ScannerMetricsDto> {
        val metricsSummary = metricsCollector.getMetricsSummary()
        val queueStatistics = runBlocking {
            queueManager.getQueueStatistics()
        }
        
        val metrics = ScannerMetricsDto(
            scanning = ScanningMetrics(
                totalFilesScanned = metricsSummary.totalFilesScanned,
                totalFilesQueued = metricsSummary.totalFilesQueued,
                totalFilesSkipped = metricsSummary.totalFilesSkipped,
                scanRate = calculateScanRate(metricsSummary),
                averageScanDuration = metricsSummary.averageScanDuration
            ),
            queue = QueueMetrics(
                depth = queueStatistics.currentDepth,
                throughput = queueStatistics.throughput,
                deadLetterCount = queueStatistics.deadLetterCount,
                depthByPriority = queueStatistics.depthByPriority.mapKeys { it.key.name }
            ),
            performance = PerformanceMetrics(
                averageScanDuration = metricsSummary.averageScanDuration,
                averageProcessingDuration = metricsSummary.averageProcessingDuration,
                failureRate = calculateFailureRate(metricsSummary, queueStatistics),
                totalErrors = metricsSummary.totalErrors
            ),
            cluster = ClusterMetrics(
                instanceCount = 1, // TODO: Get from cluster coordinator
                healthyInstances = 1,
                totalCapacity = calculateCapacity(metricsSummary)
            )
        )
        
        return ResponseEntity.ok(metrics)
    }
    
    @GetMapping("/queue")
    fun getQueueMetrics(): ResponseEntity<QueueStatistics> {
        val stats = runBlocking {
            queueManager.getQueueStatistics()
        }
        return ResponseEntity.ok(stats)
    }
    
    @PostMapping("/queue/cleanup")
    fun cleanupExpiredLocks(): ResponseEntity<CleanupResult> {
        val cleaned = runBlocking {
            queueManager.cleanupExpiredLocks()
        }
        
        return ResponseEntity.ok(
            CleanupResult(
                cleanedLocks = cleaned,
                message = "Cleaned up $cleaned expired locks"
            )
        )
    }
    
    private fun calculateScanRate(summary: MetricsSummary): Double {
        return if (summary.averageScanDuration.toMillis() > 0) {
            summary.totalFilesScanned.toDouble() / (summary.averageScanDuration.toMillis() / 1000.0)
        } else 0.0
    }
    
    private fun calculateFailureRate(summary: MetricsSummary, queueStats: QueueStatistics): Double {
        val total = queueStats.totalProcessed + queueStats.totalFailed
        return if (total > 0) {
            (queueStats.totalFailed.toDouble() / total) * 100
        } else 0.0
    }
    
    private fun calculateCapacity(summary: MetricsSummary): Double {
        // Estimate capacity based on average processing rate
        return if (summary.averageProcessingDuration.toMillis() > 0) {
            1000.0 / summary.averageProcessingDuration.toMillis() * 60 // files per minute
        } else 0.0
    }
}

data class ScannerMetricsDto(
    val scanning: ScanningMetrics,
    val queue: QueueMetrics,
    val performance: PerformanceMetrics,
    val cluster: ClusterMetrics
)

data class ScanningMetrics(
    val totalFilesScanned: Long,
    val totalFilesQueued: Long,
    val totalFilesSkipped: Long,
    val scanRate: Double,
    val averageScanDuration: Duration
)

data class QueueMetrics(
    val depth: Long,
    val throughput: Double,
    val deadLetterCount: Long,
    val depthByPriority: Map<String, Long>
)

data class PerformanceMetrics(
    val averageScanDuration: Duration,
    val averageProcessingDuration: Duration,
    val failureRate: Double,
    val totalErrors: Long
)

data class ClusterMetrics(
    val instanceCount: Int,
    val healthyInstances: Int,
    val totalCapacity: Double
)

data class CleanupResult(
    val cleanedLocks: Int,
    val message: String
)