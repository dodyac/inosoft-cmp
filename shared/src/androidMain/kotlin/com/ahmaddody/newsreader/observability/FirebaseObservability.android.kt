package com.ahmaddody.newsreader.observability

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

/**
 * Android side of the observability contracts. These are the only files in the project allowed to
 * reference a Firebase type.
 */
internal class FirebaseCrashReporter(
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
) : CrashReporter {
    override fun log(message: String) = crashlytics.log(message)

    override fun setKey(key: String, value: String) = crashlytics.setCustomKey(key, value)

    override fun recordNonFatal(error: Throwable) = crashlytics.recordException(error)
}

internal class FirebaseTraceHandle(private val trace: Trace) : TraceHandle {
    private var stopped = false

    /** Firebase rejects attribute names over 32 chars and values over 100; truncate, never crash. */
    override fun putAttribute(key: String, value: String) {
        if (stopped) return
        trace.putAttribute(key.take(MaxAttributeNameLength), value.take(MaxAttributeValueLength))
    }

    override fun putMetric(name: String, value: Long) {
        if (stopped) return
        trace.putMetric(name.take(MaxAttributeNameLength), value)
    }

    override fun stop() {
        if (stopped) return
        stopped = true
        trace.stop()
    }

    private companion object {
        const val MaxAttributeNameLength = 32
        const val MaxAttributeValueLength = 100
    }
}

internal class FirebasePerformanceTracer(
    private val performance: FirebasePerformance = FirebasePerformance.getInstance(),
) : PerformanceTracer {
    override fun startTrace(name: String): TraceHandle =
        FirebaseTraceHandle(performance.newTrace(name).apply { start() })
}

internal class FirebaseAnalyticsTracker(
    private val analytics: FirebaseAnalytics,
) : AnalyticsTracker {
    override fun track(event: String, params: Map<String, String>) {
        val bundle = Bundle(params.size)
        params.forEach { (key, value) -> bundle.putString(key, value) }
        analytics.logEvent(event, bundle)
    }
}
