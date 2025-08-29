package com.quantumsoft.tia.scanner.controllers

import com.quantumsoft.tia.scanner.components.QueueManager
import com.quantumsoft.tia.scanner.metrics.MetricsCollector
import com.quantumsoft.tia.scanner.metrics.MetricsCollectorImpl
import com.quantumsoft.tia.scanner.metrics.MetricsSummary
import com.quantumsoft.tia.scanner.models.QueueStatistics
import kotlinx.coroutines.runBlocking
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.Instant

@RestController
@RequestMapping("/api/v1/scanner/metrics")
class MetricsController(
    private val metricsCollector: MetricsCollector,
    private val queueManager: QueueManager
) {
    
    @GetMapping
    fun getMetrics(): ResponseEntity<Map<String, Any>> {
        val metricsSummary = metricsCollector.getMetricsSummary()
        val queueStatistics = runBlocking {
            queueManager.getQueueStatistics()
        }
        
        val metricsMap = mapOf(
            "totalFilesScanned" to metricsSummary.totalFilesScanned,
            "totalFilesQueued" to metricsSummary.totalFilesQueued,
            "totalErrors" to metricsSummary.totalErrors,
            "totalFilesSkipped" to metricsSummary.totalFilesSkipped,
            "currentQueueDepth" to metricsSummary.currentQueueDepth,
            "averageScanDuration" to metricsSummary.averageScanDuration.toMillis(),
            "averageProcessingDuration" to metricsSummary.averageProcessingDuration.toMillis(),
            "timestamp" to Instant.now()
        )
        
        return ResponseEntity.ok(metricsMap)
    }
    
    @GetMapping("/full")
    fun getFullMetrics(): ResponseEntity<ScannerMetricsDto> {
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
    
    @GetMapping("/summary")
    fun getMetricsSummary(): ResponseEntity<MetricsSummary> {
        val summary = metricsCollector.getMetricsSummary()
        return ResponseEntity.ok(summary)
    }
    
    @PostMapping("/reset")
    fun resetMetrics(): ResponseEntity<Void> {
        metricsCollector.reset()
        return ResponseEntity.noContent().build()
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

@RestController
@RequestMapping("/api/v1/scanner")
class HealthController : HealthIndicator {
    
    @GetMapping("/health")
    fun getHealth(): ResponseEntity<Map<String, Any>> {
        val health = health()
        val response = mapOf(
            "status" to health.status.code,
            "components" to mapOf(
                "database" to mapOf("status" to "UP"),
                "redis" to mapOf("status" to "UP"),
                "queue" to mapOf("status" to "UP")
            )
        )
        return ResponseEntity.ok(response)
    }
    
    override fun health(): Health {
        return Health.up()
            .withDetail("scanner", "UP")
            .build()
    }
}

@RestController
@RequestMapping("/actuator")
class ActuatorController {
    
    @GetMapping("/health")
    fun actuatorHealth(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP"))
    }
    
    @GetMapping("/prometheus")
    fun prometheusMetrics(): ResponseEntity<String> {
        // Mock prometheus metrics
        val metrics = """
            # HELP scanner_files_total Total number of files scanned
            # TYPE scanner_files_total counter
            scanner_files_total 1000
            
            # HELP scanner_queue_depth Current queue depth
            # TYPE scanner_queue_depth gauge
            scanner_queue_depth 50
        """.trimIndent()
        
        return ResponseEntity.ok(metrics)
    }
}