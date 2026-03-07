package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data

import android.util.Log
import com.meta.wearable.dat.externalsampleapps.cameraaccess.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

object WorkoutPlanGenerator {
    private const val TAG = "WorkoutPlanGenerator"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generatePlan(gender: String, frequency: String): Result<WorkoutPlan> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = SettingsManager.geminiAPIKey
                if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
                    return@withContext Result.failure(Exception("Gemini API key not configured"))
                }

                val workoutDays = when (frequency) {
                    "0-2" -> 3
                    "3-5" -> 4
                    "6+" -> 5
                    else -> 3
                }
                val difficulty = when (frequency) {
                    "0-2" -> "Beginner"
                    "3-5" -> "Intermediate"
                    "6+" -> "Advanced"
                    else -> "Beginner"
                }

                val prompt = buildPrompt(gender, frequency, workoutDays, difficulty)
                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("text", prompt)
                        }))
                    }))
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 4096)
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                response.close()

                if (response.code !in 200..299) {
                    Log.e(TAG, "Gemini API error: ${response.code} - ${responseBody.take(200)}")
                    return@withContext Result.failure(Exception("API error: ${response.code}"))
                }

                val json = JSONObject(responseBody)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "") ?: ""

                val planJson = extractJson(text)
                val plan = parsePlan(planJson, gender, frequency, difficulty)
                Result.success(plan)
            } catch (e: Exception) {
                Log.e(TAG, "Plan generation failed: ${e.message}", e)
                Result.failure(e)
            }
        }

    private fun buildPrompt(gender: String, frequency: String, workoutDays: Int, difficulty: String): String {
        return """Generate a personalized weekly workout plan as JSON. Respond with ONLY valid JSON, no markdown or extra text.

User profile:
- Gender: $gender
- Workout frequency preference: $frequency per week
- Suggested difficulty: $difficulty

Requirements:
- Plan exactly 7 days (Monday through Sunday)
- Include exactly $workoutDays workout days and ${7 - workoutDays} rest days
- Each workout should have 4-6 exercises
- IMPORTANT: The FIRST exercise of the FIRST workout day MUST always be "Bicep Curl" (3 sets x 12 reps)
- IMPORTANT: Saturday MUST always be a workout day (not rest), and its FIRST exercise MUST be "Bicep Curl" (3 sets x 12 reps)
- Vary focus areas (Upper Body, Lower Body, Core, Full Body, Cardio, etc.)
- Include a weekly goal summary

JSON schema:
{
  "weeklyGoal": "string describing the week's goal",
  "days": [
    {
      "dayOfWeek": "Monday",
      "isRestDay": false,
      "focusArea": "Upper Body",
      "workout": {
        "name": "Workout Name",
        "category": "Strength|Cardio|Yoga|HIIT|Walking",
        "durationMin": 30,
        "exercises": [
          {"name": "Exercise Name", "sets": 3, "reps": 12},
          {"name": "Timed Exercise", "durationSec": 60}
        ]
      }
    },
    {
      "dayOfWeek": "Tuesday",
      "isRestDay": true,
      "focusArea": "Rest & Recovery",
      "workout": null
    }
  ]
}"""
    }

    private fun extractJson(text: String): String {
        var cleaned = text.trim()
        if (cleaned.startsWith("```json")) cleaned = cleaned.removePrefix("```json")
        if (cleaned.startsWith("```")) cleaned = cleaned.removePrefix("```")
        if (cleaned.endsWith("```")) cleaned = cleaned.removeSuffix("```")
        return cleaned.trim()
    }

    private fun parsePlan(jsonStr: String, gender: String, frequency: String, difficulty: String): WorkoutPlan {
        val json = JSONObject(jsonStr)
        val daysArray = json.getJSONArray("days")
        val days = mutableListOf<DayPlan>()

        for (i in 0 until daysArray.length()) {
            val dayObj = daysArray.getJSONObject(i)
            val isRestDay = dayObj.optBoolean("isRestDay", false)
            var workout: Workout? = null

            if (!isRestDay && !dayObj.isNull("workout")) {
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

                val categoryStr = wObj.optString("category", "Strength")
                val category = try {
                    WorkoutCategory.valueOf(categoryStr)
                } catch (_: Exception) {
                    WorkoutCategory.Strength
                }

                val dayAbbrev = dayObj.getString("dayOfWeek").take(3).lowercase()
                workout = Workout(
                    id = "plan_$dayAbbrev",
                    name = wObj.getString("name"),
                    category = category,
                    difficulty = try { Difficulty.valueOf(difficulty) } catch (_: Exception) { Difficulty.Beginner },
                    durationMin = wObj.optInt("durationMin", 30),
                    exercises = exercises
                )
            }

            days.add(DayPlan(
                dayOfWeek = dayObj.getString("dayOfWeek"),
                isRestDay = isRestDay,
                focusArea = dayObj.optString("focusArea", if (isRestDay) "Rest & Recovery" else ""),
                workout = workout
            ))
        }

        // Enforce Saturday always starts with Bicep Curl
        val bicepCurl = Exercise(name = "Bicep Curl", sets = 3, reps = 12)
        val enforcedDays = days.map { day ->
            if (day.dayOfWeek == "Saturday") {
                val workout = day.workout
                if (workout != null) {
                    val exercises = workout.exercises.toMutableList()
                    if (exercises.firstOrNull()?.name != "Bicep Curl") {
                        exercises.removeAll { it.name == "Bicep Curl" }
                        exercises.add(0, bicepCurl)
                    }
                    day.copy(isRestDay = false, workout = workout.copy(exercises = exercises))
                } else {
                    // Saturday was rest day — make it a workout day with Bicep Curl first
                    day.copy(
                        isRestDay = false,
                        focusArea = "Upper Body",
                        workout = Workout(
                            id = "plan_sat",
                            name = "Saturday Pump",
                            category = WorkoutCategory.Strength,
                            difficulty = try { Difficulty.valueOf(difficulty) } catch (_: Exception) { Difficulty.Beginner },
                            durationMin = 30,
                            exercises = listOf(
                                bicepCurl,
                                Exercise(name = "Hammer Curl", sets = 3, reps = 12),
                                Exercise(name = "Triceps Dip", sets = 3, reps = 10),
                                Exercise(name = "Push-up", sets = 3, reps = 15),
                                Exercise(name = "Shoulder Press", sets = 3, reps = 10),
                            )
                        )
                    )
                }
            } else day
        }

        return WorkoutPlan(
            id = UUID.randomUUID().toString(),
            generatedAt = Instant.now().toString(),
            gender = gender,
            workoutFrequency = frequency,
            days = enforcedDays,
            weeklyGoal = json.optString("weeklyGoal", "Stay consistent and build strength"),
            difficultyLevel = try { Difficulty.valueOf(difficulty) } catch (_: Exception) { Difficulty.Beginner }
        )
    }
}
