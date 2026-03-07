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
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
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
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .clip(barShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.04f))
                ),
                shape = barShape,
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwitchButton(
                label = "Stop",
                onClick = onStopStream,
                isDestructive = true,
                modifier = Modifier.weight(1f),
            )

            CaptureButton(onClick = onCapturePhoto)

            // AI toggle — glass circle with glow when active
            Button(
                onClick = onToggleAI,
                modifier = Modifier
                    .aspectRatio(1f)
                    .then(
                        if (isAIActive) Modifier.drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(AppColor.Accent.copy(alpha = 0.3f), Color.Transparent),
                                ),
                                radius = size.maxDimension * 0.7f,
                            )
                        } else Modifier
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                (if (isAIActive) AppColor.Accent else Color.White).copy(alpha = 0.3f),
                                (if (isAIActive) AppColor.Accent else Color.White).copy(alpha = 0.08f),
                            )
                        ),
                        CircleShape,
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAIActive) AppColor.Accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = if (isAIActive) "Stop AI" else "Start AI",
                    tint = if (isAIActive) AppColor.Accent else Color.White,
                )
            }

            // Exercise guide test
            Button(
                onClick = onTestExerciseGuide,
                modifier = Modifier
                    .aspectRatio(1f)
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(AppColor.AccentBlue.copy(alpha = 0.3f), AppColor.AccentBlue.copy(alpha = 0.08f))
                        ),
                        CircleShape,
                    ),
                colors = ButtonDefaults.buttonColors(containerColor = AppColor.AccentBlue.copy(alpha = 0.15f)),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = "Test Exercise Guide", tint = AppColor.AccentBlue)
            }

            // Live toggle — glass circle with glow when active
            Button(
                onClick = onToggleLive,
                modifier = Modifier
                    .aspectRatio(1f)
                    .then(
                        if (isLiveActive) Modifier.drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(AppColor.Error.copy(alpha = 0.3f), Color.Transparent),
                                ),
                                radius = size.maxDimension * 0.7f,
                            )
                        } else Modifier
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                (if (isLiveActive) AppColor.Error else Color.White).copy(alpha = 0.3f),
                                (if (isLiveActive) AppColor.Error else Color.White).copy(alpha = 0.08f),
                            )
                        ),
                        CircleShape,
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLiveActive) AppColor.Error.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = if (isLiveActive) "Stop Live" else "Start Live",
                    tint = if (isLiveActive) AppColor.Error else Color.White,
                )
            }
        }
    }
}
