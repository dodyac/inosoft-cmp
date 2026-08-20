package com.ahmaddody.newsreader.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.ahmaddody.newsreader.R
import com.ahmaddody.newsreader.presentation.common.NewsDimens
import com.ahmaddody.newsreader.presentation.common.UiTags
import kotlin.math.abs

private const val MinScale = 1f
private const val MaxScale = 5f
private const val DoubleTapScale = 2.5f

/**
 * Full-screen viewer for the article image: pinch to zoom, drag to pan, double-tap to toggle.
 *
 * Panning is clamped to the scaled image bounds so the picture cannot be flung off-screen, and
 * releasing the zoom snaps back to centre — otherwise a zoomed-out image drifts away from the
 * middle of the screen and looks broken.
 */
@Composable
fun ZoomableImageViewer(
    imageUrl: String,
    contentDescription: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var scale by remember { mutableFloatStateOf(MinScale) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var viewerSize by remember { mutableStateOf(IntSize.Zero) }

        fun clampOffset(candidate: Offset, currentScale: Float): Offset {
            if (currentScale <= MinScale || viewerSize == IntSize.Zero) return Offset.Zero
            val maxX = abs(viewerSize.width * (currentScale - 1f)) / 2f
            val maxY = abs(viewerSize.height * (currentScale - 1f)) / 2f
            return Offset(
                x = candidate.x.coerceIn(-maxX, maxX),
                y = candidate.y.coerceIn(-maxY, maxY),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag(UiTags.ImageViewer)
                .onSizeChanged { viewerSize = it },
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (scale * zoom).coerceIn(MinScale, MaxScale)
                            scale = nextScale
                            offset = clampOffset(offset + pan, nextScale)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                val zoomedIn = scale > MinScale
                                scale = if (zoomedIn) MinScale else DoubleTapScale
                                offset = Offset.Zero
                            },
                        )
                    },
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(NewsDimens.SpaceSm)
                    .align(Alignment.TopStart)
                    .testTag(UiTags.ImageViewerClose),
                colors = IconButtonDefaults.iconButtonColors(
                    // A scrim keeps the icon legible over a bright photo, and matches iOS.
                    containerColor = Color.Black.copy(alpha = 0.35f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.image_viewer_close),
                )
            }
        }
    }
}
