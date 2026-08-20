package com.ahmaddody.newsreader.data.repository

import com.ahmaddody.newsreader.data.local.ArticleLocalDataSource
import com.ahmaddody.newsreader.data.remote.ArticleRemoteDataSource
import com.ahmaddody.newsreader.data.remote.NewsDataException
import com.ahmaddody.newsreader.data.remote.toDomainOrNull
import com.ahmaddody.newsreader.domain.model.AppError
import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.domain.model.RefreshResult
import com.ahmaddody.newsreader.domain.repository.ArticleRepository
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
) : ArticleRepository {
    override fun observeArticles(feed: NewsFeed) = local.observeArticles(feed)

    override fun observeArticle(articleId: String) = local.observeArticle(articleId)

    override suspend fun refreshArticles(feed: NewsFeed): RefreshResult {
        val remoteArticles = try {
            remote.fetchArticles(feed)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            return remoteFailure(feed, error.toAppError())
        }

        val mappedArticles = remoteArticles
            .mapNotNull { article -> article.toDomainOrNull() }
            .distinctBy { article -> article.id }

        if (remoteArticles.isNotEmpty() && mappedArticles.isEmpty()) {
            return remoteFailure(feed, AppError.MalformedResponse)
        }

        return try {
            // Fetch and validate first. Room performs delete + insert atomically so a failed
            // write cannot erase a previously usable cache.
            local.replaceAll(feed, mappedArticles)
            RefreshResult.Success(mappedArticles.size)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            RefreshResult.Failure(
                error = AppError.DatabaseUnavailable,
                hasUsableCache = safeHasArticles(feed),
            )
        }
    }

    private suspend fun remoteFailure(feed: NewsFeed, error: AppError): RefreshResult.Failure {
        return try {
            RefreshResult.Failure(error = error, hasUsableCache = local.hasArticles(feed))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
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
