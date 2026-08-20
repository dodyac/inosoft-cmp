package com.ahmaddody.newsreader.data.remote

import io.ktor.client.engine.darwin.Darwin

internal fun createIosNewsHttpClient(
    apiKey: String,
    enableNetworkLogs: Boolean,
) = createNewsHttpClient(
    engine = Darwin.create(),
    apiKey = apiKey,
    enableNetworkLogs = enableNetworkLogs,
)
