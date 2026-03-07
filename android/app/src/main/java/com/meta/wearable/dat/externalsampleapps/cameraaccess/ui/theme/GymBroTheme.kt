package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.AppColor

data class GlassStyle(
    val blurRadius: Dp = 20.dp,
    val backgroundAlpha: Float = 0.10f,
    val borderWidth: Dp = 1.dp,
    val borderAlpha: Float = 0.20f,
    val cornerRadius: Dp = 24.dp,
    val highlightAlpha: Float = 0.12f,
)

val LocalGlassStyle = staticCompositionLocalOf { GlassStyle() }

private val GymBroColorScheme = darkColorScheme(
    primary = AppColor.Accent,
    onPrimary = AppColor.TextOnAccent,
    primaryContainer = AppColor.Primary,
    onPrimaryContainer = AppColor.TextPrimary,
    secondary = AppColor.Primary,
    onSecondary = AppColor.TextPrimary,
    background = AppColor.Background,
    onBackground = AppColor.TextPrimary,
    surface = AppColor.Surface,
    onSurface = AppColor.TextPrimary,
    surfaceVariant = AppColor.CardBackground,
    onSurfaceVariant = AppColor.TextSecondary,
    error = AppColor.Error,
    onError = AppColor.TextPrimary,
    outline = AppColor.CardBorder
)

val GymBroTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun GymBroTheme(
    glassStyle: GlassStyle = GlassStyle(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalGlassStyle provides glassStyle) {
        MaterialTheme(
            colorScheme = GymBroColorScheme,
            typography = GymBroTypography,
            content = content
        )
    }
}
