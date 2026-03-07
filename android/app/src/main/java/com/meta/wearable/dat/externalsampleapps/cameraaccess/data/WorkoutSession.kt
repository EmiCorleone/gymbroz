package com.meta.wearable.dat.externalsampleapps.cameraaccess.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val durationMinutes: Int = 0,
    val totalReps: Int = 0,
    val totalExercises: Int = 0,
    val isPhoneMode: Boolean = false,
    val videoUrl: String? = null
)
