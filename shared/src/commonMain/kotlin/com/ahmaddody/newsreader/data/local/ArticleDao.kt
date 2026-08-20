package com.ahmaddody.newsreader.data.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ArticleDao {
    @Query("SELECT * FROM articles WHERE feed = :feed ORDER BY publishedAtEpochMillis DESC")
    fun observeAll(feed: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :articleId LIMIT 1")
    fun observeById(articleId: String): Flow<ArticleEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM articles WHERE feed = :feed LIMIT 1)")
    suspend fun hasArticles(feed: String): Boolean

    @Upsert
    suspend fun upsertAll(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles WHERE feed = :feed")
    suspend fun deleteFeed(feed: String)

    /** Replaces one feed only, so a refresh of one tab never discards the other tab's cache. */
    @Transaction
    suspend fun replaceFeed(feed: String, articles: List<ArticleEntity>) {
        deleteFeed(feed)
        if (articles.isNotEmpty()) upsertAll(articles)
    }
}
