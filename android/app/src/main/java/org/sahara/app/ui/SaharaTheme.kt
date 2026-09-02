package org.sahara.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =============================================================================
// SAHARA / SHEGUARD COLOR SYSTEM
// =============================================================================

object SaharaColors {
    // Primary Brand
    val PinkPrimary = Color(0xFFF34B86)
    val SoftPink = Color(0xFFFDE7EF)
    val VerySoftPink = Color(0xFFFFF4F8)
    val PinkDeep = Color(0xFFD6336C)

    // Secondary Accent
    val SoftBlue = Color(0xFF6F9BEF)
    val VerySoftBlue = Color(0xFFEEF3FF)
    val BlueMuted = Color(0xFF4C7AD9)

    // Supporting Accent
    val SoftYellow = Color(0xFFF7C94B)
    val VerySoftYellow = Color(0xFFFFF7DF)
    val YellowDark = Color(0xFFC7951B)

    // Backgrounds & Surfaces
    val WarmWhite = Color(0xFFFFFCFA)
    val PureWhite = Color(0xFFFFFFFF)
    val SurfaceCard = Color(0xFFFFFFFF)
    val SurfaceSubtle = Color(0xFFF9F7F5)
    val BorderSubtle = Color(0xFFF0EBE7)

    // Typography
    val TextPrimary = Color(0xFF1B1D2A) // Deep navy / charcoal
    val TextSecondary = Color(0xFF6C727F) // Muted slate
    val TextDisabled = Color(0xFFA0A5B1) // Light grey
    val TextInverse = Color(0xFFFFFFFF)

    // Semantic States
    val SuccessGreen = Color(0xFF4E9F76) // Muted sage green
    val SuccessGreenBg = Color(0xFFEBF6F0)
    val DangerCoral = Color(0xFFE05353) // Warm coral (used ONLY when necessary)
    val DangerCoralBg = Color(0xFFFDF0F0)

    // Soft Shadow Tint
    val ShadowTint = Color(0x0F1B1D2A)
}

// =============================================================================
// TYPOGRAPHY HIERARCHY
// =============================================================================

val SaharaTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        color = SaharaColors.TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = SaharaColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = SaharaColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = SaharaColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = SaharaColors.TextSecondary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = SaharaColors.TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = SaharaColors.TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = SaharaColors.TextPrimary
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = SaharaColors.TextSecondary
    )
)

// =============================================================================
// SHAPES (20–28px rounded cards)
// =============================================================================

val SaharaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val SaharaColorScheme = lightColorScheme(
    primary = SaharaColors.PinkPrimary,
    onPrimary = SaharaColors.TextInverse,
    primaryContainer = SaharaColors.SoftPink,
    onPrimaryContainer = SaharaColors.PinkDeep,
    secondary = SaharaColors.SoftBlue,
    onSecondary = SaharaColors.TextInverse,
    secondaryContainer = SaharaColors.VerySoftBlue,
    onSecondaryContainer = SaharaColors.BlueMuted,
    tertiary = SaharaColors.SoftYellow,
    onTertiary = SaharaColors.TextPrimary,
    background = SaharaColors.WarmWhite,
    onBackground = SaharaColors.TextPrimary,
    surface = SaharaColors.PureWhite,
    onSurface = SaharaColors.TextPrimary,
    surfaceVariant = SaharaColors.SurfaceSubtle,
    onSurfaceVariant = SaharaColors.TextSecondary,
    outline = SaharaColors.BorderSubtle
)

@Composable
fun SaharaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SaharaColorScheme,
        typography = SaharaTypography,
        shapes = SaharaShapes,
        content = content
    )
}
