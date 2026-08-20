package com.ahmaddody.newsreader.data.local

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private const val DatabaseName = "inosoft-news.db"

@OptIn(ExperimentalForeignApi::class)
internal fun createDatabase(): NewsDatabase {
    val builder: RoomDatabase.Builder<NewsDatabase> = Room.databaseBuilder(
        name = "${documentDirectory()}/$DatabaseName",
    )

    return builder
        .addMigrations(*newsDatabaseMigrations)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )

    return requireNotNull(directory?.path) {
        "The iOS documents directory is unavailable."
    }
}
