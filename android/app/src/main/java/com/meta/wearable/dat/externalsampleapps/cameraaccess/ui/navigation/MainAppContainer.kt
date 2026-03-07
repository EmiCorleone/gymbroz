package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.AppColor
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.CameraAccessScaffold
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.dashboard.DashboardScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.dashboard.DashboardViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.profile.ProfileScreen
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
    onRequestWearablesPermission: suspend (Permission) -> PermissionStatus
) {
    var currentTab by remember { mutableStateOf(BottomNavItem.Dashboard) }
    val dashboardViewModel: DashboardViewModel = viewModel()
    val dashState by dashboardViewModel.state.collectAsStateWithLifecycle()

    GymBroTheme {
        Scaffold(
            containerColor = AppColor.Background,
            bottomBar = {
                NavigationBar(
                    containerColor = AppColor.Surface.copy(alpha = 0.95f),
                    contentColor = AppColor.TextPrimary,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(80.dp)
                ) {
                    BottomNavItem.entries.forEach { item ->
                        val isSelected = currentTab == item
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = item },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppColor.Accent,
                                selectedTextColor = AppColor.Accent,
                                unselectedIconColor = AppColor.TextMuted,
                                unselectedTextColor = AppColor.TextMuted,
                                indicatorColor = AppColor.Accent.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
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
                        // Reuse the existing CameraAccessScaffold for the workout tab
                        CameraAccessScaffold(
                            viewModel = wearablesViewModel,
                            onRequestWearablesPermission = onRequestWearablesPermission
                        )
                    }
                    BottomNavItem.Profile -> {
                        ProfileScreen(
                            profile = dashState.profile,
                            totalWorkouts = dashState.totalWorkouts,
                            totalReps = dashState.totalReps,
                            onOpenSettings = { /* TODO: open settings */ }
                        )
                    }
                }
            }
        }
    }
}
