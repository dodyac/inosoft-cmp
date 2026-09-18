package com.ahmaddody.newsreader.debug

import androidx.compose.runtime.Composable
import com.ae.log.AELog
import com.ae.log.analytics.analytics
import com.ae.log.crashes.crashes
import com.ae.log.ktor.AELogKtorInterceptor
import com.ae.log.logs.log
import com.ae.log.network.config.InterceptorDefaults
import com.ae.log.ui.AELogOverlay
import com.ahmaddody.newsreader.observability.AnalyticsTracker
import com.ahmaddody.newsreader.observability.CrashReporter
import com.ahmaddody.newsreader.observability.Observability
import io.ktor.client.HttpClientConfig

/**
 * Debug variant of the developer tooling seam.
 *
 * Two independent gates keep the in-app inspector out of production, as the architecture requires:
 * the dependency is declared `debugImplementation` only, so the code is absent from the release
 * artifact entirely, and [AELog.isEnabled] is bound to the build flag as a runtime backstop.
 * The release source set below provides the same API with empty bodies.
 */
object DebugTools {
    fun install(isDebugBuild: Boolean) {
        AELog.isEnabled = isDebugBuild
    }

    /**
     * The overlay renders request and response bodies on screen, so header exclusion and body
     * truncation are configured here rather than left at their permissive defaults.
     */
    val customizeHttpClient: HttpClientConfig<*>.() -> Unit = {
        // AELog's README suggests Ktor's DoubleReceive plugin so the inspector does not consume the
        // response stream. Ktor 3 dropped that plugin — response bodies are replayable by default —
        // so there is nothing to install here.
        install(AELogKtorInterceptor) {
            excludeHeaders = InterceptorDefaults.COMMON_EXCLUDED + setOf("X-Api-Key")
            maxBodyBytes = 100_000
        }
    }

    /**
     * Mirrors telemetry into the overlay so events can be verified on-device without opening the
     * Firebase console. Firebase still receives everything it received before.
     */
    fun decorateObservability(observability: Observability): Observability = observability.copy(
        crash = MirroringCrashReporter(observability.crash),
        analytics = MirroringAnalyticsTracker(observability.analytics),
    )

    @Composable
    fun Overlay() {
        AELogOverlay()
        DiagnosticsLauncher()
    }
}

private class MirroringCrashReporter(private val delegate: CrashReporter) : CrashReporter {
    override fun log(message: String) {
        AELog.log.d(message)
        delegate.log(message)
    }

    override fun setKey(key: String, value: String) {
        AELog.log.d("key $key=$value")
        delegate.setKey(key, value)
    }

    override fun recordNonFatal(error: Throwable) {
        AELog.crashes.recordNonFatal(error)
        delegate.recordNonFatal(error)
    }
}

private class MirroringAnalyticsTracker(private val delegate: AnalyticsTracker) : AnalyticsTracker {
    override fun track(event: String, params: Map<String, String>) {
        AELog.analytics.logEvent(event, properties = params)
        delegate.track(event, params)
    }
}
