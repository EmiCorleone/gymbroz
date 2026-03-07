package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.ExerciseGuideUiState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components.WorkoutTimer
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.Workout
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors

@Composable
fun ActiveWorkoutScreen(
    workout: Workout,
    exerciseGuides: Map<String, ExerciseGuideUiState> = emptyMap(),
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HealthColors.Background)
    ) {
        WorkoutTimer(
            exercises = workout.exercises,
            onComplete = onComplete,
            exerciseGuideContent = { exerciseName ->
                val guide = exerciseGuides[exerciseName]
                if (guide != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Full body image
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                guide.isGenerating && guide.imageBase64 == null -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = HealthColors.Accent,
                                        strokeWidth = 3.dp
                                    )
                                }
                                guide.imageBase64 != null -> {
                                    val bitmap = remember(guide.imageBase64) {
                                        val bytes = Base64.decode(guide.imageBase64, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Full body guide",
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                                else -> {
                                    Text("Full body", color = HealthColors.TextDim, fontSize = 12.sp)
                                }
                            }
                        }
                        // Close-up image
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                guide.isGenerating && guide.closeUpImageBase64 == null -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = HealthColors.Accent,
                                        strokeWidth = 3.dp
                                    )
                                }
                                guide.closeUpImageBase64 != null -> {
                                    val bitmap = remember(guide.closeUpImageBase64) {
                                        val bytes = Base64.decode(guide.closeUpImageBase64, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Close-up form guide",
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                                else -> {
                                    Text("Close-up", color = HealthColors.TextDim, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
