package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.DayPlan
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.WorkoutPlan
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography
import java.time.LocalDate

@Composable
fun WorkoutPlanScreen(
    plan: WorkoutPlan,
    onStartWorkout: (workoutId: String) -> Unit,
    onRegenerate: () -> Unit,
    onBack: () -> Unit
) {
    val todayIndex = remember {
        val dayName = LocalDate.now().dayOfWeek.name.lowercase()
            .replaceFirstChar { it.uppercase() }
        plan.days.indexOfFirst { it.dayOfWeek.equals(dayName, ignoreCase = true) }
            .coerceAtLeast(0)
    }
    var selectedDayIndex by remember { mutableIntStateOf(todayIndex) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HealthColors.Background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = HealthColors.TextPrimary
                )
            }
            Text(
                text = "My Workout Plan",
                color = HealthColors.TextPrimary,
                fontSize = HealthTypography.largeHeaderSize,
                fontWeight = HealthTypography.largeHeaderWeight,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRegenerate) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Regenerate",
                    tint = HealthColors.Accent
                )
            }
        }

        // Weekly goal
        if (plan.weeklyGoal.isNotBlank()) {
            Text(
                text = plan.weeklyGoal,
                color = HealthColors.TextSecondary,
                fontSize = HealthTypography.bodySize,
                modifier = Modifier.padding(horizontal = HealthSpacing.md)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Day tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            plan.days.forEachIndexed { index, day ->
                val isSelected = index == selectedDayIndex
                val isToday = index == todayIndex
                val label = day.dayOfWeek.take(3)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isSelected -> HealthColors.Accent
                                else -> HealthColors.Card
                            }
                        )
                        .clickable { selectedDayIndex = index }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            color = if (isSelected) HealthColors.Background else HealthColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isToday) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) HealthColors.Background else HealthColors.Accent)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected day content
        val selectedDay = plan.days.getOrNull(selectedDayIndex)
        if (selectedDay != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = HealthSpacing.md)
                    .verticalScroll(rememberScrollState())
            ) {
                if (selectedDay.isRestDay) {
                    RestDayCard(selectedDay)
                } else if (selectedDay.workout != null) {
                    WorkoutDayCard(
                        day = selectedDay,
                        onStartWorkout = { onStartWorkout(selectedDay.workout.id) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RestDayCard(day: DayPlan) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HealthSpacing.cardRadius))
            .background(HealthColors.Card)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.SelfImprovement,
            contentDescription = null,
            tint = HealthColors.Accent,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = day.focusArea.ifBlank { "Rest & Recovery" },
            color = HealthColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Take it easy today. Stretch, hydrate, and let your body recover.",
            color = HealthColors.TextSecondary,
            fontSize = HealthTypography.bodySize
        )
    }
}

@Composable
private fun WorkoutDayCard(day: DayPlan, onStartWorkout: () -> Unit) {
    val workout = day.workout ?: return

    // Focus area & workout name
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HealthSpacing.cardRadius))
            .background(HealthColors.Card)
            .padding(HealthSpacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = HealthColors.Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = day.focusArea,
                color = HealthColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = workout.name,
            color = HealthColors.TextPrimary,
            fontSize = HealthTypography.sectionHeaderSize,
            fontWeight = HealthTypography.sectionHeaderWeight
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = HealthColors.TextDim,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${workout.durationMin} min",
                color = HealthColors.TextDim,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = workout.category.name,
                color = HealthColors.TextDim,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = workout.difficulty.name,
                color = HealthColors.TextDim,
                fontSize = 13.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Exercises list
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HealthSpacing.cardRadius))
            .background(HealthColors.Card)
            .padding(HealthSpacing.md)
    ) {
        Text(
            text = "Exercises",
            color = HealthColors.TextPrimary,
            fontSize = HealthTypography.sectionHeaderSize,
            fontWeight = HealthTypography.sectionHeaderWeight
        )
        Spacer(modifier = Modifier.height(12.dp))

        workout.exercises.forEachIndexed { index, exercise ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(HealthColors.CardSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = HealthColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        color = HealthColors.TextPrimary,
                        fontSize = HealthTypography.bodySize,
                        fontWeight = FontWeight.Medium
                    )
                    val detail = when {
                        exercise.sets != null && exercise.reps != null ->
                            "${exercise.sets} sets x ${exercise.reps} reps"
                        exercise.durationSec != null ->
                            "${exercise.durationSec}s"
                        else -> ""
                    }
                    if (detail.isNotEmpty()) {
                        Text(
                            text = detail,
                            color = HealthColors.TextDim,
                            fontSize = HealthTypography.subLabelSize
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Start Workout button
    Button(
        onClick = onStartWorkout,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = HealthColors.Accent),
        shape = RoundedCornerShape(HealthSpacing.cardRadius)
    ) {
        Text(
            text = "Start Workout",
            color = HealthColors.Background,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
