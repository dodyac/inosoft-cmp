import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

kotlin {
    android {
        namespace = "com.ahmaddody.newsreader.shared"
        compileSdk = 36
        minSdk = 24

        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {}.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "NusaNewsShared"
            // Static, which also sidesteps CrashKiOS's Gradle plugin: that plugin only adds
            // `-U _FIRCLSExceptionRecordNSException` for *dynamic* frameworks, and it is pinned to
            // the Kotlin artifacts DSL that Kotlin 2.2 removed. Nothing is lost by leaving it out.
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json)

            // Kermit gives the platform log sinks (Logcat / os_log); okio gives multiplatform
            // file access for the rotating session log. Both are behind AppLogger.
            implementation(libs.kermit)
            implementation(libs.okio)

            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.koin.core)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            // FileProvider, for sharing an exported log bundle as a content URI.
            implementation(libs.androidx.core)

            // Exposed as `api` so the Firebase Gradle plugins applied to :androidApp (Crashlytics
            // mapping upload, Performance bytecode instrumentation) see the SDKs on the app's
            // compile classpath. Only the files in observability/ reference these types.
            api(project.dependencies.platform(libs.firebase.bom))
            api(libs.firebase.analytics)
            api(libs.firebase.crashlytics)
            api(libs.firebase.perf)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            // Symbolicated Kotlin stack traces and breadcrumbs from shared code into Crashlytics.
            // An uncaught Kotlin exception on iOS is not actionable without it.
            implementation(libs.crashkios.crashlytics)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        getByName("androidHostTest").dependencies {
            implementation(kotlin("test-junit"))
        }

        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.androidx.test.runner)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
