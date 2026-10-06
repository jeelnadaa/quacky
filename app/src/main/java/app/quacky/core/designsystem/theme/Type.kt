package app.quacky.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Satoshi font family definition.
 * TODO: When Satoshi font files (satoshi_regular.otf, etc.) are added to res/font/,
 * swap this definition with Font(R.font.satoshi_*) entries.
 * Defaults gracefully to FontFamily.SansSerif to prevent build failures.
 */
val SatoshiFontFamily: FontFamily = FontFamily.SansSerif

val QuackyTypography = Typography(
    // Display: 34sp / Bold
    displayLarge = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        color = QuackyTextPrimary
    ),
    // Title: 22sp / Bold
    titleLarge = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = QuackyTextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = QuackyTextPrimary
    ),
    // Section header: 13sp / Medium, uppercase, +1sp letter-spacing
    labelSmall = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp,
        color = QuackyTextSecondary
    ),
    // Body: 15sp / Regular
    bodyLarge = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = QuackyTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = QuackyTextSecondary
    ),
    // Caption / Hints: 12sp / Regular
    labelMedium = TextStyle(
        fontFamily = SatoshiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = QuackyTextTertiary
    )
)
