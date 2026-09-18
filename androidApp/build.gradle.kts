import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use(::load)
    }
}

val newsApiKey = providers.environmentVariable("NEWS_API_KEY").orNull
    ?: localProperties.getProperty("NEWS_API_KEY")
    ?: ""

fun String.asBuildConfigValue(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.ahmaddody.newsreader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ahmaddody.newsreader"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "NEWS_API_KEY", newsApiKey.asBuildConfigValue())
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/INDEX.LIST",
        )
    }

    buildTypes {
        debug {
            // Debug builds are not minified, so there is no mapping file worth uploading.
            // Collection itself stays on so the wiring can be verified from a dev device.
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                // R8 is on, so without the mapping file every release stack trace is noise.
                mappingFileUploadEnabled = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Needed because androidSharedModules exposes a Ktor client-config seam to the debug variant.
    implementation(libs.ktor.client.core)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)

    // In-app inspector: non-production variants only, so it cannot reach a release artifact.
    debugImplementation(libs.aelog.logs)
    debugImplementation(libs.aelog.network.ktor)
    debugImplementation(libs.aelog.analytics)
    debugImplementation(libs.aelog.crashes)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
