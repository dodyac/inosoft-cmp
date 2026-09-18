package com.ahmaddody.newsreader.observability.logging

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Android log export: app-specific storage plus a share intent.
 *
 * Nothing is uploaded and nothing is streamed. One compressed, size-capped bundle is produced on an
 * explicit action and handed to the system share sheet, so the user decides where it goes.
 */
internal class AndroidLogExporter(
    private val context: Context,
    private val sessionLog: SessionLog,
    private val appVersion: String,
) : LogExporter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun exportBundle(onResult: (LogExportResult) -> Unit) {
        scope.launch { onResult(buildAndShare()) }
    }

    private fun buildAndShare(): LogExportResult {
        val files = sessionLog.logFiles().map { path -> File(path.toString()) }.filter(File::exists)
        val buffered = sessionLog.buffer.snapshot.value

        if (files.isEmpty() && buffered.isEmpty()) return LogExportResult.NothingToExport

        return runCatching {
            val exportDir = File(context.filesDir, ExportDirectory).apply {
                // Only the newest bundle is kept; old exports are not evidence, they are clutter.
                deleteRecursively()
                mkdirs()
            }
            val bundle = File(exportDir, "nusa-news-logs-${System.currentTimeMillis()}.zip")

            ZipOutputStream(bundle.outputStream().buffered()).use { zip ->
                zip.putEntry("manifest.txt", sessionLog.bundleManifest(manifestExtras()).toByteArray())

                // The in-memory ring buffer is included because the newest entries may not have
                // been flushed to a file yet, and those are usually the interesting ones.
                zip.putEntry(
                    "buffer.log",
                    buffered.joinToString("\n") { entry -> entry.format() }.toByteArray(),
                )

                var remaining = LogExportLimits.MaxBundleBytes
                files.forEach { file ->
                    if (remaining <= 0) return@forEach
                    val bytes = file.readBytes().let { content ->
                        if (content.size <= remaining) content else content.copyOf(remaining.toInt())
                    }
                    remaining -= bytes.size
                    zip.putEntry(file.name, bytes)
                }
            }

            share(bundle)
            LogExportResult.Shared(fileName = bundle.name, byteCount = bundle.length())
        }.getOrElse { error ->
            LogExportResult.Failed(error.message ?: error::class.simpleName.orEmpty())
        }
    }

    private fun manifestExtras() = mapOf(
        "app_version" to appVersion,
        "android_sdk" to android.os.Build.VERSION.SDK_INT.toString(),
        "device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
    )

    private fun share(bundle: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.logs", bundle)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Nusa News session logs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share session logs")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun ZipOutputStream.putEntry(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    private companion object {
        const val ExportDirectory = "log-exports"
    }
}
