package com.meta.wearable.dat.externalsampleapps.cameraaccess.health

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.*
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.MockData
import com.meta.wearable.dat.externalsampleapps.cameraaccess.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ExerciseGuideUiState(
    val isGenerating: Boolean = false,
    val imageBase64: String? = null,
    val closeUpImageBase64: String? = null,
    val exerciseName: String? = null,
    val error: String? = null,
)

data class HealthUiState(
    val user: UserProfile = MockData.mockUser,
    val workouts: List<Workout> = MockData.mockWorkouts,
    val steps: List<StepDay> = MockData.mockSteps,
    val nutrition: DailyNutrition = MockData.mockNutrition,
    val sleep: List<SleepLog> = MockData.mockSleep,
    val stress: List<StressEntry> = MockData.mockStress,
    val meditation: List<MeditationSession> = MockData.mockMeditation,
    val meditationStreak: Int = MockData.meditationStreak,
    val journal: List<JournalEntry> = MockData.mockJournal,
    val vitals: List<VitalReading> = MockData.mockVitals,
    val medications: List<Medication> = MockData.mockMedications,
    val appointments: List<Appointment> = MockData.mockAppointments,
    val communityPosts: List<CommunityPost> = MockData.mockCommunityPosts,
    val selectedDate: String = "2026-03-07",
    val workoutPlan: WorkoutPlan? = null,
    val isPlanLoading: Boolean = false,
    val planError: String? = null,
    val exerciseGuide: ExerciseGuideUiState = ExerciseGuideUiState(),
    val exerciseGuides: Map<String, ExerciseGuideUiState> = emptyMap(),
)

class HealthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    fun generateWorkoutPlan(gender: String, frequency: String, deviceId: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isPlanLoading = true, planError = null) }
            val result = WorkoutPlanGenerator.generatePlan(gender, frequency)
            result.fold(
                onSuccess = { plan ->
                    _uiState.update { state ->
                        val planWorkouts = plan.days.mapNotNull { it.workout }
                        state.copy(
                            workoutPlan = plan,
                            isPlanLoading = false,
                            planError = null,
                            workouts = state.workouts + planWorkouts
                        )
                    }
                    // Generate exercise guide images for all exercises in the plan
                    val allExerciseNames = plan.days
                        .mapNotNull { it.workout }
                        .flatMap { w -> w.exercises.map { it.name } }
                        .distinct()
                    generateAllExerciseGuides(allExerciseNames)

                    if (deviceId.isNotBlank()) {
                        launch {
                            SupabaseClient.saveWorkoutPlan(plan, deviceId).onFailure { e ->
                                Log.e("HealthViewModel", "Failed to save plan to Supabase: ${e.message}")
                            }
                        }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isPlanLoading = false, planError = e.message ?: "Failed to generate plan") }
                }
            )
        }
    }

    fun loadWorkoutPlan(deviceId: String) {
        viewModelScope.launch {
            SupabaseClient.getWorkoutPlan(deviceId).onSuccess { plan ->
                if (plan != null) {
                    _uiState.update { state ->
                        val planWorkouts = plan.days.mapNotNull { it.workout }
                        state.copy(
                            workoutPlan = plan,
                            workouts = state.workouts + planWorkouts
                        )
                    }
                }
            }
        }
    }

    fun addMeal(meal: Meal) {
        _uiState.update { state ->
            val updatedMeals = state.nutrition.meals + meal
            state.copy(
                nutrition = state.nutrition.copy(
                    meals = updatedMeals,
                    totalCalories = updatedMeals.sumOf { it.calories },
                    carbs = state.nutrition.carbs.copy(current = updatedMeals.sumOf { it.carbs }),
                    proteins = state.nutrition.proteins.copy(current = updatedMeals.sumOf { it.proteins }),
                    fats = state.nutrition.fats.copy(current = updatedMeals.sumOf { it.fats })
                )
            )
        }
    }

    fun removeMeal(mealId: String) {
        _uiState.update { state ->
            val updatedMeals = state.nutrition.meals.filter { it.id != mealId }
            state.copy(
                nutrition = state.nutrition.copy(
                    meals = updatedMeals,
                    totalCalories = updatedMeals.sumOf { it.calories },
                    carbs = state.nutrition.carbs.copy(current = updatedMeals.sumOf { it.carbs }),
                    proteins = state.nutrition.proteins.copy(current = updatedMeals.sumOf { it.proteins }),
                    fats = state.nutrition.fats.copy(current = updatedMeals.sumOf { it.fats })
                )
            )
        }
    }

    fun togglePostLike(postId: String) {
        _uiState.update { state ->
            state.copy(
                communityPosts = state.communityPosts.map { post ->
                    if (post.id == postId) {
                        post.copy(
                            liked = !post.liked,
                            likes = if (post.liked) post.likes - 1 else post.likes + 1
                        )
                    } else post
                }
            )
        }
    }

    fun toggleMedReminder(medId: String) {
        _uiState.update { state ->
            state.copy(
                medications = state.medications.map { med ->
                    if (med.id == medId) med.copy(reminderEnabled = !med.reminderEnabled) else med
                }
            )
        }
    }

    fun selectDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    // Exercise guide image generation
    var mirrorPhotoPath: String? = null
    private var guideJob: Job? = null
    private val guideHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun generateExerciseGuide(exerciseName: String) {
        if (_uiState.value.exerciseGuide.isGenerating) return
        _uiState.update { it.copy(exerciseGuide = ExerciseGuideUiState(isGenerating = true, exerciseName = exerciseName)) }

        guideJob?.cancel()
        guideJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { generateGuideImage(exerciseName) }
            result.fold(
                onSuccess = { base64 ->
                    _uiState.update { it.copy(exerciseGuide = ExerciseGuideUiState(imageBase64 = base64, exerciseName = exerciseName)) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(exerciseGuide = ExerciseGuideUiState(error = e.message ?: "Failed to generate guide")) }
                }
            )
        }
    }

    fun dismissExerciseGuide() {
        guideJob?.cancel()
        _uiState.update { it.copy(exerciseGuide = ExerciseGuideUiState()) }
    }

    private var allGuidesJob: Job? = null

    fun generateAllExerciseGuides(exercises: List<String>) {
        allGuidesJob?.cancel()
        val initial = exercises.associateWith { name ->
            _uiState.value.exerciseGuides[name] ?: ExerciseGuideUiState(isGenerating = true, exerciseName = name)
        }
        _uiState.update { it.copy(exerciseGuides = it.exerciseGuides + initial.filter { (_, v) -> v.imageBase64 == null }) }

        allGuidesJob = viewModelScope.launch {
            for (name in exercises) {
                val existing = _uiState.value.exerciseGuides[name]
                if (existing?.imageBase64 != null && existing.closeUpImageBase64 != null) continue

                _uiState.update {
                    it.copy(exerciseGuides = it.exerciseGuides + (name to ExerciseGuideUiState(isGenerating = true, exerciseName = name)))
                }

                // Image 1: Full body showing user doing the exercise
                val fullBodyResult = withContext(Dispatchers.IO) { generateGuideImage(name) }
                val fullBodyBase64 = fullBodyResult.getOrNull()

                // Image 2: Close-up of the arm/muscle and proper form
                val closeUpResult = withContext(Dispatchers.IO) { generateCloseUpImage(name) }
                val closeUpBase64 = closeUpResult.getOrNull()

                val error = if (fullBodyBase64 == null && closeUpBase64 == null) {
                    fullBodyResult.exceptionOrNull()?.message ?: "Failed to generate guides"
                } else null

                _uiState.update {
                    it.copy(exerciseGuides = it.exerciseGuides + (name to ExerciseGuideUiState(
                        imageBase64 = fullBodyBase64,
                        closeUpImageBase64 = closeUpBase64,
                        exerciseName = name,
                        error = error,
                    )))
                }
            }
        }
    }

    private fun generateGuideImage(exerciseName: String): Result<String> {
        val apiKey = SettingsManager.geminiAPIKey
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            return Result.failure(Exception("Gemini API key not configured"))
        }

        val parts = JSONArray()

        // Add mirror photo if available
        val photoPath = mirrorPhotoPath
        if (photoPath != null) {
            val bitmap = loadBitmapWithExifRotation(photoPath)
            if (bitmap != null) {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                parts.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64)
                    })
                })
            }
        }

        parts.put(JSONObject().apply {
            put("text", "Edit this image. Keep the EXACT same person, background, environment, and clothing. " +
                "Only change their body position to show the correct form for performing: $exerciseName. " +
                "The person should be in the starting position of the exercise with proper form. " +
                "Do NOT change the person's appearance, face, or surroundings. Just adjust the pose.")
        })

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", parts)
            }))
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
                put("imageConfig", JSONObject().apply {
                    put("aspectRatio", "9:16")
                })
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = guideHttpClient.newCall(request).execute()
        val body = response.body?.string() ?: ""
        response.close()

        if (response.code !in 200..299) {
            return Result.failure(Exception("API error: ${response.code}"))
        }

        val json = JSONObject(body)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val resParts = candidate?.optJSONObject("content")?.optJSONArray("parts")
        if (resParts != null) {
            for (i in 0 until resParts.length()) {
                val part = resParts.getJSONObject(i)
                val inlineData = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                if (inlineData != null) {
                    val data = inlineData.optString("data", "")
                    if (data.isNotBlank()) return Result.success(data)
                }
            }
        }
        return Result.failure(Exception("No image in response"))
    }

    private fun generateCloseUpImage(exerciseName: String): Result<String> {
        val apiKey = SettingsManager.geminiAPIKey
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            return Result.failure(Exception("Gemini API key not configured"))
        }

        val parts = JSONArray()
        parts.put(JSONObject().apply {
            put("text", "Generate a close-up instructional image for the exercise: $exerciseName. " +
                "Show ONLY the specific body part and muscle group involved — for example, if it's a Bicep Curl, " +
                "show a close-up of the arm from elbow to hand gripping the dumbbell, with the bicep muscle visible. " +
                "Show the correct grip, wrist position, and muscle engagement. " +
                "Use clean lighting, fitness photography style. No text overlays. " +
                "Focus tightly on the form detail, not the full body.")
        })

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", parts)
            }))
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
                put("imageConfig", JSONObject().apply {
                    put("aspectRatio", "1:1")
                })
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = guideHttpClient.newCall(request).execute()
        val body = response.body?.string() ?: ""
        response.close()

        if (response.code !in 200..299) {
            return Result.failure(Exception("API error: ${response.code}"))
        }

        val json = JSONObject(body)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val resParts = candidate?.optJSONObject("content")?.optJSONArray("parts")
        if (resParts != null) {
            for (i in 0 until resParts.length()) {
                val part = resParts.getJSONObject(i)
                val inlineData = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                if (inlineData != null) {
                    val data = inlineData.optString("data", "")
                    if (data.isNotBlank()) return Result.success(data)
                }
            }
        }
        return Result.failure(Exception("No image in response"))
    }

    private fun loadBitmapWithExifRotation(path: String): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        val rotation = try {
            val exif = androidx.exifinterface.media.ExifInterface(path)
            when (exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) { 0f }
        if (rotation == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return rotated
    }
}
