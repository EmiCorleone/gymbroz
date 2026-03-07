package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

@Composable
fun StreakCard(
    title: String,
    streakDays: Int,
    accentColor: Color = HealthColors.Accent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(150.dp)
            .clip(RoundedCornerShape(HealthSpacing.cardRadius))
            .background(HealthColors.Card)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(accentColor)
        )

        Column(
            modifier = Modifier.padding(HealthSpacing.md)
        ) {
            Text(
                text = title,
                color = HealthColors.TextSecondary,
                fontSize = HealthTypography.subLabelSize,
                fontWeight = HealthTypography.subLabelWeight
            )

            Spacer(modifier = Modifier.height(HealthSpacing.sm))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$streakDays",
                    color = HealthColors.TextPrimary,
                    fontSize = HealthTypography.metricValueSize,
                    fontWeight = HealthTypography.metricValueWeight
                )

                Text(
                    text = " days",
                    color = HealthColors.TextDim,
                    fontSize = HealthTypography.bodySize,
                    fontWeight = HealthTypography.bodyWeight,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}
