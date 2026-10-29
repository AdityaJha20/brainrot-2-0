plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Compile to Java 17 bytecode (the Android target) with whatever JDK 17+ runs Gradle.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    // Shared fixtures live outside this module so the TypeScript twin can read the same files.
    systemProperty("fixturesDir", rootProject.file("shared-fixtures/scoring").absolutePath)
    inputs.dir(rootProject.file("shared-fixtures/scoring"))
    testLogging { events("passed", "failed") }
}
