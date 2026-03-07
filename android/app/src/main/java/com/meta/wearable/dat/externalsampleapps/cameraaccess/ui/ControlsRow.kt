package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ControlsRow(
    onStopStream: () -> Unit,
    onCapturePhoto: () -> Unit,
    onToggleAI: () -> Unit,
    isAIActive: Boolean,
    onTestExerciseGuide: () -> Unit,
    onToggleLive: () -> Unit,
    isLiveActive: Boolean,
    onToggleRepCounter: () -> Unit,
    isRepCounterActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .clip(barShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), barShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwitchButton(
                label = "Stop",
                onClick = onStopStream,
                isDestructive = true,
                modifier = Modifier.weight(1f),
            )

            CaptureButton(onClick = onCapturePhoto)

            // AI toggle
            ControlCircleButton(
                onClick = onToggleAI,
                isActive = isAIActive,
                activeColor = AppColor.Accent,
                icon = Icons.Default.AutoAwesome,
                contentDescription = if (isAIActive) "Stop AI" else "Start AI",
            )

            // Rep counter toggle
            ControlCircleButton(
                onClick = onToggleRepCounter,
                isActive = isRepCounterActive,
                activeColor = Color(0xFF00CC6A),
                icon = Icons.Default.FitnessCenter,
                contentDescription = if (isRepCounterActive) "Stop Rep Counter" else "Start Rep Counter",
            )

            // Exercise guide
            ControlCircleButton(
                onClick = onTestExerciseGuide,
                isActive = false,
                activeColor = AppColor.AccentBlue,
                icon = Icons.Default.RepeatOne,
                contentDescription = "Exercise Guide",
                inactiveColor = AppColor.AccentBlue.copy(alpha = 0.7f),
            )

            // Live toggle
            ControlCircleButton(
                onClick = onToggleLive,
                isActive = isLiveActive,
                activeColor = AppColor.Error,
                icon = Icons.Default.Videocam,
                contentDescription = if (isLiveActive) "Stop Live" else "Start Live",
            )
        }
    }
}

@Composable
private fun ControlCircleButton(
    onClick: () -> Unit,
    isActive: Boolean,
    activeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    inactiveColor: Color = Color.White.copy(alpha = 0.7f),
) {
    val borderColor = if (isActive) activeColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f)
    val bgColor = if (isActive) activeColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f)
    val iconTint = if (isActive) activeColor else inactiveColor

    Button(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(1f)
            .border(1.dp, borderColor, CircleShape),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
        )
    }
}
