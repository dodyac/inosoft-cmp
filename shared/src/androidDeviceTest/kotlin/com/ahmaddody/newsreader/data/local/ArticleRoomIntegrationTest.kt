package com.ahmaddody.newsreader.data.local

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.model.NewsFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ArticleRoomIntegrationTest {
    private lateinit var database: NewsDatabase
    private lateinit var local: RoomArticleLocalDataSource

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder<NewsDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        local = RoomArticleLocalDataSource(database.articleDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun replace_all_is_observed_in_descending_time_order_and_removes_stale_rows() = runTest {
        val stale = article(
            id = "stale",
            title = "Stale story",
            publishedAt = "2026-08-12T03:00:00Z",
        )
        local.replaceAll(NewsFeed.TopHeadlines, listOf(stale))
        assertEquals(
            listOf("stale"),
            local.observeArticles(NewsFeed.TopHeadlines).first().map(Article::id),
        )

        val older = article(
            id = "older",
            title = "Older story",
            publishedAt = "2026-08-13T03:00:00Z",
        )
        val newer = article(
            id = "newer",
            title = "Newer story",
            publishedAt = "2026-08-14T03:00:00Z",
        )

        local.replaceAll(NewsFeed.TopHeadlines, listOf(older, newer))

        assertEquals(
            listOf("newer", "older"),
            local.observeArticles(NewsFeed.TopHeadlines).first().map(Article::id),
        )
        assertEquals(null, local.observeArticle("stale").first())
        assertEquals(newer, local.observeArticle("newer").first())
    }

    @Test
    fun each_feed_keeps_its_own_cache_and_shares_no_rows() = runTest {
        val headline = article(id = "shared-url", title = "Seen in top headlines")
        val everything = article(id = "shared-url", title = "Seen in everything")

        local.replaceAll(NewsFeed.TopHeadlines, listOf(headline))
        local.replaceAll(NewsFeed.Everything, listOf(everything))

        // The same article URL exists in both feeds; the composite key keeps both rows.
        assertEquals(
            listOf("Seen in top headlines"),
            local.observeArticles(NewsFeed.TopHeadlines).first().map(Article::title),
        )
        assertEquals(
            listOf("Seen in everything"),
            local.observeArticles(NewsFeed.Everything).first().map(Article::title),
        )

        // Clearing one feed leaves the other readable offline.
        local.replaceAll(NewsFeed.Everything, emptyList())
        assertTrue(local.hasArticles(NewsFeed.TopHeadlines))
        assertEquals(false, local.hasArticles(NewsFeed.Everything))
    }
}

private fun article(
    id: String,
    title: String,
    publishedAt: String = "2026-08-14T03:00:00Z",
) = Article(
    id = id,
    title = title,
    description = null,
    imageUrl = null,
    publishedAt = publishedAt,
    sourceName = "Integration test",
    articleUrl = "https://example.com/$id",
)
