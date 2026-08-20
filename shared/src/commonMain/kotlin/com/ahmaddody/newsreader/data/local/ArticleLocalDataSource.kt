package com.ahmaddody.newsreader.data.local

import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.model.NewsFeed
import kotlinx.coroutines.flow.Flow

internal interface ArticleLocalDataSource {
    fun observeArticles(feed: NewsFeed): Flow<List<Article>>

    fun observeArticle(articleId: String): Flow<Article?>

    suspend fun hasArticles(feed: NewsFeed): Boolean

    suspend fun replaceAll(feed: NewsFeed, articles: List<Article>)
}
