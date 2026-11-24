import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "jr.brian.frontend"
    compileSdk = 37

    defaultConfig {
        minSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Dev-side ScreenScraper credentials. Populated at build time
        // from the SCREENSCRAPER_DEV_ID / SCREENSCRAPER_DEV_PASSWORD
        // env vars (set as GitHub Actions secrets); empty strings when
        // unavailable. ScreenScraperConfig.hasDevCredentials gates the
        // scraper so a build without them degrades gracefully to a
        // "scraper not configured" state rather than hitting the API
        // and failing on auth.
        val devId = (project.findProperty("screenscraper.devId") as? String)
            ?: System.getenv("SCREENSCRAPER_DEV_ID")
            ?: ""
        val devPassword = (project.findProperty("screenscraper.devPassword") as? String)
            ?: System.getenv("SCREENSCRAPER_DEV_PASSWORD")
            ?: ""
        buildConfigField("String", "SCREENSCRAPER_DEV_ID", "\"$devId\"")
        buildConfigField("String", "SCREENSCRAPER_DEV_PASSWORD", "\"$devPassword\"")
    }

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
        compose = true
        // Scraper dev creds (Phase 4a) ship via BuildConfig fields
        // injected from CI secrets. Empty values must still compile so
        // local / forked builds work without the secrets available.
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    api(project(":core-model"))
    api(project(":core-ui"))

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // kotlinx.serialization runtime — SystemDef is @Serializable so the
    // SystemCatalog can decode assets/systems/*.json at load time.
    implementation(libs.kotlinx.serialization.json)

    // Room — library engine lives here per plan §3.2.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Scraper (Phase 4) — OkHttp for the HTTP clients, WorkManager +
    // hilt-work for the background scrape worker.
    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.mockk)
}
