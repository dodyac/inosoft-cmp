package com.ahmaddody.newsreader.data.repository

import com.ahmaddody.newsreader.data.local.ArticleLocalDataSource
import com.ahmaddody.newsreader.data.remote.ArticleDto
import com.ahmaddody.newsreader.data.remote.ArticleRemoteDataSource
import com.ahmaddody.newsreader.data.remote.NewsDataException
import com.ahmaddody.newsreader.data.remote.SourceDto
import com.ahmaddody.newsreader.domain.model.AppError
import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OfflineFirstArticleRepositoryTest {
    @Test
    fun refresh_fetches_maps_saves_and_exposes_local_source_articles() = runTest {
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource(
            result = Result.success(
                listOf(
                    articleDto(
                        title = "  KMP reaches more teams  ",
                        url = "https://example.com/kmp",
                    ),
                    articleDto(title = "[Removed]", url = "https://example.com/removed"),
                ),
            ),
        )
        val repository = OfflineFirstArticleRepository(remote, local)

        val result = repository.refreshArticles(NewsFeed.TopHeadlines)

        assertEquals(RefreshResult.Success(updatedCount = 1), result)
        assertEquals(1, remote.fetchCount)
        assertEquals(1, local.replaceCount)
        assertEquals(
            listOf("KMP reaches more teams"),
            repository.observeArticles(NewsFeed.TopHeadlines).first().map(Article::title),
        )
    }

    @Test
    fun remote_failure_preserves_and_reports_existing_cache() = runTest {
        val cached = article(id = "cached-id", title = "Saved while online")
        val local = FakeLocalDataSource(listOf(cached))
        val remote = FakeRemoteDataSource(Result.failure(Exception("offline")))
        val repository = OfflineFirstArticleRepository(remote, local)

        val result = assertIs<RefreshResult.Failure>(repository.refreshArticles(NewsFeed.TopHeadlines))

        assertEquals(AppError.NetworkUnavailable, result.error)
        assertTrue(result.hasUsableCache)
        assertEquals(0, local.replaceCount)
        assertEquals(listOf(cached), repository.observeArticles(NewsFeed.TopHeadlines).first())
    }

    @Test
    fun remote_failure_without_cache_reports_network_error() = runTest {
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource(Result.failure(Exception("offline")))
        val repository = OfflineFirstArticleRepository(remote, local)

        val result = assertIs<RefreshResult.Failure>(repository.refreshArticles(NewsFeed.TopHeadlines))

        assertEquals(AppError.NetworkUnavailable, result.error)
        assertFalse(result.hasUsableCache)
        assertEquals(0, local.replaceCount)
    }

    @Test
    fun remote_and_local_failure_returns_database_error_without_claiming_cache() = runTest {
        val local = FakeLocalDataSource().apply { failCacheCheck = true }
        val remote = FakeRemoteDataSource(Result.failure(Exception("offline")))
        val repository = OfflineFirstArticleRepository(remote, local)

        val result = assertIs<RefreshResult.Failure>(repository.refreshArticles(NewsFeed.TopHeadlines))

        assertEquals(AppError.DatabaseUnavailable, result.error)
        assertFalse(result.hasUsableCache)
        assertEquals(0, local.replaceCount)
    }

    @Test
    fun malformed_non_empty_response_does_not_erase_cache() = runTest {
        val cached = article(id = "safe-cache", title = "Keep this story")
        val local = FakeLocalDataSource(listOf(cached))
        val remote = FakeRemoteDataSource(
            Result.success(
                listOf(
                    articleDto(
                        title = "",
                        url = "https://example.com/invalid",
                    ),
                ),
            ),
        )
        val repository = OfflineFirstArticleRepository(remote, local)

        val result = assertIs<RefreshResult.Failure>(repository.refreshArticles(NewsFeed.TopHeadlines))

        assertEquals(AppError.MalformedResponse, result.error)
        assertTrue(result.hasUsableCache)
        assertEquals(0, local.replaceCount)
        assertEquals(listOf(cached), repository.observeArticles(NewsFeed.TopHeadlines).first())
    }

    @Test
    fun omitted_articles_payload_does_not_erase_cache() = runTest {
        val cached = article(id = "safe-cache", title = "Keep this story")
        val local = FakeLocalDataSource(listOf(cached))
        val remote = FakeRemoteDataSource(Result.failure(NewsDataException.MalformedResponse))
        val repository = OfflineFirstArticleRepository(remote, local)

        val result = assertIs<RefreshResult.Failure>(repository.refreshArticles(NewsFeed.TopHeadlines))

        assertEquals(AppError.MalformedResponse, result.error)
        assertTrue(result.hasUsableCache)
        assertEquals(0, local.replaceCount)
        assertEquals(listOf(cached), repository.observeArticles(NewsFeed.TopHeadlines).first())
    }

    @Test
    fun database_write_failure_returns_storage_error_and_preserves_cache() = runTest {
        val cached = article(id = "cached-id", title = "Safe cached story")
        val local = FakeLocalDataSource(listOf(cached)).apply { failReplacement = true }
        val remote = FakeRemoteDataSource(
            Result.success(listOf(articleDto("New story", "https://example.com/new"))),
        )
        val repository = OfflineFirstArticleRepository(remote, local)

        val result = assertIs<RefreshResult.Failure>(repository.refreshArticles(NewsFeed.TopHeadlines))

        assertEquals(AppError.DatabaseUnavailable, result.error)
        assertTrue(result.hasUsableCache)
        assertEquals(listOf(cached), repository.observeArticles(NewsFeed.TopHeadlines).first())
    }

    @Test
    fun refreshing_one_feed_leaves_the_other_feeds_cache_intact() = runTest {
        val headline = article(id = "headline", title = "Cached headline")
        val local = FakeLocalDataSource(listOf(headline), initialFeed = NewsFeed.TopHeadlines)
        val remote = FakeRemoteDataSource(
            result = Result.failure(Exception("unused")),
            resultsByFeed = mapOf(
                NewsFeed.Everything to Result.success(
                    listOf(articleDto("Everything story", "https://example.com/everything")),
                ),
            ),
        )
        val repository = OfflineFirstArticleRepository(remote, local)

        val result = repository.refreshArticles(NewsFeed.Everything)

        assertEquals(RefreshResult.Success(updatedCount = 1), result)
        assertEquals(NewsFeed.Everything, remote.lastRequestedFeed)
        assertEquals(
            listOf("Everything story"),
            repository.observeArticles(NewsFeed.Everything).first().map(Article::title),
        )
        // The top-headlines cache is untouched, so switching tabs offline still shows content.
        assertEquals(listOf(headline), repository.observeArticles(NewsFeed.TopHeadlines).first())
    }

    @Test
    fun failing_feed_reports_only_its_own_cache_state() = runTest {
        val headline = article(id = "headline", title = "Cached headline")
        val local = FakeLocalDataSource(listOf(headline), initialFeed = NewsFeed.TopHeadlines)
        val remote = FakeRemoteDataSource(Result.failure(Exception("offline")))
        val repository = OfflineFirstArticleRepository(remote, local)

        val result = assertIs<RefreshResult.Failure>(repository.refreshArticles(NewsFeed.Everything))

        // A populated top-headlines cache must not be reported as usable content for Everything.
        assertFalse(result.hasUsableCache)
    }

    @Test
    fun refresh_rethrows_coroutine_cancellation() = runTest {
        val local = FakeLocalDataSource()
        val remote = FakeRemoteDataSource(Result.failure(CancellationException("cancelled")))
        val repository = OfflineFirstArticleRepository(remote, local)

        kotlin.test.assertFailsWith<CancellationException> {
            repository.refreshArticles(NewsFeed.TopHeadlines)
        }
    }
}

private class FakeRemoteDataSource(
    private val result: Result<List<ArticleDto>>,
    private val resultsByFeed: Map<NewsFeed, Result<List<ArticleDto>>> = emptyMap(),
) : ArticleRemoteDataSource {
    var fetchCount = 0
        private set
    var lastRequestedFeed: NewsFeed? = null
        private set

    override suspend fun fetchArticles(feed: NewsFeed): List<ArticleDto> {
        fetchCount += 1
        lastRequestedFeed = feed
        return (resultsByFeed[feed] ?: result).getOrThrow()
    }
}

private class FakeLocalDataSource(
    initialArticles: List<Article> = emptyList(),
    initialFeed: NewsFeed = NewsFeed.TopHeadlines,
) : ArticleLocalDataSource {
    private val articlesByFeed = MutableStateFlow(mapOf(initialFeed to initialArticles))

    var replaceCount = 0
        private set
    var failCacheCheck = false
    var failReplacement = false

    override fun observeArticles(feed: NewsFeed): Flow<List<Article>> =
        articlesByFeed.map { cache -> cache[feed].orEmpty() }

    override fun observeArticle(articleId: String): Flow<Article?> = articlesByFeed.map { cache ->
        cache.values.flatten().firstOrNull { it.id == articleId }
    }

    override suspend fun hasArticles(feed: NewsFeed): Boolean {
        if (failCacheCheck) error("database unavailable")
        return articlesByFeed.value[feed].orEmpty().isNotEmpty()
    }

    override suspend fun replaceAll(feed: NewsFeed, articles: List<Article>) {
        if (failReplacement) error("database write unavailable")
        replaceCount += 1
        articlesByFeed.value = articlesByFeed.value + (feed to articles)
    }
}

private fun articleDto(
    title: String,
    url: String,
) = ArticleDto(
    source = SourceDto(name = "INOSOFT Daily"),
    title = title,
    description = "A useful description",
    url = url,
    imageUrl = "https://example.com/image.jpg",
    publishedAt = "2026-08-14T03:00:00Z",
)

private fun article(
    id: String,
    title: String,
) = Article(
    id = id,
    title = title,
    description = "Cached description",
    imageUrl = null,
    publishedAt = "2026-08-14T03:00:00Z",
    sourceName = "Cached source",
    articleUrl = null,
)
