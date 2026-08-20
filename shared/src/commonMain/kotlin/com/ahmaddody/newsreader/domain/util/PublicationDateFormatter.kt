package com.ahmaddody.newsreader.domain.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val monthNames = listOf(
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec",
)

fun formatPublicationDate(
    isoTimestamp: String,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String = runCatching {
    val dateTime = Instant.parse(isoTimestamp).toLocalDateTime(timeZone)
    val day = dateTime.day.toString().padStart(2, '0')
    val month = monthNames[dateTime.month.ordinal]
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    "$day $month ${dateTime.year}, $hour:$minute"
}.getOrElse { isoTimestamp }

