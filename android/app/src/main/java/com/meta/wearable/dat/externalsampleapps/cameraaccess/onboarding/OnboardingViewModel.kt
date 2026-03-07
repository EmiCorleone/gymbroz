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
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.postgrest

data class OnboardingState(
    val email: String = "",
    val password: String = "",
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

    fun updateEmail(email: String) {
        _state.value = _state.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _state.value = _state.value.copy(password = password)
    }

    fun authenticateWithEmail(isSignUp: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val auth = com.meta.wearable.dat.externalsampleapps.cameraaccess.data.GymBroSupabaseClient.client.auth
                val emailValue = _state.value.email
                val passwordValue = _state.value.password
                
                if (isSignUp) {
                    auth.signUpWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
                        email = emailValue
                        password = passwordValue
                    }
                } else {
                    auth.signInWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
                        email = emailValue
                        password = passwordValue
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Authentication failed")
            }
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
            try {
                // Perform anonymous login with Supabase
                val auth = com.meta.wearable.dat.externalsampleapps.cameraaccess.data.GymBroSupabaseClient.client.auth
                if (auth.currentSessionOrNull() == null) {
                    auth.signInAnonymously()
                }

                val s = _state.value
                var finalPhotoPath = s.mirrorPhotoPath

                // Upload the mirror photo to Supabase Storage if it exists
                if (finalPhotoPath != null) {
                    val file = java.io.File(finalPhotoPath)
                    if (file.exists()) {
                        val userId = auth.currentUserOrNull()?.id ?: "unknown"
                        // Upload to gymbro_assets/{userId}/mirror_photo.jpg
                        val storage = com.meta.wearable.dat.externalsampleapps.cameraaccess.data.GymBroSupabaseClient.client.storage
                        val bucket = storage.from("gymbro_assets")
                        val remotePath = "$userId/mirror_photo_${System.currentTimeMillis()}.jpg"
                        
                        // Use put to upload the bytes
                        bucket.upload(remotePath, file.readBytes(), upsert = true)
                        
                        // Get the public URL to save in the database
                        finalPhotoPath = bucket.publicUrl(remotePath)
                    }
                }

                val profile = UserProfile(
                    name = s.name,
                    gender = s.gender,
                    age = s.age.toIntOrNull() ?: 0,
                    heightCm = s.heightCm.toIntOrNull() ?: 0,
                    weightKg = s.weightKg.toIntOrNull() ?: 0,
                    fitnessGoal = s.fitnessGoal,
                    experienceLevel = s.experienceLevel,
                    weeklyWorkouts = s.weeklyWorkouts,
                    mirrorPhotoPath = finalPhotoPath
                )
                
                // Save locally first for offline support and immediate UI updates
                repository.saveProfile(profile)

                // Save gender + frequency for workout plan generation
                OnboardingRepository(getApplication()).save(s.gender, s.weeklyWorkouts)

                // Push to Supabase Postgres
                val postgrest = com.meta.wearable.dat.externalsampleapps.cameraaccess.data.GymBroSupabaseClient.client.postgrest
                val userId = auth.currentUserOrNull()?.id ?: return@launch
                
                // Construct a data class for the Supabase insert that matches the columns
                @kotlinx.serialization.Serializable
                data class SupabaseProfile(
                    val id: String,
                    val name: String,
                    val gender: String,
                    val age: Int,
                    val height_cm: Int,
                    val weight_kg: Int,
                    val fitness_goal: String,
                    val experience_level: String,
                    val weekly_workouts: String,
                    val mirror_photo_url: String?
                )
                
                val supabaseProfile = SupabaseProfile(
                    id = userId,
                    name = profile.name,
                    gender = profile.gender,
                    age = profile.age,
                    height_cm = profile.heightCm,
                    weight_kg = profile.weightKg,
                    fitness_goal = profile.fitnessGoal,
                    experience_level = profile.experienceLevel,
                    weekly_workouts = profile.weeklyWorkouts,
                    mirror_photo_url = profile.mirrorPhotoPath
                )
                
                postgrest.from("user_profiles").upsert(supabaseProfile)
                
                _isOnboardingComplete.value = true
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                // In a production app, we would show a toast/snackbar here.
                // For now, we still save the profile locally so the user isn't totally blocked.
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
                OnboardingRepository(getApplication()).save(s.gender, s.weeklyWorkouts)
                _isOnboardingComplete.value = true
                onComplete()
            }
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
