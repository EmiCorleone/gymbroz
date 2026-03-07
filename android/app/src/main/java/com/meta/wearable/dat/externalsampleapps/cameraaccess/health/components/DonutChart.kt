package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

@Composable
fun DonutChart(
    progress: Float,
    centerValue: String,
    centerLabel: String,
    size: Dp = 160.dp,
    modifier: Modifier = Modifier
) {
    val bgColor = HealthColors.CardSecondary
    val accentColor = HealthColors.Accent

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = size.toPx() / 6f

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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                color = HealthColors.TextPrimary,
                fontSize = HealthTypography.metricValueSize,
                fontWeight = HealthTypography.metricValueWeight
            )
            Text(
                text = centerLabel,
                color = HealthColors.TextSecondary,
                fontSize = HealthTypography.subLabelSize,
                fontWeight = HealthTypography.subLabelWeight
            )
        }
    }
}
