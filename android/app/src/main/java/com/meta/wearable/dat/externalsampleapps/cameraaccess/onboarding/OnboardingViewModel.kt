package com.meta.wearable.dat.externalsampleapps.cameraaccess.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.UserProfile
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingState(
    val name: String = "",
    val gender: String = "",
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val fitnessGoal: String = "",
    val experienceLevel: String = "",
    val weeklyWorkouts: String = "",
    val mirrorPhotoPath: String? = null
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(application)

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _isOnboardingComplete = MutableStateFlow<Boolean?>(null) // null = loading
    val isOnboardingComplete: StateFlow<Boolean?> = _isOnboardingComplete.asStateFlow()

    init {
        viewModelScope.launch {
            _isOnboardingComplete.value = repository.isOnboardingComplete()
        }
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun updateGender(gender: String) {
        _state.value = _state.value.copy(gender = gender)
    }

    fun updateAge(age: String) {
        _state.value = _state.value.copy(age = age)
    }

    fun updateHeight(height: String) {
        _state.value = _state.value.copy(heightCm = height)
    }

    fun updateWeight(weight: String) {
        _state.value = _state.value.copy(weightKg = weight)
    }

    fun updateFitnessGoal(goal: String) {
        _state.value = _state.value.copy(fitnessGoal = goal)
    }

    fun updateExperienceLevel(level: String) {
        _state.value = _state.value.copy(experienceLevel = level)
    }

    fun updateWeeklyWorkouts(workouts: String) {
        _state.value = _state.value.copy(weeklyWorkouts = workouts)
    }

    fun updateMirrorPhotoPath(path: String) {
        _state.value = _state.value.copy(mirrorPhotoPath = path)
    }

    fun onLogout() {
        _state.value = OnboardingState()
        _isOnboardingComplete.value = false
    }

    fun saveProfile(onComplete: () -> Unit) {
        viewModelScope.launch {
            val s = _state.value
            val profile = UserProfile(
                name = s.name,
                gender = s.gender,
                age = s.age.toIntOrNull() ?: 0,
                heightCm = s.heightCm.toIntOrNull() ?: 0,
                weightKg = s.weightKg.toIntOrNull() ?: 0,
                fitnessGoal = s.fitnessGoal,
                experienceLevel = s.experienceLevel,
                weeklyWorkouts = s.weeklyWorkouts,
                mirrorPhotoPath = s.mirrorPhotoPath
            )
            repository.saveProfile(profile)
            _isOnboardingComplete.value = true
            onComplete()
        }
    }

    fun restartOnboarding() {
        viewModelScope.launch {
            repository.clearProfile()
            _state.value = OnboardingState()
            _isOnboardingComplete.value = false
        }
    }
}
