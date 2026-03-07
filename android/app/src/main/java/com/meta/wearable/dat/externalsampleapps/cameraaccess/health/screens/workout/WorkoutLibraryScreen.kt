package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.Difficulty
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.Workout
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.WorkoutCategory
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

@Composable
fun WorkoutLibraryScreen(
    workouts: List<Workout>,
    onWorkoutClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(HealthColors.Background)
            .padding(HealthSpacing.screenPadding)
    ) {
        // Header row
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
                text = "Workout Library",
                color = HealthColors.TextPrimary,
                fontSize = HealthTypography.sectionHeaderSize,
                fontWeight = HealthTypography.sectionHeaderWeight
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Workout Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(workouts) { workout ->
                WorkoutGridCard(
                    workout = workout,
                    onClick = { onWorkoutClick(workout.id) }
                )
            }
        }
    }
}

@Composable
private fun WorkoutGridCard(
    workout: Workout,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (workout.category) {
        WorkoutCategory.HIIT -> HealthColors.Heart
        WorkoutCategory.Strength -> HealthColors.Accent
        WorkoutCategory.Yoga -> HealthColors.Lavender
        WorkoutCategory.Cardio -> HealthColors.AccentTeal
        WorkoutCategory.Walking -> HealthColors.ProteinColor
    }

    val difficultyColor = when (workout.difficulty) {
        Difficulty.Beginner -> Color(0xFF34C759)
        Difficulty.Intermediate -> Color(0xFFFFCC00)
        Difficulty.Advanced -> Color(0xFFFF3B30)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(HealthSpacing.cardRadius))
            .background(HealthColors.Card)
            .clickable { onClick() }
    ) {
        // Colored top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(categoryColor)
        )

        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Workout name
            Text(
                text = workout.name,
                color = HealthColors.TextPrimary,
                fontSize = HealthTypography.bodySize,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(4.dp))

            // Duration row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Duration",
                    tint = HealthColors.TextDim,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${workout.durationMin} min",
                    color = HealthColors.TextDim,
                    fontSize = HealthTypography.subLabelSize,
                    fontWeight = HealthTypography.subLabelWeight
                )
            }
        }
    }
}
