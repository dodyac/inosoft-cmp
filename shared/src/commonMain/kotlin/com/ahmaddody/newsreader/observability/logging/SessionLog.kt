package com.ahmaddody.newsreader.observability.logging

import com.ahmaddody.newsreader.observability.Session
import okio.FileSystem
import okio.Path

/**
 * Assembles the session logging pipeline: redaction first, then the platform sink, the bounded
 * in-memory buffer and the rotating file.
 *
 * Nothing reaches a sink except through [logger], which is the redacting wrapper.
 */
class SessionLog private constructor(
    val logger: AppLogger,
    val buffer: SessionLogBuffer,
    private val fileWriter: RotatingLogFileWriter?,
) {
    fun logFiles(): List<Path> = fileWriter?.logFiles().orEmpty()

    /** Header written into every exported bundle so a support ticket is self-describing. */
    fun bundleManifest(extra: Map<String, String> = emptyMap()): String = buildString {
        appendLine("session_id=${Session.id}")
        appendLine("exported_at=${currentTimeMillis()}")
        extra.forEach { (key, value) -> appendLine("$key=$value") }
    }

    companion object {
        /**
         * [directory] is app-specific storage supplied by the platform. When it is null — tests,
         * or a device where storage is unavailable — logging still works, just without a file.
         */
        fun create(
            fileSystem: FileSystem? = null,
            directory: Path? = null,
            bufferCapacity: Int = SessionLogBuffer.DefaultCapacity,
        ): SessionLog {
            val buffer = SessionLogBuffer(bufferCapacity)
            val fileWriter = if (fileSystem != null && directory != null) {
                RotatingLogFileWriter(fileSystem, directory)
            } else {
                null
            }

            val sinks = listOfNotNull(KermitAppLogger(), buffer, fileWriter)
            return SessionLog(
                logger = RedactingAppLogger(CompositeAppLogger(sinks)),
                buffer = buffer,
                fileWriter = fileWriter,
            )
        }
    }
}
