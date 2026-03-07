package com.meta.wearable.dat.externalsampleapps.cameraaccess.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // ── Sessions ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun observeAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 5): List<WorkoutSession>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE startTime >= :since ORDER BY startTime DESC")
    suspend fun getSessionsSince(since: Long): List<WorkoutSession>

    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun getTotalSessionCount(): Int

    @Query("SELECT COALESCE(SUM(totalReps), 0) FROM workout_sessions")
    suspend fun getTotalReps(): Int

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM workout_sessions")
    suspend fun getTotalMinutes(): Int

    // ── Exercise Sets ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseSet(exerciseSet: ExerciseSet): Long

    @Update
    suspend fun updateExerciseSet(exerciseSet: ExerciseSet)

    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId ORDER BY startTime ASC")
    suspend fun getExerciseSetsForSession(sessionId: Long): List<ExerciseSet>

    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId ORDER BY startTime ASC")
    fun observeExerciseSetsForSession(sessionId: Long): Flow<List<ExerciseSet>>

    // ── Stats ──

    @Query("""
        SELECT COUNT(DISTINCT date(startTime / 1000, 'unixepoch', 'localtime'))
        FROM workout_sessions
        WHERE startTime >= :since
    """)
    suspend fun getActiveDaysSince(since: Long): Int

    @Query("""
        SELECT date(startTime / 1000, 'unixepoch', 'localtime') as day
        FROM workout_sessions
        WHERE startTime >= :since
        GROUP BY day
        ORDER BY day ASC
    """)
    suspend fun getActiveDaysList(since: Long): List<String>
}
