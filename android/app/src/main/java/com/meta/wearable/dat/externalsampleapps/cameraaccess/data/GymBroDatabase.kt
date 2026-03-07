package com.meta.wearable.dat.externalsampleapps.cameraaccess.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserProfile::class, WorkoutSession::class, ExerciseSet::class, ExerciseGuideImage::class, CachedWorkoutPlan::class],
    version = 4,
    exportSchema = false
)
abstract class GymBroDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseGuideDao(): ExerciseGuideDao
    abstract fun cachedWorkoutPlanDao(): CachedWorkoutPlanDao

    companion object {
        @Volatile
        private var INSTANCE: GymBroDatabase? = null

        fun getInstance(context: Context): GymBroDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymBroDatabase::class.java,
                    "gymbro_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
