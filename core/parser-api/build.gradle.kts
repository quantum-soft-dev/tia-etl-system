plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

group = "com.quantum.etl.core"
version = rootProject.version

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    
    // Logging
    api("org.slf4j:slf4j-api:2.0.16")
    
    // Database connections (API only, implementation in services)
    compileOnly("org.postgresql:postgresql:42.7.4")
    compileOnly("com.clickhouse:clickhouse-jdbc:0.7.1")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "TIA ETL Parser API",
            "Implementation-Version" to version,
            "Implementation-Vendor" to "Quantum ETL"
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("TIA ETL Parser API")
                description.set("Core API for TIA ETL data parsers")
            }
        }
    }
}