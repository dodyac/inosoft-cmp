package com.ahmaddody.newsreader.observability

import kotlin.random.Random

/**
 * Generated in `:shared` at application start and held for the lifetime of the process. It is set
 * as a Crashlytics custom key so a crash report can be joined to the logs of the session that
 * produced it. It is not a user identifier and contains no PII.
 */
object Session {
    val id: String = buildString {
        repeat(4) { append(Random.nextInt(0, 0x10000).toString(16).padStart(4, '0')) }
    }
}
