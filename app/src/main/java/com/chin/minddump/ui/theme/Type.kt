@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.chin.minddump.ui.theme

import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chin.minddump.R

/**
 * M3 Expressive Typography using Google Sans Flex variable font.
 *
 * Key axes:
 * - wght (weight): 100–1000
 * - wdth (width): 75–125 (wider for display text)
 * - ROND (roundness): 0–100 (100 = fully rounded for M3 Expressive)
 * - GRAD (grade): -50 to 150
 */

/**
 * Static FontFamily with ROND=100 (fully rounded) and wider display text.
 * Used for the default app typography.
 */
val GoogleSansFlexExpressive: FontFamily = createGoogleSansFlex(
    roundness = 100f,
    wideForExpressive = true,
)

/**
 * Creates a Google Sans Flex FontFamily with specified variation settings.
 */
private fun createGoogleSansFlex(
    roundness: Float,
    wideForExpressive: Boolean = false,
): FontFamily {
    val displayWidth = if (wideForExpressive) 110f else 100f
    val normalWidth = 100f

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        FontFamily(
            Font(
                R.font.google_sans_flex,
                weight = FontWeight.Light,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(300),
                    FontVariation.width(normalWidth),
                    FontVariation.Setting("ROND", roundness),
                ),
            ),
            Font(
                R.font.google_sans_flex,
                weight = FontWeight.Normal,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(400),
                    FontVariation.width(normalWidth),
                    FontVariation.Setting("ROND", roundness),
                ),
            ),
            Font(
                R.font.google_sans_flex,
                weight = FontWeight.Medium,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(500),
                    FontVariation.width(normalWidth),
                    FontVariation.Setting("ROND", roundness),
                ),
            ),
            Font(
                R.font.google_sans_flex,
                weight = FontWeight.SemiBold,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(600),
                    FontVariation.width(displayWidth),
                    FontVariation.Setting("ROND", roundness),
                ),
            ),
            Font(
                R.font.google_sans_flex,
                weight = FontWeight.Bold,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(700),
                    FontVariation.width(displayWidth),
                    FontVariation.Setting("ROND", roundness),
                ),
            ),
            Font(
                R.font.google_sans_flex,
                weight = FontWeight.ExtraBold,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(800),
                    FontVariation.width(displayWidth),
                    FontVariation.Setting("ROND", roundness),
                ),
            ),
        )
    } else {
        // Fallback for API < 26 (below minSdk 29, but defensive)
        FontFamily(
            Font(R.font.google_sans_flex, FontWeight.Light),
            Font(R.font.google_sans_flex, FontWeight.Normal),
            Font(R.font.google_sans_flex, FontWeight.Medium),
            Font(R.font.google_sans_flex, FontWeight.SemiBold),
            Font(R.font.google_sans_flex, FontWeight.Bold),
            Font(R.font.google_sans_flex, FontWeight.ExtraBold),
        )
    }
}

/**
 * Creates M3 Expressive Typography with the specified font family.
 */
fun createTypography(fontFamily: FontFamily = GoogleSansFlexExpressive): Typography =
    Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
    )
