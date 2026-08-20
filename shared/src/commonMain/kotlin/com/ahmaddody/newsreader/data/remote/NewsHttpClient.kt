package com.ahmaddody.newsreader.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val RequestTimeoutMillis = 15_000L
private const val ConnectTimeoutMillis = 10_000L
private const val SocketTimeoutMillis = 15_000L
private const val ApiKeyHeader = "X-Api-Key"

internal fun createNewsHttpClient(
    engine: HttpClientEngine,
    apiKey: String,
    enableNetworkLogs: Boolean,
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
        logger = object : Logger {
            override fun log(message: String) {
                println("NewsHttpClient: $message")
            }
        }
        level = if (enableNetworkLogs) LogLevel.HEADERS else LogLevel.NONE
        sanitizeHeader { header -> header.equals(ApiKeyHeader, ignoreCase = true) }
    }

    defaultRequest {
        url("https://newsapi.org/v2/")
        if (apiKey.isNotBlank()) {
            header(ApiKeyHeader, apiKey)
        }
    }
}
