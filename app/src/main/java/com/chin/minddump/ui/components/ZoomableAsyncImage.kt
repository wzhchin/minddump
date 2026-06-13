package com.chin.minddump.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.chin.minddump.ui.theme.shimmerEffect
import java.io.File

/**
 * Image that opens a full-screen zoomable preview ([ImagePreviewDialog]) on tap.
 * Ported from LastChat, retargeted to Coil 3. Loading state shows a shimmer
 * placeholder via MindDump's [shimmerEffect].
 *
 * MindDump loads from local [File]s, so a [File] overload is provided in
 * addition to the String-URI version.
 */
@Composable
fun ZoomableAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
) {
    var showImageViewer by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var loading by remember(model) { mutableStateOf(true) }

    val coilModel = ImageRequest.Builder(context)
        .data(model)
        .crossfade(false)
        .build()

    AsyncImage(
        model = coilModel,
        contentDescription = contentDescription,
        modifier = modifier
            .then(if (loading) Modifier.shimmerEffect() else Modifier)
            .clickable {
                val data = model
                val imageUri = when (data) {
                    is File -> data.absolutePath
                    is String -> data
                    else -> ""
                }
                if (imageUri.isNotBlank()) showImageViewer = true
            },
        contentScale = contentScale,
        alpha = alpha,
        alignment = alignment,
        onLoading = { loading = true },
        onSuccess = { loading = false },
        onError = { loading = false },
    )

    if (showImageViewer) {
        val imageUri = when (model) {
            is File -> model.absolutePath
            is String -> model
            else -> ""
        }
        ImagePreviewDialog(
            images = listOf(imageUri),
            onDismissRequest = { showImageViewer = false },
        )
    }
}
