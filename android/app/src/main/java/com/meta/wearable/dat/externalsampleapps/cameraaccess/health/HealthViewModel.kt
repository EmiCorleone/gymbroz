package com.meta.wearable.dat.externalsampleapps.cameraaccess.health

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.CachedWorkoutPlan
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.ExerciseGuideImage
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.GymBroDatabase
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

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()
    private val db = GymBroDatabase.getInstance(application)
    private val guideDao = db.exerciseGuideDao()
    private val planDao = db.cachedWorkoutPlanDao()

    private val guideImagesDir = java.io.File(application.filesDir, "exercise_guides").also { it.mkdirs() }

    init {
        // Load cached workout plan and exercise guide images from DB on startup
        viewModelScope.launch {
            // Load cached plan
            val cachedPlan = withContext(Dispatchers.IO) { planDao.get() }
            if (cachedPlan != null) {
                val plan = deserializeWorkoutPlan(cachedPlan.planJson)
                if (plan != null) {
                    val planWorkouts = plan.days.mapNotNull { it.workout }
                    _uiState.update { state ->
                        state.copy(
                            workoutPlan = plan,
                            workouts = state.workouts + planWorkouts
                        )
                    }
                    Log.d("HealthViewModel", "Loaded cached workout plan from DB (${plan.days.size} days)")
                }
            }

            // Load cached exercise guide images (paths only from DB, read files on demand)
            val cached = withContext(Dispatchers.IO) {
                try {
                    guideDao.getAll()
                } catch (e: Exception) {
                    Log.e("HealthViewModel", "Failed to load exercise guides from DB: ${e.message}")
                    emptyList()
                }
            }
            if (cached.isNotEmpty()) {
                val guides = withContext(Dispatchers.IO) {
                    cached.mapNotNull { img ->
                        val fullBody = img.fullBodyPath?.let { readFileAsBase64(it) }
                        val closeUp = img.closeUpPath?.let { readFileAsBase64(it) }
                        if (fullBody != null || closeUp != null) {
                            img.exerciseName to ExerciseGuideUiState(
                                imageBase64 = fullBody,
                                closeUpImageBase64 = closeUp,
                                exerciseName = img.exerciseName,
                            )
                        } else null
                    }.toMap()
                }
                _uiState.update { it.copy(exerciseGuides = it.exerciseGuides + guides) }
                Log.d("HealthViewModel", "Loaded ${guides.size} cached exercise guides from disk")
            }
        }
    }

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
                    // Save plan to DB for persistence
                    launch {
                        val json = serializeWorkoutPlan(plan)
                        withContext(Dispatchers.IO) {
                            planDao.upsert(CachedWorkoutPlan(planJson = json))
                        }
                        Log.d("HealthViewModel", "Saved workout plan to DB")
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

    fun generateSingleExerciseGuide(exerciseName: String) {
        val existing = _uiState.value.exerciseGuides[exerciseName]
        if (existing?.imageBase64 != null || existing?.isGenerating == true) return

        Log.d("HealthViewModel", "generateSingleExerciseGuide: $exerciseName")
        _uiState.update {
            it.copy(exerciseGuides = it.exerciseGuides + (exerciseName to ExerciseGuideUiState(isGenerating = true, exerciseName = exerciseName)))
        }

        viewModelScope.launch {
            val fullBodyResult = withContext(Dispatchers.IO) { generateGuideImage(exerciseName) }
            val fullBodyBase64 = fullBodyResult.getOrNull()

            val closeUpResult = withContext(Dispatchers.IO) { generateCloseUpImage(exerciseName) }
            val closeUpBase64 = closeUpResult.getOrNull()

            val error = if (fullBodyBase64 == null && closeUpBase64 == null) {
                fullBodyResult.exceptionOrNull()?.message ?: "Failed"
            } else null

            val guideState = ExerciseGuideUiState(
                imageBase64 = fullBodyBase64,
                closeUpImageBase64 = closeUpBase64,
                exerciseName = exerciseName,
                error = error,
            )
            _uiState.update {
                it.copy(exerciseGuides = it.exerciseGuides + (exerciseName to guideState))
            }
            Log.d("HealthViewModel", "$exerciseName done — fullBody=${fullBodyBase64 != null}, closeUp=${closeUpBase64 != null}")

            // Save to disk + DB
            if (fullBodyBase64 != null || closeUpBase64 != null) {
                withContext(Dispatchers.IO) {
                    val safeName = exerciseName.replace(Regex("[^a-zA-Z0-9]"), "_")
                    val fullBodyPath = fullBodyBase64?.let { base64 ->
                        val file = java.io.File(guideImagesDir, "${safeName}_full.jpg")
                        file.writeBytes(Base64.decode(base64, Base64.DEFAULT))
                        file.absolutePath
                    }
                    val closeUpPath = closeUpBase64?.let { base64 ->
                        val file = java.io.File(guideImagesDir, "${safeName}_closeup.jpg")
                        file.writeBytes(Base64.decode(base64, Base64.DEFAULT))
                        file.absolutePath
                    }
                    guideDao.upsert(ExerciseGuideImage(
                        exerciseName = exerciseName,
                        fullBodyPath = fullBodyPath,
                        closeUpPath = closeUpPath,
                    ))
                }
                Log.d("HealthViewModel", "Saved $exerciseName to disk+DB")
            }
        }
    }

    private var allGuidesJob: Job? = null

    fun generateAllExerciseGuides(exercises: List<String>) {
        allGuidesJob?.cancel()
        Log.d("HealthViewModel", "generateAllExerciseGuides: ${exercises.size} exercises: $exercises")

        // Mark ungenerated exercises as loading
        val initial = exercises.associateWith { name ->
            _uiState.value.exerciseGuides[name] ?: ExerciseGuideUiState(isGenerating = true, exerciseName = name)
        }
        _uiState.update { it.copy(exerciseGuides = it.exerciseGuides + initial.filter { (_, v) -> v.imageBase64 == null }) }

        allGuidesJob = viewModelScope.launch {
            for ((index, name) in exercises.withIndex()) {
                val existing = _uiState.value.exerciseGuides[name]
                if (existing?.imageBase64 != null && existing.closeUpImageBase64 != null) {
                    Log.d("HealthViewModel", "[$index/${exercises.size}] $name — already cached, skipping")
                    continue
                }

                Log.d("HealthViewModel", "[$index/${exercises.size}] Generating images for: $name")
                _uiState.update {
                    it.copy(exerciseGuides = it.exerciseGuides + (name to ExerciseGuideUiState(isGenerating = true, exerciseName = name)))
                }

                // Image 1: Full body
                val fullBodyResult = withContext(Dispatchers.IO) { generateGuideImage(name) }
                val fullBodyBase64 = fullBodyResult.getOrNull()
                if (fullBodyBase64 == null) {
                    Log.e("HealthViewModel", "[$index] Full body FAILED for $name: ${fullBodyResult.exceptionOrNull()?.message}")
                }

                // Image 2: Close-up
                val closeUpResult = withContext(Dispatchers.IO) { generateCloseUpImage(name) }
                val closeUpBase64 = closeUpResult.getOrNull()
                if (closeUpBase64 == null) {
                    Log.e("HealthViewModel", "[$index] Close-up FAILED for $name: ${closeUpResult.exceptionOrNull()?.message}")
                }

                val error = if (fullBodyBase64 == null && closeUpBase64 == null) {
                    fullBodyResult.exceptionOrNull()?.message ?: "Failed to generate guides"
                } else null

                val guideState = ExerciseGuideUiState(
                    imageBase64 = fullBodyBase64,
                    closeUpImageBase64 = closeUpBase64,
                    exerciseName = name,
                    error = error,
                )
                _uiState.update {
                    it.copy(exerciseGuides = it.exerciseGuides + (name to guideState))
                }
                Log.d("HealthViewModel", "[$index] $name done — fullBody=${fullBodyBase64 != null}, closeUp=${closeUpBase64 != null}")

                // Save images to disk and paths to DB for persistence
                if (fullBodyBase64 != null || closeUpBase64 != null) {
                    withContext(Dispatchers.IO) {
                        val safeName = name.replace(Regex("[^a-zA-Z0-9]"), "_")
                        val fullBodyPath = fullBodyBase64?.let { base64 ->
                            val file = java.io.File(guideImagesDir, "${safeName}_full.jpg")
                            file.writeBytes(Base64.decode(base64, Base64.DEFAULT))
                            file.absolutePath
                        }
                        val closeUpPath = closeUpBase64?.let { base64 ->
                            val file = java.io.File(guideImagesDir, "${safeName}_closeup.jpg")
                            file.writeBytes(Base64.decode(base64, Base64.DEFAULT))
                            file.absolutePath
                        }
                        guideDao.upsert(ExerciseGuideImage(
                            exerciseName = name,
                            fullBodyPath = fullBodyPath,
                            closeUpPath = closeUpPath,
                        ))
                    }
                    Log.d("HealthViewModel", "[$index] Saved $name to disk+DB")
                }
            }
            Log.d("HealthViewModel", "generateAllExerciseGuides COMPLETE — all ${exercises.size} done")
        }
    }

    private fun generateGuideImage(exerciseName: String): Result<String> {
        val apiKey = SettingsManager.geminiAPIKey
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            return Result.failure(Exception("Gemini API key not configured"))
        }

        val parts = JSONArray()

        // Add mirror photo — this is the user's selfie used as reference
        val photoPath = mirrorPhotoPath
        Log.d("HealthViewModel", "generateGuideImage($exerciseName) mirrorPhotoPath=$photoPath")
        if (photoPath != null) {
            val file = java.io.File(photoPath)
            if (file.exists()) {
                val bitmap = loadBitmapWithExifRotation(photoPath)
                if (bitmap != null) {
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                    val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    Log.d("HealthViewModel", "Attached mirror photo (${baos.size()} bytes) for $exerciseName")
                    parts.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64)
                        })
                    })
                } else {
                    Log.e("HealthViewModel", "Failed to decode bitmap from $photoPath")
                }
            } else {
                Log.e("HealthViewModel", "Mirror photo file does not exist: $photoPath")
            }
        } else {
            Log.w("HealthViewModel", "No mirror photo path set — generating without user reference")
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
            Log.e("HealthViewModel", "API error ${response.code} for $exerciseName: ${body.take(500)}")
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
                    if (data.isNotBlank()) {
                        Log.d("HealthViewModel", "Got full-body image for $exerciseName (${data.length} chars)")
                        return Result.success(data)
                    }
                }
            }
        }
        Log.e("HealthViewModel", "No image in response for $exerciseName. Body: ${body.take(500)}")
        return Result.failure(Exception("No image in response"))
    }

    private fun generateCloseUpImage(exerciseName: String): Result<String> {
        val apiKey = SettingsManager.geminiAPIKey
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            return Result.failure(Exception("Gemini API key not configured"))
        }

        val parts = JSONArray()

        // Include the user's mirror photo so the close-up is based on THEM
        val photoPath = mirrorPhotoPath
        Log.d("HealthViewModel", "generateCloseUpImage($exerciseName) mirrorPhotoPath=$photoPath")
        if (photoPath != null) {
            val file = java.io.File(photoPath)
            if (file.exists()) {
                val bitmap = loadBitmapWithExifRotation(photoPath)
                if (bitmap != null) {
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                    val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    Log.d("HealthViewModel", "Attached mirror photo (${baos.size()} bytes) for close-up $exerciseName")
                    parts.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64)
                        })
                    })
                } else {
                    Log.e("HealthViewModel", "Failed to decode bitmap from $photoPath for close-up")
                }
            } else {
                Log.e("HealthViewModel", "Mirror photo file does not exist: $photoPath for close-up")
            }
        } else {
            Log.w("HealthViewModel", "No mirror photo path for close-up — generating without user reference")
        }

        parts.put(JSONObject().apply {
            put("text", "Edit this image. Keep the EXACT same person — same face, skin tone, body, clothing, " +
                "and environment. Change their pose to show the correct form for: $exerciseName. " +
                "Then CROP the result to a close-up view focusing on the primary muscles and limbs involved. " +
                "For example, if it's a bicep curl, crop tightly on the arm showing the grip, bicep contraction, " +
                "and elbow position. The person in the image must look identical to the original photo. " +
                "Do NOT generate a different person. Just re-pose and crop.")
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
            Log.e("HealthViewModel", "API error ${response.code} for close-up $exerciseName: ${body.take(500)}")
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
                    if (data.isNotBlank()) {
                        Log.d("HealthViewModel", "Got close-up image for $exerciseName (${data.length} chars)")
                        return Result.success(data)
                    }
                }
            }
        }
        Log.e("HealthViewModel", "No image in close-up response for $exerciseName. Body: ${body.take(500)}")
        return Result.failure(Exception("No image in response"))
    }

    private fun serializeWorkoutPlan(plan: WorkoutPlan): String {
        val json = JSONObject()
        json.put("id", plan.id)
        json.put("generatedAt", plan.generatedAt)
        json.put("gender", plan.gender)
        json.put("workoutFrequency", plan.workoutFrequency)
        json.put("weeklyGoal", plan.weeklyGoal)
        json.put("difficultyLevel", plan.difficultyLevel.name)
        val daysArray = JSONArray()
        for (day in plan.days) {
            val dayObj = JSONObject()
            dayObj.put("dayOfWeek", day.dayOfWeek)
            dayObj.put("isRestDay", day.isRestDay)
            dayObj.put("focusArea", day.focusArea)
            if (day.workout != null) {
                val wObj = JSONObject()
                wObj.put("id", day.workout.id)
                wObj.put("name", day.workout.name)
                wObj.put("category", day.workout.category.name)
                wObj.put("difficulty", day.workout.difficulty.name)
                wObj.put("durationMin", day.workout.durationMin)
                val exArray = JSONArray()
                for (ex in day.workout.exercises) {
                    val exObj = JSONObject()
                    exObj.put("name", ex.name)
                    if (ex.sets != null) exObj.put("sets", ex.sets)
                    if (ex.reps != null) exObj.put("reps", ex.reps)
                    if (ex.durationSec != null) exObj.put("durationSec", ex.durationSec)
                    exArray.put(exObj)
                }
                wObj.put("exercises", exArray)
                dayObj.put("workout", wObj)
            }
            daysArray.put(dayObj)
        }
        json.put("days", daysArray)
        return json.toString()
    }

    private fun deserializeWorkoutPlan(jsonStr: String): WorkoutPlan? {
        return try {
            val json = JSONObject(jsonStr)
            val daysArray = json.getJSONArray("days")
            val days = mutableListOf<DayPlan>()
            for (i in 0 until daysArray.length()) {
                val dayObj = daysArray.getJSONObject(i)
                var workout: Workout? = null
                if (!dayObj.isNull("workout") && dayObj.has("workout")) {
                    val wObj = dayObj.getJSONObject("workout")
                    val exercises = mutableListOf<Exercise>()
                    val exArray = wObj.getJSONArray("exercises")
                    for (j in 0 until exArray.length()) {
                        val exObj = exArray.getJSONObject(j)
                        exercises.add(Exercise(
                            name = exObj.getString("name"),
                            sets = if (exObj.has("sets")) exObj.getInt("sets") else null,
                            reps = if (exObj.has("reps")) exObj.getInt("reps") else null,
                            durationSec = if (exObj.has("durationSec")) exObj.getInt("durationSec") else null
                        ))
                    }
                    workout = Workout(
                        id = wObj.getString("id"),
                        name = wObj.getString("name"),
                        category = try { WorkoutCategory.valueOf(wObj.getString("category")) } catch (_: Exception) { WorkoutCategory.Strength },
                        difficulty = try { Difficulty.valueOf(wObj.getString("difficulty")) } catch (_: Exception) { Difficulty.Beginner },
                        durationMin = wObj.optInt("durationMin", 30),
                        exercises = exercises
                    )
                }
                days.add(DayPlan(
                    dayOfWeek = dayObj.getString("dayOfWeek"),
                    isRestDay = dayObj.optBoolean("isRestDay", false),
                    focusArea = dayObj.optString("focusArea", ""),
                    workout = workout
                ))
            }
            WorkoutPlan(
                id = json.getString("id"),
                generatedAt = json.getString("generatedAt"),
                gender = json.getString("gender"),
                workoutFrequency = json.getString("workoutFrequency"),
                days = days,
                weeklyGoal = json.optString("weeklyGoal", ""),
                difficultyLevel = try { Difficulty.valueOf(json.getString("difficultyLevel")) } catch (_: Exception) { Difficulty.Beginner }
            )
        } catch (e: Exception) {
            Log.e("HealthViewModel", "Failed to deserialize workout plan: ${e.message}")
            null
        }
    }

    private fun readFileAsBase64(path: String): String? {
        return try {
            val file = java.io.File(path)
            if (file.exists()) Base64.encodeToString(file.readBytes(), Base64.NO_WRAP) else null
        } catch (e: Exception) {
            Log.e("HealthViewModel", "Failed to read file $path: ${e.message}")
            null
        }
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
