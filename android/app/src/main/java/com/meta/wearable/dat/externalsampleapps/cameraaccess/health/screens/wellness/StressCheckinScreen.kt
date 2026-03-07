package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.wellness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

@Composable
fun StressCheckinScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HealthColors.Background)
            .padding(HealthSpacing.screenPadding)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = HealthColors.TextPrimary)
            }
            Text(
                "Stress Check-in",
                fontSize = HealthTypography.sectionHeaderSize,
                fontWeight = HealthTypography.sectionHeaderWeight,
                color = HealthColors.TextPrimary
            )
        }

        // Placeholder content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.SelfImprovement,
                    contentDescription = null,
                    tint = HealthColors.TextDim,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Coming Soon",
                    fontSize = HealthTypography.sectionHeaderSize,
                    fontWeight = HealthTypography.sectionHeaderWeight,
                    color = HealthColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Monitor your stress levels and mood",
                    fontSize = HealthTypography.bodySize,
                    color = HealthColors.TextDim,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
