package com.ahmaddody.newsreader.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmaddody.newsreader.BuildConfig
import com.ahmaddody.newsreader.domain.model.NewsFeed
import com.ahmaddody.newsreader.observability.CrashKeys
import com.ahmaddody.newsreader.observability.CrashReporter
import com.ahmaddody.newsreader.observability.FeedDiagnostics
import com.ahmaddody.newsreader.observability.Session
import com.ahmaddody.newsreader.observability.SessionDiagnostics
import com.ahmaddody.newsreader.observability.logging.LogEntry
import com.ahmaddody.newsreader.observability.logging.LogExportResult
import com.ahmaddody.newsreader.observability.logging.LogExporter
import com.ahmaddody.newsreader.observability.logging.LogLevel
import com.ahmaddody.newsreader.observability.logging.SessionLog
import org.koin.compose.koinInject

/**
 * The in-app diagnostics screen the architecture calls for: build info, environment, sessionId,
 * the state of each feed's last refresh, and a log-export action.
 *
 * It exists because the Android Studio Database Inspector needs API 26+ and a USB connection, and
 * neither is available to someone holding a device in the field. Debug source set only.
 *
 * Every colour comes from the theme rather than a literal, so it reads correctly in both light and
 * dark, and the whole surface is inset with `safeDrawingPadding` because the app draws edge to edge.
 */
@Composable
internal fun DiagnosticsScreen(onClose: () -> Unit) {
    val sessionLog = koinInject<SessionLog>()
    val logExporter = koinInject<LogExporter>()
    val crash = koinInject<CrashReporter>()

    val feeds by SessionDiagnostics.feeds.collectAsState()
    val entries by sessionLog.buffer.snapshot.collectAsState()
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Opaque on purpose: this sits on top of the app, and a translucent panel over a news feed is
    // unreadable.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Diagnostics", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClose) { Text("Close") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { BuildInfoCard(sessionLog, entries.size) }
                item { FeedsCard(feeds) }
                item {
                    ActionsCard(
                        crash = crash,
                        onExport = {
                            statusMessage = "Exporting…"
                            logExporter.exportBundle { result ->
                                statusMessage = when (result) {
                                    is LogExportResult.Shared ->
                                        "Shared ${result.fileName} (${result.byteCount} bytes)"
                                    LogExportResult.NothingToExport -> "Nothing to export yet"
                                    is LogExportResult.Failed -> "Failed: ${result.reason}"
                                }
                            }
                        },
                        onClearBuffer = {
                            sessionLog.buffer.clear()
                            statusMessage = "Buffer cleared"
                        },
                        status = statusMessage,
                    )
                }
                item {
                    Text(
                        "Session log · newest first",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                items(entries.asReversed()) { entry -> LogEntryRow(entry) }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun BuildInfoCard(sessionLog: SessionLog, bufferedCount: Int) {
    SectionCard("Build & session") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DiagnosticRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            DiagnosticRow("Variant", BuildConfig.BUILD_TYPE)
            DiagnosticRow("Application id", BuildConfig.APPLICATION_ID)
            DiagnosticRow("API key configured", BuildConfig.NEWS_API_KEY.isNotBlank().toString())
            // The join key between a crash report and this session's exported log bundle.
            DiagnosticRow("Session id", Session.id, monospace = true)
            DiagnosticRow("Log files on device", sessionLog.logFiles().size.toString())
            DiagnosticRow("Buffered entries", bufferedCount.toString())
        }
    }
}

@Composable
private fun FeedsCard(feeds: Map<NewsFeed, FeedDiagnostics>) {
    SectionCard("Feeds") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // This app is read-only, so there is no pending write queue to inspect. Each feed's
            // last refresh outcome is the equivalent evidence.
            NewsFeed.entries.forEach { feed -> FeedDiagnosticsRow(feed, feeds[feed]) }
        }
    }
}

@Composable
private fun ActionsCard(
    crash: CrashReporter,
    onExport: () -> Unit,
    onClearBuffer: () -> Unit,
    status: String?,
) {
    SectionCard("Actions") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onExport) { Text("Export logs") }
                OutlinedButton(onClick = onClearBuffer) { Text("Clear buffer") }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                "Crashlytics verification",
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        // Keys and a breadcrumb first, so the console entry has the same shape a
                        // real failure would rather than a bare stack trace.
                        crash.setKey(CrashKeys.Screen, "diagnostics")
                        crash.log("non-fatal test triggered from the diagnostics screen")
                        crash.recordNonFatal(
                            IllegalStateException("Diagnostics test non-fatal, session ${Session.id}"),
                        )
                    },
                ) {
                    Text("Send non-fatal")
                }
                Button(
                    onClick = {
                        crash.setKey(CrashKeys.Screen, "diagnostics")
                        crash.log("forced crash triggered from the diagnostics screen")
                        throw RuntimeException("Diagnostics test crash, session ${Session.id}")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text("Force crash")
                }
            }
            Text(
                "A crash report uploads on the next launch, so relaunch the app afterwards.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            status?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, monospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (monospace) FontFamily.Monospace else null,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun FeedDiagnosticsRow(feed: NewsFeed, diagnostics: FeedDiagnostics?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = feed.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        val detail = when (diagnostics) {
            null -> "not refreshed this session"
            else -> buildString {
                append(diagnostics.state.name.lowercase())
                diagnostics.lastArticleCount?.let { count -> append(" · $count articles") }
                diagnostics.lastDurationMillis?.let { millis -> append(" · ${millis}ms") }
                diagnostics.lastError?.let { error -> append(" · $error") }
                if (diagnostics.servedFromCache) append(" · served from cache")
            }
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = if (diagnostics?.lastError != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = entry.level.label,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = entry.level.tint(),
        )
        Text(
            text = entry.tag,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 4.dp),
        )
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The theme declares no tertiary colour, so warnings borrow `primary` rather than falling through
 * to Material's baseline purple, which clashes with this scheme in both modes.
 */
@Composable
private fun LogLevel.tint(): Color = when (this) {
    LogLevel.Error -> MaterialTheme.colorScheme.error
    LogLevel.Warn -> MaterialTheme.colorScheme.primary
    LogLevel.Info -> MaterialTheme.colorScheme.onSurface
    LogLevel.Debug, LogLevel.Verbose -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Debug-only affordance so the screen is reachable without adding a route to the app. */
@Composable
internal fun DiagnosticsLauncher() {
    var open by remember { mutableStateOf(false) }

    if (open) {
        DiagnosticsScreen(onClose = { open = false })
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentAlignment = Alignment.BottomStart,
    ) {
        FilledTonalButton(
            onClick = { open = true },
            modifier = Modifier.padding(start = 16.dp, bottom = 16.dp),
        ) {
            Text("Diag")
        }
    }
}
