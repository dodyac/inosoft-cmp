package com.ahmaddody.newsreader.observability.logging

import com.ahmaddody.newsreader.observability.Session
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionLogBufferTest {
    @Test
    fun `keeps only the most recent entries`() {
        val buffer = SessionLogBuffer(capacity = 3)

        repeat(5) { index -> buffer.log(LogLevel.Info, LogTags.Sync, "entry $index") }

        val messages = buffer.snapshot.value.map(LogEntry::message)
        assertEquals(listOf("entry 2", "entry 3", "entry 4"), messages)
    }

    @Test
    fun `every entry carries the session id so a bundle can be joined to a crash report`() {
        val buffer = SessionLogBuffer(capacity = 2)

        buffer.log(LogLevel.Warn, LogTags.Network, "timeout")

        val entry = buffer.snapshot.value.single()
        assertEquals(Session.id, entry.sessionId)
        assertTrue(entry.format().contains(Session.id))
    }

    @Test
    fun `clear empties the buffer`() {
        val buffer = SessionLogBuffer(capacity = 4)
        buffer.log(LogLevel.Debug, LogTags.Domain, "something")

        buffer.clear()

        assertTrue(buffer.snapshot.value.isEmpty())
    }
}
