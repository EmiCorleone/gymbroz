package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.AppColor
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.AnimatedMeshGradientBackground
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.GlassCard
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.GradientButton
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.SectionHeader
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.glassBorder
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    onStartWorkout: () -> Unit,
    onOpenProfile: () -> Unit,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val state by dashboardViewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { dashboardViewModel.loadDashboardData() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated gradient background
        AnimatedMeshGradientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ---- GREETING ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "What's up${if (state.profile?.name?.isNotBlank() == true) ", ${state.profile?.name}" else ""}!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = AppColor.TextPrimary
                    )
                    Text(
                        text = LocalDate.now().let { "${it.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth}" },
                        fontSize = 14.sp,
                        color = AppColor.TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                // Profile avatar — glass border with gradient shimmer
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(AppColor.Accent.copy(alpha = 0.2f), Color.Transparent),
                                ),
                                radius = size.maxDimension * 0.8f,
                            )
                        }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            2.dp,
                            Brush.linearGradient(
                                listOf(AppColor.Accent.copy(alpha = 0.6f), AppColor.Primary.copy(alpha = 0.3f))
                            ),
                            CircleShape
                        )
                        .clickable { onOpenProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.profile?.name?.firstOrNull()?.uppercase() ?: "\uD83D\uDCAA",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColor.Accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- STAT CARDS with individual glows ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStatCard(
                    value = state.totalWorkouts.toString(),
                    label = "WORKOUTS",
                    icon = Icons.Default.FitnessCenter,
                    accentColor = AppColor.Accent,
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    value = state.totalReps.toString(),
                    label = "TOTAL REPS",
                    icon = Icons.Default.TrendingUp,
                    accentColor = AppColor.Primary,
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    value = "${state.currentStreak}",
                    label = "DAY STREAK",
                    icon = Icons.Default.Timer,
                    accentColor = AppColor.AccentOrange,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- WEEKLY ACTIVITY ----
            SectionHeader("This Week")
            Spacer(modifier = Modifier.height(8.dp))
            WeeklyActivityRow(activeDays = state.weekActiveDays)

            Spacer(modifier = Modifier.height(24.dp))

            // ---- START WORKOUT BUTTON ----
            GradientButton(
                text = "Start Workout \uD83C\uDFCB\uFE0F",
                onClick = onStartWorkout
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ---- RECENT WORKOUTS ----
            if (state.recentSessions.isNotEmpty()) {
                SectionHeader("Recent Workouts", action = "See All", onAction = { /* TODO */ })
                Spacer(modifier = Modifier.height(8.dp))
                state.recentSessions.forEach { session ->
                    RecentWorkoutCard(session)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                EmptyWorkoutState()
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- PROGRESS CHART ----
            SectionHeader("Progress")
            Spacer(modifier = Modifier.height(8.dp))
            ProgressChartCard(totalWorkouts = state.totalWorkouts)

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// ---- MINI STAT CARD with individual glow ----

@Composable
private fun MiniStatCard(
    value: String,
    label: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, glowColor = accentColor) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AppColor.TextMuted,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---- WEEKLY ACTIVITY ----

@Composable
private fun WeeklyActivityRow(activeDays: List<String>) {
    val today = LocalDate.now()
    val daysOfWeek = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val activeDaySet = activeDays.toSet()

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { date ->
                val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2)
                val isActive = activeDaySet.contains(date.toString())
                val isToday = date == today

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dayName,
                        fontSize = 11.sp,
                        color = if (isToday) AppColor.TextPrimary else AppColor.TextMuted,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .then(
                                if (isActive) Modifier.drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            listOf(AppColor.Accent.copy(alpha = 0.25f), Color.Transparent),
                                        ),
                                        radius = size.maxDimension * 0.9f,
                                    )
                                } else Modifier
                            )
                            .clip(CircleShape)
                            .background(
                                if (isActive) AppColor.Accent
                                else if (isToday) AppColor.Accent.copy(alpha = 0.12f)
                                else Color.White.copy(alpha = 0.06f)
                            )
                            .then(
                                if (isToday && !isActive) Modifier.border(
                                    1.dp,
                                    Brush.linearGradient(
                                        listOf(AppColor.Accent.copy(alpha = 0.5f), AppColor.Accent.copy(alpha = 0.15f))
                                    ),
                                    CircleShape
                                )
                                else if (!isActive) Modifier.border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.08f),
                                    CircleShape
                                )
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive) {
                            Text("\u2713", fontSize = 14.sp, color = AppColor.TextOnAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ---- RECENT WORKOUT CARD ----

@Composable
private fun RecentWorkoutCard(session: com.meta.wearable.dat.externalsampleapps.cameraaccess.data.WorkoutSession) {
    val date = java.time.Instant.ofEpochMilli(session.startTime)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    val timeAgo = when {
        date == LocalDate.now() -> "Today"
        date == LocalDate.now().minusDays(1) -> "Yesterday"
        else -> date.let { "${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth}" }
    }

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColor.Accent.copy(alpha = 0.12f))
                        .glassBorder(RoundedCornerShape(12.dp), isSelected = true),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = AppColor.Accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("Workout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextPrimary)
                    Text("$timeAgo \u2022 ${session.durationMinutes}min", fontSize = 13.sp, color = AppColor.TextMuted)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${session.totalReps}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.Accent)
                Text("reps", fontSize = 11.sp, color = AppColor.TextMuted)
            }
        }
    }
}

// ---- EMPTY STATE ----

@Composable
private fun EmptyWorkoutState() {
    GlassCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("\uD83C\uDFCB\uFE0F", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("No workouts yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Start your first workout and your\nstats will appear here",
                fontSize = 14.sp, color = AppColor.TextMuted, textAlign = TextAlign.Center, lineHeight = 20.sp
            )
        }
    }
}

// ---- PROGRESS CHART CARD ----

@Composable
private fun ProgressChartCard(totalWorkouts: Int) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                Text(
                    "$totalWorkouts workouts",
                    fontSize = 13.sp, color = AppColor.Accent, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            DashboardChartCanvas()
        }
    }
}

@Composable
private fun DashboardChartCanvas() {
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val width = size.width
        val height = size.height

        // Grid lines — glass-style
        for (y in listOf(0.33f, 0.66f)) {
            drawLine(
                Color.White.copy(alpha = 0.06f),
                Offset(0f, height * y),
                Offset(width, height * y),
                strokeWidth = 1f
            )
        }

        val path = Path().apply {
            moveTo(0f, height * 0.8f)
            cubicTo(width * 0.2f, height * 0.6f, width * 0.4f, height * 0.7f, width * 0.5f, height * 0.4f)
            cubicTo(width * 0.6f, height * 0.3f, width * 0.8f, height * 0.35f, width, height * 0.15f)
        }

        // Glass fill under line
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            fillPath,
            Brush.verticalGradient(
                listOf(Color(0xFF00E676).copy(alpha = 0.12f), Color.Transparent),
            ),
        )

        // Glow
        drawPath(
            path,
            Color(0xFF00E676).copy(alpha = 0.15f),
            style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Line
        drawPath(
            path,
            Brush.horizontalGradient(listOf(Color(0xFF6C63FF), Color(0xFF00E676))),
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // End dot with glow
        drawCircle(Color(0xFF00E676).copy(alpha = 0.3f), radius = 12f, center = Offset(width, height * 0.15f))
        drawCircle(Color(0xFF00E676), radius = 6f, center = Offset(width, height * 0.15f))
        drawCircle(AppColor.Background, radius = 3f, center = Offset(width, height * 0.15f))
    }
}
