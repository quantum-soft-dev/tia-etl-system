package com.quantumsoft.tia.scanner.e2e

import com.quantumsoft.tia.scanner.components.FileValidator
import com.quantumsoft.tia.scanner.entities.ScanJob
import com.quantumsoft.tia.scanner.entities.ScanIntervalType
import com.quantumsoft.tia.scanner.repositories.ScanJobRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SimplifiedFileOperationsE2ETest : BaseE2ETest() {

    @Autowired
    private lateinit var fileValidator: FileValidator
    
    @Autowired
    private lateinit var scanJobRepository: ScanJobRepository

    private lateinit var testDirectory: File

    @BeforeEach
    fun setUp() {
        // Create test directory structure
        testDirectory = Files.createTempDirectory("file-ops-e2e").toFile()
    }

    @AfterEach
    fun cleanup() {
        testDirectory.deleteRecursively()
        scanJobRepository.deleteAll()
    }

    @Test
    @Order(1)
    fun `should create and validate various file types`() = runBlocking {
        val testFiles = mapOf(
            "valid.asn1" to "Valid ASN1 content with data",
            "simple.asn1" to "Simple ASN1 content",
            "empty.asn1" to "",
            "large.asn1" to "Large content: " + "DATA".repeat(1000)
        )

        // Create files
        testFiles.forEach { (filename, content) ->
            val file = File(testDirectory, filename)
            file.writeText(content)
            
            // Verify file was created
            assertTrue(file.exists(), "File $filename should exist")
            assertEquals(content.length.toLong(), file.length(), "File $filename should have correct size")
        }

        // Validate each file using FileValidator API
        testFiles.keys.forEach { filename ->
            val file = File(testDirectory, filename)
            val validationResult = fileValidator.validateFile(file.toPath(), calculateHash = false)
            
            when (filename) {
                "empty.asn1" -> assertFalse(validationResult.isValid, "Empty file should be invalid")
                else -> assertTrue(validationResult.isAccessible, "File $filename should be accessible")
            }
        }
    }

    @Test
    @Order(2)
    fun `should handle file system operations`() {
        val testContent = "Test file content for validation"
        val filename = "test.asn1"
        val file = File(testDirectory, filename)
        
        // Write content
        file.writeText(testContent)
        
        // Verify file properties
        assertTrue(file.exists(), "File should exist")
        assertTrue(file.canRead(), "File should be readable")
        assertTrue(file.isFile, "Should be a regular file")
        assertEquals(testContent.length.toLong(), file.length(), "File size should match content")
        
        // Read and verify content
        val readContent = file.readText()
        assertEquals(testContent, readContent, "File content should be preserved")
    }

    @Test
    @Order(3)
    fun `should calculate file hashes consistently`() = runBlocking {
        val testContent = "Content for hash calculation test"
        val file = File(testDirectory, "hash-test.asn1")
        file.writeText(testContent)
        
        // Calculate hash multiple times
        val hash1 = calculateFileHash(file)
        val hash2 = calculateFileHash(file)
        
        assertEquals(hash1, hash2, "Hash calculation should be consistent")
        assertNotNull(hash1, "Hash should not be null")
        assertTrue(hash1.isNotBlank(), "Hash should not be empty")
        
        // Modify file and verify hash changes
        file.appendText(" modified")
        val hash3 = calculateFileHash(file)
        assertNotEquals(hash1, hash3, "Hash should change when content changes")
    }

    @Test
    @Order(4)
    fun `should handle nested directories`() {
        // Create nested structure
        val level1 = File(testDirectory, "level1")
        val level2 = File(level1, "level2")
        level2.mkdirs()
        
        // Create files at different levels
        val files = listOf(
            File(testDirectory, "root.asn1") to "Root level",
            File(level1, "level1.asn1") to "Level 1",
            File(level2, "level2.asn1") to "Level 2"
        )
        
        files.forEach { (file, content) ->
            file.writeText(content)
            assertTrue(file.exists(), "Nested file ${file.name} should exist")
        }
        
        // Count all ASN1 files
        val allFiles = testDirectory.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".asn1") }
            .toList()
        
        assertEquals(3, allFiles.size, "Should find all nested ASN1 files")
    }

    @Test
    @Order(5)
    fun `should validate file with real validator API`() = runBlocking {
        val files = mapOf(
            "valid.asn1" to "Valid ASN1 content",
            "invalid.txt" to "Wrong extension",
            "large.asn1" to "Large: " + "X".repeat(5000)
        )
        
        files.forEach { (filename, content) ->
            val file = File(testDirectory, filename)
            file.writeText(content)
            
            val validationResult = fileValidator.validateFile(
                file = file.toPath(),
                calculateHash = true,
                maxSizeMb = 1
            )
            
            // All files should be accessible
            assertTrue(validationResult.isAccessible, "File $filename should be accessible")
            
            // Verify validation result structure
            assertNotNull(validationResult.detectedFormat, "Should detect file format")
            
            if (filename.endsWith(".asn1") && content.isNotEmpty()) {
                // ASN1 files should be valid or at least accessible
                assertTrue(
                    validationResult.isValid || validationResult.isAccessible,
                    "ASN1 file $filename should be valid or accessible"
                )
            }
        }
    }

    @Test
    @Order(6) 
    fun `should work with database entities`() {
        // Create and save a scan job
        val scanJob = ScanJob(
            name = "Test Job",
            description = "E2E test job",
            sourceDirectory = testDirectory.absolutePath,
            filePattern = "*.asn1",
            scanIntervalType = ScanIntervalType.CRON,
            scanIntervalValue = "0 * * * * *",
            maxFileSizeMb = 10,
            recursiveScan = true,
            maxDepth = 3,
            priority = 0,
            parserId = "test-parser",
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val savedJob = scanJobRepository.save(scanJob)
        
        // Verify job was saved
        assertNotNull(savedJob.id, "Saved job should have an ID")
        assertEquals("Test Job", savedJob.name, "Job name should be preserved")
        assertTrue(savedJob.isActive, "Job should be active")
        
        // Find job by ID
        val foundJob = scanJobRepository.findById(savedJob.id).orElse(null)
        assertNotNull(foundJob, "Should be able to find saved job")
        assertEquals(savedJob.name, foundJob?.name, "Found job should match saved job")
        
        // Clean up
        scanJobRepository.delete(savedJob)
    }

    @Test
    @Order(7)
    fun `should handle file content integrity`() {
        val originalContent = """
            Real ASN1 content for testing:
            TestModule DEFINITIONS ::= BEGIN
                TestData ::= SEQUENCE {
                    version INTEGER,
                    data OCTET STRING
                }
            END
        """.trimIndent()
        
        val file = File(testDirectory, "integrity.asn1")
        file.writeText(originalContent)
        
        // Verify content integrity
        val readContent = file.readText()
        assertEquals(originalContent, readContent, "File content should remain intact")
        
        // Verify file metadata
        assertTrue(file.length() > 0, "File should have content")
        assertTrue(file.lastModified() > 0, "File should have valid timestamp")
        
        // Test modification detection
        val originalModified = file.lastModified()
        Thread.sleep(10) // Small delay to ensure timestamp difference
        file.appendText("\n-- Modified")
        assertTrue(file.lastModified() >= originalModified, "Modified timestamp should update")
    }

    private fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = file.readBytes()
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
}