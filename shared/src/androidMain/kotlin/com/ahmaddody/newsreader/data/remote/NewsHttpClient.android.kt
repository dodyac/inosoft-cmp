package com.ahmaddody.newsreader.data.remote

import io.ktor.client.engine.okhttp.OkHttp

internal fun createAndroidNewsHttpClient(
    apiKey: String,
    enableNetworkLogs: Boolean,
) = createNewsHttpClient(
    engine = OkHttp.create(),
    apiKey = apiKey,
    enableNetworkLogs = enableNetworkLogs,
)

