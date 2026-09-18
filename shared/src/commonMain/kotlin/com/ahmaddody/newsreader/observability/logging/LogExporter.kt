package com.ahmaddody.newsreader.observability.logging

/**
 * Local logs leave the device only on an explicit user or support action, never as a continuous
 * stream, and always as one compressed, size-capped bundle. The platform decides where that bundle
 * goes: app-specific storage plus a share intent on Android, Files / share sheet on iOS.
 *
 * The result is delivered by callback rather than by `suspend`, for the same reason [NewsFacade]
 * exposes callbacks: a Kotlin suspend function is not consumable from Swift, and the iOS
 * implementation of this contract is Swift.
 */
interface LogExporter {
    fun exportBundle(onResult: (LogExportResult) -> Unit)
}

sealed interface LogExportResult {
    /** The bundle was produced and handed to the platform share mechanism. */
    data class Shared(val fileName: String, val byteCount: Long) : LogExportResult

    data object NothingToExport : LogExportResult

    data class Failed(val reason: String) : LogExportResult
}

object NoOpLogExporter : LogExporter {
    override fun exportBundle(onResult: (LogExportResult) -> Unit) {
        onResult(LogExportResult.NothingToExport)
    }
}

/** Bundle limits, shared by both platform implementations so the two behave the same. */
object LogExportLimits {
    const val MaxBundleBytes: Long = 2L * 1024 * 1024
}
