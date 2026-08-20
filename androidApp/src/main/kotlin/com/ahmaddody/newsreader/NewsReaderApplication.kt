package com.ahmaddody.newsreader

import android.app.Application
import android.util.Log
import com.ahmaddody.newsreader.di.androidSharedModules
import com.ahmaddody.newsreader.presentation.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.logger.AndroidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

private const val SetupTag = "NewsReaderSetup"

class NewsReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        warnAboutMissingApiKey()

        startKoin {
            logger(AndroidLogger(Level.ERROR))
            androidContext(this@NewsReaderApplication)
            modules(
                androidSharedModules(
                    context = this@NewsReaderApplication,
                    apiKey = BuildConfig.NEWS_API_KEY,
                    enableNetworkLogs = BuildConfig.DEBUG,
                ) + appModule,
            )
        }
    }

    /**
     * Configuration problems are a developer concern, so they are reported here instead of in the
     * UI. Readers only ever see a plain "news isn't available" message.
     */
    private fun warnAboutMissingApiKey() {
        if (BuildConfig.NEWS_API_KEY.isBlank()) {
            Log.w(
                SetupTag,
                "NEWS_API_KEY is not configured. Add it to local.properties or export it as an " +
                    "environment variable, then rebuild. The app will run with cached data only.",
            )
        }
    }
}
