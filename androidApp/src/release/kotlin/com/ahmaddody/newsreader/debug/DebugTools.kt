package com.ahmaddody.newsreader.debug

import androidx.compose.runtime.Composable
import com.ahmaddody.newsreader.observability.Observability
import io.ktor.client.HttpClientConfig

/**
 * Release variant. Same API as the debug source set, no behaviour and — more importantly — no
 * reference to the debug tooling library, which is not a dependency of this variant at all.
 */
object DebugTools {
    fun install(isDebugBuild: Boolean) = Unit

    val customizeHttpClient: HttpClientConfig<*>.() -> Unit = {}

    fun decorateObservability(observability: Observability): Observability = observability

    @Composable
    fun Overlay() = Unit
}
