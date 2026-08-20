package com.ahmaddody.newsreader.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.ahmaddody.newsreader.domain.model.NewsFeed

@Database(
    entities = [ArticleEntity::class],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(NewsDatabaseConstructor::class)
internal abstract class NewsDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object NewsDatabaseConstructor : RoomDatabaseConstructor<NewsDatabase> {
    override fun initialize(): NewsDatabase
}

/**
 * Version 2 introduced the per-feed cache: the primary key became `(id, feed)`.
 *
 * SQLite cannot alter a primary key in place, so the table is recreated and existing rows are
 * attributed to the feed they were originally fetched from. Cached articles survive the upgrade
 * instead of being destroyed, which is the whole point of an offline-first cache.
 */
internal object MigrateArticlesToPerFeedCache : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS articles_new (
                id TEXT NOT NULL,
                feed TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                imageUrl TEXT,
                publishedAt TEXT NOT NULL,
                publishedAtEpochMillis INTEGER NOT NULL,
                sourceName TEXT,
                articleUrl TEXT,
                PRIMARY KEY(id, feed)
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            INSERT OR REPLACE INTO articles_new (
                id, feed, title, description, imageUrl,
                publishedAt, publishedAtEpochMillis, sourceName, articleUrl
            )
            SELECT id, '${NewsFeed.TopHeadlines.name}', title, description, imageUrl,
                   publishedAt, publishedAtEpochMillis, sourceName, articleUrl
            FROM articles
            """.trimIndent(),
        )
        connection.execSQL("DROP TABLE articles")
        connection.execSQL("ALTER TABLE articles_new RENAME TO articles")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS index_articles_feed_publishedAtEpochMillis " +
                "ON articles (feed, publishedAtEpochMillis)",
        )
    }
}

internal val newsDatabaseMigrations = arrayOf(MigrateArticlesToPerFeedCache)
