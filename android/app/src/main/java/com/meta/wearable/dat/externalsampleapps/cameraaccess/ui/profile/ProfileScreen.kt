package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.UserProfile
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.AppColor
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.GlassCard
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.SectionHeader

@Composable
fun ProfileScreen(
    profile: UserProfile?,
    totalWorkouts: Int,
    totalReps: Int,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColor.Background)
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppColor.TextMuted)
            }
        }
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape).background(AppColor.CardBackground)
                .border(3.dp, AppColor.Accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val photoBitmap = remember(profile?.mirrorPhotoPath) {
                profile?.mirrorPhotoPath?.let { path ->
                    try { BitmapFactory.decodeFile(path)?.asImageBitmap() } catch (e: Exception) { null }
                }
            }
            if (photoBitmap != null) {
                Image(bitmap = photoBitmap, contentDescription = "Profile photo", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Text(text = profile?.name?.firstOrNull()?.uppercase() ?: "?", fontSize = 36.sp, fontWeight = FontWeight.Black, color = AppColor.Accent)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = profile?.name ?: "Gymbro User", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AppColor.TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        val goalDisplay = when (profile?.fitnessGoal) {
            "build_muscle" -> "Building Muscle"
            "lose_weight" -> "Losing Weight"
            "stay_active" -> "Staying Active"
            "improve_health" -> "Improving Health"
            else -> "Fitness Enthusiast"
        }
        Text(text = goalDisplay, fontSize = 14.sp, color = AppColor.Accent, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ProfileStat(value = totalWorkouts.toString(), label = "Workouts")
            ProfileStat(value = totalReps.toString(), label = "Total Reps")
            val level = when (profile?.experienceLevel) {
                "beginner" -> "Beginner"
                "intermediate" -> "Intermediate"
                "advanced" -> "Advanced"
                else -> "-"
            }
            ProfileStat(value = level, label = "Level")
        }
        Spacer(modifier = Modifier.height(32.dp))
        SectionHeader("Profile Details")
        Spacer(modifier = Modifier.height(8.dp))
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (profile != null) {
                    ProfileInfoRow("Gender", profile.gender)
                    if (profile.age > 0) ProfileInfoRow("Age", "${profile.age} years")
                    if (profile.heightCm > 0) ProfileInfoRow("Height", "${profile.heightCm} cm")
                    if (profile.weightKg > 0) ProfileInfoRow("Weight", "${profile.weightKg} kg")
                    ProfileInfoRow("Weekly Goal", "${profile.weeklyWorkouts} workouts")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        GlassCard(onClick = onOpenSettings) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = AppColor.TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Settings", fontSize = 16.sp, color = AppColor.TextPrimary, fontWeight = FontWeight.Medium)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppColor.TextMuted, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
        Text(label, fontSize = 12.sp, color = AppColor.TextMuted)
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = AppColor.TextMuted)
        Text(value, fontSize = 14.sp, color = AppColor.TextPrimary, fontWeight = FontWeight.Medium)
    }
}
