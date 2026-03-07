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
import java.util.concurrent.TimeUnit

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun isConfigured(): Boolean {
        val url = SettingsManager.supabaseUrl
        return url.isNotBlank() && url != "YOUR_SUPABASE_URL"
    }

    suspend fun saveWorkoutPlan(plan: WorkoutPlan, deviceId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!isConfigured()) {
                Log.d(TAG, "Supabase not configured, skipping save")
                return@withContext Result.success(Unit)
            }

            try {
                val url = "${SettingsManager.supabaseUrl}/rest/v1/workout_plans"
                val key = SettingsManager.supabaseAnonKey

                val planJson = serializePlan(plan)
                val body = JSONObject().apply {
                    put("device_id", deviceId)
                    put("gender", plan.gender)
                    put("workout_frequency", plan.workoutFrequency)
                    put("plan_json", planJson)
                    put("weekly_goal", plan.weeklyGoal)
                    put("difficulty", plan.difficultyLevel.name)
                }

                val request = Request.Builder()
                    .url(url)
                    .header("apikey", key)
                    .header("Authorization", "Bearer $key")
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                response.close()

                if (response.code in 200..299) {
                    Log.d(TAG, "Plan saved to Supabase")
                    Result.success(Unit)
                } else {
                    Log.e(TAG, "Supabase save error: ${response.code} - ${responseBody.take(200)}")
                    Result.failure(Exception("Supabase error: ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Supabase save failed: ${e.message}", e)
                Result.failure(e)
            }
        }

    suspend fun getWorkoutPlan(deviceId: String): Result<WorkoutPlan?> =
        withContext(Dispatchers.IO) {
            if (!isConfigured()) {
                Log.d(TAG, "Supabase not configured, skipping load")
                return@withContext Result.success(null)
            }

            try {
                val url = "${SettingsManager.supabaseUrl}/rest/v1/workout_plans?device_id=eq.$deviceId&order=created_at.desc&limit=1"
                val key = SettingsManager.supabaseAnonKey

                val request = Request.Builder()
                    .url(url)
                    .header("apikey", key)
                    .header("Authorization", "Bearer $key")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                response.close()

                if (response.code !in 200..299) {
                    Log.e(TAG, "Supabase load error: ${response.code}")
                    return@withContext Result.failure(Exception("Supabase error: ${response.code}"))
                }

                val arr = JSONArray(responseBody)
                if (arr.length() == 0) {
                    return@withContext Result.success(null)
                }

                val row = arr.getJSONObject(0)
                val planJson = row.get("plan_json")
                val planObj = when (planJson) {
                    is JSONObject -> planJson
                    is String -> JSONObject(planJson)
                    else -> return@withContext Result.success(null)
                }

                val plan = deserializePlan(
                    planObj,
                    row.optString("gender", ""),
                    row.optString("workout_frequency", ""),
                    row.optString("weekly_goal", ""),
                    row.optString("difficulty", "Beginner")
                )
                Result.success(plan)
            } catch (e: Exception) {
                Log.e(TAG, "Supabase load failed: ${e.message}", e)
                Result.failure(e)
            }
        }

    private fun serializePlan(plan: WorkoutPlan): JSONObject {
        val daysArray = JSONArray()
        for (day in plan.days) {
            val dayObj = JSONObject().apply {
                put("dayOfWeek", day.dayOfWeek)
                put("isRestDay", day.isRestDay)
                put("focusArea", day.focusArea)
                if (day.workout != null) {
                    put("workout", JSONObject().apply {
                        put("id", day.workout.id)
                        put("name", day.workout.name)
                        put("category", day.workout.category.name)
                        put("difficulty", day.workout.difficulty.name)
                        put("durationMin", day.workout.durationMin)
                        put("exercises", JSONArray().apply {
                            for (ex in day.workout.exercises) {
                                put(JSONObject().apply {
                                    put("name", ex.name)
                                    if (ex.sets != null) put("sets", ex.sets)
                                    if (ex.reps != null) put("reps", ex.reps)
                                    if (ex.durationSec != null) put("durationSec", ex.durationSec)
                                })
                            }
                        })
                    })
                } else {
                    put("workout", JSONObject.NULL)
                }
            }
            daysArray.put(dayObj)
        }

        return JSONObject().apply {
            put("id", plan.id)
            put("generatedAt", plan.generatedAt)
            put("days", daysArray)
        }
    }

    private fun deserializePlan(
        planObj: JSONObject,
        gender: String,
        frequency: String,
        weeklyGoal: String,
        difficulty: String
    ): WorkoutPlan {
        val daysArray = planObj.getJSONArray("days")
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

                val diffLevel = try {
                    Difficulty.valueOf(wObj.optString("difficulty", difficulty))
                } catch (_: Exception) {
                    Difficulty.Beginner
                }

                workout = Workout(
                    id = wObj.optString("id", "plan_${dayObj.getString("dayOfWeek").take(3).lowercase()}"),
                    name = wObj.getString("name"),
                    category = category,
                    difficulty = diffLevel,
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

        return WorkoutPlan(
            id = planObj.optString("id", ""),
            generatedAt = planObj.optString("generatedAt", ""),
            gender = gender,
            workoutFrequency = frequency,
            days = days,
            weeklyGoal = weeklyGoal,
            difficultyLevel = try { Difficulty.valueOf(difficulty) } catch (_: Exception) { Difficulty.Beginner }
        )
    }
}
