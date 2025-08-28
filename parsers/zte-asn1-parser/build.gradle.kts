plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    // kotlin("kapt") // временно отключен из-за проблем с Kotlin 2.0
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.owasp.dependencycheck") version "8.4.0"
    jacoco
}

group = "com.quantumsoft.tia.parsers"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo1.maven.org/maven2")
    }
}

dependencies {
    // Core Parser API
    implementation(project(":core:parser-api"))
    
    // jasn1 for ASN.1 processing (used by generated pgw_r8_new classes)
    implementation("org.openmuc:jasn1:1.10.0")
    
    // Kotlin Standard Library
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    
    // Spring Boot Framework
    implementation("org.springframework.boot:spring-boot-starter:3.3.5")
    implementation("org.springframework.boot:spring-boot-starter-actuator:3.3.5")
    implementation("org.springframework.boot:spring-boot-starter-validation:3.3.5")
    implementation("org.springframework.boot:spring-boot-configuration-processor:3.3.5")
    
    // Metrics and Monitoring
    implementation("io.micrometer:micrometer-core:1.13.8")
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.8")
    
    // ClickHouse Database
    implementation("com.clickhouse:clickhouse-jdbc:0.7.1")
    
    // Connection Pooling
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // Jackson for JSON processing
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    
    // MapStruct for DTO mappings (временно отключен из-за проблем с kapt)
    // implementation("org.mapstruct:mapstruct:1.6.3")
    // kapt("org.mapstruct:mapstruct-processor:1.6.3")
    
    // Validation
    implementation("jakarta.validation:jakarta.validation-api:3.1.0")
    implementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Testing Dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.5") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
    
    // Integration Testing
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:clickhouse:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    
    // Annotation Processing (временно отключен из-за проблем с kapt)
    // kapt("org.springframework.boot:spring-boot-configuration-processor:3.3.5")
}

// Kotlin Compiler Configuration
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xjvm-default=all"
        )
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// Kapt Configuration (временно отключен)
// kapt {
//     arguments {
//         arg("mapstruct.defaultComponentModel", "spring")
//         arg("mapstruct.unmappedTargetPolicy", "ERROR")
//     }
// }

// Spring Boot Configuration
springBoot {
    mainClass.set("com.quantumsoft.tia.parsers.zte.ZteAsn1ParserApplication")
    buildInfo()
}

// Test Configuration
tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
    
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
    
    finalizedBy(tasks.jacocoTestReport)
}

// Integration Test Configuration
sourceSets {
    create("integrationTest") {
        kotlin.srcDir("src/integrationTest/kotlin")
        resources.srcDir("src/integrationTest/resources")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests"
    group = "verification"
    
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    
    useJUnitPlatform()
    
    testLogging {
        events("passed", "skipped", "failed")
    }
    
    mustRunAfter("test")
}

// JaCoCo Test Coverage
jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
    }
    
    executionData.setFrom(fileTree(layout.buildDirectory).include("/jacoco/*.exec"))
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

// OWASP Dependency Check Configuration
dependencyCheck {
    format = "ALL"
    suppressionFile = file("owasp-suppressions.xml").takeIf { it.exists() }?.absolutePath
    failBuildOnCVSS = 7.0f
    // analyzers configuration removed - not needed for Java/Kotlin project
}

// Fat JAR Configuration for deployment
tasks.bootJar {
    enabled = true
    archiveClassifier.set("")
    archiveFileName.set("${project.name}.jar")
    
    // Handle duplicate files
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes[
            "Implementation-Title"] = project.name
        attributes["Implementation-Version"] = project.version
        attributes["Implementation-Vendor"] = "Quantum Soft"
        attributes["Main-Class"] = "com.quantumsoft.tia.parsers.zte.ZteAsn1ParserApplication"
        attributes["Parser-Id"] = "zte-asn1-parser"
        attributes["Parser-Version"] = project.version
        attributes["Build-Time"] = System.currentTimeMillis().toString()
    }
    
    // Ensure all dependencies are included
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    }
}

// Regular JAR (thin JAR)
tasks.jar {
    enabled = true
    archiveClassifier.set("thin")
    
    manifest {
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = project.version
        attributes["Implementation-Vendor"] = "Quantum Soft"
        attributes["Parser-Id"] = "zte-asn1-parser"
        attributes["Parser-Version"] = project.version
    }
}

// Build Task Dependencies
tasks.build {
    dependsOn(tasks.jacocoTestReport)
    dependsOn(tasks.bootJar)
}

// Custom Tasks
tasks.register("qualityCheck") {
    description = "Runs all quality checks (tests, coverage, security)"
    group = "verification"
    
    dependsOn(
        tasks.test,
        tasks.jacocoTestReport,
        tasks.jacocoTestCoverageVerification,
        tasks.dependencyCheckAnalyze
    )
}

tasks.register("deploymentPackage") {
    description = "Creates deployment package with all necessary files"
    group = "deployment"
    
    dependsOn(tasks.bootJar)
    
    doLast {
        val deploymentDir = file("${layout.buildDirectory.get()}/deployment")
        val libsDir = file("${layout.buildDirectory.get()}/libs")
        
        deploymentDir.mkdirs()
        
        // Copy fat JAR
        copy {
            from(libsDir)
            into(deploymentDir)
            include("${project.name}.jar")
        }
        
        // Copy Kubernetes manifests
        copy {
            from("k8s")
            into("$deploymentDir/k8s")
        }
        
        // Copy Helm chart
        copy {
            from("charts")
            into("$deploymentDir/charts")
        }
        
        // Copy deployment scripts
        copy {
            from("scripts")
            into("$deploymentDir/scripts")
            include("*.sh")
        }
        
        // Create metadata file
        val metadata = mapOf(
            "parserId" to "zte-asn1-parser",
            "name" to "ZTE ASN.1 CDR Parser",
            "version" to project.version.toString(),
            "description" to "Processes ZTE ZXUN CG ASN.1 encoded CDR files with support for multiple record types",
            "supportedFormats" to listOf("asn1", "ber", "cdr"),
            "targetTable" to "zte_cdr_records",
            "maxFileSize" to 1073741824L,
            "batchSize" to 1000,
            "requiresValidation" to true,
            "buildTime" to System.currentTimeMillis().toString(),
            "jarPath" to "/opt/tia/parsers/zte-asn1-parser/current/zte-asn1-parser.jar",
            "mainClass" to "com.quantumsoft.tia.parsers.zte.ZteAsn1ParserApplication"
        )
        
        file("$deploymentDir/metadata.json").writeText(
            groovy.json.JsonBuilder(metadata).toPrettyString()
        )
        
        println("Deployment package created at: $deploymentDir")
    }
}

tasks.register("dockerBuildLocal") {
    description = "Build Docker image locally"
    group = "deployment"
    
    dependsOn(tasks.bootJar)
    
    doLast {
        exec {
            commandLine("docker", "build", "-t", "tia-etl/zte-asn1-parser:${project.version}", ".")
        }
    }
}

tasks.register("version") {
    description = "Prints the project version"
    group = "help"
    
    doLast {
        println(project.version)
    }
}

// Clean task configuration
tasks.clean {
    delete("logs")
    delete("out")
}

// Wrapper task configuration (if needed, configure in settings.gradle.kts)