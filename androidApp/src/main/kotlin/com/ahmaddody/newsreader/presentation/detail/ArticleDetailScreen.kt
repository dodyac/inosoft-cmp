package com.ahmaddody.newsreader.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ahmaddody.newsreader.R
import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.util.formatPublicationDate
import com.ahmaddody.newsreader.presentation.common.NewsDimens
import com.ahmaddody.newsreader.presentation.common.UiTags
import com.ahmaddody.newsreader.presentation.common.errorMessage

@Composable
fun ArticleDetailRoute(
    viewModel: ArticleDetailViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ArticleDetailScreen(uiState = uiState, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    uiState: ArticleDetailUiState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(UiTags.ArticleDetail),
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.article != null -> ArticleDetail(uiState.article)
                uiState.error != null -> DetailError(errorMessage(uiState.error))
            }
        }
    }
}

@Composable
private fun ArticleDetail(article: Article) {
    var showImageViewer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = NewsDimens.SpaceLg,
                end = NewsDimens.SpaceLg,
                bottom = NewsDimens.SpaceXxl,
            ),
    ) {
        DetailImage(article, onClick = { showImageViewer = true })
        Spacer(Modifier.height(NewsDimens.SpaceXl))
        Text(
            text = article.sourceName ?: stringResource(R.string.source_unknown),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(NewsDimens.SpaceSm))
        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(NewsDimens.SpaceMd))
        Text(
            text = formatPublicationDate(article.publishedAt),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(NewsDimens.SpaceXl))
        Text(
            text = article.description ?: stringResource(R.string.description_unavailable),
            style = MaterialTheme.typography.bodyLarge,
        )
    }

    val imageUrl = article.imageUrl
    if (showImageViewer && imageUrl != null) {
        ZoomableImageViewer(
            imageUrl = imageUrl,
            contentDescription = stringResource(
                R.string.article_image_description,
                article.title,
            ),
            onDismiss = { showImageViewer = false },
        )
    }
}

@Composable
private fun DetailImage(
    article: Article,
    onClick: () -> Unit,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(NewsDimens.DetailImageHeight)
        .clip(RoundedCornerShape(NewsDimens.CardRadius))

    if (article.imageUrl == null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ImageNotSupported,
                contentDescription = stringResource(R.string.article_image_unavailable),
                modifier = Modifier.size(NewsDimens.EmptyIconSize),
            )
        }
    } else {
        val openLabel = stringResource(R.string.image_viewer_open)
        AsyncImage(
            model = article.imageUrl,
            contentDescription = stringResource(R.string.article_image_description, article.title),
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag(UiTags.DetailImage)
                .clickable(onClick = onClick)
                .semantics { onClick(label = openLabel, action = null) },
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun DetailError(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(NewsDimens.SpaceXl),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Newspaper,
            contentDescription = null,
            modifier = Modifier.size(NewsDimens.EmptyIconSize),
        )
        Spacer(Modifier.height(NewsDimens.SpaceLg))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
