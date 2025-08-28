import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    kotlin("plugin.jpa") version "2.0.21"
    // jacoco temporarily disabled due to Java 24 compatibility issue
}

group = "com.quantumsoft.tia"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    
    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core:4.30.0")
    
    // Redis
    implementation("redis.clients:jedis")
    
    // Scheduling
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    
    // Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // Parser API
    implementation(project(":core:parser-api"))
    
    // Testing - TDD Stack
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-jvm:1.13.8")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // Integration Testing
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    
    // Contract Testing
    testImplementation("com.github.tomakehurst:wiremock-jre8:3.0.1")
    testImplementation("io.rest-assured:rest-assured:5.3.2")
    testImplementation("io.rest-assured:kotlin-extensions:5.3.2")
    
    // Architecture Testing
    testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")
    
    // Mutation Testing
    testImplementation("org.pitest:pitest-junit5-plugin:1.2.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Jacoco temporarily disabled due to Java 24 compatibility issue
// Will re-enable when upgrading to compatible version

// Test profiles
val testProfile = project.findProperty("profile") ?: "unit"

tasks.test {
    when (testProfile) {
        "unit" -> {
            include("**/*Test.class")
            exclude("**/*IntegrationTest.class")
            exclude("**/*ContractTest.class")
            exclude("**/*E2ETest.class")
            exclude("**/*LoadTest.class")
        }
        "integration" -> {
            include("**/*IntegrationTest.class")
        }
        "contract" -> {
            include("**/*ContractTest.class")
        }
        "e2e" -> {
            include("**/*E2ETest.class")
        }
        "performance" -> {
            include("**/*LoadTest.class")
        }
        "all" -> {
            include("**/*Test.class")
            include("**/*IntegrationTest.class")
            include("**/*ContractTest.class")
            include("**/*E2ETest.class")
        }
    }
}