package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components.StreakCard
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.DayPlan
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.WorkoutPlan
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutOverviewScreen(
    workoutPlan: WorkoutPlan?,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val formattedDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))

    Column(
        modifier = modifier
            .background(HealthColors.Background)
            .padding(HealthSpacing.screenPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PulseUp Fitness",
                color = HealthColors.Accent,
                fontSize = HealthTypography.largeHeaderSize,
                fontWeight = HealthTypography.largeHeaderWeight
            )
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "QR Code",
                    tint = HealthColors.TextPrimary
                )
            }
        }

        // Status text
        Text(
            text = "Open \u00B7 Closes at 21:50",
            color = HealthColors.TextSecondary,
            fontSize = HealthTypography.bodySize,
            fontWeight = HealthTypography.bodyWeight
        )

        // Date
        Text(
            text = formattedDate,
            color = HealthColors.TextDim,
            fontSize = HealthTypography.bodySize,
            fontWeight = HealthTypography.bodyWeight
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Today's Workout Card
        TodayWorkoutCard(
            workoutPlan = workoutPlan,
            onNavigateToPlan = onNavigateToPlan
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Streak Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StreakCard(
                title = "Training",
                streakDays = 5,
                accentColor = HealthColors.Accent,
                modifier = Modifier.weight(1f)
            )
            StreakCard(
                title = "Nutrition",
                streakDays = 3,
                accentColor = HealthColors.ProteinColor,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Personal Training Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(HealthSpacing.cardRadius))
                .background(HealthColors.Card)
                .padding(HealthSpacing.md)
        ) {
            Text(
                text = "Personal Training",
                color = HealthColors.TextPrimary,
                fontSize = HealthTypography.sectionHeaderSize,
                fontWeight = HealthTypography.sectionHeaderWeight
            )
            Text(
                text = "Book a session with your trainer",
                color = HealthColors.TextSecondary,
                fontSize = HealthTypography.bodySize,
                fontWeight = HealthTypography.bodyWeight
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HealthColors.Accent
                ),
                shape = RoundedCornerShape(HealthSpacing.cardRadius)
            ) {
                Text(
                    text = "Book Now",
                    color = HealthColors.Background,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Workout Library Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(HealthSpacing.cardRadius))
                .background(HealthColors.Card)
                .clickable { onNavigateToLibrary() }
                .padding(HealthSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "Workout Library",
                    tint = HealthColors.TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Workout Library",
                    color = HealthColors.TextPrimary,
                    fontSize = HealthTypography.bodySize,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = HealthColors.TextDim
            )
        }
    }
}

@Composable
private fun TodayWorkoutCard(
    workoutPlan: WorkoutPlan?,
    onNavigateToPlan: () -> Unit
) {
    val todayDayName = LocalDate.now().dayOfWeek.name.lowercase()
        .replaceFirstChar { it.uppercase() }
    val todayPlan: DayPlan? = workoutPlan?.days?.find {
        it.dayOfWeek.equals(todayDayName, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HealthSpacing.cardRadius))
            .background(if (workoutPlan != null) HealthColors.Accent else HealthColors.Card)
            .clickable { onNavigateToPlan() }
            .padding(HealthSpacing.md)
    ) {
        if (workoutPlan != null && todayPlan != null) {
            Text(
                text = "Today's Workout",
                color = HealthColors.Background,
                fontSize = HealthTypography.subLabelSize,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (todayPlan.isRestDay) {
                Text(
                    text = "Rest Day",
                    color = HealthColors.Background,
                    fontSize = HealthTypography.sectionHeaderSize,
                    fontWeight = HealthTypography.sectionHeaderWeight
                )
                Text(
                    text = todayPlan.focusArea.ifBlank { "Rest & Recovery" },
                    color = HealthColors.Background.copy(alpha = 0.7f),
                    fontSize = HealthTypography.bodySize
                )
            } else if (todayPlan.workout != null) {
                Text(
                    text = todayPlan.workout.name,
                    color = HealthColors.Background,
                    fontSize = HealthTypography.sectionHeaderSize,
                    fontWeight = HealthTypography.sectionHeaderWeight
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = todayPlan.focusArea,
                        color = HealthColors.Background.copy(alpha = 0.7f),
                        fontSize = HealthTypography.bodySize
                    )
                    Text(
                        text = "  ·  ${todayPlan.workout.durationMin} min",
                        color = HealthColors.Background.copy(alpha = 0.7f),
                        fontSize = HealthTypography.bodySize
                    )
                }
            }
        } else {
            Text(
                text = "Generate Your Plan",
                color = HealthColors.Accent,
                fontSize = HealthTypography.sectionHeaderSize,
                fontWeight = HealthTypography.sectionHeaderWeight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Get a personalized AI-generated weekly workout plan",
                color = HealthColors.TextSecondary,
                fontSize = HealthTypography.bodySize
            )
        }
    }
}
