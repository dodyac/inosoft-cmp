package com.ahmaddody.newsreader.domain.usecase

import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.domain.repository.ArticleRepository

class ObserveArticles(
    private val repository: ArticleRepository,
) {
    operator fun invoke(feed: NewsFeed) = repository.observeArticles(feed)
}
