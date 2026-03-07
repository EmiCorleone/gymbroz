package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.HealthViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.dashboard.DashboardScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.nutrition.AddMealScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.nutrition.MealPlanScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.nutrition.NutritionOverviewScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.profile.AppointmentsScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.profile.MedicalScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.profile.MedicationsScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.profile.PersonalInfoScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.profile.ProfileScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.profile.VitalsLogScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.wellness.JournalScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.wellness.MeditationScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.wellness.SleepTrackerScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.wellness.StressCheckinScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout.ActiveWorkoutScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout.WorkoutDetailScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout.WorkoutLibraryScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout.WorkoutOverviewScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout.WorkoutPlanScreen
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

sealed class HealthRoute(val route: String) {
    data object Dashboard : HealthRoute("dashboard")
    data object Community : HealthRoute("community")
    data object WorkoutOverview : HealthRoute("workout_overview")
    data object WorkoutLibrary : HealthRoute("workout_library")
    data object WorkoutDetail : HealthRoute("workout_detail/{workoutId}") {
        fun createRoute(workoutId: String) = "workout_detail/$workoutId"
    }
    data object ActiveWorkout : HealthRoute("active_workout/{workoutId}") {
        fun createRoute(workoutId: String) = "active_workout/$workoutId"
    }
    data object NutritionOverview : HealthRoute("nutrition_overview")
    data object MealPlan : HealthRoute("meal_plan")
    data object AddMeal : HealthRoute("add_meal")
    data object SleepTracker : HealthRoute("sleep_tracker")
    data object StressCheckin : HealthRoute("stress_checkin")
    data object Meditation : HealthRoute("meditation")
    data object Journal : HealthRoute("journal")
    data object Profile : HealthRoute("profile")
    data object PersonalInfo : HealthRoute("personal_info")
    data object HealthGoals : HealthRoute("health_goals")
    data object Medical : HealthRoute("medical")
    data object Vitals : HealthRoute("vitals")
    data object Medications : HealthRoute("medications")
    data object Appointments : HealthRoute("appointments")
    data object WorkoutPlan : HealthRoute("workout_plan")
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, HealthRoute.Dashboard.route),
    BottomNavItem("Workout", Icons.Filled.FitnessCenter, HealthRoute.WorkoutOverview.route),
    BottomNavItem("Nutrition", Icons.Filled.Restaurant, HealthRoute.NutritionOverview.route),
    BottomNavItem("Profile", Icons.Filled.Person, HealthRoute.Profile.route),
)

@Composable
fun HealthNavGraph(
    modifier: Modifier = Modifier,
    viewModel: HealthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = HealthColors.Background,
        bottomBar = {
            HealthBottomBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HealthRoute.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home tab
            composable(HealthRoute.Dashboard.route) {
                DashboardScreen(
                    uiState = uiState,
                    onNavigateToCommunity = { navController.navigate(HealthRoute.Community.route) },
                    onAddMeal = { navController.navigate(HealthRoute.AddMeal.route) }
                )
            }

            // Workout tab
            composable(HealthRoute.WorkoutOverview.route) {
                WorkoutOverviewScreen(
                    workoutPlan = uiState.workoutPlan,
                    onNavigateToLibrary = { navController.navigate(HealthRoute.WorkoutLibrary.route) },
                    onNavigateToPlan = { navController.navigate(HealthRoute.WorkoutPlan.route) }
                )
            }
            composable(HealthRoute.WorkoutPlan.route) {
                val plan = uiState.workoutPlan
                if (plan != null) {
                    WorkoutPlanScreen(
                        plan = plan,
                        onStartWorkout = { workoutId ->
                            navController.navigate(HealthRoute.ActiveWorkout.createRoute(workoutId))
                        },
                        onRegenerate = {
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(HealthRoute.WorkoutLibrary.route) {
                WorkoutLibraryScreen(
                    workouts = uiState.workouts,
                    onWorkoutClick = { workoutId ->
                        navController.navigate(HealthRoute.WorkoutDetail.createRoute(workoutId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(HealthRoute.WorkoutDetail.route) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
                val workout = uiState.workouts.find { it.id == workoutId } ?: return@composable
                WorkoutDetailScreen(
                    workout = workout,
                    onStartWorkout = {
                        navController.navigate(HealthRoute.ActiveWorkout.createRoute(workoutId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(HealthRoute.ActiveWorkout.route) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
                val workout = uiState.workouts.find { it.id == workoutId } ?: return@composable
                ActiveWorkoutScreen(
                    workout = workout,
                    onComplete = { navController.popBackStack() }
                )
            }

            // Nutrition tab
            composable(HealthRoute.NutritionOverview.route) {
                NutritionOverviewScreen(
                    nutrition = uiState.nutrition,
                    selectedDate = uiState.selectedDate,
                    onDateSelected = { viewModel.selectDate(it) },
                    onNavigateToMealPlan = { navController.navigate(HealthRoute.MealPlan.route) },
                    onNavigateToAddMeal = { navController.navigate(HealthRoute.AddMeal.route) }
                )
            }
            composable(HealthRoute.MealPlan.route) {
                MealPlanScreen(
                    meals = uiState.nutrition.meals,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(HealthRoute.AddMeal.route) {
                AddMealScreen(
                    onSave = { meal ->
                        viewModel.addMeal(meal)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Wellness screens
            composable(HealthRoute.SleepTracker.route) {
                SleepTrackerScreen(onBack = { navController.popBackStack() })
            }
            composable(HealthRoute.StressCheckin.route) {
                StressCheckinScreen(onBack = { navController.popBackStack() })
            }
            composable(HealthRoute.Meditation.route) {
                MeditationScreen(onBack = { navController.popBackStack() })
            }
            composable(HealthRoute.Journal.route) {
                JournalScreen(onBack = { navController.popBackStack() })
            }

            // Profile tab & sub-screens
            composable(HealthRoute.Profile.route) {
                ProfileScreen(
                    user = uiState.user,
                    onNavigateTo = { route -> navController.navigate(route) }
                )
            }
            composable(HealthRoute.PersonalInfo.route) {
                PersonalInfoScreen(onBack = { navController.popBackStack() })
            }
            composable(HealthRoute.Medical.route) {
                MedicalScreen(onBack = { navController.popBackStack() })
            }
            composable(HealthRoute.Vitals.route) {
                VitalsLogScreen(onBack = { navController.popBackStack() })
            }
            composable(HealthRoute.Medications.route) {
                MedicationsScreen(onBack = { navController.popBackStack() })
            }
            composable(HealthRoute.Appointments.route) {
                AppointmentsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun HealthBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = HealthColors.Surface,
        contentColor = HealthColors.TextPrimary
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                icon = {
                    Icon(item.icon, contentDescription = item.label)
                },
                label = {
                    Text(
                        item.label,
                        fontSize = HealthTypography.tabLabelSize,
                        fontWeight = HealthTypography.tabLabelWeight
                    )
                },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = HealthColors.Accent,
                    selectedTextColor = HealthColors.Accent,
                    unselectedIconColor = HealthColors.TextDim,
                    unselectedTextColor = HealthColors.TextDim,
                    indicatorColor = HealthColors.Accent.copy(alpha = 0.12f)
                )
            )
        }
    }
}
