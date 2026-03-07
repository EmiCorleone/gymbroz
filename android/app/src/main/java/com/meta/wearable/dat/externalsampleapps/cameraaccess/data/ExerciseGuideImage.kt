package com.meta.wearable.dat.externalsampleapps.cameraaccess.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_guide_images")
data class ExerciseGuideImage(
    @PrimaryKey
    val exerciseName: String,
    val fullBodyBase64: String? = null,
    val closeUpBase64: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
