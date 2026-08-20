package com.ahmaddody.newsreader.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import com.ahmaddody.newsreader.data.local.ArticleLocalDataSource
import com.ahmaddody.newsreader.data.repository.OfflineFirstArticleRepository
import com.ahmaddody.newsreader.domain.model.AppError
import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NewsHttpClientTest {
    @Test
    fun top_headlines_request_uses_single_v2_path_country_and_header_key() = runTest {
        val engine = MockEngine { request ->
            assertEquals(
                "https://newsapi.org/v2/top-headlines?country=id&pageSize=50",
                request.url.toString(),
            )
            assertEquals("local-test-key", request.headers["X-Api-Key"])
            respond(
                content = ByteReadChannel(
                    """
                    {
                      "status": "ok",
                      "totalResults": 0,
                      "articles": []
                    }
                    """.trimIndent(),
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createNewsHttpClient(
            engine = engine,
            apiKey = "local-test-key",
            enableNetworkLogs = false,
        )
        val dataSource = KtorArticleRemoteDataSource(client, hasApiKey = true)

        try {
            assertEquals(emptyList(), dataSource.fetchArticles(NewsFeed.TopHeadlines))
        } finally {
            client.close()
        }
    }

    @Test
    fun everything_feed_requests_the_keyword_endpoint_instead_of_country_filtering() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/v2/everything", request.url.encodedPath)
            assertEquals("indonesia", request.url.parameters["q"])
            assertEquals("id", request.url.parameters["language"])
            assertEquals("publishedAt", request.url.parameters["sortBy"])
            assertEquals(null, request.url.parameters["country"])
            respond(
                content = ByteReadChannel(
                    """{ "status": "ok", "totalResults": 0, "articles": [] }""",
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createNewsHttpClient(
            engine = engine,
            apiKey = "local-test-key",
            enableNetworkLogs = false,
        )

        try {
            val dataSource = KtorArticleRemoteDataSource(client, hasApiKey = true)
            assertEquals(emptyList(), dataSource.fetchArticles(NewsFeed.Everything))
        } finally {
            client.close()
        }
    }

    @Test
    fun malformed_json_maps_to_domain_error_and_preserves_cache() = runTest {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel("{ definitely-not-json"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createNewsHttpClient(
            engine = engine,
            apiKey = "local-test-key",
            enableNetworkLogs = false,
        )
        val cached = Article(
            id = "cached",
            title = "Cached story",
            description = null,
            imageUrl = null,
            publishedAt = "2026-08-14T03:00:00Z",
            sourceName = null,
            articleUrl = null,
        )
        val local = NetworkTestLocalDataSource(listOf(cached))
        val repository = OfflineFirstArticleRepository(
            remote = KtorArticleRemoteDataSource(client, hasApiKey = true),
            local = local,
        )

        try {
            val result = assertIs<RefreshResult.Failure>(repository.refreshArticles(NewsFeed.TopHeadlines))
            assertEquals(AppError.MalformedResponse, result.error)
            assertTrue(result.hasUsableCache)
            assertEquals(listOf(cached), local.currentArticles)
        } finally {
            client.close()
        }
    }
}

private class NetworkTestLocalDataSource(initial: List<Article>) : ArticleLocalDataSource {
    private val state = MutableStateFlow(initial)
    val currentArticles: List<Article>
        get() = state.value

    override fun observeArticles(feed: NewsFeed): Flow<List<Article>> = state

    override fun observeArticle(articleId: String): Flow<Article?> =
        state.map { articles -> articles.firstOrNull { it.id == articleId } }

    override suspend fun hasArticles(feed: NewsFeed) = state.value.isNotEmpty()

    override suspend fun replaceAll(feed: NewsFeed, articles: List<Article>) {
        state.value = articles
    }
}
