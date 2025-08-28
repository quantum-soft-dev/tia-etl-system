package com.quantumsoft.tia.scanner.components

import com.quantumsoft.tia.scanner.metrics.MetricsCollector
import com.quantumsoft.tia.scanner.models.ScanConfiguration
import com.quantumsoft.tia.scanner.models.ScanResult
import com.quantumsoft.tia.scanner.models.ScannedFile
import com.quantumsoft.tia.scanner.models.SkippedFile
import kotlinx.coroutines.withTimeout
import java.nio.file.*
import java.time.Duration
import java.time.Instant
import kotlin.io.path.*

/**
 * Directory scanner component that recursively scans directories for files matching patterns.
 * Implements file discovery, filtering, validation, and metrics collection.
 */
class DirectoryScanner(
    private val metricsCollector: MetricsCollector? = null
) {
    
    /**
     * Scans the configured directory for files matching the specified pattern and constraints.
     */
    suspend fun scan(config: ScanConfiguration): ScanResult {
        val startTime = Instant.now()
        
        return try {
            withTimeout(config.scanTimeout.toMillis()) {
                performScan(config, startTime)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            ScanResult(
                timedOut = true,
                errors = listOf("Scan timeout exceeded: ${config.scanTimeout}"),
                scanDuration = Duration.between(startTime, Instant.now()),
                timestamp = startTime
            )
        } catch (e: Exception) {
            ScanResult(
                errors = listOf("Scan failed: ${e.message}"),
                scanDuration = Duration.between(startTime, Instant.now()),
                timestamp = startTime
            )
        }
    }
    
    private fun performScan(config: ScanConfiguration, startTime: Instant): ScanResult {
        val sourceDir = Paths.get(config.sourceDirectory)
        
        // Check if directory exists
        if (!sourceDir.exists()) {
            return ScanResult(
                errors = listOf("Directory does not exist: ${config.sourceDirectory}"),
                scanDuration = Duration.between(startTime, Instant.now()),
                timestamp = startTime
            )
        }
        
        if (!sourceDir.isDirectory()) {
            return ScanResult(
                errors = listOf("Path is not a directory: ${config.sourceDirectory}"),
                scanDuration = Duration.between(startTime, Instant.now()),
                timestamp = startTime
            )
        }
        
        val files = mutableListOf<ScannedFile>()
        val skippedFiles = mutableListOf<SkippedFile>()
        val errors = mutableListOf<String>()
        val skippedDirectories = mutableListOf<String>()
        
        try {
            val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:${config.filePattern}")
            // Create case-insensitive version for pattern matching
            val caseInsensitivePattern = config.filePattern.lowercase()
            val maxDepth = if (config.recursiveScan) config.maxDepth + 1 else 1
            
            val visitOptions = if (config.followSymlinks) arrayOf(FileVisitOption.FOLLOW_LINKS) else arrayOf<FileVisitOption>()
            Files.walk(sourceDir, maxDepth, *visitOptions)
                .use { paths ->
                    paths.forEach { path ->
                        try {
                            when {
                                path == sourceDir -> return@forEach // Skip root directory
                                path.isDirectory() -> return@forEach // Skip directories
                                !config.followSymlinks && path.isSymbolicLink() -> return@forEach // Skip symlinks if not following
                                !matchesPattern(path.fileName.toString(), caseInsensitivePattern) -> return@forEach // Skip files that don't match pattern
                                else -> {
                                    val fileSize = path.fileSize()
                                    val maxSizeBytes = config.maxFileSizeMb * 1024L * 1024L
                                    
                                    if (fileSize > maxSizeBytes) {
                                        skippedFiles.add(
                                            SkippedFile(
                                                filePath = path.toString(),
                                                reason = "File size ${fileSize / (1024 * 1024)}MB exceeds size limit ${config.maxFileSizeMb}MB"
                                            )
                                        )
                                        return@forEach
                                    }
                                    
                                    files.add(
                                        ScannedFile(
                                            filePath = path.toString(),
                                            fileName = path.fileName.toString(),
                                            fileSizeBytes = fileSize,
                                            lastModified = path.getLastModifiedTime().toInstant()
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            // Log error but continue scanning
                            errors.add("Error scanning ${path}: ${e.message}")
                        }
                    }
                }
        } catch (e: Exception) {
            errors.add("Error during directory walk: ${e.message}")
        }
        
        val endTime = Instant.now()
        val scanDuration = Duration.between(startTime, endTime)
        
        // Emit metrics
        metricsCollector?.let { collector ->
            collector.recordScanDuration(config.sourceDirectory, scanDuration)
            collector.recordFilesScanned(files.size)
            collector.recordScanRate(files.size.toDouble() / scanDuration.toSeconds().coerceAtLeast(1))
        }
        
        return ScanResult(
            filesDiscovered = files.size,
            files = files,
            errors = errors,
            skippedDirectories = skippedDirectories,
            skippedFiles = skippedFiles,
            timedOut = false,
            scanDuration = scanDuration,
            timestamp = startTime
        )
    }
    
    /**
     * Case-insensitive pattern matching for filenames
     */
    private fun matchesPattern(fileName: String, pattern: String): Boolean {
        val lowerFileName = fileName.lowercase()
        val globPattern = pattern.replace("*", ".*").replace("?", ".")
        return lowerFileName.matches(Regex(globPattern))
    }
}