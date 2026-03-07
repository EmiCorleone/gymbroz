package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data

import androidx.compose.ui.graphics.Color

enum class WorkoutCategory { Strength, Cardio, Yoga, HIIT, Walking }
enum class Difficulty { Beginner, Intermediate, Advanced }
enum class MealType { Breakfast, Lunch, Dinner, Snack }
enum class MedFrequency { Daily, TwiceDaily, Weekly }
enum class MeditationType { Guided, Unguided }

data class EmergencyContact(val name: String, val phone: String)
data class UserGoals(val steps: Int, val calories: Int, val sleepHours: Int, val targetWeight: Int)
data class UserPreferences(val units: String = "imperial", val workoutReminders: Boolean = true, val medicationAlerts: Boolean = true, val sleepReminders: Boolean = true)

data class UserProfile(
    val name: String, val age: Int, val avatar: String, val height: String,
    val weight: Int, val bloodType: String,
    val emergencyContact: EmergencyContact, val goals: UserGoals, val preferences: UserPreferences
)

data class Exercise(val name: String, val sets: Int? = null, val reps: Int? = null, val durationSec: Int? = null)

data class Workout(
    val id: String, val name: String, val category: WorkoutCategory,
    val difficulty: Difficulty, val durationMin: Int, val exercises: List<Exercise>,
    val thumbnail: String = ""
)

data class StepDay(val date: String, val steps: Int, val caloriesBurned: Int, val activeMinutes: Int)

data class Meal(
    val id: String, val name: String, val calories: Int,
    val carbs: Int, val proteins: Int, val fats: Int,
    val mealType: MealType, val time: String, val date: String,
    val thumbnail: String? = null
)

data class MacroInfo(val current: Int, val goal: Int)

data class DailyNutrition(
    val date: String, val totalCalories: Int, val goalCalories: Int,
    val carbs: MacroInfo, val proteins: MacroInfo, val fats: MacroInfo,
    val meals: List<Meal>
)

data class SleepLog(
    val date: String, val bedtime: String, val wakeTime: String,
    val totalHours: Float, val quality: Int,
    val deep: Float, val light: Float, val rem: Float
)

data class StressEntry(val date: String, val level: Int, val mood: String, val notes: String? = null)
data class MeditationSession(val date: String, val durationMin: Int, val type: MeditationType)

data class JournalEntry(val id: String, val date: String, val content: String, val moodTags: List<String>)

data class VitalReading(
    val date: String, val systolic: Int? = null, val diastolic: Int? = null,
    val heartRate: Int? = null, val weight: Float? = null, val notes: String? = null
)

data class Medication(
    val id: String, val name: String, val dosage: String,
    val frequency: MedFrequency, val time: String, val reminderEnabled: Boolean
)

data class Appointment(
    val id: String, val doctor: String, val specialty: String,
    val date: String, val time: String, val location: String, val reason: String? = null
)

data class CommunityPost(
    val id: String, val author: String, val avatarColor: Color,
    val text: String, val timestamp: String,
    val likes: Int, val comments: Int, val liked: Boolean
)

data class DayPlan(
    val dayOfWeek: String,
    val isRestDay: Boolean = false,
    val focusArea: String = "",
    val workout: Workout? = null
)

data class WorkoutPlan(
    val id: String,
    val generatedAt: String,
    val gender: String,
    val workoutFrequency: String,
    val days: List<DayPlan>,
    val weeklyGoal: String = "",
    val difficultyLevel: Difficulty = Difficulty.Beginner
)
