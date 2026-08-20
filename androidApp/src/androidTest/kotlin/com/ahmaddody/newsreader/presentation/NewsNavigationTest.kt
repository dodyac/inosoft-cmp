package com.ahmaddody.newsreader.presentation

import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.test.platform.app.InstrumentationRegistry
import com.ahmaddody.newsreader.R
import com.ahmaddody.newsreader.domain.model.AppError
import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.domain.model.RefreshResult
import com.ahmaddody.newsreader.domain.repository.ArticleRepository
import com.ahmaddody.newsreader.domain.usecase.ObserveArticle
import com.ahmaddody.newsreader.domain.usecase.ObserveArticles
import com.ahmaddody.newsreader.domain.usecase.RefreshArticles
import com.ahmaddody.newsreader.presentation.common.UiTags
import com.ahmaddody.newsreader.presentation.detail.ArticleDetailViewModel
import com.ahmaddody.newsreader.presentation.list.ArticleListViewModel
import com.ahmaddody.newsreader.presentation.navigation.NewsNavHost
import com.ahmaddody.newsreader.presentation.theme.NewsReaderTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class NewsNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val backContentDescription: String
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.navigate_back)

    @Test
    fun app_opens_list_taps_article_shows_detail_and_navigates_back() {
        val article = testArticle()
        val repository = FakeArticleRepository(
            cachedArticles = listOf(article),
            refreshResult = RefreshResult.Success(updatedCount = 1),
        )
        val listViewModel = ArticleListViewModel(
            observeArticles = ObserveArticles(repository),
            refreshArticles = RefreshArticles(repository),
        )

        composeRule.setContent {
            NewsReaderTheme {
                NewsNavHost(
                    listViewModel = listViewModel,
                    detailViewModelProvider = { articleId ->
                        remember(articleId) {
                            ArticleDetailViewModel(articleId, ObserveArticle(repository))
                        }
                    },
                )
            }
        }

        composeRule.waitUntilExactlyOneExists(hasTestTag(UiTags.article(article.id)))
        composeRule.onNodeWithTag(UiTags.article(article.id)).performClick()

        composeRule.onNodeWithTag(UiTags.ArticleDetail).assertIsDisplayed()
        composeRule.onNodeWithText(article.description!!).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(backContentDescription).performClick()
        composeRule.onNodeWithTag(UiTags.ArticleList).assertIsDisplayed()
    }

    @Test
    fun cached_articles_remain_visible_when_refresh_fails_offline() {
        val article = testArticle(title = "Available without a connection")
        val repository = FakeArticleRepository(
            cachedArticles = listOf(article),
            refreshResult = RefreshResult.Failure(
                error = AppError.NetworkUnavailable,
                hasUsableCache = true,
            ),
        )
        val listViewModel = ArticleListViewModel(
            observeArticles = ObserveArticles(repository),
            refreshArticles = RefreshArticles(repository),
        )

        composeRule.setContent {
            NewsReaderTheme {
                NewsNavHost(
                    listViewModel = listViewModel,
                    detailViewModelProvider = { articleId ->
                        remember(articleId) {
                            ArticleDetailViewModel(articleId, ObserveArticle(repository))
                        }
                    },
                )
            }
        }

        composeRule.waitUntilExactlyOneExists(hasTestTag(UiTags.article(article.id)))
        composeRule.onNodeWithText(article.title).assertIsDisplayed()
        composeRule.onNodeWithTag(UiTags.SavedArticlesNotice).assertIsDisplayed()
    }

    @Test
    fun switching_feed_chip_shows_the_other_feeds_cached_articles() {
        val headline = testArticle(title = "Only in top headlines")
        val everything = testArticle(
            title = "Only in everything",
            id = "https://example.com/everything"
        )
        val repository = FakeArticleRepository(
            cachedArticles = listOf(headline),
            refreshResult = RefreshResult.Success(updatedCount = 1),
            everythingArticles = listOf(everything),
        )
        val listViewModel = ArticleListViewModel(
            observeArticles = ObserveArticles(repository),
            refreshArticles = RefreshArticles(repository),
        )

        composeRule.setContent {
            NewsReaderTheme {
                NewsNavHost(
                    listViewModel = listViewModel,
                    detailViewModelProvider = { articleId ->
                        remember(articleId) {
                            ArticleDetailViewModel(articleId, ObserveArticle(repository))
                        }
                    },
                )
            }
        }

        composeRule.waitUntilExactlyOneExists(hasTestTag(UiTags.article(headline.id)))
        composeRule.onNodeWithTag(UiTags.feedChip(NewsFeed.Everything)).performClick()

        composeRule.waitUntilExactlyOneExists(hasTestTag(UiTags.article(everything.id)))
        composeRule.onNodeWithText(everything.title).assertIsDisplayed()
        composeRule.onNodeWithText(headline.title).assertIsNotDisplayed()
    }

    @Test
    fun tapping_the_detail_image_opens_and_closes_the_full_screen_viewer() {
        val article = testArticle()
        val repository = FakeArticleRepository(
            cachedArticles = listOf(article),
            refreshResult = RefreshResult.Success(updatedCount = 1),
        )
        val listViewModel = ArticleListViewModel(
            observeArticles = ObserveArticles(repository),
            refreshArticles = RefreshArticles(repository),
        )

        composeRule.setContent {
            NewsReaderTheme {
                NewsNavHost(
                    listViewModel = listViewModel,
                    detailViewModelProvider = { articleId ->
                        remember(articleId) {
                            ArticleDetailViewModel(articleId, ObserveArticle(repository))
                        }
                    },
                )
            }
        }

        composeRule.waitUntilExactlyOneExists(hasTestTag(UiTags.article(article.id)))
        composeRule.onNodeWithTag(UiTags.article(article.id)).performClick()

        composeRule.waitUntilExactlyOneExists(hasTestTag(UiTags.DetailImage))
        composeRule.onNodeWithTag(UiTags.DetailImage).performClick()

        composeRule.waitUntilExactlyOneExists(hasTestTag(UiTags.ImageViewer))
        composeRule.onNodeWithTag(UiTags.ImageViewer).assertIsDisplayed()

        composeRule.onNodeWithTag(UiTags.ImageViewerClose).performClick()
        composeRule.waitUntilDoesNotExist(hasTestTag(UiTags.ImageViewer))
        composeRule.onNodeWithTag(UiTags.ArticleDetail).assertIsDisplayed()
    }
}

private class FakeArticleRepository(
    cachedArticles: List<Article>,
    private val refreshResult: RefreshResult,
    everythingArticles: List<Article> = emptyList(),
) : ArticleRepository {
    private val articlesByFeed = MutableStateFlow(
        mapOf(
            NewsFeed.TopHeadlines to cachedArticles,
            NewsFeed.Everything to everythingArticles,
        ),
    )

    override fun observeArticles(feed: NewsFeed): Flow<List<Article>> =
        articlesByFeed.map { cache -> cache[feed].orEmpty() }

    override fun observeArticle(articleId: String): Flow<Article?> = articlesByFeed.map { cache ->
        cache.values.flatten().firstOrNull { it.id == articleId }
    }

    override suspend fun refreshArticles(feed: NewsFeed) = refreshResult
}

private fun testArticle(
    title: String = "Kotlin Multiplatform in production",
    id: String = "https://example.com/kmp-production",
) = Article(
    id = id,
    title = title,
    description = "A complete description shown only on the detail screen.",
    imageUrl = "https://example.com/image.jpg",
    publishedAt = "2026-08-14T03:05:00Z",
    sourceName = "INOSOFT Daily",
    articleUrl = id,
)
