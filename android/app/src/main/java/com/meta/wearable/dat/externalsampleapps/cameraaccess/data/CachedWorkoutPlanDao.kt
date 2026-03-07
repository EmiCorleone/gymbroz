package com.meta.wearable.dat.externalsampleapps.cameraaccess.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedWorkoutPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: CachedWorkoutPlan)

    @Query("SELECT * FROM cached_workout_plan WHERE id = 1 LIMIT 1")
    suspend fun get(): CachedWorkoutPlan?

    @Query("DELETE FROM cached_workout_plan")
    suspend fun delete()
}
