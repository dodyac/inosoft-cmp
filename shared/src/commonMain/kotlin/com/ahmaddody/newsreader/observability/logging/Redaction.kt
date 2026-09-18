package com.ahmaddody.newsreader.observability.logging

/**
 * Redaction is applied here, at the wrapper, and never at call sites.
 *
 * Google's terms prohibit sending personally identifiable information to Analytics, and data
 * exposing PII is subject to deletion without notice. Policy without a mechanism is not
 * enforcement, so every message and every throwable message passes through [redact] before it can
 * reach any sink — the console, the ring buffer, the on-device file or an exported bundle.
 */
object Redaction {
    private const val Mask = "***"

    private val rules: List<Pair<Regex, String>> = listOf(
        // Authorization: Bearer <token>
        Regex("""(?i)(bearer\s+)[A-Za-z0-9._\-]+""") to "$1$Mask",
        // JSON web tokens anywhere in a payload.
        Regex("""eyJ[A-Za-z0-9._\-]{10,}""") to Mask,
        // key=value / "key": "value" for anything credential-shaped.
        Regex(
            """(?i)\b(x-api-key|api[_-]?key|apikey|access[_-]?token|refresh[_-]?token|token|password|passwd|secret|signature)\b(\s*[:=]\s*"?)[^\s,&"}\]]+""",
        ) to "$1$2$Mask",
        // Email addresses.
        Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""") to Mask,
        // Long digit runs: phone numbers, account and document ids.
        Regex("""\b\d{9,}\b""") to Mask,
    )

    fun redact(value: String): String =
        rules.fold(value) { acc, (pattern, replacement) -> pattern.replace(acc, replacement) }
}

/**
 * The only way into the logging pipeline. [RedactingAppLogger] wraps the real logger so a call site
 * physically cannot bypass redaction.
 */
class RedactingAppLogger(private val delegate: AppLogger) : AppLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val safeMessage = Redaction.redact(message)
        val safeThrowableText = throwable?.let { error ->
            // The throwable itself is not forwarded: its message may carry a URL or a payload, and
            // a sink would print it unredacted.
            " | ${error::class.simpleName}: ${Redaction.redact(error.message.orEmpty())}"
        }.orEmpty()
        delegate.log(level, tag, safeMessage + safeThrowableText, throwable = null)
    }
}
