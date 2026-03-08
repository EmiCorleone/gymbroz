package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.navigation

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.HealthViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout.PlanGeneratingScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout.WorkoutOverviewScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout.WorkoutPlanScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout.WorkoutDetailScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout.WorkoutLibraryScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout.ActiveWorkoutScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.onboarding.OnboardingRepository
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.AppColor
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.CameraAccessScaffold
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.dashboard.DashboardScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.dashboard.DashboardViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.profile.ProfileScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.SettingsScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.GymBroTheme
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel

enum class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Dashboard("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Workout("Workout", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    Profile("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun MainAppContainer(
    wearablesViewModel: WearablesViewModel,
    onRequestWearablesPermission: suspend (Permission) -> PermissionStatus,
    onRestartOnboarding: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var currentTab by remember { mutableStateOf(BottomNavItem.Dashboard) }
    val dashboardViewModel: DashboardViewModel = viewModel()
    val dashState by dashboardViewModel.state.collectAsStateWithLifecycle()

    // Health/Plan state
    val healthViewModel: HealthViewModel = viewModel()
    val healthState by healthViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Plan sub-navigation state — default to "camera" so glasses app is usable immediately
    var planScreen by remember { mutableStateOf<String>("camera") }
    var selectedWorkoutId by remember { mutableStateOf<String?>(null) }
    var showPlanOverlay by remember { mutableStateOf(false) }

    // Settings sub-navigation
    var showSettings by remember { mutableStateOf(false) }

    // Load mirror photo path eagerly so it's available for image generation
    LaunchedEffect(Unit) {
        val repo = com.meta.wearable.dat.externalsampleapps.cameraaccess.data.WorkoutRepository(context.applicationContext as android.app.Application)
        val profile = repo.getProfile()
        val path = profile?.mirrorPhotoPath
        healthViewModel.mirrorPhotoPath = path
        android.util.Log.d("MainAppContainer", "Mirror photo path loaded: $path")
    }

    // Auto-generate plan on first visit if onboarding data available
    var planGenerated by remember { mutableStateOf(false) }
    LaunchedEffect(currentTab) {
        if (currentTab == BottomNavItem.Workout && !planGenerated && healthState.workoutPlan == null && !healthState.isPlanLoading) {
            // Ensure mirror photo is loaded before generating (handles race condition)
            if (healthViewModel.mirrorPhotoPath == null) {
                val repo = com.meta.wearable.dat.externalsampleapps.cameraaccess.data.WorkoutRepository(context.applicationContext as android.app.Application)
                val profile = repo.getProfile()
                healthViewModel.mirrorPhotoPath = profile?.mirrorPhotoPath
                android.util.Log.d("MainAppContainer", "Mirror photo path (re-loaded before plan gen): ${healthViewModel.mirrorPhotoPath}")
            }
            val onboardingRepo = OnboardingRepository(context)
            val data = onboardingRepo.load()
            if (data.gender.isNotBlank()) {
                val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
                healthViewModel.generateWorkoutPlan(data.gender, data.workoutFrequency, deviceId)
                planGenerated = true
            }
        }
    }

    GymBroTheme {
        Scaffold(
            containerColor = AppColor.Background,
            bottomBar = {
                GlassBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                )
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(initialAlpha = 0.8f) togetherWith fadeOut(targetAlpha = 0.8f)
                },
                modifier = Modifier.padding(innerPadding),
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    BottomNavItem.Dashboard -> {
                        DashboardScreen(
                            onStartWorkout = { currentTab = BottomNavItem.Workout },
                            onOpenProfile = { currentTab = BottomNavItem.Profile },
                            dashboardViewModel = dashboardViewModel
                        )
                    }
                    BottomNavItem.Workout -> {
                        // Glasses app is ALWAYS the base layer — fully interactive
                        Box(modifier = Modifier.fillMaxSize()) {
                            CameraAccessScaffold(
                                viewModel = wearablesViewModel,
                                onRequestWearablesPermission = onRequestWearablesPermission,
                                onRestartOnboarding = onRestartOnboarding,
                            )

                            // Active workout overlay — minimal, doesn't block glasses
                            if (planScreen == "active" && selectedWorkoutId != null) {
                                val workout = healthState.workouts.find { it.id == selectedWorkoutId }
                                if (workout != null) {
                                    ActiveWorkoutScreen(
                                        workout = workout,
                                        exerciseGuides = healthState.exerciseGuides,
                                        onComplete = {
                                            planScreen = "camera"
                                            showPlanOverlay = false
                                        },
                                        onDismiss = {
                                            planScreen = "camera"
                                            showPlanOverlay = false
                                        },
                                        onRequestGuide = { name ->
                                            healthViewModel.generateSingleExerciseGuide(name)
                                        }
                                    )
                                }
                            }

                            // Plan overlay — upper portion, leaves bottom buttons accessible
                            AnimatedVisibility(
                                visible = showPlanOverlay && planScreen != "active",
                                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                                modifier = Modifier.align(Alignment.TopCenter)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.55f)
                                        .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black.copy(alpha = 0.85f))
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                                            ),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                ) {
                                    // Close button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.1f))
                                            .clickable { showPlanOverlay = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }

                                    // Plan content
                                    Box(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                                        when {
                                            healthState.isPlanLoading || healthState.planError != null -> {
                                                PlanGeneratingScreen(
                                                    isLoading = healthState.isPlanLoading,
                                                    error = healthState.planError,
                                                    onRetry = {
                                                        val onboardingRepo = OnboardingRepository(context)
                                                        val data = onboardingRepo.load()
                                                        if (data.gender.isNotBlank()) {
                                                            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
                                                            healthViewModel.generateWorkoutPlan(data.gender, data.workoutFrequency, deviceId)
                                                        }
                                                    }
                                                )
                                            }
                                            else -> {
                                                val plan = healthState.workoutPlan
                                                when {
                                                    planScreen == "overview" || planScreen == "camera" || plan == null -> {
                                                        WorkoutOverviewScreen(
                                                            workoutPlan = plan,
                                                            onNavigateToLibrary = { planScreen = "library" },
                                                            onNavigateToPlan = {
                                                                if (plan != null) planScreen = "plan"
                                                                else {
                                                                    val onboardingRepo = OnboardingRepository(context)
                                                                    val data = onboardingRepo.load()
                                                                    if (data.gender.isNotBlank()) {
                                                                        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
                                                                        healthViewModel.generateWorkoutPlan(data.gender, data.workoutFrequency, deviceId)
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                                                    planScreen == "plan" && plan != null -> {
                                                        WorkoutPlanScreen(
                                                            plan = plan,
                                                            onStartWorkout = { workoutId ->
                                                                selectedWorkoutId = workoutId
                                                                planScreen = "active"
                                                                showPlanOverlay = false
                                                            },
                                                            onRegenerate = {
                                                                val onboardingRepo = OnboardingRepository(context)
                                                                val data = onboardingRepo.load()
                                                                if (data.gender.isNotBlank()) {
                                                                    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
                                                                    healthViewModel.generateWorkoutPlan(data.gender, data.workoutFrequency, deviceId)
                                                                }
                                                                planScreen = "overview"
                                                            },
                                                            onBack = { planScreen = "overview" }
                                                        )
                                                    }
                                                    planScreen == "library" -> {
                                                        WorkoutLibraryScreen(
                                                            workouts = healthState.workouts,
                                                            onWorkoutClick = { workoutId ->
                                                                selectedWorkoutId = workoutId
                                                                planScreen = "detail"
                                                            },
                                                            onBack = { planScreen = "overview" }
                                                        )
                                                    }
                                                    planScreen == "detail" && selectedWorkoutId != null -> {
                                                        val workout = healthState.workouts.find { it.id == selectedWorkoutId }
                                                        if (workout != null) {
                                                            WorkoutDetailScreen(
                                                                workout = workout,
                                                                onStartWorkout = {
                                                                    planScreen = "active"
                                                                    showPlanOverlay = false
                                                                },
                                                                onBack = { planScreen = "overview" }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Floating toggle button — show/hide plan overlay (top-right, away from glasses buttons)
                            if (!showPlanOverlay && planScreen != "active") {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .statusBarsPadding()
                                        .padding(end = 12.dp, top = 8.dp)
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(AppColor.Accent, AppColor.Accent.copy(alpha = 0.7f))
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                        .clickable { showPlanOverlay = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.FitnessCenter,
                                        contentDescription = "Show workout plan",
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                    BottomNavItem.Profile -> {
                        if (showSettings) {
                            SettingsScreen(
                                onBack = {
                                    showSettings = false
                                    dashboardViewModel.loadDashboardData()
                                },
                                onRestartOnboarding = onRestartOnboarding,
                                onProfilePhotoUpdated = {
                                    dashboardViewModel.loadDashboardData()
                                }
                            )
                        } else {
                            ProfileScreen(
                                profile = dashState.profile,
                                totalWorkouts = dashState.totalWorkouts,
                                totalReps = dashState.totalReps,
                                onOpenSettings = { showSettings = true },
                                onRestartOnboarding = onRestartOnboarding,
                                onLogout = onLogout
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassBottomBar(
    currentTab: BottomNavItem,
    onTabSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(barShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.03f))
                ),
                shape = barShape,
            )
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomNavItem.entries.forEach { item ->
                val isSelected = currentTab == item
                GlassNavItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onTabSelected(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GlassNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .clip(pillShape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(AppColor.Accent.copy(alpha = 0.12f), pillShape)
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    AppColor.Accent.copy(alpha = 0.3f),
                                    AppColor.Accent.copy(alpha = 0.08f),
                                )
                            ),
                            pillShape,
                        )
                } else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            modifier = Modifier
                .size(24.dp)
                .then(
                    if (isSelected) Modifier.drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(AppColor.Accent.copy(alpha = 0.3f), Color.Transparent),
                            ),
                            radius = size.maxDimension,
                        )
                    } else Modifier
                ),
            tint = if (isSelected) AppColor.Accent else AppColor.TextMuted,
        )
        Text(
            text = item.label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) AppColor.Accent else AppColor.TextMuted,
        )
    }
}
