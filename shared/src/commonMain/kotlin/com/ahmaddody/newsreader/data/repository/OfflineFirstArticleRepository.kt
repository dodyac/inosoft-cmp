package com.ahmaddody.newsreader.data.repository

import com.ahmaddody.newsreader.data.local.ArticleLocalDataSource
import com.ahmaddody.newsreader.data.remote.ArticleRemoteDataSource
import com.ahmaddody.newsreader.data.remote.NewsDataException
import com.ahmaddody.newsreader.data.remote.toDomainOrNull
import com.ahmaddody.newsreader.domain.model.AppError
import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.domain.model.RefreshResult
import com.ahmaddody.newsreader.domain.repository.ArticleRepository
import com.ahmaddody.newsreader.observability.AnalyticsEvents
import com.ahmaddody.newsreader.observability.AnalyticsParams
import com.ahmaddody.newsreader.observability.CrashKeys
import com.ahmaddody.newsreader.observability.Observability
import com.ahmaddody.newsreader.observability.SessionDiagnostics
import com.ahmaddody.newsreader.observability.logging.LogTags
import com.ahmaddody.newsreader.observability.logging.e
import com.ahmaddody.newsreader.observability.TraceHandle
import com.ahmaddody.newsreader.observability.Traces
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import io.ktor.serialization.ContentConvertException

internal class OfflineFirstArticleRepository(
    private val remote: ArticleRemoteDataSource,
    private val local: ArticleLocalDataSource,
    private val observability: Observability = Observability.NoOp,
) : ArticleRepository {
    override fun observeArticles(feed: NewsFeed) = local.observeArticles(feed)

    override fun observeArticle(articleId: String) = local.observeArticle(articleId)

    /**
     * Instrumented at the boundary rather than at each step: one custom trace covers the whole
     * refresh so Performance Monitoring measures what the reader actually waits for, with the
     * network and cache legs recorded as metrics on the same trace.
     */
    override suspend fun refreshArticles(feed: NewsFeed): RefreshResult {
        observability.crash.setKey(CrashKeys.Feed, feed.name)
        observability.breadcrumb(LogTags.Sync, "refresh(${feed.name}) started")
        SessionDiagnostics.refreshStarted(feed)

        val trace = observability.performance.startTrace(Traces.RefreshArticles)
        trace.putAttribute(Traces.Attribute.Feed, feed.name)

        return try {
            refreshInstrumented(feed, trace).also { result -> report(feed, trace, result) }
        } finally {
            trace.stop()
        }
    }

    private suspend fun refreshInstrumented(feed: NewsFeed, trace: TraceHandle): RefreshResult {
        val remoteArticles = try {
            remote.fetchArticles(feed)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            return remoteFailure(feed, error.toAppError(), error)
        }
        trace.putMetric(Traces.Metric.ArticlesFetched, remoteArticles.size.toLong())
        observability.breadcrumb(LogTags.Network, "fetched ${remoteArticles.size} articles for ${feed.name}")

        val mappedArticles = remoteArticles
            .mapNotNull { article -> article.toDomainOrNull() }
            .distinctBy { article -> article.id }
        trace.putMetric(
            Traces.Metric.ArticlesDropped,
            (remoteArticles.size - mappedArticles.size).toLong(),
        )

        if (remoteArticles.isNotEmpty() && mappedArticles.isEmpty()) {
            return remoteFailure(feed, AppError.MalformedResponse, cause = null)
        }

        return try {
            // Fetch and validate first. Room performs delete + insert atomically so a failed
            // write cannot erase a previously usable cache.
            local.replaceAll(feed, mappedArticles)
            trace.putMetric(Traces.Metric.ArticlesCached, mappedArticles.size.toLong())
            observability.breadcrumb(LogTags.Database, "cached ${mappedArticles.size} articles for ${feed.name}")
            RefreshResult.Success(mappedArticles.size)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            // A failing local write is a defect, not an environment condition: always a non-fatal.
            observability.logger.e(LogTags.Database, "cache write failed for ${feed.name}", error)
            observability.crash.recordNonFatal(error)
            RefreshResult.Failure(
                error = AppError.DatabaseUnavailable,
                hasUsableCache = safeHasArticles(feed),
            )
        }
    }

    private fun report(feed: NewsFeed, trace: TraceHandle, result: RefreshResult) {
        when (result) {
            is RefreshResult.Success -> {
                trace.putAttribute(Traces.Attribute.Outcome, Traces.Outcome.Success)
                SessionDiagnostics.refreshSucceeded(feed, result.updatedCount)
                observability.breadcrumb(
                    LogTags.Sync,
                    "refresh(${feed.name}) succeeded with ${result.updatedCount} articles",
                )
                observability.analytics.track(
                    AnalyticsEvents.FeedRefreshSucceeded,
                    mapOf(
                        AnalyticsParams.Feed to feed.name,
                        AnalyticsParams.ArticleCount to result.updatedCount.toString(),
                    ),
                )
            }

            is RefreshResult.Failure -> {
                val error = result.error.telemetryName()
                trace.putAttribute(Traces.Attribute.Outcome, Traces.Outcome.Failure)
                trace.putAttribute(Traces.Attribute.Error, error)
                observability.crash.setKey(CrashKeys.LastAppError, error)
                SessionDiagnostics.refreshFailed(feed, error, result.hasUsableCache)
                observability.breadcrumb(
                    LogTags.Sync,
                    "refresh(${feed.name}) failed: $error, usable cache=${result.hasUsableCache}",
                )
                observability.analytics.track(
                    AnalyticsEvents.FeedRefreshFailed,
                    mapOf(AnalyticsParams.Feed to feed.name, AnalyticsParams.Error to error),
                )
                if (result.hasUsableCache) {
                    observability.analytics.track(
                        AnalyticsEvents.CachedContentShown,
                        mapOf(AnalyticsParams.Feed to feed.name, AnalyticsParams.Error to error),
                    )
                }
            }
        }
    }

    private suspend fun remoteFailure(
        feed: NewsFeed,
        error: AppError,
        cause: Throwable?,
    ): RefreshResult.Failure {
        // Connectivity and quota failures are expected in the field and would drown the Crashlytics
        // non-fatal stream; only failures that indicate a defect on our side are recorded.
        if (error.isDefect()) {
            observability.logger.e(
                LogTags.Sync,
                "refresh(${feed.name}) hit a defect-class failure: ${error.telemetryName()}",
                cause,
            )
            cause?.let(observability.crash::recordNonFatal)
        }

        return try {
            RefreshResult.Failure(error = error, hasUsableCache = local.hasArticles(feed))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (databaseError: Throwable) {
            observability.crash.recordNonFatal(databaseError)
            RefreshResult.Failure(
                error = AppError.DatabaseUnavailable,
                hasUsableCache = false,
            )
        }
    }

    private suspend fun safeHasArticles(feed: NewsFeed): Boolean = try {
        local.hasArticles(feed)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        false
    }
}

/** Stable, low-cardinality names. Enum names would change silently on a refactor. */
internal fun AppError.telemetryName(): String = when (this) {
    AppError.MissingApiKey -> "missing_api_key"
    AppError.NetworkUnavailable -> "network_unavailable"
    AppError.Timeout -> "timeout"
    AppError.Unauthorized -> "unauthorized"
    AppError.RateLimited -> "rate_limited"
    AppError.RequestRejected -> "request_rejected"
    AppError.ServerUnavailable -> "server_unavailable"
    AppError.MalformedResponse -> "malformed_response"
    AppError.DatabaseUnavailable -> "database_unavailable"
    AppError.ArticleNotFound -> "article_not_found"
    AppError.Unknown -> "unknown"
}

/** True when the failure points at our code or our contract, not at the network or the plan. */
private fun AppError.isDefect(): Boolean = when (this) {
    AppError.MalformedResponse,
    AppError.DatabaseUnavailable,
    AppError.Unknown,
    -> true

    else -> false
}

private fun Throwable.toAppError(): AppError = when (this) {
    NewsDataException.MissingApiKey -> AppError.MissingApiKey
    NewsDataException.MalformedResponse -> AppError.MalformedResponse
    is NewsDataException.ApiResponse -> when (code) {
        "apiKeyDisabled", "apiKeyExhausted", "apiKeyInvalid", "apiKeyMissing" -> AppError.Unauthorized
        "rateLimited", "maximumResultsReached" -> AppError.RateLimited
        else -> AppError.RequestRejected
    }
    is HttpRequestTimeoutException,
    is ConnectTimeoutException,
    is SocketTimeoutException,
    -> AppError.Timeout
    is ClientRequestException -> when (response.status) {
        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> AppError.Unauthorized
        HttpStatusCode.TooManyRequests -> AppError.RateLimited
        else -> AppError.RequestRejected
    }
    is ServerResponseException -> AppError.ServerUnavailable
    is SerializationException,
    is ContentConvertException,
    -> AppError.MalformedResponse
    else -> AppError.NetworkUnavailable
}
