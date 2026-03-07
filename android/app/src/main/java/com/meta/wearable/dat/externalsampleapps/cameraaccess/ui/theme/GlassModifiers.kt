package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.AppColor

/**
 * Glass-style translucent background with vertical gradient (brighter at top).
 */
fun Modifier.glassBackground(
    alpha: Float = 0.10f,
    shape: Shape,
): Modifier = this.background(
    brush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = alpha + 0.04f),
            Color.White.copy(alpha = alpha),
        )
    ),
    shape = shape,
)

/**
 * Glass-style gradient border with shimmer effect.
 * When selected, uses accent-colored gradient instead of white.
 */
fun Modifier.glassBorder(
    shape: Shape,
    isSelected: Boolean = false,
    accentColor: Color = AppColor.Accent,
    width: Dp = 1.dp,
): Modifier {
    val colors = if (isSelected) {
        listOf(
            accentColor.copy(alpha = 0.6f),
            accentColor.copy(alpha = 0.1f),
            accentColor.copy(alpha = 0.35f),
        )
    } else {
        listOf(
            AppColor.ShimmerStart,
            AppColor.ShimmerMid,
            AppColor.ShimmerEnd,
        )
    }
    return this.border(
        width = width,
        brush = Brush.linearGradient(
            colors = colors,
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        ),
        shape = shape,
    )
}

/**
 * Outer radial glow drawn behind the composable.
 */
fun Modifier.glassGlow(
    color: Color,
    radiusFraction: Float = 0.7f,
): Modifier = this.drawBehind {
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.15f), Color.Transparent),
            center = center,
            radius = size.maxDimension * radiusFraction,
        ),
        cornerRadius = CornerRadius(24.dp.toPx()),
    )
}

/**
 * Top-edge light highlight strip (simulates light hitting glass edge).
 */
fun Modifier.glassHighlight(
    cornerRadius: Dp = 24.dp,
    alpha: Float = 0.12f,
): Modifier = this.drawBehind {
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = alpha), Color.Transparent),
            startY = 0f,
            endY = 3.dp.toPx(),
        ),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        size = Size(size.width, 3.dp.toPx()),
    )
}

/**
 * Animated diagonal shimmer sweep across the composable.
 */
fun Modifier.glassShimmer(
    durationMillis: Int = 3000,
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "glass_shimmer")
    val progress by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_progress",
    )

    this.drawBehind {
        val shimmerWidth = size.width * 0.4f
        val x = size.width * progress
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                start = Offset(x - shimmerWidth, 0f),
                end = Offset(x + shimmerWidth, size.height),
            ),
        )
    }
}
