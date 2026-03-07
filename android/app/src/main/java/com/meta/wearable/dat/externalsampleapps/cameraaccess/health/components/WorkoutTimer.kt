package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.Exercise
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography
import kotlinx.coroutines.delay

@Composable
fun WorkoutTimer(
    exercises: List<Exercise>,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    exerciseGuideContent: @Composable ((exerciseName: String) -> Unit)? = null
) {
    if (exercises.isEmpty()) return

    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableIntStateOf(exercises[0].durationSec ?: 0) }
    var currentSet by remember { mutableIntStateOf(1) }

    val currentExercise = exercises[currentExerciseIndex]
    val isDurationBased = currentExercise.durationSec != null
    val totalExercises = exercises.size
    val overallProgress = (currentExerciseIndex.toFloat()) / totalExercises
    val accentColor = HealthColors.Accent
    val bgBarColor = HealthColors.CardSecondary

    LaunchedEffect(currentExerciseIndex) {
        timeRemaining = exercises[currentExerciseIndex].durationSec ?: 0
        currentSet = 1
    }

    LaunchedEffect(isRunning, timeRemaining) {
        if (isRunning && isDurationBased && timeRemaining > 0) {
            delay(1000L)
            timeRemaining--
            if (timeRemaining <= 0) {
                if (currentExerciseIndex < totalExercises - 1) {
                    currentExerciseIndex++
                } else {
                    isRunning = false
                    onComplete()
                }
            }
        }
    }

    fun advanceExercise() {
        if (currentExerciseIndex < totalExercises - 1) {
            currentExerciseIndex++
        } else {
            isRunning = false
            onComplete()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HealthColors.Background)
            .padding(HealthSpacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = currentExercise.name,
            color = HealthColors.TextPrimary,
            fontSize = HealthTypography.largeHeaderSize,
            fontWeight = HealthTypography.largeHeaderWeight,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(HealthSpacing.md))

        exerciseGuideContent?.invoke(currentExercise.name)

        Spacer(modifier = Modifier.height(HealthSpacing.md))

        if (isDurationBased) {
            val minutes = timeRemaining / 60
            val seconds = timeRemaining % 60
            Text(
                text = "%02d:%02d".format(minutes, seconds),
                color = HealthColors.Accent,
                fontSize = HealthTypography.metricValueSize,
                fontWeight = HealthTypography.metricValueWeight
            )
        } else {
            Text(
                text = "Set $currentSet of ${currentExercise.sets ?: 1}",
                color = HealthColors.TextSecondary,
                fontSize = HealthTypography.sectionHeaderSize,
                fontWeight = HealthTypography.sectionHeaderWeight
            )
            Spacer(modifier = Modifier.height(HealthSpacing.xs))
            Text(
                text = "${currentExercise.reps ?: 0} reps",
                color = HealthColors.Accent,
                fontSize = HealthTypography.metricValueSize,
                fontWeight = HealthTypography.metricValueWeight
            )
        }

        Spacer(modifier = Modifier.height(HealthSpacing.xl))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val barHeight = size.height
            val radius = barHeight / 2f

            drawRoundRect(
                color = bgBarColor,
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(radius, radius)
            )

            if (overallProgress > 0f) {
                drawRoundRect(
                    color = accentColor,
                    size = Size(size.width * overallProgress, barHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }

        Spacer(modifier = Modifier.height(HealthSpacing.xl))

        Row(
            horizontalArrangement = Arrangement.spacedBy(HealthSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(HealthColors.Heart.copy(alpha = 0.2f))
                    .clickable {
                        isRunning = false
                        currentExerciseIndex = 0
                        timeRemaining = exercises[0].durationSec ?: 0
                        currentSet = 1
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = HealthColors.Heart,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(HealthColors.Accent)
                    .clickable {
                        if (isDurationBased) {
                            isRunning = !isRunning
                        } else {
                            val totalSets = currentExercise.sets ?: 1
                            if (currentSet < totalSets) {
                                currentSet++
                            } else {
                                advanceExercise()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Play",
                    tint = HealthColors.Background,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(HealthSpacing.xl))

        if (currentExerciseIndex < totalExercises - 1) {
            Text(
                text = "NEXT UP: ${exercises[currentExerciseIndex + 1].name}",
                color = HealthColors.TextDim,
                fontSize = HealthTypography.subLabelSize,
                fontWeight = HealthTypography.tabLabelWeight,
                textAlign = TextAlign.Center
            )
        }
    }
}
