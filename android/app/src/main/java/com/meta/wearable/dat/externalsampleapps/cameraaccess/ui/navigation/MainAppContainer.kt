package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
