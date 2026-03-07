package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

@Composable
fun MetricCard(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(HealthSpacing.cardRadius))
            .background(HealthColors.Card)
            .padding(HealthSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = HealthColors.Accent,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(HealthSpacing.sm))

        Text(
            text = value,
            color = HealthColors.TextPrimary,
            fontSize = HealthTypography.metricValueSize,
            fontWeight = HealthTypography.metricValueWeight
        )

        Text(
            text = unit,
            color = HealthColors.TextSecondary,
            fontSize = HealthTypography.metricUnitSize,
            fontWeight = HealthTypography.metricUnitWeight
        )

        Spacer(modifier = Modifier.height(HealthSpacing.xs))

        Text(
            text = label,
            color = HealthColors.TextDim,
            fontSize = HealthTypography.subLabelSize,
            fontWeight = HealthTypography.subLabelWeight
        )
    }
}
