package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.UserProfile
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.WorkoutRepository
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardState(
    val profile: UserProfile? = null,
    val totalWorkouts: Int = 0,
    val totalReps: Int = 0,
    val totalMinutes: Int = 0,
    val currentStreak: Int = 0,
    val recentSessions: List<WorkoutSession> = emptyList(),
    val weekActiveDays: List<String> = emptyList(), // "YYYY-MM-DD" format
    val isLoading: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(application)

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            val profile = repository.getProfile()
            var totalWorkouts = repository.getTotalSessionCount()
            var totalReps = repository.getTotalReps()
            var totalMinutes = repository.getTotalMinutes()
            var streak = repository.getCurrentStreak()
            var recentSessions = repository.getRecentSessions(5)
            
            val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            var weekDays = repository.getActiveDaysList(sevenDaysAgo)

            // If there's no data, populate with some realistic mock data
            // as requested, max streak of 2 days
            if (totalWorkouts == 0) {
                totalWorkouts = 12
                totalReps = 3450
                totalMinutes = 540
                streak = 2 // max 2 days as requested
                
                // create realistic looking active days including today and yesterday for the streak
                val today = java.time.LocalDate.now()
                val yesterday = today.minusDays(1)
                val threeDaysAgo = today.minusDays(3)
                val fiveDaysAgo = today.minusDays(5)
                
                weekDays = listOf(
                    today.toString(),
                    yesterday.toString(),
                    threeDaysAgo.toString(),
                    fiveDaysAgo.toString()
                )
                
                // Mocks some recent sessions
                val mockSession1 = WorkoutSession(
                    id = 1,
                    startTime = System.currentTimeMillis() - (2 * 60 * 60 * 1000), // 2 hours ago
                    endTime = System.currentTimeMillis() - (1 * 60 * 60 * 1000),
                    durationMinutes = 60,
                    totalReps = 320,
                    isPhoneMode = false
                )
                
                val mockSession2 = WorkoutSession(
                    id = 2,
                    startTime = System.currentTimeMillis() - (26 * 60 * 60 * 1000), // Yesterday
                    endTime = System.currentTimeMillis() - (25 * 60 * 60 * 1000),
                    durationMinutes = 45,
                    totalReps = 250,
                    isPhoneMode = false
                )

                val mockSession3 = WorkoutSession(
                    id = 3,
                    startTime = System.currentTimeMillis() - (74 * 60 * 60 * 1000), // 3 days ago
                    endTime = System.currentTimeMillis() - (73 * 60 * 60 * 1000),
                    durationMinutes = 55,
                    totalReps = 280,
                    isPhoneMode = false
                )

                recentSessions = listOf(mockSession1, mockSession2, mockSession3)
            }

            _state.value = DashboardState(
                profile = profile,
                totalWorkouts = totalWorkouts,
                totalReps = totalReps,
                totalMinutes = totalMinutes,
                currentStreak = streak,
                recentSessions = recentSessions,
                weekActiveDays = weekDays,
                isLoading = false
            )
        }
    }
}
