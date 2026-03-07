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
            val totalWorkouts = repository.getTotalSessionCount()
            val totalReps = repository.getTotalReps()
            val totalMinutes = repository.getTotalMinutes()
            val streak = repository.getCurrentStreak()
            val recentSessions = repository.getRecentSessions(5)
            val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            val weekDays = repository.getActiveDaysList(sevenDaysAgo)

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
