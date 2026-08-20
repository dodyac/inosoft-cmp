package com.ahmaddody.newsreader.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.model.NewsFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals

/**
 * Version 1 shipped a single-feed cache keyed by `id` alone. The tab switcher introduced version 2
 * with a composite `(id, feed)` key, which SQLite can only reach by recreating the table.
 *
 * This test writes a genuine version 1 database, opens it through the production builder, and
 * asserts the previously cached article survived — a destructive fallback would silently delete the
 * offline content this app exists to protect.
 */
@RunWith(AndroidJUnit4::class)
class NewsDatabaseMigrationTest {
    private val databaseName = "migration-under-test.db"
    private lateinit var context: Context
    private lateinit var databaseFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseFile = context.getDatabasePath(databaseName)
        deleteDatabaseFiles()
        databaseFile.parentFile?.mkdirs()
    }

    @After
    fun tearDown() {
        deleteDatabaseFiles()
    }

    @Test
    fun upgrading_from_version_1_keeps_cached_articles_as_top_headlines() = runTest {
        seedVersion1Database()

        val database = Room.databaseBuilder<NewsDatabase>(context, databaseFile.absolutePath)
            .addMigrations(*newsDatabaseMigrations)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

        try {
            val local = RoomArticleLocalDataSource(database.articleDao())

            val migrated = local.observeArticles(NewsFeed.TopHeadlines).first()
            assertEquals(listOf("Cached before the upgrade"), migrated.map(Article::title))
            assertEquals(emptyList(), local.observeArticles(NewsFeed.Everything).first())

            // The recreated table still accepts writes, so the new schema is fully usable.
            local.replaceAll(NewsFeed.Everything, migrated)
            assertEquals(1, local.observeArticles(NewsFeed.Everything).first().size)
            assertEquals(1, local.observeArticles(NewsFeed.TopHeadlines).first().size)
        } finally {
            database.close()
        }
    }

    private fun seedVersion1Database() {
        val driver = BundledSQLiteDriver()
        val connection: SQLiteConnection = driver.open(databaseFile.absolutePath)
        try {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `articles` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                    "`description` TEXT, `imageUrl` TEXT, `publishedAt` TEXT NOT NULL, " +
                    "`publishedAtEpochMillis` INTEGER NOT NULL, `sourceName` TEXT, " +
                    "`articleUrl` TEXT, PRIMARY KEY(`id`))",
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_articles_publishedAtEpochMillis` " +
                    "ON `articles` (`publishedAtEpochMillis`)",
            )
            connection.execSQL(
                "INSERT INTO articles VALUES ('https://example.com/legacy', " +
                    "'Cached before the upgrade', 'Still readable offline', NULL, " +
                    "'2026-08-14T03:00:00Z', 1786676400000, 'Legacy source', " +
                    "'https://example.com/legacy')",
            )
            // Room refuses to open a database it cannot identify, so reproduce its bookkeeping.
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table " +
                    "(id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            connection.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
                    "VALUES (42, '$VERSION_1_IDENTITY_HASH')",
            )
            connection.execSQL("PRAGMA user_version = 1")
        } finally {
            connection.close()
        }
    }

    private fun deleteDatabaseFiles() {
        listOf("", "-wal", "-shm").forEach { suffix ->
            File(databaseFile.absolutePath + suffix).delete()
        }
    }

    private companion object {
        /** From `shared/schemas/...NewsDatabase/1.json`; Room compares this before migrating. */
        const val VERSION_1_IDENTITY_HASH = "f6f81c7570fdcc7e13e26a2f90913ead"
    }
}
