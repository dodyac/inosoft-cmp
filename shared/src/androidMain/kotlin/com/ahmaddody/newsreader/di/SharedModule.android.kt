package com.ahmaddody.newsreader.di

import android.content.Context
import com.ahmaddody.newsreader.data.local.createDatabase
import com.ahmaddody.newsreader.data.remote.createAndroidNewsHttpClient
import com.ahmaddody.newsreader.observability.FirebaseAnalyticsTracker
import com.ahmaddody.newsreader.observability.FirebaseCrashReporter
import com.ahmaddody.newsreader.observability.FirebasePerformanceTracer
import com.ahmaddody.newsreader.observability.Observability
import com.ahmaddody.newsreader.observability.logging.AndroidLogExporter
import com.ahmaddody.newsreader.observability.logging.SessionLog
import androidx.core.content.pm.PackageInfoCompat
import com.google.firebase.analytics.FirebaseAnalytics
import io.ktor.client.HttpClientConfig
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * [customizeHttpClient] and [decorateObservability] are the non-production seams: a debug variant
 * passes the in-app inspector's Ktor plugin and telemetry mirrors, a release build passes nothing
 * and never links the debug library at all.
 */
fun androidSharedModules(
    context: Context,
    apiKey: String,
    enableNetworkLogs: Boolean,
    customizeHttpClient: HttpClientConfig<*>.() -> Unit = {},
    decorateObservability: (Observability) -> Observability = { it },
): List<Module> {
    val sessionLog = createSessionLog(context)
    val observability = decorateObservability(
        createAndroidObservability(context, sessionLog, hasApiKey = apiKey.isNotBlank()),
    )

    val platformModule = module {
        single { createDatabase(context) }
        single { sessionLog }
        single {
            createAndroidNewsHttpClient(
                apiKey = apiKey,
                enableNetworkLogs = enableNetworkLogs,
                appLogger = observability.logger,
                customize = customizeHttpClient,
            )
        }
    }

    return listOf(
        platformModule,
        observabilityModule(observability),
        sharedCoreModule(hasApiKey = apiKey.isNotBlank()),
    )
}

/**
 * Logs live in app-specific storage: no runtime permission is required to write there, and the
 * contents are removed when the app is uninstalled.
 */
private fun createSessionLog(context: Context): SessionLog = SessionLog.create(
    fileSystem = FileSystem.SYSTEM,
    directory = context.filesDir.resolve("logs").toOkioPath(),
)

/**
 * Firebase auto-initialises through its own ContentProvider, so no explicit `initializeApp` call is
 * needed here — the SDK is ready by the time [android.app.Application.onCreate] runs.
 */
private fun createAndroidObservability(
    context: Context,
    sessionLog: SessionLog,
    hasApiKey: Boolean,
): Observability {
    val observability = Observability(
        crash = FirebaseCrashReporter(),
        performance = FirebasePerformanceTracer(),
        analytics = FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(context)),
        logger = sessionLog.logger,
        logExporter = AndroidLogExporter(context, sessionLog, appVersion(context)),
    )
    observability.applyStartupKeys(hasApiKey)
    return observability
}

private fun appVersion(context: Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
}.getOrDefault("unknown")
