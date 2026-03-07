package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.AppColor
import kotlin.math.sin

/**
 * Full-screen animated gradient background with slowly drifting radial orbs.
 * Creates a subtle, living dark background that gives glass surfaces something to refract against.
 */
@Composable
fun AnimatedMeshGradientBackground(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppColor.Background,
) {
    val transition = rememberInfiniteTransition(label = "mesh_gradient")

    val phase1 by transition.animateFloat(
        initialValue = 0f, targetValue = 6.2832f, // 2*PI
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase1",
    )
    val phase2 by transition.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase2",
    )
    val phase3 by transition.animateFloat(
        initialValue = 0f, targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase3",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Dark base
        drawRect(backgroundColor)

        val w = size.width
        val h = size.height

        // Orb 1 — purple, drifts around upper-left area
        val orb1Center = Offset(
            x = w * (0.25f + 0.15f * sin(phase1)),
            y = h * (0.2f + 0.1f * sin(phase1 * 0.7f)),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AppColor.AmbientPurple, Color.Transparent),
                center = orb1Center,
                radius = w * 0.6f,
            ),
            center = orb1Center,
            radius = w * 0.6f,
        )

        // Orb 2 — blue, drifts around center-right
        val orb2Center = Offset(
            x = w * (0.75f + 0.12f * sin(phase2)),
            y = h * (0.45f + 0.12f * sin(phase2 * 0.8f)),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AppColor.AmbientBlue, Color.Transparent),
                center = orb2Center,
                radius = w * 0.55f,
            ),
            center = orb2Center,
            radius = w * 0.55f,
        )

        // Orb 3 — green, drifts around lower area
        val orb3Center = Offset(
            x = w * (0.4f + 0.18f * sin(phase3)),
            y = h * (0.78f + 0.08f * sin(phase3 * 0.6f)),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AppColor.AmbientGreen, Color.Transparent),
                center = orb3Center,
                radius = w * 0.5f,
            ),
            center = orb3Center,
            radius = w * 0.5f,
        )
    }
}
