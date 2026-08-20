package com.ahmaddody.newsreader.di

import android.content.Context
import com.ahmaddody.newsreader.data.local.createDatabase
import com.ahmaddody.newsreader.data.remote.createAndroidNewsHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidSharedModules(
    context: Context,
    apiKey: String,
    enableNetworkLogs: Boolean,
): List<Module> {
    val platformModule = module {
        single { createDatabase(context) }
        single { createAndroidNewsHttpClient(apiKey, enableNetworkLogs) }
    }

    return listOf(platformModule, sharedCoreModule(hasApiKey = apiKey.isNotBlank()))
}
