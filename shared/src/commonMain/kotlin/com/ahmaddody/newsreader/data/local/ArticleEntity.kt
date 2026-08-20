package com.ahmaddody.newsreader.data.local

import androidx.room3.Entity
import androidx.room3.Index
import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.model.NewsFeed
import kotlin.time.Instant

/**
 * One cached article per feed. The same story can legitimately appear in both NewsAPI collections,
 * so the primary key is composite: a row belongs to exactly one [feed].
 */
@Entity(
    tableName = "articles",
    primaryKeys = ["id", "feed"],
    indices = [Index(value = ["feed", "publishedAtEpochMillis"])],
)
internal data class ArticleEntity(
    val id: String,
    val feed: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val publishedAt: String,
    val publishedAtEpochMillis: Long,
    val sourceName: String?,
    val articleUrl: String?,
)

internal fun ArticleEntity.toDomain() = Article(
    id = id,
    title = title,
    description = description,
    imageUrl = imageUrl,
    publishedAt = publishedAt,
    sourceName = sourceName,
    articleUrl = articleUrl,
)

internal fun Article.toEntity(feed: NewsFeed) = ArticleEntity(
    id = id,
    feed = feed.name,
    title = title,
    description = description,
    imageUrl = imageUrl,
    publishedAt = publishedAt,
    publishedAtEpochMillis = Instant.parse(publishedAt).toEpochMilliseconds(),
    sourceName = sourceName,
    articleUrl = articleUrl,
)
