package com.quantumsoft.tia.parsers.zte.config

import com.quantumsoft.tia.parsers.zte.asn1.Asn1Decoder
import com.quantumsoft.tia.parsers.zte.core.ZteAsn1Parser
import com.quantumsoft.tia.parsers.zte.database.DatabaseWriter
import com.quantumsoft.tia.parsers.zte.utils.MetricsCollector
import com.quantumsoft.tia.parsers.zte.validation.DataValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

/**
 * Integration test for Spring Boot dependency injection configuration.
 * 
 * This test verifies that all components are properly wired through Spring's
 * IoC container and that the Dependency Inversion Principle is correctly
 * implemented across all parser components.
 * 
 * ## Test Coverage
 * 
 * - Bean creation and dependency injection
 * - Configuration property binding and validation
 * - Component interface compliance and substitutability
 * - Test profile configuration isolation
 * - Factory pattern implementation for dynamic configuration
 * 
 * @author TIA ETL Team
 * @since 1.0.0
 */
@SpringBootTest(
    classes = [
        ZteParserConfiguration::class,
        ZteParserTestConfiguration::class,
        ZteParserFactoryConfiguration::class
    ]
)
@ActiveProfiles("test")
class ZteParserConfigurationIntegrationTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext
    
    @Autowired
    private lateinit var zteParserProperties: ZteParserProperties
    
    @Autowired
    private lateinit var asn1Decoder: Asn1Decoder
    
    @Autowired
    private lateinit var dataValidator: DataValidator
    
    @Autowired
    private lateinit var databaseWriter: DatabaseWriter
    
    @Autowired
    private lateinit var metricsCollector: MetricsCollector
    
    @Autowired
    private lateinit var zteAsn1Parser: ZteAsn1Parser

    @Test
    fun `Spring context should load successfully with all required beans`() {
        // Then - Context loads and all beans are available
        assertThat(applicationContext).isNotNull
        assertThat(applicationContext.beanDefinitionCount).isGreaterThan(0)
    }

    @Test
    fun `configuration properties should be properly bound and validated`() {
        // Then - Properties are loaded with expected default values
        assertThat(zteParserProperties).isNotNull
        assertThat(zteParserProperties.parser.strictParsing).isTrue()
        assertThat(zteParserProperties.parser.maxRecordsInMemory).isEqualTo(10000)
        assertThat(zteParserProperties.asn1.strictValidation).isTrue()
        assertThat(zteParserProperties.asn1.maxStructureDepth).isEqualTo(50)
        assertThat(zteParserProperties.database.batchSize).isEqualTo(10000)
        assertThat(zteParserProperties.metrics.enableDetailedMetrics).isTrue()
    }

    @Test
    fun `ASN1 decoder bean should be properly configured and injectable`() {
        // Then - ASN.1 decoder is available and properly typed
        assertThat(asn1Decoder).isNotNull
        assertThat(asn1Decoder).isInstanceOf(Asn1Decoder::class.java)
        
        // Verify interface contract compliance
        assertThat(asn1Decoder.javaClass.interfaces).contains(Asn1Decoder::class.java)
    }

    @Test
    fun `data validator bean should be properly configured and injectable`() {
        // Then - Data validator is available and properly typed
        assertThat(dataValidator).isNotNull
        assertThat(dataValidator).isInstanceOf(DataValidator::class.java)
        
        // Verify interface contract compliance
        assertThat(dataValidator.javaClass.interfaces).contains(DataValidator::class.java)
    }

    @Test
    fun `database writer bean should be properly configured and injectable`() {
        // Then - Database writer is available and properly typed
        assertThat(databaseWriter).isNotNull
        assertThat(databaseWriter).isInstanceOf(DatabaseWriter::class.java)
        
        // Verify interface contract compliance
        assertThat(databaseWriter.javaClass.interfaces).contains(DatabaseWriter::class.java)
    }

    @Test
    fun `metrics collector bean should be properly configured and injectable`() {
        // Then - Metrics collector is available and properly typed
        assertThat(metricsCollector).isNotNull
        assertThat(metricsCollector).isInstanceOf(MetricsCollector::class.java)
        
        // Verify interface contract compliance
        assertThat(metricsCollector.javaClass.interfaces).contains(MetricsCollector::class.java)
    }

    @Test
    fun `ZTE ASN1 parser should be properly configured with all dependencies`() {
        // Then - Parser is available and properly typed
        assertThat(zteAsn1Parser).isNotNull
        assertThat(zteAsn1Parser).isInstanceOf(ZteAsn1Parser::class.java)
        
        // Verify interface contract compliance
        assertThat(zteAsn1Parser.javaClass.interfaces).contains(ZteAsn1Parser::class.java)
    }

    @Test
    fun `parser factory should be available for dynamic configuration`() {
        // Given
        val parserFactory = applicationContext.getBean("parserFactory") as (ZteParserProperties) -> ZteAsn1Parser
        
        // When - Create parser with custom configuration
        val customProperties = zteParserProperties.copy(
            parser = zteParserProperties.parser.copy(
                strictParsing = false,
                maxRecordsInMemory = 5000
            )
        )
        val customParser = parserFactory(customProperties)
        
        // Then - Factory creates properly configured parser
        assertThat(customParser).isNotNull
        assertThat(customParser).isInstanceOf(ZteAsn1Parser::class.java)
        assertThat(customParser).isNotSameAs(zteAsn1Parser)
    }

    @Test
    fun `all beans should follow Single Responsibility Principle`() {
        // Then - Each component has focused responsibilities
        assertThat(asn1Decoder.javaClass.methods.filter { it.name.startsWith("decode") || it.name.startsWith("extract") })
            .hasSizeGreaterThan(0)
            .allMatch { it.name.contains("asn1") || it.name.contains("field") || it.name.contains("structure") }
        
        assertThat(dataValidator.javaClass.methods.filter { it.name.startsWith("validate") })
            .hasSizeGreaterThan(0)
            .allMatch { it.name.contains("validate") }
        
        assertThat(databaseWriter.javaClass.methods.filter { it.name.contains("insert") || it.name.contains("batch") })
            .hasSizeGreaterThan(0)
            .allMatch { it.name.contains("insert") || it.name.contains("schema") || it.name.contains("database") }
    }

    @Test
    fun `all beans should be singletons by default`() {
        // Given
        val decoder1 = applicationContext.getBean(Asn1Decoder::class.java)
        val decoder2 = applicationContext.getBean(Asn1Decoder::class.java)
        
        val validator1 = applicationContext.getBean(DataValidator::class.java)
        val validator2 = applicationContext.getBean(DataValidator::class.java)
        
        val writer1 = applicationContext.getBean(DatabaseWriter::class.java)
        val writer2 = applicationContext.getBean(DatabaseWriter::class.java)
        
        val collector1 = applicationContext.getBean(MetricsCollector::class.java)
        val collector2 = applicationContext.getBean(MetricsCollector::class.java)
        
        val parser1 = applicationContext.getBean(ZteAsn1Parser::class.java)
        val parser2 = applicationContext.getBean(ZteAsn1Parser::class.java)
        
        // Then - Same instances are returned (singleton scope)
        assertThat(decoder1).isSameAs(decoder2)
        assertThat(validator1).isSameAs(validator2)
        assertThat(writer1).isSameAs(writer2)
        assertThat(collector1).isSameAs(collector2)
        assertThat(parser1).isSameAs(parser2)
    }

    @Test
    fun `bean dependency graph should follow Dependency Inversion Principle`() {
        // Then - High-level modules (parser) depend on abstractions (interfaces)
        // not on low-level modules (implementations)
        
        // Verify parser depends on interfaces, not implementations
        val parserConstructor = zteAsn1Parser.javaClass.constructors[0]
        val parameterTypes = parserConstructor.parameterTypes
        
        assertThat(parameterTypes).contains(Asn1Decoder::class.java)
        assertThat(parameterTypes).contains(DataValidator::class.java)
        assertThat(parameterTypes).contains(MetricsCollector::class.java)
        
        // Verify no direct dependencies on implementation classes
        assertThat(parameterTypes).allMatch { it.isInterface }
    }

    @Test
    fun `test profile should use mock implementations`() {
        // Then - Test profile provides mock implementations
        assertThat(asn1Decoder.javaClass.simpleName).contains("Mock")
        assertThat(dataValidator.javaClass.simpleName).contains("Mock")
        assertThat(databaseWriter.javaClass.simpleName).contains("Mock")
    }

    @Test
    fun `configuration validation should prevent invalid property values`() {
        // Given - Properties with validation constraints
        val parserConfig = zteParserProperties.parser
        val asn1Config = zteParserProperties.asn1
        val databaseConfig = zteParserProperties.database
        
        // Then - All values are within valid ranges
        assertThat(parserConfig.maxRecordsInMemory).isBetween(100, 100000)
        assertThat(parserConfig.threadPoolSize).isBetween(1, 32)
        assertThat(parserConfig.maxMemoryMb).isBetween(64L, 2048L)
        
        assertThat(asn1Config.maxStructureDepth).isBetween(10, 100)
        assertThat(asn1Config.maxFieldLength).isBetween(1024L, 10485760L)
        
        assertThat(databaseConfig.batchSize).isBetween(100, 50000)
        assertThat(databaseConfig.maxRetryAttempts).isBetween(0, 10)
    }
}