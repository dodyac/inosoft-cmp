package com.ahmaddody.newsreader.di

import com.ahmaddody.newsreader.observability.AnalyticsTracker
import com.ahmaddody.newsreader.observability.CrashKeys
import com.ahmaddody.newsreader.observability.CrashReporter
import com.ahmaddody.newsreader.observability.Observability
import com.ahmaddody.newsreader.observability.PerformanceTracer
import com.ahmaddody.newsreader.observability.Session
import com.ahmaddody.newsreader.observability.logging.AppLogger
import com.ahmaddody.newsreader.observability.logging.LogExporter
import com.ahmaddody.newsreader.observability.logging.LogTags
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The platform builds the [Observability] bundle; the graph shape is identical on both sides, so it
 * is declared once here.
 */
internal fun observabilityModule(observability: Observability): Module = module {
    single { observability }
    single<CrashReporter> { observability.crash }
    single<PerformanceTracer> { observability.performance }
    single<AnalyticsTracker> { observability.analytics }
    single<AppLogger> { observability.logger }
    single<LogExporter> { observability.logExporter }
}

/**
 * Keys set once per process. [Session.id] is the join key between a crash report and the session
 * that produced it; it is not a user identifier and carries no PII.
 */
fun Observability.applyStartupKeys(hasApiKey: Boolean) {
    crash.setKey(CrashKeys.SessionId, Session.id)
    crash.setKey(CrashKeys.HasApiKey, hasApiKey.toString())
    breadcrumb(LogTags.Lifecycle, "session ${Session.id} started, api key configured=$hasApiKey")
}
