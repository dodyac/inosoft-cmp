package com.ahmaddody.newsreader.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmaddody.newsreader.domain.model.AppError
import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.usecase.ObserveArticle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ArticleDetailUiState(
    val isLoading: Boolean = true,
    val article: Article? = null,
    val error: AppError? = null,
)

class ArticleDetailViewModel(
    articleId: String,
    observeArticle: ObserveArticle,
) : ViewModel() {
    val uiState = observeArticle(articleId)
        .map { article ->
            if (article == null) {
                ArticleDetailUiState(isLoading = false, error = AppError.ArticleNotFound)
            } else {
                ArticleDetailUiState(isLoading = false, article = article)
            }
        }
        .catch {
            emit(ArticleDetailUiState(isLoading = false, error = AppError.DatabaseUnavailable))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ArticleDetailUiState(),
        )
}

