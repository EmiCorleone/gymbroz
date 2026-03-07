package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.Difficulty
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.Workout
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

@Composable
fun WorkoutDetailScreen(
    workout: Workout,
    onStartWorkout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val difficultyColor = when (workout.difficulty) {
        Difficulty.Beginner -> Color(0xFF34C759)
        Difficulty.Intermediate -> Color(0xFFFFCC00)
        Difficulty.Advanced -> Color(0xFFFF3B30)
    }

    Column(
        modifier = modifier
            .background(HealthColors.Background)
            .padding(HealthSpacing.screenPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = HealthColors.TextPrimary
                )
            }
            Text(
                text = workout.name,
                color = HealthColors.TextPrimary,
                fontSize = HealthTypography.sectionHeaderSize,
                fontWeight = HealthTypography.sectionHeaderWeight
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Difficulty badge
        Text(
            text = workout.difficulty.name,
            color = difficultyColor,
            fontSize = HealthTypography.subLabelSize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(difficultyColor.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Duration row
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Duration",
                tint = HealthColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${workout.durationMin} min",
                color = HealthColors.TextSecondary,
                fontSize = HealthTypography.bodySize,
                fontWeight = HealthTypography.bodyWeight
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Exercises section title
        Text(
            text = "Exercises",
            color = HealthColors.TextPrimary,
            fontSize = HealthTypography.sectionHeaderSize,
            fontWeight = HealthTypography.sectionHeaderWeight
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Exercise list
        workout.exercises.forEachIndexed { index, exercise ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Number circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(HealthColors.Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = HealthColors.Background,
                        fontSize = HealthTypography.subLabelSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = exercise.name,
                        color = HealthColors.TextPrimary,
                        fontSize = HealthTypography.bodySize,
                        fontWeight = HealthTypography.bodyWeight
                    )
                    val detailText = if (exercise.sets != null && exercise.reps != null) {
                        "${exercise.sets} sets \u00D7 ${exercise.reps} reps"
                    } else if (exercise.durationSec != null) {
                        val totalSec = exercise.durationSec
                        val minutes = totalSec / 60
                        val seconds = totalSec % 60
                        if (minutes > 0 && seconds > 0) {
                            "$minutes min $seconds sec"
                        } else if (minutes > 0) {
                            "$minutes min"
                        } else {
                            "$seconds sec"
                        }
                    } else {
                        ""
                    }
                    if (detailText.isNotEmpty()) {
                        Text(
                            text = detailText,
                            color = HealthColors.TextSecondary,
                            fontSize = HealthTypography.subLabelSize,
                            fontWeight = HealthTypography.subLabelWeight
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Start Workout button
        Button(
            onClick = onStartWorkout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HealthColors.Accent
            ),
            shape = RoundedCornerShape(HealthSpacing.cardRadius)
        ) {
            Text(
                text = "Start Workout",
                color = HealthColors.Background,
                fontWeight = FontWeight.Bold,
                fontSize = HealthTypography.bodySize
            )
        }
    }
}
