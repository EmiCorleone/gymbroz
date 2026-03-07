package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

@Composable
fun MacroBar(
    label: String,
    current: Int,
    goal: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (current.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val bgColor = HealthColors.CardSecondary

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HealthSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )

        Text(
            text = label,
            color = HealthColors.TextSecondary,
            fontSize = HealthTypography.bodySize,
            fontWeight = HealthTypography.bodyWeight,
            modifier = Modifier.width(56.dp)
        )

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
        ) {
            val barHeight = size.height
            val radius = barHeight / 2f

            drawRoundRect(
                color = bgColor,
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(radius, radius)
            )

            if (progress > 0f) {
                drawRoundRect(
                    color = color,
                    size = Size(size.width * progress, barHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }

        Text(
            text = "${current}/${goal}g",
            color = HealthColors.TextDim,
            fontSize = HealthTypography.subLabelSize,
            fontWeight = HealthTypography.subLabelWeight
        )
    }
}
