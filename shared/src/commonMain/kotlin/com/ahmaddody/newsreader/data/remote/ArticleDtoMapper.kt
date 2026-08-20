package com.ahmaddody.newsreader.data.remote

import com.ahmaddody.newsreader.domain.model.Article
import kotlin.time.Instant

internal fun ArticleDto.toDomainOrNull(): Article? {
    val cleanTitle = title?.trim()?.takeIf { it.isNotEmpty() && it != "[Removed]" } ?: return null
    val cleanPublishedAt = publishedAt?.trim()?.takeIf { timestamp ->
        runCatching { Instant.parse(timestamp) }.isSuccess
    } ?: return null
    val cleanUrl = url.cleanNullable()
    val cleanSource = source?.name.cleanNullable()
    val stableId = cleanUrl ?: listOf(
        cleanSource?.lowercase().orEmpty(),
        cleanTitle.lowercase(),
        cleanPublishedAt,
    ).joinToString("|")

    return Article(
        id = stableId,
        title = cleanTitle,
        description = description.cleanNullable(),
        imageUrl = imageUrl.cleanNullable(),
        publishedAt = cleanPublishedAt,
        sourceName = cleanSource,
        articleUrl = cleanUrl,
    )
}

private fun String?.cleanNullable(): String? = this?.trim()?.takeIf(String::isNotEmpty)
