package com.chin.minddump.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.rememberAsyncImagePainter
import com.jvziyaoyao.scale.image.pager.ImagePager
import com.jvziyaoyao.scale.zoomable.pager.rememberZoomablePagerState

/**
 * Full-screen zoomable image preview. Ported from LastChat; the save-to-downloads
 * action was dropped (MindDump has no such util) — dismiss by tap-outside / back.
 */
@Composable
fun ImagePreviewDialog(
    images: List<String>,
    onDismissRequest: () -> Unit,
) {
    val state = rememberZoomablePagerState { images.size }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            ImagePager(
                modifier = Modifier.fillMaxSize(),
                pagerState = state,
                imageLoader = { index ->
                    val painter = rememberAsyncImagePainter(images[index])
                    Pair(painter, painter.intrinsicSize)
                },
            )
        }
    }
}
