package com.ahmaddody.newsreader.observability

import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.observability.logging.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * What the debug-only diagnostics screen reads.
 *
 * The architecture calls for a pending sync queue here. This app is read-only and has no write
 * queue, so the equivalent evidence is recorded instead: per-feed refresh outcome, the last error,
 * how many articles were committed, and when — enough to answer "why is this device showing stale
 * news" without a debugger.
 */
object SessionDiagnostics {
    private val _feeds = MutableStateFlow<Map<NewsFeed, FeedDiagnostics>>(emptyMap())
    val feeds: StateFlow<Map<NewsFeed, FeedDiagnostics>> = _feeds.asStateFlow()

    fun refreshStarted(feed: NewsFeed) = update(feed) { current ->
        current.copy(state = RefreshState.InFlight, startedAtMillis = currentTimeMillis())
    }

    fun refreshSucceeded(feed: NewsFeed, articleCount: Int) = update(feed) { current ->
        current.copy(
            state = RefreshState.Succeeded,
            lastError = null,
            lastArticleCount = articleCount,
            finishedAtMillis = currentTimeMillis(),
        )
    }

    fun refreshFailed(feed: NewsFeed, error: String, hasUsableCache: Boolean) = update(feed) { current ->
        current.copy(
            state = RefreshState.Failed,
            lastError = error,
            servedFromCache = hasUsableCache,
            finishedAtMillis = currentTimeMillis(),
        )
    }

    fun reset() {
        _feeds.value = emptyMap()
    }

    private fun update(feed: NewsFeed, block: (FeedDiagnostics) -> FeedDiagnostics) {
        _feeds.update { current ->
            current + (feed to block(current[feed] ?: FeedDiagnostics()))
        }
    }
}

data class FeedDiagnostics(
    val state: RefreshState = RefreshState.Idle,
    val lastError: String? = null,
    val lastArticleCount: Int? = null,
    val servedFromCache: Boolean = false,
    val startedAtMillis: Long? = null,
    val finishedAtMillis: Long? = null,
) {
    val lastDurationMillis: Long?
        get() = if (startedAtMillis != null && finishedAtMillis != null) {
            finishedAtMillis - startedAtMillis
        } else {
            null
        }
}

enum class RefreshState { Idle, InFlight, Succeeded, Failed }
