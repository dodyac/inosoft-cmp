package com.ahmaddody.newsreader.observability.logging

import com.ahmaddody.newsreader.observability.Session

/**
 * The shared logging abstraction. Kermit is the implementation, but no call site names it, so the
 * library can be swapped and — more importantly — every entry is forced through one redacting
 * wrapper instead of relying on discipline at call sites.
 */
interface AppLogger {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
}

enum class LogLevel(val label: String) {
    Verbose("V"),
    Debug("D"),
    Info("I"),
    Warn("W"),
    Error("E"),
}

/**
 * One line of session evidence. [sessionId] is on every entry so an exported bundle can be joined
 * to the Crashlytics report of the same session.
 */
data class LogEntry(
    val timestampMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val sessionId: String = Session.id,
) {
    fun format(): String = "$timestampMillis ${level.label}/$tag [$sessionId] $message"
}

fun AppLogger.v(tag: String, message: String) = log(LogLevel.Verbose, tag, message)
fun AppLogger.d(tag: String, message: String) = log(LogLevel.Debug, tag, message)
fun AppLogger.i(tag: String, message: String) = log(LogLevel.Info, tag, message)
fun AppLogger.w(tag: String, message: String, throwable: Throwable? = null) =
    log(LogLevel.Warn, tag, message, throwable)

fun AppLogger.e(tag: String, message: String, throwable: Throwable? = null) =
    log(LogLevel.Error, tag, message, throwable)

/** Used in tests and wherever logging is not wired yet. */
object NoOpAppLogger : AppLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
}

/** Tags are a fixed set so a session log can be filtered without guessing at strings. */
object LogTags {
    const val Lifecycle = "Lifecycle"
    const val Navigation = "Navigation"
    const val Network = "Network"
    const val Sync = "Sync"
    const val Database = "Database"
    const val Domain = "Domain"
}
