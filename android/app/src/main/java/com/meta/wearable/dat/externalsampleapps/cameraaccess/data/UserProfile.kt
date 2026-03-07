package com.meta.wearable.dat.externalsampleapps.cameraaccess.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val gender: String = "", // "Male", "Female", "Other"
    val age: Int = 0,
    val heightCm: Int = 0,
    val weightKg: Int = 0,
    val fitnessGoal: String = "", // "build_muscle", "lose_weight", "stay_active", "improve_health"
    val experienceLevel: String = "", // "beginner", "intermediate", "advanced"
    val weeklyWorkouts: String = "", // "0-2", "3-5", "6+"
    val mirrorPhotoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
