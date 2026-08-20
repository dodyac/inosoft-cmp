package com.ahmaddody.newsreader.presentation.common

import com.ahmaddody.newsreader.domain.model.NewsFeed

object UiTags {
    const val ArticleList = "article_list"
    const val ArticleDetail = "article_detail"
    const val InitialLoading = "initial_loading"
    const val ErrorState = "error_state"
    const val EmptyState = "empty_state"
    const val SavedArticlesNotice = "saved_articles_notice"

    const val FeedSelector = "feed_selector"
    const val DetailImage = "detail_image"
    const val ImageViewer = "image_viewer"
    const val ImageViewerClose = "image_viewer_close"

    fun article(id: String) = "article_$id"

    fun feedChip(feed: NewsFeed) = "feed_chip_${feed.name}"
}

