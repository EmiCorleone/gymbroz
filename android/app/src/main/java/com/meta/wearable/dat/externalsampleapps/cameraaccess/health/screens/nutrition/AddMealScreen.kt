package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.Meal
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.MealType
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun AddMealScreen(
    onSave: (Meal) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var proteins by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MealType.Breakfast) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = HealthColors.Card,
        unfocusedContainerColor = HealthColors.Card,
        focusedBorderColor = HealthColors.Accent,
        unfocusedBorderColor = HealthColors.CardSecondary,
        focusedTextColor = HealthColors.TextPrimary,
        unfocusedTextColor = HealthColors.TextPrimary,
        cursorColor = HealthColors.Accent,
        focusedLabelColor = HealthColors.Accent,
        unfocusedLabelColor = HealthColors.TextSecondary
    )

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
                text = "Add Meal",
                color = HealthColors.TextPrimary,
                fontSize = HealthTypography.sectionHeaderSize,
                fontWeight = HealthTypography.sectionHeaderWeight
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Food Name
        Text(
            text = "Food Name",
            color = HealthColors.TextSecondary,
            fontSize = HealthTypography.subLabelSize,
            fontWeight = HealthTypography.subLabelWeight
        )
        OutlinedTextField(
            value = foodName,
            onValueChange = { foodName = it },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors,
            shape = RoundedCornerShape(HealthSpacing.cardRadius)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Calories
        Text(
            text = "Calories",
            color = HealthColors.TextSecondary,
            fontSize = HealthTypography.subLabelSize,
            fontWeight = HealthTypography.subLabelWeight
        )
        OutlinedTextField(
            value = calories,
            onValueChange = { calories = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = textFieldColors,
            shape = RoundedCornerShape(HealthSpacing.cardRadius)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Macros
        Text(
            text = "Macros",
            color = HealthColors.TextSecondary,
            fontSize = HealthTypography.subLabelSize,
            fontWeight = HealthTypography.subLabelWeight
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = carbs,
                onValueChange = { carbs = it },
                label = { Text("Carbs") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldColors,
                shape = RoundedCornerShape(HealthSpacing.cardRadius)
            )
            OutlinedTextField(
                value = proteins,
                onValueChange = { proteins = it },
                label = { Text("Proteins") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldColors,
                shape = RoundedCornerShape(HealthSpacing.cardRadius)
            )
            OutlinedTextField(
                value = fats,
                onValueChange = { fats = it },
                label = { Text("Fats") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = textFieldColors,
                shape = RoundedCornerShape(HealthSpacing.cardRadius)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Meal Type
        Text(
            text = "Meal Type",
            color = HealthColors.TextSecondary,
            fontSize = HealthTypography.subLabelSize,
            fontWeight = HealthTypography.subLabelWeight
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MealType.values().forEach { type ->
                val isSelected = selectedType == type
                Button(
                    onClick = { selectedType = type },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(HealthSpacing.cardRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) HealthColors.Accent else HealthColors.Card,
                        contentColor = if (isSelected) Color.Black else HealthColors.TextSecondary
                    )
                ) {
                    Text(
                        text = type.name,
                        fontSize = HealthTypography.subLabelSize,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Save Meal button
        Button(
            onClick = {
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val currentTime = timeFormat.format(Date())
                val meal = Meal(
                    id = UUID.randomUUID().toString(),
                    name = foodName,
                    calories = calories.toIntOrNull() ?: 0,
                    carbs = carbs.toIntOrNull() ?: 0,
                    proteins = proteins.toIntOrNull() ?: 0,
                    fats = fats.toIntOrNull() ?: 0,
                    mealType = selectedType,
                    time = currentTime,
                    date = "2026-03-07"
                )
                onSave(meal)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(HealthSpacing.cardRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = HealthColors.Accent,
                contentColor = Color.Black
            )
        ) {
            Text(
                text = "Save Meal",
                fontWeight = FontWeight.Bold,
                fontSize = HealthTypography.bodySize
            )
        }
    }
}
