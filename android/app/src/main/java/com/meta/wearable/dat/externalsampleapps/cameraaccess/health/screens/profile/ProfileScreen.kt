package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.UserProfile
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

@Composable
fun ProfileScreen(
    user: UserProfile,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val menuItems = listOf(
        Triple("Personal Info", Icons.Filled.Person, "personal_info"),
        Triple("Health Goals", Icons.Filled.Flag, "health_goals"),
        Triple("Medical", Icons.Filled.LocalHospital, "medical"),
        Triple("Vitals Log", Icons.Filled.MonitorHeart, "vitals"),
        Triple("Medications", Icons.Filled.Medication, "medications"),
        Triple("Appointments", Icons.Filled.CalendarMonth, "appointments")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HealthColors.Background)
            .padding(HealthSpacing.screenPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(HealthColors.Accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.name.first().uppercase(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = HealthColors.Background
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Name
        Text(
            text = user.name,
            fontSize = HealthTypography.sectionHeaderSize,
            fontWeight = HealthTypography.sectionHeaderWeight,
            color = HealthColors.TextPrimary
        )

        // Age
        Text(
            text = "Age ${user.age}",
            fontSize = HealthTypography.bodySize,
            color = HealthColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stat pills row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatPill(text = user.height)
            StatPill(text = "${user.weight} lbs")
            StatPill(text = user.bloodType)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Menu items
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            menuItems.forEach { (label, icon, route) ->
                ProfileMenuItem(
                    label = label,
                    icon = icon,
                    onClick = { onNavigateTo(route) }
                )
            }
        }
    }
}

@Composable
private fun StatPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(HealthSpacing.cardRadius))
            .background(HealthColors.Card)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = HealthTypography.bodySize,
            color = HealthColors.TextPrimary
        )
    }
}

@Composable
private fun ProfileMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HealthSpacing.cardRadius))
            .background(HealthColors.Card)
            .clickable { onClick() }
            .padding(HealthSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = HealthColors.TextPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = HealthTypography.bodySize,
            fontWeight = FontWeight.SemiBold,
            color = HealthColors.TextPrimary
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = HealthColors.TextDim
        )
    }
}
