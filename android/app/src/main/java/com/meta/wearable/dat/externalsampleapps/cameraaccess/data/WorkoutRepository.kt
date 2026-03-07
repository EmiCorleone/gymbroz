package com.meta.wearable.dat.externalsampleapps.cameraaccess.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest

/**
 * Repository wrapping DAOs with business logic for workout tracking.
 */
class WorkoutRepository(context: Context) {
    private val db = GymBroDatabase.getInstance(context)
    private val profileDao = db.userProfileDao()
    private val workoutDao = db.workoutDao()

    // ── Profile ──

    suspend fun saveProfile(profile: UserProfile): Long = profileDao.insert(profile)
    suspend fun getProfile(): UserProfile? = profileDao.getProfile()
    fun observeProfile(): Flow<UserProfile?> = profileDao.observeProfile()
    suspend fun isOnboardingComplete(): Boolean = profileDao.getProfileCount() > 0
    suspend fun updateProfile(profile: UserProfile) = profileDao.update(profile)
    suspend fun clearProfile() = profileDao.deleteAll()

    // ── Sessions ──

    suspend fun startSession(isPhoneMode: Boolean = false): Long {
        val session = WorkoutSession(isPhoneMode = isPhoneMode)
        val localId = workoutDao.insertSession(session)
        
        // Sync to Supabase
        try {
            val auth = GymBroSupabaseClient.client.auth
            val postgrest = GymBroSupabaseClient.client.postgrest
            val userId = auth.currentUserOrNull()?.id ?: return localId
            
            @kotlinx.serialization.Serializable
            data class SupabaseWorkoutSession(
                val id: String, // String representation of the local Long ID
                val user_id: String,
                val is_phone_mode: Boolean,
                val total_reps: Int,
                val total_exercises: Int,
                val duration_minutes: Int
            )
            
            val supabaseSession = SupabaseWorkoutSession(
                id = localId.toString(),
                user_id = userId,
                is_phone_mode = session.isPhoneMode,
                total_reps = session.totalReps,
                total_exercises = session.totalExercises,
                duration_minutes = session.durationMinutes
            )
            postgrest.from("workout_sessions").upsert(supabaseSession)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return localId
    }

    suspend fun endSession(sessionId: Long, totalReps: Int, totalExercises: Int, videoUrl: String? = null) {
        val session = workoutDao.getSessionById(sessionId) ?: return
        val now = System.currentTimeMillis()
        val durationMinutes = ((now - session.startTime) / 60000).toInt()
        val updatedSession = session.copy(
            endTime = now,
            durationMinutes = durationMinutes,
            totalReps = totalReps,
            totalExercises = totalExercises,
            videoUrl = videoUrl
        )
        workoutDao.updateSession(updatedSession)
        
        // Sync to Supabase
        try {
            val auth = GymBroSupabaseClient.client.auth
            val postgrest = GymBroSupabaseClient.client.postgrest
            val userId = auth.currentUserOrNull()?.id ?: return
            
            @kotlinx.serialization.Serializable
            data class SupabaseWorkoutSession(
                val id: String, 
                val user_id: String,
                val is_phone_mode: Boolean,
                val total_reps: Int,
                val total_exercises: Int,
                val duration_minutes: Int,
                val video_url: String?
            )
            
            val supabaseSession = SupabaseWorkoutSession(
                id = updatedSession.id.toString(),
                user_id = userId,
                is_phone_mode = updatedSession.isPhoneMode,
                total_reps = updatedSession.totalReps,
                total_exercises = updatedSession.totalExercises,
                duration_minutes = updatedSession.durationMinutes,
                video_url = updatedSession.videoUrl
            )
            postgrest.from("workout_sessions").upsert(supabaseSession)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getRecentSessions(limit: Int = 5) = workoutDao.getRecentSessions(limit)
    fun observeAllSessions(): Flow<List<WorkoutSession>> = workoutDao.observeAllSessions()

    // ── Exercise Sets ──

    suspend fun logExerciseSet(
        sessionId: Long,
        exerciseName: String,
        repCount: Int,
        guideImagePath: String? = null
    ): Long {
        val set = ExerciseSet(
            sessionId = sessionId,
            exerciseName = exerciseName,
            repCount = repCount,
            endTime = System.currentTimeMillis(),
            guideImagePath = guideImagePath
        )
        val localId = workoutDao.insertExerciseSet(set)
        
        // Sync to Supabase
        try {
            val auth = GymBroSupabaseClient.client.auth
            val postgrest = GymBroSupabaseClient.client.postgrest
            val userId = auth.currentUserOrNull()?.id ?: return localId
            
            @kotlinx.serialization.Serializable
            data class SupabaseExerciseSet(
                val id: String,
                val session_id: String,
                val user_id: String,
                val exercise_name: String,
                val rep_count: Int,
                val guide_image_url: String?
            )
            
            val supabaseSet = SupabaseExerciseSet(
                id = localId.toString(),
                session_id = sessionId.toString(),
                user_id = userId,
                exercise_name = set.exerciseName,
                rep_count = set.repCount,
                guide_image_url = set.guideImagePath // If valid URL, else skipped/null
            )
            postgrest.from("exercise_sets").insert(supabaseSet)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return localId
    }

    suspend fun logRepEvent(
        sessionId: Long,
        exerciseName: String,
        repNumber: Int
    ) {
        // Sync directly to Supabase since we don't have a local representation of rep_events
        try {
            val auth = GymBroSupabaseClient.client.auth
            val postgrest = GymBroSupabaseClient.client.postgrest
            val userId = auth.currentUserOrNull()?.id ?: return
            
            @kotlinx.serialization.Serializable
            data class SupabaseRepEvent(
                val session_id: String,
                val user_id: String,
                val exercise_name: String,
                val rep_number: Int
            )
            
            val supabaseEvent = SupabaseRepEvent(
                session_id = sessionId.toString(),
                user_id = userId,
                exercise_name = exerciseName,
                rep_number = repNumber
            )
            postgrest.from("rep_events").insert(supabaseEvent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getExerciseSetsForSession(sessionId: Long) =
        workoutDao.getExerciseSetsForSession(sessionId)

    // ── Stats ──

    suspend fun getTotalSessionCount() = workoutDao.getTotalSessionCount()
    suspend fun getTotalReps() = workoutDao.getTotalReps()
    suspend fun getTotalMinutes() = workoutDao.getTotalMinutes()

    suspend fun getActiveDaysSince(since: Long) = workoutDao.getActiveDaysSince(since)
    suspend fun getActiveDaysList(since: Long) = workoutDao.getActiveDaysList(since)

    /**
     * Calculate current streak (consecutive days with at least 1 workout, going backward from today).
     */
    suspend fun getCurrentStreak(): Int {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val activeDays = workoutDao.getActiveDaysList(thirtyDaysAgo)
        if (activeDays.isEmpty()) return 0

        // activeDays are in "YYYY-MM-DD" format, sorted ascending
        var streak = 0
        val today = java.time.LocalDate.now()
        var checkDate = today

        for (i in activeDays.indices.reversed()) {
            val dayStr = activeDays[i]
            val day = java.time.LocalDate.parse(dayStr)
            if (day == checkDate) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else if (day.isBefore(checkDate)) {
                break
            }
        }
        return streak
    }
}
