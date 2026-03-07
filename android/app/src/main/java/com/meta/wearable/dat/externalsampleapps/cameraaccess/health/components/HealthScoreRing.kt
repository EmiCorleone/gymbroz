package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

@Composable
fun HealthScoreRing(
    progress: Float,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val bgColor = HealthColors.CardSecondary
    val accentColor = HealthColors.Accent
    val percentage = (progress.coerceIn(0f, 1f) * 100).toInt()

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = size.toPx() / 8f

            drawArc(
                color = bgColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = progress.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "${percentage}%",
            color = HealthColors.TextPrimary,
            fontSize = HealthTypography.subLabelSize,
            fontWeight = HealthTypography.metricValueWeight
        )
    }
}
