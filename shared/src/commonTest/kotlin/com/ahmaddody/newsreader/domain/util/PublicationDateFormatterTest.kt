package com.ahmaddody.newsreader.domain.util

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class PublicationDateFormatterTest {
    @Test
    fun formats_valid_iso_timestamp_in_requested_timezone() {
        assertEquals(
            expected = "14 Aug 2026, 03:05",
            actual = formatPublicationDate(
                isoTimestamp = "2026-08-14T03:05:00Z",
                timeZone = TimeZone.UTC,
            ),
        )
    }

    @Test
    fun preserves_unknown_timestamp_instead_of_crashing() {
        assertEquals("unknown", formatPublicationDate("unknown", TimeZone.UTC))
    }
}

