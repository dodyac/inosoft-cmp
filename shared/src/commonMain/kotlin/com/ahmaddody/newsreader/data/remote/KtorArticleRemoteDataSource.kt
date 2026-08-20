package com.ahmaddody.newsreader.data.remote

import com.ahmaddody.newsreader.domain.model.NewsFeed
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/** Free NewsAPI plans cap a page at 100 items; 50 keeps payloads small while filling a screen. */
private const val PageSize = 50
private const val TopHeadlinesCountry = "id"
private const val EverythingQuery = "indonesia"
private const val EverythingLanguage = "id"
private const val EverythingSort = "publishedAt"

internal class KtorArticleRemoteDataSource(
    private val client: HttpClient,
    private val hasApiKey: Boolean,
) : ArticleRemoteDataSource {
    override suspend fun fetchArticles(feed: NewsFeed): List<ArticleDto> {
        if (!hasApiKey) throw NewsDataException.MissingApiKey

        val response = when (feed) {
            NewsFeed.TopHeadlines -> client.get("top-headlines") {
                parameter("country", TopHeadlinesCountry)
                parameter("pageSize", PageSize)
            }

            NewsFeed.Everything -> client.get("everything") {
                parameter("q", EverythingQuery)
                parameter("language", EverythingLanguage)
                parameter("sortBy", EverythingSort)
                parameter("pageSize", PageSize)
            }
        }.body<NewsApiResponse>()

        if (response.status != "ok") {
            throw NewsDataException.ApiResponse(response.code)
        }

        return response.articles ?: throw NewsDataException.MalformedResponse
    }
}

internal sealed class NewsDataException(message: String) : Exception(message) {
    data object MissingApiKey : NewsDataException("NewsAPI key is not configured")
    data object MalformedResponse : NewsDataException("NewsAPI omitted its articles payload")
    data class ApiResponse(val code: String?) : NewsDataException("NewsAPI rejected the response: $code")
}
