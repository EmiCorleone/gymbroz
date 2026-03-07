package com.meta.wearable.dat.externalsampleapps.cameraaccess.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_workout_plan")
data class CachedWorkoutPlan(
    @PrimaryKey
    val id: Int = 1, // Single-row table
    val planJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
