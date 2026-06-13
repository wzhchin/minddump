package com.chin.minddump.ui.theme

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Premium haptic feedback patterns for a tactile, immersive experience.
 * Each pattern is designed to feel distinct and purposeful.
 */
sealed class HapticPattern {
    /** Quick, light tap — for button clicks, selections */
    data object Tick : HapticPattern()

    /** Soft pop — for toggles, confirmations */
    data object Pop : HapticPattern()

    /** Heavy thud — for drops, important actions */
    data object Thud : HapticPattern()

    /** Rising buildup — for long press detection */
    data object Buildup : HapticPattern()

    /** Success confirmation — double tap */
    data object Success : HapticPattern()

    /** Error feedback — sharp, distinct */
    data object Error : HapticPattern()

    /** Drag start — subtle lift feel */
    data object DragStart : HapticPattern()

    /** Drag end — settling feel */
    data object DragEnd : HapticPattern()

    /** Send message — whoosh feel */
    data object Send : HapticPattern()

    /** Scroll edge — subtle resistance */
    data object ScrollEdge : HapticPattern()

    /** Selection change — light double tick */
    data object Selection : HapticPattern()

    /** Cancel/dismiss — quick fade */
    data object Cancel : HapticPattern()
}

/**
 * Performs premium haptic feedback patterns.
 * Falls back gracefully on devices without vibrator hardware or on older Android versions.
 */
class PremiumHaptics(
    private val hapticFeedback: HapticFeedback,
    private val vibrator: Vibrator?,
) {
    fun perform(pattern: HapticPattern) {
        try {
            if (vibrator?.hasVibrator() != true) {
                fallback(pattern)
                return
            }
            when (pattern) {
                HapticPattern.Tick -> vibratePredefined(VibrationEffect.EFFECT_TICK) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                HapticPattern.Pop -> vibratePredefined(VibrationEffect.EFFECT_CLICK) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                HapticPattern.Thud -> vibratePredefined(VibrationEffect.EFFECT_HEAVY_CLICK) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                HapticPattern.Buildup -> vibrateWaveform(
                    timings = longArrayOf(0, 20, 30, 40),
                    amplitudes = intArrayOf(0, 80, 150, 255),
                ) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                HapticPattern.Success -> vibratePredefined(VibrationEffect.EFFECT_DOUBLE_CLICK) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                HapticPattern.Error -> vibrateWaveform(
                    timings = longArrayOf(0, 30, 50, 30),
                    amplitudes = intArrayOf(0, 200, 0, 200),
                ) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                HapticPattern.DragStart -> vibratePredefined(VibrationEffect.EFFECT_TICK) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                HapticPattern.DragEnd -> vibratePredefined(VibrationEffect.EFFECT_HEAVY_CLICK) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                HapticPattern.Send -> vibrateOneShot(40, 220) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                HapticPattern.ScrollEdge -> vibrateOneShot(15, 60) {
                    // Too subtle for fallback; skip silently
                }

                HapticPattern.Selection -> vibratePredefined(VibrationEffect.EFFECT_DOUBLE_CLICK) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                HapticPattern.Cancel -> vibrateOneShot(25, 100) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        } catch (e: Exception) {
            Log.w("PremiumHaptics", "Vibration failed", e)
        }
    }

    private fun vibratePredefined(effectId: Int, fallback: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(effectId))
        } else {
            fallback()
        }
    }

    private fun vibrateWaveform(
        timings: LongArray,
        amplitudes: IntArray,
        fallback: () -> Unit,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            fallback()
        }
    }

    private fun vibrateOneShot(durationMs: Long, amplitude: Int, fallback: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            fallback()
        }
    }

    private fun fallback(pattern: HapticPattern) {
        when (pattern) {
            HapticPattern.Tick,
            HapticPattern.DragStart,
            HapticPattern.ScrollEdge,
            HapticPattern.Selection,
            HapticPattern.Cancel,
            -> hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)

            else -> hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

/**
 * Remember a [PremiumHaptics] instance bound to the device's vibrator service.
 */
@Composable
fun rememberPremiumHaptics(): PremiumHaptics {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val vibrator = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(VibratorManager::class.java)
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
        } catch (e: Exception) {
            Log.w("PremiumHaptics", "Failed to get vibrator service", e)
            null
        }
    }

    return remember { PremiumHaptics(hapticFeedback, vibrator) }
}
