package com.ahmaddody.newsreader.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ahmaddody.newsreader.R
import com.ahmaddody.newsreader.domain.model.AppError
import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.domain.util.formatPublicationDate
import com.ahmaddody.newsreader.presentation.common.NewsDimens
import com.ahmaddody.newsreader.presentation.common.UiTags
import com.ahmaddody.newsreader.presentation.common.errorMessage

@Composable
fun ArticleListRoute(
    viewModel: ArticleListViewModel,
    onArticleClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val transientMessage = uiState.message?.let { errorMessage(it) }
    val retryLabel = stringResource(R.string.retry)

    LaunchedEffect(transientMessage) {
        if (transientMessage == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = transientMessage,
            actionLabel = retryLabel,
            duration = SnackbarDuration.Long,
        )
        viewModel.consumeMessage()
        if (result == SnackbarResult.ActionPerformed) viewModel.refresh()
    }

    ArticleListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onRefresh = viewModel::refresh,
        onFeedSelected = viewModel::selectFeed,
        onArticleClick = onArticleClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    uiState: ArticleListUiState,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    onFeedSelected: (NewsFeed) -> Unit,
    onArticleClick: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.list_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.list_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            FeedSelector(
                selectedFeed = uiState.selectedFeed,
                onFeedSelected = onFeedSelected,
            )
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    uiState.isInitialLoading -> LoadingState()
                    uiState.blockingError != null -> ErrorState(
                        error = uiState.blockingError,
                        onRetry = onRefresh,
                    )
                    uiState.articles.isEmpty() -> EmptyState(onRefresh)
                    else -> ArticleFeed(
                        articles = uiState.articles,
                        showingSavedArticles = uiState.showingSavedArticles,
                        onArticleClick = onArticleClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedSelector(
    selectedFeed: NewsFeed,
    onFeedSelected: (NewsFeed) -> Unit,
) {
    val feedSelectorLabel = stringResource(R.string.feed_selector)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NewsDimens.SpaceLg, vertical = NewsDimens.SpaceSm)
            .selectableGroup()
            .semantics { contentDescription = feedSelectorLabel }
            .testTag(UiTags.FeedSelector),
        horizontalArrangement = Arrangement.spacedBy(NewsDimens.SpaceSm),
    ) {
        NewsFeed.entries.forEach { feed ->
            FilterChip(
                selected = feed == selectedFeed,
                onClick = { onFeedSelected(feed) },
                label = { Text(stringResource(feed.labelRes)) },
                modifier = Modifier
                    .testTag(UiTags.feedChip(feed))
                    .semantics { selected = feed == selectedFeed },
                shape = RoundedCornerShape(NewsDimens.CardRadius),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

private val NewsFeed.labelRes: Int
    get() = when (this) {
        NewsFeed.TopHeadlines -> R.string.feed_top_headlines
        NewsFeed.Everything -> R.string.feed_everything
    }

@Composable
private fun ArticleFeed(
    articles: List<Article>,
    showingSavedArticles: Boolean,
    onArticleClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTags.ArticleList),
        contentPadding = PaddingValues(
            start = NewsDimens.SpaceLg,
            top = NewsDimens.SpaceSm,
            end = NewsDimens.SpaceLg,
            bottom = NewsDimens.SpaceXxl,
        ),
        verticalArrangement = Arrangement.spacedBy(NewsDimens.SpaceMd),
    ) {
        if (showingSavedArticles) {
            item(key = "saved-articles-notice") {
                SavedArticlesNotice()
            }
        }

        items(
            items = articles,
            key = Article::id,
        ) { article ->
            ArticleCard(
                article = article,
                onClick = { onArticleClick(article.id) },
            )
        }
    }
}

@Composable
private fun SavedArticlesNotice() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTags.SavedArticlesNotice),
        shape = RoundedCornerShape(NewsDimens.CardRadius),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(NewsDimens.SpaceLg),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
            )
            Spacer(Modifier.width(NewsDimens.SpaceMd))
            Column {
                Text(
                    text = stringResource(R.string.saved_articles_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(NewsDimens.SpaceXs))
                Text(
                    text = stringResource(R.string.saved_articles_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ArticleCard(
    article: Article,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTags.article(article.id)),
        shape = RoundedCornerShape(NewsDimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = NewsDimens.CardElevation),
    ) {
        Row(
            modifier = Modifier.padding(NewsDimens.SpaceMd),
            verticalAlignment = Alignment.Top,
        ) {
            ArticleImage(article)
            Spacer(Modifier.width(NewsDimens.SpaceMd))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NewsDimens.SpaceXs),
            ) {
                Text(
                    text = article.sourceName ?: stringResource(R.string.source_unknown),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = article.description ?: stringResource(R.string.description_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(NewsDimens.SpaceXs))
                Text(
                    text = formatPublicationDate(article.publishedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ArticleImage(article: Article) {
    val modifier = Modifier
        .width(NewsDimens.ListImageWidth)
        .height(NewsDimens.ListImageHeight)
        .clip(RoundedCornerShape(NewsDimens.ImageRadius))

    if (article.imageUrl == null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ImageNotSupported,
                contentDescription = stringResource(R.string.article_image_unavailable),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        AsyncImage(
            model = article.imageUrl,
            contentDescription = stringResource(R.string.article_image_description, article.title),
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTags.InitialLoading),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(NewsDimens.SpaceLg))
            Text(
                text = stringResource(R.string.loading_articles),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun EmptyState(onRefresh: () -> Unit) {
    StatusState(
        testTag = UiTags.EmptyState,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Newspaper,
                contentDescription = null,
                modifier = Modifier.size(NewsDimens.EmptyIconSize),
            )
        },
        title = stringResource(R.string.empty_title),
        description = stringResource(R.string.empty_description),
        onRetry = onRefresh,
    )
}

@Composable
private fun ErrorState(
    error: AppError,
    onRetry: () -> Unit,
) {
    StatusState(
        testTag = UiTags.ErrorState,
        icon = {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(NewsDimens.EmptyIconSize),
            )
        },
        title = stringResource(R.string.error_title),
        description = errorMessage(error),
        onRetry = onRetry,
    )
}

@Composable
private fun StatusState(
    testTag: String,
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(testTag)
            .padding(NewsDimens.SpaceXl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(Modifier.height(NewsDimens.SpaceLg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(NewsDimens.SpaceSm))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(NewsDimens.SpaceXl))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}
