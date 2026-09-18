package com.ahmaddody.newsreader.di

import com.ahmaddody.newsreader.data.local.ArticleLocalDataSource
import com.ahmaddody.newsreader.data.local.NewsDatabase
import com.ahmaddody.newsreader.data.local.RoomArticleLocalDataSource
import com.ahmaddody.newsreader.data.remote.ArticleRemoteDataSource
import com.ahmaddody.newsreader.data.remote.KtorArticleRemoteDataSource
import com.ahmaddody.newsreader.data.repository.OfflineFirstArticleRepository
import com.ahmaddody.newsreader.domain.repository.ArticleRepository
import com.ahmaddody.newsreader.domain.usecase.ObserveArticle
import com.ahmaddody.newsreader.domain.usecase.ObserveArticles
import com.ahmaddody.newsreader.domain.usecase.RefreshArticles
import io.ktor.client.HttpClient
import org.koin.dsl.module

internal fun sharedCoreModule(hasApiKey: Boolean) = module {
    single<ArticleLocalDataSource> { RoomArticleLocalDataSource(get<NewsDatabase>().articleDao()) }
    single<ArticleRemoteDataSource> { KtorArticleRemoteDataSource(get<HttpClient>(), hasApiKey) }
    single<ArticleRepository> { OfflineFirstArticleRepository(get(), get(), get()) }

    factory { ObserveArticles(get()) }
    factory { ObserveArticle(get()) }
    factory { RefreshArticles(get()) }
}

