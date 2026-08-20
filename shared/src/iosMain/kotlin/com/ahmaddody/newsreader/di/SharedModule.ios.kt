package com.ahmaddody.newsreader.di

import com.ahmaddody.newsreader.data.local.createDatabase
import com.ahmaddody.newsreader.data.remote.createIosNewsHttpClient
import com.ahmaddody.newsreader.ios.NewsFacade
import org.koin.dsl.module
import org.koin.core.context.startKoin

fun initKoinIos(
    apiKey: String,
    enableNetworkLogs: Boolean,
) {
    val platformModule = module {
        single { createDatabase() }
        single { createIosNewsHttpClient(apiKey, enableNetworkLogs) }
        single { NewsFacade(get(), get(), get()) }
    }

    startKoin {
        modules(platformModule, sharedCoreModule(hasApiKey = apiKey.isNotBlank()))
    }
}
