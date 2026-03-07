package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components.DonutChart
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components.MacroBar
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components.WeekCalendar
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.DailyNutrition
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

@Composable
fun NutritionOverviewScreen(
    nutrition: DailyNutrition,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onNavigateToMealPlan: () -> Unit,
    onNavigateToAddMeal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HealthColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(HealthSpacing.screenPadding)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nutrition",
                    color = HealthColors.TextPrimary,
                    fontSize = HealthTypography.sectionHeaderSize,
                    fontWeight = HealthTypography.sectionHeaderWeight
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = HealthColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Week calendar
            WeekCalendar(
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main content row: donut chart + macro bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DonutChart(
                    progress = nutrition.totalCalories.toFloat() / nutrition.goalCalories,
                    centerValue = nutrition.totalCalories.toString(),
                    centerLabel = "kcal"
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    MacroBar(
                        label = "Carbs",
                        current = nutrition.carbs.current,
                        goal = nutrition.carbs.goal,
                        color = HealthColors.CarbsColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MacroBar(
                        label = "Protein",
                        current = nutrition.proteins.current,
                        goal = nutrition.proteins.goal,
                        color = HealthColors.ProteinColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MacroBar(
                        label = "Fats",
                        current = nutrition.fats.current,
                        goal = nutrition.fats.goal,
                        color = HealthColors.FatsColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Check calories card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HealthSpacing.cardRadius),
                colors = CardDefaults.cardColors(containerColor = HealthColors.Card)
            ) {
                Row(
                    modifier = Modifier.padding(HealthSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Camera",
                        tint = HealthColors.Accent
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Scan your meal",
                            color = HealthColors.TextPrimary,
                            fontSize = HealthTypography.bodySize,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Take a photo to estimate calories",
                            color = HealthColors.TextSecondary,
                            fontSize = HealthTypography.subLabelSize
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // View Meal Plan row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToMealPlan() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = "Meal Plan",
                    tint = HealthColors.Accent
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "View Meal Plan",
                    color = HealthColors.TextPrimary,
                    fontSize = HealthTypography.bodySize
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Navigate",
                    tint = HealthColors.TextDim
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // FAB at bottom center
        FloatingActionButton(
            onClick = onNavigateToAddMeal,
            containerColor = HealthColors.Accent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Meal",
                tint = Color.Black
            )
        }
    }
}
