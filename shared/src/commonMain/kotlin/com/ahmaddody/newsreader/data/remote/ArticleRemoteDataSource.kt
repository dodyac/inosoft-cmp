package com.ahmaddody.newsreader.data.remote

import com.ahmaddody.newsreader.domain.model.NewsFeed

internal interface ArticleRemoteDataSource {
    suspend fun fetchArticles(feed: NewsFeed): List<ArticleDto>
}
