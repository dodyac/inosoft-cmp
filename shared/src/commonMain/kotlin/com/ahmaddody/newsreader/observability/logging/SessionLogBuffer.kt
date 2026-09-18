package com.ahmaddody.newsreader.observability.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Bounded in-memory ring buffer holding the most recent [capacity] entries.
 *
 * Memory use is fixed regardless of session length, which matters because a field session can run
 * for hours. Backed by a [MutableStateFlow] rather than a lock so it is thread-safe on Native as
 * well as the JVM, and so the diagnostics screen can observe it directly.
 */
class SessionLogBuffer(private val capacity: Int = DefaultCapacity) : AppLogger {
    private val _snapshot = MutableStateFlow<List<LogEntry>>(emptyList())

    /** Oldest first. */
    val snapshot: StateFlow<List<LogEntry>> = _snapshot.asStateFlow()

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val entry = LogEntry(
            timestampMillis = currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
        )
        _snapshot.update { current ->
            val next = current + entry
            if (next.size > capacity) next.subList(next.size - capacity, next.size) else next
        }
    }

    fun clear() {
        _snapshot.value = emptyList()
    }

    companion object {
        const val DefaultCapacity = 500
    }
}
