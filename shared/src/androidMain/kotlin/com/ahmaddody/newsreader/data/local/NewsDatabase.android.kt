package com.ahmaddody.newsreader.data.local

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

private const val DatabaseName = "inosoft-news.db"

internal fun createDatabase(context: Context): NewsDatabase {
    val appContext = context.applicationContext
    val databaseFile = appContext.getDatabasePath(DatabaseName)
    val builder: RoomDatabase.Builder<NewsDatabase> = Room.databaseBuilder(
        context = appContext,
        name = databaseFile.absolutePath,
    )

    return builder
        .addMigrations(*newsDatabaseMigrations)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

