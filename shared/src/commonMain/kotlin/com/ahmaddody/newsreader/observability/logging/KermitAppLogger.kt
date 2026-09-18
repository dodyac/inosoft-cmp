package com.ahmaddody.newsreader.observability.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.platformLogWriter

/**
 * Kermit adapter. Kermit gives us the platform sinks for free — Logcat on Android, os_log on iOS —
 * which is the only reason a logging library is in the dependency list at all.
 */
class KermitAppLogger(
    minSeverity: Severity = Severity.Verbose,
) : AppLogger {
    private val logger = Logger(
        config = StaticConfig(
            minSeverity = minSeverity,
            logWriterList = listOf(platformLogWriter()),
        ),
        tag = DefaultTag,
    )

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        logger.log(level.toSeverity(), tag, throwable, message)
    }

    private fun LogLevel.toSeverity(): Severity = when (this) {
        LogLevel.Verbose -> Severity.Verbose
        LogLevel.Debug -> Severity.Debug
        LogLevel.Info -> Severity.Info
        LogLevel.Warn -> Severity.Warn
        LogLevel.Error -> Severity.Error
    }

    private companion object {
        const val DefaultTag = "NusaNews"
    }
}

/** Writes one entry to every sink. Order is irrelevant; failure of one must not stop the others. */
class CompositeAppLogger(private val delegates: List<AppLogger>) : AppLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        delegates.forEach { delegate ->
            runCatching { delegate.log(level, tag, message, throwable) }
        }
    }
}
