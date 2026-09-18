package com.ahmaddody.newsreader.observability

/**
 * Used by tests, by builds without a Firebase configuration, and by any platform where the SDKs are
 * not wired yet. Instrumentation call sites must never have to null-check.
 */
object NoOpCrashReporter : CrashReporter {
    override fun log(message: String) = Unit
    override fun setKey(key: String, value: String) = Unit
    override fun recordNonFatal(error: Throwable) = Unit
}

object NoOpTraceHandle : TraceHandle {
    override fun putAttribute(key: String, value: String) = Unit
    override fun putMetric(name: String, value: Long) = Unit
    override fun stop() = Unit
}

object NoOpPerformanceTracer : PerformanceTracer {
    override fun startTrace(name: String): TraceHandle = NoOpTraceHandle
}

object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: String, params: Map<String, String>) = Unit
}
