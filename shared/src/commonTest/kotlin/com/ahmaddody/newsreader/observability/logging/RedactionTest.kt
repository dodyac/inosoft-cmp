package com.ahmaddody.newsreader.observability.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Redaction is a contractual requirement, not a style preference, so it is tested like one: these
 * assert that a secret cannot reach a sink even when a call site passes it in directly.
 */
class RedactionTest {
    @Test
    fun `masks bearer tokens`() {
        val redacted = Redaction.redact("Authorization: Bearer abc.def-123_XYZ")
        assertFalse(redacted.contains("abc.def-123_XYZ"))
        assertTrue(redacted.contains("Bearer"))
    }

    @Test
    fun `masks api key assignments regardless of syntax`() {
        assertFalse(Redaction.redact("X-Api-Key: 9f8e7d6c5b").contains("9f8e7d6c5b"))
        assertFalse(Redaction.redact("""{"api_key":"9f8e7d6c5b"}""").contains("9f8e7d6c5b"))
        assertFalse(Redaction.redact("?apiKey=9f8e7d6c5b&q=news").contains("9f8e7d6c5b"))
    }

    @Test
    fun `masks json web tokens anywhere in a payload`() {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.signature"
        assertFalse(Redaction.redact("token was $jwt today").contains("eyJ"))
    }

    @Test
    fun `masks emails and long digit runs`() {
        assertFalse(Redaction.redact("reader@example.com asked").contains("@example.com"))
        assertFalse(Redaction.redact("account 628123456789 failed").contains("628123456789"))
    }

    @Test
    fun `leaves ordinary diagnostic text alone`() {
        val message = "refresh(TopHeadlines) failed: rate_limited, usable cache=true"
        assertEquals(message, Redaction.redact(message))
    }

    @Test
    fun `wrapper redacts before the delegate ever sees the message`() {
        val recorder = RecordingLogger()
        val logger = RedactingAppLogger(recorder)

        logger.log(LogLevel.Info, LogTags.Network, "sent X-Api-Key: 9f8e7d6c5b")

        assertEquals(1, recorder.entries.size)
        assertFalse(recorder.entries.single().contains("9f8e7d6c5b"))
    }

    @Test
    fun `wrapper redacts throwable messages and does not forward the throwable`() {
        val recorder = RecordingLogger()
        val logger = RedactingAppLogger(recorder)

        logger.log(
            LogLevel.Error,
            LogTags.Network,
            "request failed",
            IllegalStateException("bad token Bearer abc.def-123"),
        )

        val recorded = recorder.entries.single()
        assertFalse(recorded.contains("abc.def-123"))
        assertTrue(recorded.contains("IllegalStateException"))
        assertTrue(recorder.throwables.all { throwable -> throwable == null })
    }
}

private class RecordingLogger : AppLogger {
    val entries = mutableListOf<String>()
    val throwables = mutableListOf<Throwable?>()

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        entries += message
        throwables += throwable
    }
}
