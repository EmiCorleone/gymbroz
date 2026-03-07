package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.nutrition

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.Meal
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

@Composable
fun MealPlanScreen(
    meals: List<Meal>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HealthColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(HealthSpacing.screenPadding)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = HealthColors.TextPrimary
                )
            }

            Text(
                text = "Meal Plan",
                color = HealthColors.TextPrimary,
                fontSize = HealthTypography.sectionHeaderSize,
                fontWeight = HealthTypography.sectionHeaderWeight
            )

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = { isEditing = !isEditing }) {
                Text(
                    text = if (isEditing) "Done" else "Edit",
                    color = HealthColors.Accent,
                    fontSize = HealthTypography.bodySize
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Motivational card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HealthSpacing.cardRadius),
            colors = CardDefaults.cardColors(containerColor = HealthColors.CardSecondary)
        ) {
            Column(modifier = Modifier.padding(HealthSpacing.md)) {
                Text(
                    text = "This week's plan",
                    color = HealthColors.TextPrimary,
                    fontSize = HealthTypography.bodySize,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Stay consistent with your nutrition goals",
                    color = HealthColors.TextSecondary,
                    fontSize = HealthTypography.subLabelSize
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Meal list grouped by mealType
        val groupedMeals = meals.groupBy { it.mealType }

        groupedMeals.forEach { (mealType, mealsInGroup) ->
            Text(
                text = mealType.name.uppercase(),
                color = HealthColors.TextSecondary,
                fontSize = HealthTypography.subLabelSize,
                fontWeight = HealthTypography.subLabelWeight,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            mealsInGroup.forEach { meal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(HealthSpacing.cardRadius),
                    colors = CardDefaults.cardColors(containerColor = HealthColors.Card)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(HealthSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditing) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove",
                                tint = HealthColors.Heart,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Completed",
                                tint = HealthColors.Accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = meal.time,
                                color = HealthColors.TextDim,
                                fontSize = HealthTypography.subLabelSize,
                                fontWeight = HealthTypography.subLabelWeight
                            )
                            Text(
                                text = meal.name,
                                color = HealthColors.TextPrimary,
                                fontSize = HealthTypography.bodySize,
                                fontWeight = HealthTypography.bodyWeight
                            )
                        }

                        if (isEditing) {
                            IconButton(onClick = { }) {
                                Icon(
                                    imageVector = Icons.Filled.SwapHoriz,
                                    contentDescription = "Swap",
                                    tint = HealthColors.TextSecondary
                                )
                            }
                        } else {
                            Text(
                                text = "${meal.calories} cal",
                                color = HealthColors.TextSecondary,
                                fontSize = HealthTypography.bodySize
                            )
                        }
                    }
                }
            }
        }
    }
}
