package com.ahmaddody.newsreader.observability.logging

import okio.FileSystem
import okio.Path
import okio.buffer

/**
 * Rotating on-device log file.
 *
 * Three limits, because a log that can grow without bound is a defect of its own on a field device:
 * files roll at [maxFileBytes], at most [maxFiles] are retained, and anything older than
 * [retentionMillis] is deleted on startup and on every roll.
 *
 * Files live in app-specific storage (supplied by the platform as [directory]), so no runtime
 * permission is required and everything is removed when the app is uninstalled.
 */
class RotatingLogFileWriter(
    private val fileSystem: FileSystem,
    private val directory: Path,
    private val maxFileBytes: Long = DefaultMaxFileBytes,
    private val maxFiles: Int = DefaultMaxFiles,
    private val retentionMillis: Long = DefaultRetentionMillis,
) : AppLogger {
    private var currentFile: Path? = null

    init {
        runCatching {
            fileSystem.createDirectories(directory)
            sweep()
        }
    }

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val entry = LogEntry(
            timestampMillis = currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
        )
        // Logging must never be able to take the app down, whatever the storage state.
        runCatching {
            val target = resolveTarget()
            val sink = fileSystem.appendingSink(target).buffer()
            try {
                sink.writeUtf8(entry.format())
                sink.writeUtf8("\n")
            } finally {
                sink.close()
            }
        }
    }

    /** Newest first. Used by the exporter and the diagnostics screen. */
    fun logFiles(): List<Path> = runCatching {
        fileSystem.list(directory)
            .filter { path -> path.name.startsWith(FilePrefix) && path.name.endsWith(FileSuffix) }
            .sortedByDescending { path -> path.name }
    }.getOrDefault(emptyList())

    private fun resolveTarget(): Path {
        val existing = currentFile
        if (existing != null && sizeOf(existing) < maxFileBytes) return existing

        if (existing != null) sweep()
        val created = directory / "$FilePrefix${currentTimeMillis()}$FileSuffix"
        currentFile = created
        return created
    }

    private fun sizeOf(path: Path): Long =
        fileSystem.metadataOrNull(path)?.size ?: 0L

    /** Applies both the time-boxed retention and the fixed maximum retained set. */
    private fun sweep() {
        val files = logFiles()
        val cutoff = currentTimeMillis() - retentionMillis

        files.filter { path -> timestampOf(path) < cutoff }
            .forEach { path -> runCatching { fileSystem.delete(path) } }

        logFiles().drop(maxFiles)
            .forEach { path -> runCatching { fileSystem.delete(path) } }
    }

    /**
     * The timestamp is carried in the file name rather than read from file metadata, which is not
     * reliably available across the platforms this runs on.
     */
    private fun timestampOf(path: Path): Long =
        path.name.removePrefix(FilePrefix).removeSuffix(FileSuffix).toLongOrNull() ?: Long.MAX_VALUE

    companion object {
        const val FilePrefix = "session-"
        const val FileSuffix = ".log"
        const val DefaultMaxFileBytes = 256L * 1024
        const val DefaultMaxFiles = 5
        const val DefaultRetentionMillis = 7L * 24 * 60 * 60 * 1000
    }
}
