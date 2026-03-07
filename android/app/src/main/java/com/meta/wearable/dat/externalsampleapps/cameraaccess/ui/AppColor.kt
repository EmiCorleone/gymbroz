package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.ui.graphics.Color

object AppColor {
    // ── Backgrounds ──
    val Background = Color(0xFF0A0A0A)
    val Surface = Color(0xFF1A1A2E)
    val SurfaceElevated = Color(0xFF16213E)
    val CardBackground = Color(0xFF141422)
    val CardBorder = Color(0xFF2A2A3A)

    // ── Accents ──
    val Primary = Color(0xFF6C63FF)         // Electric purple
    val PrimaryLight = Color(0xFF9D97FF)
    val Accent = Color(0xFF00E676)          // Neon green (energy/fitness)
    val AccentDark = Color(0xFF00C853)
    val AccentOrange = Color(0xFFFF6D00)    // Warm energy
    val AccentBlue = Color(0xFF00B4D8)      // Cool info

    // ── Gradients ──
    val GradientStart = Color(0xFF6C63FF)
    val GradientEnd = Color(0xFF00E676)
    val GradientOrange = Color(0xFFFF6D00)
    val GradientPink = Color(0xFFE91E63)

    // ── Text ──
    val TextPrimary = Color(0xFFF5F5F5)
    val TextSecondary = Color(0xFFB0B0B0)
    val TextMuted = Color(0xFF666666)
    val TextOnAccent = Color(0xFF0A0A0A)

    // ── Status ──
    val Success = Color(0xFF00E676)
    val Warning = Color(0xFFFFAB00)
    val Error = Color(0xFFFF5252)

    // ── Glass System ──
    val GlassWhite = Color(0x1AFFFFFF)           // 10% white - base glass fill
    val GlassWhiteMedium = Color(0x33FFFFFF)     // 20% white - elevated glass
    val GlassWhiteHigh = Color(0x4DFFFFFF)       // 30% white - hover/pressed glass
    val GlassBorderLight = Color(0x33FFFFFF)     // 20% white border
    val GlassBorderAccent = Color(0x4D00E676)    // 30% green border (selected)

    // ── Glass Shimmer (border gradient) ──
    val ShimmerStart = Color(0x4DFFFFFF)         // 30% white
    val ShimmerMid = Color(0x0DFFFFFF)           // 5% white
    val ShimmerEnd = Color(0x33FFFFFF)           // 20% white

    // ── Ambient Glow (animated background orbs) ──
    val AmbientPurple = Color(0x1A6C63FF)        // 10% purple
    val AmbientGreen = Color(0x1A00E676)         // 10% green
    val AmbientBlue = Color(0x1A00B4D8)          // 10% blue

    // ── Legacy (keeping for existing screens) ──
    val Green = Color(0xFF61BC63)
    val Red = Color(0xFFFF3B30)
    val Yellow = Color(0xFFFFCC00)
    val DeepBlue = Color(0xFF0064E0)
    val DestructiveBackground = Color(0xFFFFD8DB)
    val DestructiveForeground = Color(0xFFAA071E)
}
