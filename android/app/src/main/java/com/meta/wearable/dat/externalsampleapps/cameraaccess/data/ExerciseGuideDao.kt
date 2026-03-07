package com.meta.wearable.dat.externalsampleapps.cameraaccess.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExerciseGuideDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(guide: ExerciseGuideImage)

    @Query("SELECT * FROM exercise_guide_images WHERE exerciseName = :name LIMIT 1")
    suspend fun getByName(name: String): ExerciseGuideImage?

    @Query("SELECT * FROM exercise_guide_images")
    suspend fun getAll(): List<ExerciseGuideImage>

    @Query("DELETE FROM exercise_guide_images")
    suspend fun deleteAll()
}
