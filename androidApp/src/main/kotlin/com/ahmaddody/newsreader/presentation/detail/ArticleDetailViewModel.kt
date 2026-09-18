package com.ahmaddody.newsreader.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmaddody.newsreader.domain.model.AppError
import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.usecase.ObserveArticle
import com.ahmaddody.newsreader.observability.AnalyticsEvents
import com.ahmaddody.newsreader.observability.AnalyticsParams
import com.ahmaddody.newsreader.observability.CrashKeys
import com.ahmaddody.newsreader.observability.Observability
import com.ahmaddody.newsreader.observability.Screens
import com.ahmaddody.newsreader.observability.logging.LogTags
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
    observability: Observability = Observability.NoOp,
) : ViewModel() {
    init {
        observability.crash.setKey(CrashKeys.Screen, Screens.ArticleDetail)
        observability.breadcrumb(LogTags.Navigation, "opened ${Screens.ArticleDetail}")
        // The article id is deliberately not sent: it is high-cardinality and answers no product
        // question that Analytics is meant to answer.
        observability.analytics.track(
            AnalyticsEvents.ScreenViewed,
            mapOf(AnalyticsParams.ScreenName to Screens.ArticleDetail),
        )
        observability.analytics.track(AnalyticsEvents.ArticleOpened, emptyMap())
    }

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

