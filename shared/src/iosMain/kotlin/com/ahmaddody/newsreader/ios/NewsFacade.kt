package com.ahmaddody.newsreader.ios

import com.ahmaddody.newsreader.domain.model.AppError
import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.domain.model.RefreshResult
import com.ahmaddody.newsreader.domain.usecase.ObserveArticle
import com.ahmaddody.newsreader.domain.usecase.ObserveArticles
import com.ahmaddody.newsreader.domain.usecase.RefreshArticles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import org.koin.mp.KoinPlatform

/**
 * Swift-facing wrapper over the shared use cases.
 *
 * Kotlin `Flow` is not consumable from Swift, so observation is exposed as a callback plus a
 * cancellation handle. Business rules stay in `commonMain`; this type only adapts the calling
 * convention and mirrors the Android ViewModel's local-failure handling.
 */
class NewsFacade internal constructor(
    private val observeArticles: ObserveArticles,
    private val observeArticle: ObserveArticle,
    private val refreshArticles: RefreshArticles,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun observeArticles(
        feed: NewsFeed,
        onArticles: (List<Article>) -> Unit,
        onError: (AppError) -> Unit,
    ): SubscriptionHandle = observeArticles.invoke(feed)
        .onEach(onArticles)
        .catch {
            onError(AppError.DatabaseUnavailable)
            onArticles(emptyList())
        }
        .launchIn(scope)
        .asHandle()

    fun observeArticle(
        articleId: String,
        onArticle: (Article?) -> Unit,
        onError: (AppError) -> Unit,
    ): SubscriptionHandle = observeArticle.invoke(articleId)
        .onEach(onArticle)
        .catch { onError(AppError.DatabaseUnavailable) }
        .launchIn(scope)
        .asHandle()

    /** Exported to Swift as an `async` function; cancellation propagates from the Swift task. */
    suspend fun refresh(feed: NewsFeed): RefreshResult = refreshArticles(feed)

    private fun Job.asHandle() = SubscriptionHandle(this)
}

/** Keeps a running collection cancellable from Swift, mirroring `onDisappear` teardown. */
class SubscriptionHandle internal constructor(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}

/** Resolves the facade from the Koin graph started by [com.ahmaddody.newsreader.di.initKoinIos]. */
fun newsFacade(): NewsFacade = KoinPlatform.getKoin().get()

/**
 * Swift cannot call Kotlin functions with default arguments, so the shared formatter is re-exposed
 * with the device time zone already applied.
 */
fun formatPublicationDateForDisplay(isoTimestamp: String): String =
    com.ahmaddody.newsreader.domain.util.formatPublicationDate(isoTimestamp)
