package com.ahmaddody.newsreader.observability

import com.ahmaddody.newsreader.observability.logging.AppLogger
import com.ahmaddody.newsreader.observability.logging.LogExporter
import com.ahmaddody.newsreader.observability.logging.NoOpAppLogger
import com.ahmaddody.newsreader.observability.logging.NoOpLogExporter
import com.ahmaddody.newsreader.observability.logging.i

/**
 * Shared observability contracts.
 *
 * None of the Firebase SDKs support Kotlin Multiplatform, so each one is treated exactly like any
 * other platform capability: the contract lives in `commonMain` and a thin implementation lives on
 * each platform. Shared code never references a vendor type.
 *
 * Android implements these with the Firebase Android SDKs; iOS implements them in Swift against the
 * Firebase Apple SDKs and injects them through [com.ahmaddody.newsreader.di.initKoinIos].
 */

/** Crash and non-fatal failure reporting. Maps to Firebase Crashlytics on both platforms. */
interface CrashReporter {
    /** Breadcrumb attached to the next crash report. */
    fun log(message: String)

    /** Custom key attached to every subsequent crash report. Bounded, low-cardinality values only. */
    fun setKey(key: String, value: String)

    /** Records a handled failure that did not terminate the process. */
    fun recordNonFatal(error: Throwable)
}

/** A running custom code trace. Stopping it twice is a no-op. */
interface TraceHandle {
    /** Max 5 attributes per trace, names capped at 32 chars. Keep the taxonomy in [Traces]. */
    fun putAttribute(key: String, value: String)

    /** Max 32 metrics per trace, including the implicit duration metric. */
    fun putMetric(name: String, value: Long)

    fun stop()
}

/** Custom code traces. Maps to Firebase Performance Monitoring on both platforms. */
interface PerformanceTracer {
    fun startTrace(name: String): TraceHandle
}

/** Product usage events. Maps to Firebase Analytics on both platforms. No PII, ever. */
interface AnalyticsTracker {
    fun track(event: String, params: Map<String, String>)
}

/**
 * Convenience bundle so call sites take one dependency instead of five. The contracts stay separate
 * because the implementations are: Crashlytics, Performance and Analytics are per-platform vendor
 * SDKs, the logger is shared Kermit, and the exporter is platform storage.
 */
class Observability(
    val crash: CrashReporter,
    val performance: PerformanceTracer,
    val analytics: AnalyticsTracker,
    // Deliberately not defaulted. A default here is a silent failure: a decorator that rebuilds
    // the bundle and forgets a sink would compile and then quietly log nothing.
    val logger: AppLogger,
    val logExporter: LogExporter,
) {
    /**
     * One call, two destinations. The session log is the evidence for a defect that produced no
     * crash at all; the Crashlytics breadcrumb is what makes a crash report readable. A breadcrumb
     * belongs in both, and writing it twice at every call site is how they drift apart.
     */
    fun breadcrumb(tag: String, message: String) {
        logger.i(tag, message)
        crash.log("$tag: $message")
    }

    fun copy(
        crash: CrashReporter = this.crash,
        performance: PerformanceTracer = this.performance,
        analytics: AnalyticsTracker = this.analytics,
        logger: AppLogger = this.logger,
        logExporter: LogExporter = this.logExporter,
    ) = Observability(crash, performance, analytics, logger, logExporter)

    companion object {
        val NoOp = Observability(
            crash = NoOpCrashReporter,
            performance = NoOpPerformanceTracer,
            analytics = NoOpAnalyticsTracker,
            logger = NoOpAppLogger,
            logExporter = NoOpLogExporter,
        )
    }
}

/** Runs [block] inside a custom trace, stopping it even when [block] throws or is cancelled. */
suspend inline fun <T> PerformanceTracer.trace(
    name: String,
    block: (TraceHandle) -> T,
): T {
    val handle = startTrace(name)
    return try {
        block(handle)
    } finally {
        handle.stop()
    }
}

/** Fires an event with no parameters. */
fun AnalyticsTracker.track(event: String) = track(event, emptyMap())
