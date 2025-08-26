plugins {
    kotlin("jvm")
}

description = "Core API module defining interfaces and contracts for data parsers"

dependencies {
    // Kotlin standard library (inherited from parent)
    implementation(kotlin("stdlib"))
    
    // SLF4J for logging (inherited from parent)
    implementation("org.slf4j:slf4j-api")
    
    // Testing dependencies (inherited from parent)
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk")
    testImplementation("org.assertj:assertj-core")
}