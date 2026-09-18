package com.ahmaddody.newsreader.observability.logging

import kotlin.time.Clock

/** One place to read wall-clock time, so every sink agrees on ordering. */
internal fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
