package com.meta.wearable.dat.externalsampleapps.cameraaccess.onboarding

import android.content.Context
import android.content.SharedPreferences

data class OnboardingData(
    val gender: String = "",
    val workoutFrequency: String = "",
    val isComplete: Boolean = false
)

class OnboardingRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gymbro_onboarding", Context.MODE_PRIVATE)

    fun save(gender: String, workoutFrequency: String) {
        prefs.edit()
            .putString("gender", gender)
            .putString("workoutFrequency", workoutFrequency)
            .putBoolean("onboardingComplete", true)
            .apply()
    }

    fun load(): OnboardingData {
        return OnboardingData(
            gender = prefs.getString("gender", "") ?: "",
            workoutFrequency = prefs.getString("workoutFrequency", "") ?: "",
            isComplete = prefs.getBoolean("onboardingComplete", false)
        )
    }

    fun isComplete(): Boolean = prefs.getBoolean("onboardingComplete", false)
}
