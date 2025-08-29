package com.quantumsoft.tia.scanner.e2e

import com.quantumsoft.tia.scanner.config.TestConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.profiles.active=test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.quartz.job-store-type=memory",
        "spring.quartz.properties.org.quartz.jobStore.class=org.quartz.simpl.RAMJobStore",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "logging.level.com.quantumsoft.tia=DEBUG"
    ]
)
@Testcontainers
@Import(TestConfiguration::class)
abstract class BaseE2ETest {

    companion object {
        
        @Container
        @JvmStatic
        val postgresql = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("tia_test")
            .withUsername("test")
            .withPassword("test")
        
        @Container
        @JvmStatic
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // Database configuration
            registry.add("spring.datasource.url", postgresql::getJdbcUrl)
            registry.add("spring.datasource.username", postgresql::getUsername)
            registry.add("spring.datasource.password", postgresql::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            
            // Redis configuration  
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port", redis::getFirstMappedPort)
            registry.add("spring.data.redis.database") { "0" }
        }
    }
}