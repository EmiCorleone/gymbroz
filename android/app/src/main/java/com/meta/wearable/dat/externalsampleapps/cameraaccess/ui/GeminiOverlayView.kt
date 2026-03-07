package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.gemini.GeminiConnectionState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.gemini.GeminiUiState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.ToolCallStatus

@Composable
fun GeminiOverlay(
    uiState: GeminiUiState,
    onDismissExerciseGuide: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Top overlay: status + transcripts + tool status
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).align(Alignment.TopStart),
        ) {
            // Status bar
            GeminiStatusBar(connectionState = uiState.connectionState)

            Spacer(modifier = Modifier.height(8.dp))

            // Transcripts
            if (uiState.userTranscript.isNotEmpty() || uiState.aiTranscript.isNotEmpty()) {
                TranscriptView(
                    userTranscript = uiState.userTranscript,
                    aiTranscript = uiState.aiTranscript,
                )
            }

            // Tool call status
            val toolStatus = uiState.toolCallStatus
            if (toolStatus !is ToolCallStatus.Idle) {
                Spacer(modifier = Modifier.height(4.dp))
                ToolCallStatusView(status = toolStatus)
            }

            // Speaking indicator
            if (uiState.isModelSpeaking) {
                Spacer(modifier = Modifier.height(4.dp))
                SpeakingIndicator()
            }
        }

        // Rep counter (bottom-left)
        if (uiState.repCounter.active) {
            RepCounterOverlay(
                exercise = uiState.repCounter.exercise,
                reps = uiState.repCounter.repCount,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            )
        }

        // Music indicator (bottom-right)
        if (uiState.music.active) {
            MusicOverlay(
                prompt = uiState.music.prompt,
                bpm = uiState.music.bpm,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }

        // Exercise guide (center overlay)
        val guide = uiState.exerciseGuide
        if (guide.isGenerating || guide.imageBase64 != null) {
            ExerciseGuideOverlay(
                isGenerating = guide.isGenerating,
                imageBase64 = guide.imageBase64,
                description = guide.description,
                error = guide.error,
                onDismiss = onDismissExerciseGuide,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }
    }
}

@Composable
fun GeminiStatusBar(
    connectionState: GeminiConnectionState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusPill(
            label = "AI",
            color = when (connectionState) {
                is GeminiConnectionState.Ready -> Color(0xFF4CAF50)
                is GeminiConnectionState.Connecting,
                is GeminiConnectionState.SettingUp -> Color(0xFFFF9800)
                is GeminiConnectionState.Error -> Color(0xFFF44336)
                is GeminiConnectionState.Disconnected -> Color(0xFF9E9E9E)
            },
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
        )
    }
}

@Composable
fun TranscriptView(
    userTranscript: String,
    aiTranscript: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (userTranscript.isNotEmpty()) {
            Text(
                text = userTranscript,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (aiTranscript.isNotEmpty()) {
            Text(
                text = aiTranscript,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ToolCallStatusView(
    status: ToolCallStatus,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (status) {
            is ToolCallStatus.Executing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            }
            is ToolCallStatus.Completed -> {
                Text(text = "✓", color = Color(0xFF4CAF50), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            is ToolCallStatus.Failed -> {
                Text(text = "✗", color = Color(0xFFF44336), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            else -> {}
        }
        Text(
            text = status.displayText,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun RepCounterOverlay(
    exercise: String,
    reps: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0xFF1B5E20).copy(alpha = 0.85f), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = exercise.replace("_", " ").uppercase(),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$reps",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = "REPS",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
        )
    }
}

@Composable
fun MusicOverlay(
    prompt: String,
    bpm: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color(0xFF4A148C).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "♪", color = Color.White, fontSize = 18.sp)
        Column {
            Text(
                text = prompt,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(120.dp),
            )
            Text(
                text = "$bpm BPM",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
fun ExerciseGuideOverlay(
    isGenerating: Boolean,
    imageBase64: String?,
    description: String?,
    error: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Exercise Guide",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (!isGenerating) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isGenerating) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Generating form guide...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            } else if (imageBase64 != null) {
                val bitmap = remember(imageBase64) {
                    val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Exercise form guide",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
                if (!description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                    )
                }
            } else if (!error.isNullOrEmpty()) {
                Text(text = error, color = Color(0xFFF44336), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SpeakingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "speaking")
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(4) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = 16f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = index * 100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar$index",
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color.White),
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Speaking", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}
