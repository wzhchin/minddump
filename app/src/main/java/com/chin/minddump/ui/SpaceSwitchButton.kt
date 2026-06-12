package com.chin.minddump.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chin.minddump.storage.Space
import com.chin.minddump.ui.theme.LocalAnimationDuration
import com.chin.minddump.R

@Composable
fun SpaceSwitchButton(
    currentSpace: Space,
    onClick: () -> Unit,
    rotationY: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val animDuration = LocalAnimationDuration.current.medium

    val containerColor by animateColorAsState(
        targetValue = when (currentSpace) {
            Space.PUBLIC -> MaterialTheme.colorScheme.primaryContainer
            Space.PRIVATE -> MaterialTheme.colorScheme.errorContainer
        },
        animationSpec = tween(animDuration),
        label = "space_color",
    )

    val contentColor by animateColorAsState(
        targetValue = when (currentSpace) {
            Space.PUBLIC -> MaterialTheme.colorScheme.onPrimaryContainer
            Space.PRIVATE -> MaterialTheme.colorScheme.onErrorContainer
        },
        animationSpec = tween(animDuration),
        label = "space_content_color",
    )

    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            this.rotationY = rotationY
            this.cameraDistance = 12f * density
        },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Icon(
            imageVector = when (currentSpace) {
                Space.PUBLIC -> Icons.Filled.Public
                Space.PRIVATE -> Icons.Filled.Lock
            },
            contentDescription = when (currentSpace) {
                Space.PUBLIC -> stringResource(R.string.switch_to_private)
                Space.PRIVATE -> stringResource(R.string.switch_to_public)
            },
            modifier = Modifier.size(18.dp),
        )
    }
}
