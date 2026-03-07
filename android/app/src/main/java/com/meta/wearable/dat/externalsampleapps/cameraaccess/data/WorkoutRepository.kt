package com.meta.wearable.dat.externalsampleapps.cameraaccess.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

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
        return workoutDao.insertSession(session)
    }

    suspend fun endSession(sessionId: Long, totalReps: Int, totalExercises: Int) {
        val session = workoutDao.getSessionById(sessionId) ?: return
        val now = System.currentTimeMillis()
        val durationMinutes = ((now - session.startTime) / 60000).toInt()
        workoutDao.updateSession(
            session.copy(
                endTime = now,
                durationMinutes = durationMinutes,
                totalReps = totalReps,
                totalExercises = totalExercises
            )
        )
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
        return workoutDao.insertExerciseSet(
            ExerciseSet(
                sessionId = sessionId,
                exerciseName = exerciseName,
                repCount = repCount,
                endTime = System.currentTimeMillis(),
                guideImagePath = guideImagePath
            )
        )
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
