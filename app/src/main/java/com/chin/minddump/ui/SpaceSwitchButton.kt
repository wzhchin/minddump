package com.chin.minddump.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chin.minddump.storage.Space

@Composable
fun SpaceSwitchButton(
    currentSpace: Space,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = when (currentSpace) {
            Space.PUBLIC -> MaterialTheme.colorScheme.primaryContainer
            Space.PRIVATE -> MaterialTheme.colorScheme.errorContainer
        },
        animationSpec = tween(300),
        label = "space_color"
    )

    val contentColor by animateColorAsState(
        targetValue = when (currentSpace) {
            Space.PUBLIC -> MaterialTheme.colorScheme.onPrimaryContainer
            Space.PRIVATE -> MaterialTheme.colorScheme.onErrorContainer
        },
        animationSpec = tween(300),
        label = "space_content_color"
    )

    LargeFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        Icon(
            imageVector = when (currentSpace) {
                Space.PUBLIC -> Icons.Filled.Public
                Space.PRIVATE -> Icons.Filled.Lock
            },
            contentDescription = when (currentSpace) {
                Space.PUBLIC -> "切换到 Private"
                Space.PRIVATE -> "切换到 Public"
            },
            modifier = Modifier.size(24.dp)
        )
    }
}
