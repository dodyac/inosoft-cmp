package com.ahmaddody.newsreader.di

import com.ahmaddody.newsreader.data.local.createDatabase
import com.ahmaddody.newsreader.data.remote.createIosNewsHttpClient
import com.ahmaddody.newsreader.ios.NewsFacade
import com.ahmaddody.newsreader.observability.AnalyticsTracker
import com.ahmaddody.newsreader.observability.CrashKiOSCrashReporter
import com.ahmaddody.newsreader.observability.Observability
import com.ahmaddody.newsreader.observability.PerformanceTracer
import com.ahmaddody.newsreader.observability.setupIosCrashReporting
import com.ahmaddody.newsreader.observability.logging.LogExporter
import com.ahmaddody.newsreader.observability.logging.NoOpLogExporter
import com.ahmaddody.newsreader.observability.logging.SessionLog
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.dsl.module
import org.koin.core.context.startKoin
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Firebase Performance and Analytics are not callable from Kotlin/Native without cinterop, so Swift
 * implements those two contracts and hands them in here. The same applies to [LogExporter]: the
 * share sheet is a UIKit concern. Crash reporting is the exception — see [setupIosCrashReporting].
 */
fun initKoinIos(
    apiKey: String,
    enableNetworkLogs: Boolean,
    performanceTracer: PerformanceTracer,
    analyticsTracker: AnalyticsTracker,
    logExporter: LogExporter = NoOpLogExporter,
) {
    // Crash reporting is the one vendor capability implemented in Kotlin on iOS: see
    // CrashKiOSCrashReporter for why a Swift adapter is not enough here.
    setupIosCrashReporting()

    val sessionLog = createSessionLog()
    val observability = Observability(
        crash = CrashKiOSCrashReporter(),
        performance = performanceTracer,
        analytics = analyticsTracker,
        logger = sessionLog.logger,
        logExporter = logExporter,
    )
    observability.applyStartupKeys(hasApiKey = apiKey.isNotBlank())

    val platformModule = module {
        single { createDatabase() }
        single { sessionLog }
        single {
            createIosNewsHttpClient(
                apiKey = apiKey,
                enableNetworkLogs = enableNetworkLogs,
                appLogger = observability.logger,
            )
        }
        single { NewsFacade(get(), get(), get(), get()) }
    }

    startKoin {
        modules(
            platformModule,
            observabilityModule(observability),
            sharedCoreModule(hasApiKey = apiKey.isNotBlank()),
        )
    }
}

/** Application Support is the iOS equivalent of Android app-specific storage: private, and removed with the app. */
private fun createSessionLog(): SessionLog {
    val base = NSSearchPathForDirectoriesInDomains(
        directory = NSApplicationSupportDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true,
    ).firstOrNull() as? String

    return if (base == null) {
        SessionLog.create()
    } else {
        SessionLog.create(
            fileSystem = FileSystem.SYSTEM,
            directory = "$base/NusaNewsLogs".toPath(),
        )
    }
}

/** Swift needs the log directory to build the export bundle; it is decided here, not there. */
fun sessionLogDirectory(): String? = NSSearchPathForDirectoriesInDomains(
    directory = NSApplicationSupportDirectory,
    domainMask = NSUserDomainMask,
    expandTilde = true,
).firstOrNull()?.let { base -> "$base/NusaNewsLogs" }
