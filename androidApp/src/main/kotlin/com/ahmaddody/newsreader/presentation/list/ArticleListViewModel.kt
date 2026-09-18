package com.ahmaddody.newsreader.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmaddody.newsreader.domain.model.AppError
import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.domain.model.RefreshResult
import com.ahmaddody.newsreader.domain.usecase.ObserveArticles
import com.ahmaddody.newsreader.domain.usecase.RefreshArticles
import com.ahmaddody.newsreader.observability.AnalyticsEvents
import com.ahmaddody.newsreader.observability.AnalyticsParams
import com.ahmaddody.newsreader.observability.CrashKeys
import com.ahmaddody.newsreader.observability.Observability
import com.ahmaddody.newsreader.observability.Screens
import com.ahmaddody.newsreader.observability.logging.LogTags
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArticleListUiState(
    val selectedFeed: NewsFeed = NewsFeed.Default,
    val articles: List<Article> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val showingSavedArticles: Boolean = false,
    val blockingError: AppError? = null,
    val message: AppError? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleListViewModel(
    private val observeArticles: ObserveArticles,
    private val refreshArticles: RefreshArticles,
    // Defaulted so UI tests can build a ViewModel without a telemetry graph. Production wiring in
    // AppModule passes the real bundle explicitly, so a missing binding still fails loudly there.
    private val observability: Observability = Observability.NoOp,
) : ViewModel() {
    private val selectedFeed = MutableStateFlow(NewsFeed.Default)
    private val syncStates = MutableStateFlow(emptyMap<NewsFeed, SyncState>())
    private val localError = MutableStateFlow<AppError?>(null)
    private val localObserverGeneration = MutableStateFlow(0)
    private val refreshJobs = mutableMapOf<NewsFeed, Job>()

    private val cachedArticles = combine(selectedFeed, localObserverGeneration) { feed, _ -> feed }
        .flatMapLatest { feed ->
            observeArticles(feed)
                .onStart { localError.value = null }
                .catch {
                    localError.value = AppError.DatabaseUnavailable
                    emit(emptyList())
                }
        }

    val uiState = combine(
        selectedFeed,
        cachedArticles,
        syncStates,
        localError,
    ) { feed, articles, syncs, databaseError ->
        val sync = syncs[feed] ?: SyncState.Loading
        val isLoading = sync is SyncState.Loading
        val awaitingCommittedArticles =
            sync is SyncState.Success && sync.updatedCount > 0 && articles.isEmpty()
        val refreshFailure = sync as? SyncState.Failure
        val awaitingKnownCache =
            refreshFailure?.hasUsableCache == true && articles.isEmpty() && databaseError == null
        ArticleListUiState(
            selectedFeed = feed,
            articles = articles,
            isInitialLoading = (isLoading || awaitingCommittedArticles || awaitingKnownCache) &&
                articles.isEmpty() && databaseError == null,
            isRefreshing = isLoading && articles.isNotEmpty(),
            showingSavedArticles = refreshFailure != null && articles.isNotEmpty(),
            blockingError = databaseError
                ?: refreshFailure
                    ?.takeUnless { it.hasUsableCache }
                    ?.error
                    ?.takeIf { articles.isEmpty() },
            message = refreshFailure
                ?.takeIf { articles.isNotEmpty() }
                ?.takeUnless(SyncState.Failure::messageConsumed)
                ?.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = ArticleListUiState(),
    )

    init {
        observability.crash.setKey(CrashKeys.Screen, Screens.ArticleList)
        observability.breadcrumb(LogTags.Navigation, "opened ${Screens.ArticleList}")
        observability.analytics.track(
            AnalyticsEvents.ScreenViewed,
            mapOf(AnalyticsParams.ScreenName to Screens.ArticleList),
        )
        refresh()
    }

    /**
     * Switching tabs shows that feed's cache immediately and syncs it only the first time, so
     * returning to a tab does not spend a request the free NewsAPI plan may not have.
     */
    fun selectFeed(feed: NewsFeed) {
        if (selectedFeed.value == feed) return
        selectedFeed.value = feed
        observability.crash.setKey(CrashKeys.Feed, feed.name)
        observability.breadcrumb(LogTags.Navigation, "selected feed ${feed.name}")
        observability.analytics.track(
            AnalyticsEvents.FeedSelected,
            mapOf(AnalyticsParams.Feed to feed.name),
        )
        if (syncStates.value[feed] == null) refresh()
    }

    fun refresh() {
        val feed = selectedFeed.value
        if (refreshJobs[feed]?.isActive == true) return
        observability.analytics.track(
            AnalyticsEvents.FeedRefreshRequested,
            mapOf(AnalyticsParams.Feed to feed.name),
        )
        // A failed local observer never recovers on its own; restart it before syncing again.
        if (localError.value != null) localObserverGeneration.value += 1
        syncStates.update(feed, SyncState.Loading)
        refreshJobs[feed] = viewModelScope.launch {
            val state = when (val result = refreshArticles(feed)) {
                is RefreshResult.Success -> SyncState.Success(result.updatedCount)
                is RefreshResult.Failure -> SyncState.Failure(
                    error = result.error,
                    hasUsableCache = result.hasUsableCache,
                )
            }
            syncStates.update(feed, state)
        }
    }

    fun consumeMessage() {
        val feed = selectedFeed.value
        val failure = syncStates.value[feed] as? SyncState.Failure ?: return
        syncStates.update(feed, failure.copy(messageConsumed = true))
    }

    private fun MutableStateFlow<Map<NewsFeed, SyncState>>.update(feed: NewsFeed, state: SyncState) {
        value = value + (feed to state)
    }

    private sealed interface SyncState {
        data object Loading : SyncState
        data class Success(val updatedCount: Int) : SyncState
        data class Failure(
            val error: AppError,
            val hasUsableCache: Boolean,
            val messageConsumed: Boolean = false,
        ) : SyncState
    }
}
