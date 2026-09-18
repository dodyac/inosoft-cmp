package com.ahmaddody.newsreader.data.remote

import com.ahmaddody.newsreader.observability.logging.AppLogger
import com.ahmaddody.newsreader.observability.logging.LogTags
import com.ahmaddody.newsreader.observability.logging.NoOpAppLogger
import com.ahmaddody.newsreader.observability.logging.d
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel as KtorLogLevel
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val RequestTimeoutMillis = 15_000L
private const val ConnectTimeoutMillis = 10_000L
private const val SocketTimeoutMillis = 15_000L
private const val ApiKeyHeader = "X-Api-Key"
private const val ApiHost = "newsapi.org"

/**
 * [customize] is the seam for debug-only client plugins. Keeping it here means the in-app network
 * inspector can be installed from a non-production variant without the debug library ever being a
 * dependency of `:shared`, which would put it in the production artifact.
 */
internal fun createNewsHttpClient(
    engine: HttpClientEngine,
    apiKey: String,
    enableNetworkLogs: Boolean,
    appLogger: AppLogger = NoOpAppLogger,
    customize: HttpClientConfig<*>.() -> Unit = {},
): HttpClient = HttpClient(engine) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                isLenient = false
            },
        )
    }

    install(HttpTimeout) {
        requestTimeoutMillis = RequestTimeoutMillis
        connectTimeoutMillis = ConnectTimeoutMillis
        socketTimeoutMillis = SocketTimeoutMillis
    }

    install(Logging) {
        // HTTP logs go through the shared redacting logger, not println, so they land in the
        // session buffer and the rotating file with everything else — and get redacted on the way.
        logger = object : KtorLogger {
            override fun log(message: String) {
                appLogger.d(LogTags.Network, message)
            }
        }
        level = if (enableNetworkLogs) KtorLogLevel.HEADERS else KtorLogLevel.NONE

        // Two independent limits on what HTTP logging can leak: which requests are logged at all,
        // and which header values are masked when they are.
        filter { request -> request.url.host.endsWith(ApiHost, ignoreCase = true) }
        sanitizeHeader { header ->
            header.equals(ApiKeyHeader, ignoreCase = true) ||
                header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                header.equals(HttpHeaders.Cookie, ignoreCase = true) ||
                header.equals(HttpHeaders.SetCookie, ignoreCase = true)
        }
    }

    defaultRequest {
        url("https://newsapi.org/v2/")
        if (apiKey.isNotBlank()) {
            header(ApiKeyHeader, apiKey)
        }
    }

    customize()
}
