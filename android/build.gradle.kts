// Phase 0 Android spike. Purpose: produce device evidence (widget, heatmap bitmap, offline persistence),
// not product UI. AGP 9 compiles Kotlin itself ("built-in Kotlin"), so no kotlin-android plugin here.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "app.brainrot.spike"
    // Compile against API 37 (required by current AndroidX); the app still targets Android 16 (API 36).
    compileSdk = 37

    defaultConfig {
        applicationId = "app.brainrot.phase0"
        // Target phone runs Android 16 (API 36). minSdk 26 keeps Credential Manager / supabase-kt options open.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1-phase0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("phase0.reportDir", rootProject.file("docs/phase0/evidence").absolutePath)
                // Robolectric's Android 16 (SDK 36) runtime touches JDK internals hidden since JDK 17+.
                it.jvmArgs(
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                )
                // Optional mirror for machines that Maven Central rate-limits (set in ~/.gradle/gradle.properties).
                providers.gradleProperty("robolectricRepoUrl").orNull?.let { url ->
                    it.systemProperty("robolectric.dependency.repo.url", url)
                }
            }
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

ksp {
    // Exported schemas are committed so every future migration can be tested against the real history.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.room.testing)
    testImplementation(libs.glance.appwidget.testing)
}
