package com.ahmaddody.newsreader.data.remote

import com.ahmaddody.newsreader.observability.logging.AppLogger
import com.ahmaddody.newsreader.observability.logging.NoOpAppLogger
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

internal fun createAndroidNewsHttpClient(
    apiKey: String,
    enableNetworkLogs: Boolean,
    appLogger: AppLogger = NoOpAppLogger,
    customize: HttpClientConfig<*>.() -> Unit = {},
) = createNewsHttpClient(
    engine = OkHttp.create(),
    apiKey = apiKey,
    enableNetworkLogs = enableNetworkLogs,
    appLogger = appLogger,
    customize = customize,
)
