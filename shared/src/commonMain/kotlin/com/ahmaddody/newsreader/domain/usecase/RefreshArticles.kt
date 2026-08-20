package com.ahmaddody.newsreader.domain.usecase

import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.domain.repository.ArticleRepository

class RefreshArticles(
    private val repository: ArticleRepository,
) {
    suspend operator fun invoke(feed: NewsFeed) = repository.refreshArticles(feed)
}
