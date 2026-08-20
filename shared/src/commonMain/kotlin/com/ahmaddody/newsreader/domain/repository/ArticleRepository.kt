package com.ahmaddody.newsreader.domain.repository

import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

interface ArticleRepository {
    fun observeArticles(feed: NewsFeed): Flow<List<Article>>

    fun observeArticle(articleId: String): Flow<Article?>

    suspend fun refreshArticles(feed: NewsFeed): RefreshResult
}
