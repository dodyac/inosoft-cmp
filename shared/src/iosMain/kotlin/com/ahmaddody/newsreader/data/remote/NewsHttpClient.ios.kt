package com.ahmaddody.newsreader.data.remote

import com.ahmaddody.newsreader.observability.logging.AppLogger
import com.ahmaddody.newsreader.observability.logging.NoOpAppLogger
import io.ktor.client.engine.darwin.Darwin

internal fun createIosNewsHttpClient(
    apiKey: String,
    enableNetworkLogs: Boolean,
    appLogger: AppLogger = NoOpAppLogger,
) = createNewsHttpClient(
    engine = Darwin.create(),
    apiKey = apiKey,
    enableNetworkLogs = enableNetworkLogs,
    appLogger = appLogger,
)
