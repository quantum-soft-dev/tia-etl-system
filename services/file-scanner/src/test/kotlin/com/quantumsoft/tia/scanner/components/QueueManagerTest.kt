package com.quantumsoft.tia.scanner.components

import com.quantumsoft.tia.scanner.models.QueuedFile
import com.quantumsoft.tia.scanner.models.FileQueueStatus
import com.quantumsoft.tia.scanner.models.ScannedFile
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.ListOperations
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class QueueManagerTest {

    private lateinit var queueManager: QueueManager
    private val redisTemplate = mockk<RedisTemplate<String, String>>()
    private val valueOps = mockk<ValueOperations<String, String>>()
    private val setOps = mockk<SetOperations<String, String>>()
    private val listOps = mockk<ListOperations<String, String>>()
    
    @BeforeEach
    fun setUp() {
        every { redisTemplate.opsForValue() } returns valueOps
        every { redisTemplate.opsForSet() } returns setOps
        every { redisTemplate.opsForList() } returns listOps
        queueManager = QueueManager(redisTemplate)
    }
    
    @Test
    fun `should queue file successfully`() = runTest {
        // Given
        val scannedFile = ScannedFile(
            filePath = "/test/path/file1.asn1",
            fileName = "file1.asn1",
            fileSizeBytes = 1024L,
            lastModified = Instant.now(),
            fileHash = "abc123"
        )
        
        every { valueOps.setIfAbsent(any(), any(), any(), any()) } returns true
        every { listOps.rightPush("file_queue", any()) } returns 1L
        every { setOps.add("queued_files", any()) } returns 1L
        
        // When
        val result = queueManager.queueFile(scannedFile, priority = 1, jobId = "job-123")
        
        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.queuedFile?.filePath).isEqualTo("/test/path/file1.asn1")
        assertThat(result.queuedFile?.priority).isEqualTo(1)
        assertThat(result.queuedFile?.jobId).isEqualTo("job-123")
        
        verify { valueOps.setIfAbsent(match { it.startsWith("lock:") }, any(), 5L, TimeUnit.MINUTES) }
        verify { listOps.rightPush("file_queue", any()) }
        verify { setOps.add("queued_files", "/test/path/file1.asn1") }
    }
    
    @Test
    fun `should fail to queue duplicate file`() = runTest {
        // Given
        val scannedFile = ScannedFile(
            filePath = "/test/path/duplicate.asn1",
            fileName = "duplicate.asn1",
            fileSizeBytes = 1024L,
            lastModified = Instant.now()
        )
        
        every { valueOps.setIfAbsent(any(), any(), any(), any()) } returns false
        
        // When
        val result = queueManager.queueFile(scannedFile, priority = 1, jobId = "job-123")
        
        // Then
        assertThat(result.isSuccess).isFalse()
        assertThat(result.error).isEqualTo("File already queued or being processed")
        
        verify(exactly = 0) { listOps.rightPush(any(), any()) }
        verify(exactly = 0) { setOps.add(any(), any()) }
    }
    
    @Test
    fun `should queue files by priority`() = runTest {
        // Given
        val highPriorityFile = ScannedFile("/high/priority.asn1", "priority.asn1", 1024L, Instant.now())
        val lowPriorityFile = ScannedFile("/low/priority.asn1", "priority.asn1", 1024L, Instant.now())
        
        every { valueOps.setIfAbsent(any(), any(), any(), any()) } returns true
        every { listOps.rightPush(any(), any()) } returns 1L
        every { setOps.add(any(), any()) } returns 1L
        
        // When
        val highResult = queueManager.queueFile(highPriorityFile, priority = 10, jobId = "job-123")
        val lowResult = queueManager.queueFile(lowPriorityFile, priority = 1, jobId = "job-123")
        
        // Then
        assertThat(highResult.isSuccess).isTrue()
        assertThat(lowResult.isSuccess).isTrue()
        
        // Verify high priority queue is used
        verify { listOps.rightPush("file_queue_priority_10", any()) }
        verify { listOps.rightPush("file_queue_priority_1", any()) }
    }
    
    @Test
    fun `should dequeue next file by priority`() = runTest {
        // Given
        val queuedFileJson = """{"filePath":"/test/file.asn1","fileName":"file.asn1","priority":5,"jobId":"job-123","queuedAt":"2024-01-01T10:00:00Z","status":"QUEUED"}"""
        
        every { listOps.leftPop("file_queue_priority_10") } returns null
        every { listOps.leftPop("file_queue_priority_5") } returns queuedFileJson
        every { setOps.remove("queued_files", "/test/file.asn1") } returns 1L
        every { setOps.add("processing_files", "/test/file.asn1") } returns 1L
        
        // When
        val result = queueManager.dequeueNext()
        
        // Then
        assertThat(result).isNotNull()
        assertThat(result?.filePath).isEqualTo("/test/file.asn1")
        assertThat(result?.priority).isEqualTo(5)
        assertThat(result?.status).isEqualTo(FileQueueStatus.PROCESSING)
        
        verify { listOps.leftPop("file_queue_priority_10") }
        verify { listOps.leftPop("file_queue_priority_5") }
        verify { setOps.remove("queued_files", "/test/file.asn1") }
        verify { setOps.add("processing_files", "/test/file.asn1") }
    }
    
    @Test
    fun `should return null when no files in queue`() = runTest {
        // Given - empty queues
        every { listOps.leftPop(any()) } returns null
        
        // When
        val result = queueManager.dequeueNext()
        
        // Then
        assertThat(result).isNull()
    }
    
    @Test
    fun `should mark file as completed`() = runTest {
        // Given
        val filePath = "/test/completed.asn1"
        
        every { setOps.remove("processing_files", filePath) } returns 1L
        every { setOps.add("completed_files", filePath) } returns 1L
        every { valueOps.set(match { it.contains("result:") }, any()) } returns Unit
        every { redisTemplate.expire(any(), any(), any()) } returns true
        
        // When
        val result = queueManager.markCompleted(filePath, "Processing completed successfully")
        
        // Then
        assertThat(result.isSuccess).isTrue()
        
        verify { setOps.remove("processing_files", filePath) }
        verify { setOps.add("completed_files", filePath) }
        verify { valueOps.set(match { it.startsWith("result:") }, "Processing completed successfully") }
        verify { redisTemplate.expire(match { it.startsWith("result:") }, 24L, TimeUnit.HOURS) }
    }
    
    @Test
    fun `should mark file as failed and retry`() = runTest {
        // Given
        val filePath = "/test/failed.asn1"
        val errorMessage = "Processing failed"
        
        every { valueOps.get(match { it.contains("retry_count:") }) } returns "1"
        every { valueOps.increment(match { it.contains("retry_count:") }) } returns 2L
        every { setOps.remove("processing_files", filePath) } returns 1L
        every { listOps.rightPush(any(), any()) } returns 1L
        every { setOps.add("queued_files", filePath) } returns 1L
        
        // When
        val result = queueManager.markFailed(filePath, errorMessage, maxRetries = 3)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.retryScheduled).isTrue()
        
        verify { valueOps.increment(match { it.startsWith("retry_count:") }) }
        verify { setOps.remove("processing_files", filePath) }
        verify { listOps.rightPush(match { it.contains("retry") }, any()) }
        verify { setOps.add("queued_files", filePath) }
    }
    
    @Test
    fun `should mark file as permanently failed after max retries`() = runTest {
        // Given
        val filePath = "/test/failed.asn1"
        val errorMessage = "Processing failed"
        
        every { valueOps.get(match { it.contains("retry_count:") }) } returns "3"
        every { setOps.remove("processing_files", filePath) } returns 1L
        every { setOps.add("failed_files", filePath) } returns 1L
        every { valueOps.set(match { it.contains("error:") }, any()) } returns Unit
        every { redisTemplate.expire(any(), any(), any()) } returns true
        
        // When
        val result = queueManager.markFailed(filePath, errorMessage, maxRetries = 3)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.retryScheduled).isFalse()
        assertThat(result.permanentFailure).isTrue()
        
        verify { setOps.remove("processing_files", filePath) }
        verify { setOps.add("failed_files", filePath) }
        verify { valueOps.set(match { it.startsWith("error:") }, errorMessage) }
        verify(exactly = 0) { listOps.rightPush(any(), any()) }
    }
    
    @Test
    fun `should get queue statistics`() = runTest {
        // Given
        every { setOps.size("queued_files") } returns 10L
        every { setOps.size("processing_files") } returns 3L
        every { setOps.size("completed_files") } returns 25L
        every { setOps.size("failed_files") } returns 2L
        every { listOps.size("file_queue_priority_10") } returns 2L
        every { listOps.size("file_queue_priority_5") } returns 5L
        every { listOps.size("file_queue_priority_1") } returns 3L
        
        // When
        val stats = queueManager.getQueueStatistics()
        
        // Then
        assertThat(stats.totalQueued).isEqualTo(10)
        assertThat(stats.totalProcessing).isEqualTo(3)
        assertThat(stats.totalCompleted).isEqualTo(25)
        assertThat(stats.totalFailed).isEqualTo(2)
        assertThat(stats.queuesByPriority[10]).isEqualTo(2)
        assertThat(stats.queuesByPriority[5]).isEqualTo(5)
        assertThat(stats.queuesByPriority[1]).isEqualTo(3)
    }
    
    @Test
    fun `should clean up expired locks`() = runTest {
        // Given
        val expiredLocks = setOf("lock:expired1.asn1", "lock:expired2.asn1")
        every { redisTemplate.keys("lock:*") } returns expiredLocks
        every { redisTemplate.getExpire("lock:expired1.asn1") } returns -1L
        every { redisTemplate.getExpire("lock:expired2.asn1") } returns -1L
        every { redisTemplate.delete("lock:expired1.asn1") } returns 1L
        every { redisTemplate.delete("lock:expired2.asn1") } returns 1L
        
        // When
        val cleanedCount = queueManager.cleanupExpiredLocks()
        
        // Then
        assertThat(cleanedCount).isEqualTo(2)
        
        verify { redisTemplate.delete("lock:expired1.asn1") }
        verify { redisTemplate.delete("lock:expired2.asn1") }
    }
    
    @Test
    fun `should handle Redis connection failures gracefully`() = runTest {
        // Given
        every { valueOps.setIfAbsent(any(), any(), any(), any()) } throws RuntimeException("Redis connection failed")
        
        val scannedFile = ScannedFile("/test/redis-fail.asn1", "redis-fail.asn1", 1024L, Instant.now())
        
        // When
        val result = queueManager.queueFile(scannedFile, priority = 1, jobId = "job-123")
        
        // Then
        assertThat(result.isSuccess).isFalse()
        assertThat(result.error).contains("Redis connection failed")
    }
    
    @Test
    fun `should batch queue multiple files efficiently`() = runTest {
        // Given
        val files = (1..5).map { index ->
            ScannedFile("/batch/file$index.asn1", "file$index.asn1", 1024L, Instant.now())
        }
        
        every { valueOps.setIfAbsent(any(), any(), any(), any()) } returns true
        every { listOps.rightPushAll(any(), any()) } returns 5L
        every { setOps.add("queued_files", *anyVararg()) } returns 5L
        
        // When
        val results = queueManager.queueFiles(files, priority = 5, jobId = "batch-job")
        
        // Then
        assertThat(results.successCount).isEqualTo(5)
        assertThat(results.failureCount).isEqualTo(0)
        assertThat(results.queuedFiles).hasSize(5)
        
        verify { listOps.rightPushAll(any(), any()) }
    }
}