package com.chin.minddump.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

// ──────────────────────────────────────────────
// 2.1 Press Scale Animation
// ──────────────────────────────────────────────

/**
 * Modifier that scales the composable down on press and springs back on release.
 * @param pressedScale target scale when pressed (default 0.97)
 * @param showElevation whether to animate shadow elevation on press
 */
fun Modifier.animatePressScale(
    pressedScale: Float = 0.92f,
    showElevation: Boolean = true,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "press_scale",
    )
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 4f else 1f,
        animationSpec = tween(durationMillis = LocalAnimationDuration.current.short),
        label = "press_elevation",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        if (showElevation) {
            shadowElevation = elevation * density
        }
    }
}

// ──────────────────────────────────────────────
// 2.2 Shimmer Effect
// ──────────────────────────────────────────────

/**
 * Modifier that adds a shimmer sweep animation over the content.
 * Uses a linear gradient that sweeps left to right on a 1500ms cycle.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val duration = LocalAnimationDuration.current.medium
    val shimmerDuration = if (duration == 0) 0 else 1500

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = shimmerDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_progress",
    )

    val shimmerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val shimmerColorMid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)

    drawWithContent {
        drawContent()
        val width = size.width
        val gradientWidth = width * 0.6f
        val offset = (progress * (width + gradientWidth)) - gradientWidth
        val brush = Brush.horizontalGradient(
            colors = listOf(
                shimmerColor,
                shimmerColorMid,
                shimmerColor,
            ),
            startX = offset,
            endX = offset + gradientWidth,
        )
        drawRect(brush = brush)
    }
}

// ──────────────────────────────────────────────
// 2.3 Pulse Animation
// ──────────────────────────────────────────────

/**
 * A composable that applies a continuous pulse animation (scale oscillation)
 * to its content. Used for recording indicators.
 *
 * @param minScale scale at rest (default 1.0)
 * @param maxScale scale at peak (default 1.3)
 * @param durationMs pulse cycle duration (default 800ms)
 * @param content the composable to pulse
 */
@Composable
fun PulseAnimation(
    minScale: Float = 1f,
    maxScale: Float = 1.3f,
    durationMs: Int = 800,
    content: @Composable () -> Unit,
) {
    val duration = LocalAnimationDuration.current.medium
    val effectiveDuration = if (duration == 0) 0 else durationMs

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = effectiveDuration, easing = Easing { it }),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    Box(contentAlignment = Alignment.Center) {
        content()
    }
}

// ──────────────────────────────────────────────
// 2.4 Staggered Entrance
// ──────────────────────────────────────────────

/**
 * Wraps [AnimatedVisibility] with a stagger delay based on [index].
 * Items beyond [maxStagger] (default 10) appear instantly.
 *
 * @param index item index in the list
 * @param visible whether the item should be visible
 * @param delayPerItemMs delay between each item (default 30ms)
 * @param maxStagger maximum number of items to stagger (default 10)
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    visible: Boolean = true,
    delayPerItemMs: Int = 30,
    maxStagger: Int = 10,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val animDuration = LocalAnimationDuration.current
    val curve = LocalMotionCurve.current.decelerate
    val effectiveIndex = if (index < maxStagger) index else 0
    val delayMs = effectiveIndex * delayPerItemMs

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { 40 },
            animationSpec = tween(
                durationMillis = animDuration.medium,
                delayMillis = delayMs,
                easing = curve,
            ),
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = animDuration.medium,
                delayMillis = delayMs,
                easing = curve,
            ),
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = animDuration.short),
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = animDuration.short),
        ),
        content = content,
    )
}

// ──────────────────────────────────────────────
// 2.5 Swipe to Dismiss
// ──────────────────────────────────────────────

/**
 * A composable that enables swipe-to-dismiss with a colored background.
 * Uses Material3 SwipeToDismissBox.
 *
 * @param onDismiss callback when the item is swiped past threshold
 * @param backgroundContent the background revealed during swipe
 * @param content the foreground content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDismissItem(
    onDismiss: () -> Unit,
    backgroundContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDismiss()
            }
            true
        },
    )

    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = { backgroundContent() },
    ) {
        content()
    }
}

// ──────────────────────────────────────────────
// 2.6 No-Ripple Clickable
// ──────────────────────────────────────────────

/**
 * Clickable modifier without Material ripple indication.
 * Combines press scale animation with optional haptic feedback
 * for a cleaner, more premium touch experience.
 *
 * @param onClick callback when clicked
 * @param onPress optional haptic to perform on press (before click)
 * @param role accessibility role
 */
fun Modifier.noRippleClickable(
    onClick: () -> Unit,
    onPress: (() -> Unit)? = null,
    role: Role? = null,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Trigger haptic on press
    if (isPressed) {
        onPress?.invoke()
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "no_ripple_scale",
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.clickable(
            interactionSource = interactionSource,
            indication = null, // No ripple
            role = role,
            onClick = onClick,
        )
}
