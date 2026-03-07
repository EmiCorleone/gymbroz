package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun WeekCalendar(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val today = remember { LocalDate.now() }
    val todayStr = remember { today.format(formatter) }
    val weekDates = remember {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        (0L..6L).map { monday.plusDays(it) }
    }
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekDates.forEachIndexed { index, date ->
            val dateStr = date.format(formatter)
            val isSelected = dateStr == selectedDate
            val isToday = dateStr == todayStr

            val shape = RoundedCornerShape(HealthSpacing.sm)

            val bgModifier = when {
                isSelected -> Modifier
                    .clip(shape)
                    .background(HealthColors.Accent)
                isToday -> Modifier
                    .clip(shape)
                    .border(1.dp, HealthColors.Accent, shape)
                else -> Modifier.clip(shape)
            }

            Column(
                modifier = Modifier
                    .width(40.dp)
                    .then(bgModifier)
                    .clickable { onDateSelected(dateStr) }
                    .padding(vertical = HealthSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayLabels[index],
                    color = if (isSelected) Color.Black else HealthColors.TextDim,
                    fontSize = HealthTypography.subLabelSize,
                    fontWeight = HealthTypography.subLabelWeight
                )

                Spacer(modifier = Modifier.height(HealthSpacing.xs))

                Text(
                    text = "${date.dayOfMonth}",
                    color = if (isSelected) Color.Black else HealthColors.TextPrimary,
                    fontSize = HealthTypography.bodySize,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else HealthTypography.bodyWeight
                )
            }
        }
    }
}
