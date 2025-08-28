package com.quantumsoft.tia.scanner.metrics

import java.time.Duration

interface MetricsCollector {
    fun recordScanDuration(directory: String, duration: Duration)
    fun recordFilesScanned(count: Int)
    fun recordScanRate(filesPerSecond: Double)
    fun recordError(errorType: String)
    fun recordSkippedFile(reason: String)
}