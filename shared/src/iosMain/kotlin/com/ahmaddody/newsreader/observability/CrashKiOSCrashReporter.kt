package com.ahmaddody.newsreader.observability

import co.touchlab.crashkios.crashlytics.CrashlyticsKotlin
import co.touchlab.crashkios.crashlytics.enableCrashlytics
import co.touchlab.crashkios.crashlytics.setCrashlyticsUnhandledExceptionHook

/**
 * iOS crash reporting, in Kotlin rather than Swift, on purpose.
 *
 * An uncaught exception thrown from shared Kotlin code does not by default produce a useful
 * Crashlytics stack trace on iOS. CrashKiOS exists for exactly that: it installs an unhandled
 * exception hook that reports the Kotlin exception, and it lets breadcrumbs, custom keys and
 * handled exceptions be sent from common Kotlin. A Swift adapter could not do either.
 *
 * Symbolication still needs the Kotlin `.dSYM` uploaded for release builds — the Xcode post-build
 * script in `iosApp/project.yml` does that. Validate on a real device before trusting iOS crash
 * reports; do not assume they are actionable.
 */
internal class CrashKiOSCrashReporter : CrashReporter {
    override fun log(message: String) = CrashlyticsKotlin.logMessage(message)

    override fun setKey(key: String, value: String) = CrashlyticsKotlin.setCustomValue(key, value)

    override fun recordNonFatal(error: Throwable) = CrashlyticsKotlin.sendHandledException(error)
}

/**
 * Called from Swift after `FirebaseApp.configure()` and before Koin starts. Splitting it out keeps
 * the ordering requirement explicit: the hook must be installed while the Firebase SDK is ready.
 */
fun setupIosCrashReporting() {
    enableCrashlytics()
    setCrashlyticsUnhandledExceptionHook()
}
