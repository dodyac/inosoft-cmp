package com.ahmaddody.newsreader.domain.usecase

import com.ahmaddody.newsreader.domain.repository.ArticleRepository

class ObserveArticle(
    private val repository: ArticleRepository,
) {
    operator fun invoke(articleId: String) = repository.observeArticle(articleId)
}

