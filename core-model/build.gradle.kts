import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "jr.brian.core.model"
    compileSdk = 37

    defaultConfig {
        minSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Keep flavor dims in sync with :app so testStandardDebugUnitTest /
    // lintStandardDebug resolve a real variant here when invoked from root.
    // :core-model doesn't produce flavor-divergent code, but the dim has to
    // exist for the task name to be valid.
    flavorDimensions += "mode"
    productFlavors {
        create("standard") { dimension = "mode" }
        create("hidden") { dimension = "mode" }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = false
        resValues = false
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Gradle 9's test task fails when no tests are discovered, even on an empty
// scaffolded module whose first tests ship in a later commit. Opt out here;
// each module's own test suite polices itself through unit-test coverage.
tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    // Hilt — the FrontendBus adapter is provided from here.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
