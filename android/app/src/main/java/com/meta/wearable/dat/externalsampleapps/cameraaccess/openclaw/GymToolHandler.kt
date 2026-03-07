package com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.meta.wearable.dat.externalsampleapps.cameraaccess.gemini.GeminiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

data class RepCounterState(
    val active: Boolean = false,
    val exercise: String = "",
    val repCount: Int = 0
)

data class MusicState(
    val active: Boolean = false,
    val prompt: String = "",
    val bpm: Int = 120
)

data class ExerciseGuideState(
    val isGenerating: Boolean = false,
    val imageBase64: String? = null,
    val description: String? = null,
    val error: String? = null
)

class GymToolHandler(private val scope: CoroutineScope, private val musicService: MusicStreamingService) {
    companion object {
        private const val TAG = "GymToolHandler"
    }

    private val _toolCallStatus = MutableStateFlow<ToolCallStatus>(ToolCallStatus.Idle)
    val toolCallStatus: StateFlow<ToolCallStatus> = _toolCallStatus.asStateFlow()

    private val _repCounter = MutableStateFlow(RepCounterState())
    val repCounter: StateFlow<RepCounterState> = _repCounter.asStateFlow()

    private val _music = MutableStateFlow(MusicState())
    val music: StateFlow<MusicState> = _music.asStateFlow()

    private val _exerciseGuide = MutableStateFlow(ExerciseGuideState())
    val exerciseGuide: StateFlow<ExerciseGuideState> = _exerciseGuide.asStateFlow()

    private val inFlightJobs = mutableMapOf<String, Job>()

    // Provide latest camera frame for exercise guide
    var latestFrame: Bitmap? = null

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    fun handleToolCall(
        call: GeminiFunctionCall,
        sendResponse: (JSONObject) -> Unit
    ) {
        val job = scope.launch {
            _toolCallStatus.value = ToolCallStatus.Executing(call.name)

            val result = when (call.name) {
                "start_rep_counting" -> handleStartRepCounting(call.args)
                "stop_rep_counting" -> handleStopRepCounting()
                "get_rep_count" -> handleGetRepCount()
                "play_music" -> handlePlayMusic(call.args)
                "stop_music" -> handleStopMusic()
                "change_music" -> handleChangeMusic(call.args)
                "generate_exercise_guide" -> handleGenerateExerciseGuide(call.args)
                else -> JSONObject().put("success", false).put("error", "Unknown tool: ${call.name}")
            }

            _toolCallStatus.value = ToolCallStatus.Completed(call.name)

            val response = JSONObject().apply {
                put("toolResponse", JSONObject().apply {
                    put("functionResponses", JSONArray().put(JSONObject().apply {
                        put("id", call.id)
                        put("name", call.name)
                        put("response", JSONObject().put("output", result))
                    }))
                })
            }
            sendResponse(response)
            inFlightJobs.remove(call.id)
        }
        inFlightJobs[call.id] = job
    }

    fun cancelAll() {
        inFlightJobs.values.forEach { it.cancel() }
        inFlightJobs.clear()
    }

    fun cancelToolCalls(ids: List<String>) {
        ids.forEach { id ->
            inFlightJobs[id]?.cancel()
            inFlightJobs.remove(id)
        }
    }

    fun dismissExerciseGuide() {
        _exerciseGuide.value = ExerciseGuideState()
    }

    // --- Tool Handlers ---

    private fun handleStartRepCounting(args: Map<String, Any?>): JSONObject {
        val exercise = args["exercise"]?.toString() ?: "bicep_curl"
        _repCounter.value = RepCounterState(active = true, exercise = exercise, repCount = 0)
        Log.d(TAG, "Started rep counting: $exercise")
        return JSONObject().apply {
            put("success", true)
            put("message", "Started counting $exercise reps. The camera is tracking your movements.")
        }
    }

    private fun handleStopRepCounting(): JSONObject {
        val finalCount = _repCounter.value.repCount
        val exercise = _repCounter.value.exercise
        _repCounter.value = RepCounterState()
        Log.d(TAG, "Stopped rep counting. Final: $finalCount")
        return JSONObject().apply {
            put("success", true)
            put("totalReps", finalCount)
            put("message", "Stopped counting. Final rep count for $exercise: $finalCount")
        }
    }

    private fun handleGetRepCount(): JSONObject {
        val state = _repCounter.value
        return JSONObject().apply {
            put("success", true)
            put("active", state.active)
            put("exercise", state.exercise)
            put("reps", state.repCount)
        }
    }

    fun incrementRepCount() {
        val current = _repCounter.value
        if (current.active) {
            _repCounter.value = current.copy(repCount = current.repCount + 1)
        }
    }

    private fun handlePlayMusic(args: Map<String, Any?>): JSONObject {
        val prompt = args["prompt"]?.toString() ?: "energetic workout music"
        val bpm = (args["bpm"] as? Number)?.toInt() ?: 120
        _music.value = MusicState(active = true, prompt = prompt, bpm = bpm)
        musicService.play(prompt, bpm)
        Log.d(TAG, "Play music: $prompt at $bpm BPM")
        return JSONObject().apply {
            put("success", true)
            put("message", "Playing: $prompt at $bpm BPM")
        }
    }

    private fun handleStopMusic(): JSONObject {
        musicService.stop()
        _music.value = MusicState()
        Log.d(TAG, "Music stopped")
        return JSONObject().apply {
            put("success", true)
            put("message", "Music stopped")
        }
    }

    private fun handleChangeMusic(args: Map<String, Any?>): JSONObject {
        val current = _music.value
        val newPrompt = args["prompt"]?.toString() ?: current.prompt
        val newBpm = (args["bpm"] as? Number)?.toInt() ?: current.bpm
        _music.value = current.copy(prompt = newPrompt, bpm = newBpm)
        if (newPrompt != current.prompt) musicService.updatePrompt(newPrompt)
        if (newBpm != current.bpm) musicService.updateBpm(newBpm)
        Log.d(TAG, "Music changed: $newPrompt at $newBpm BPM")
        return JSONObject().apply {
            put("success", true)
            put("message", "Music updated to: $newPrompt at $newBpm BPM")
        }
    }

    private suspend fun handleGenerateExerciseGuide(args: Map<String, Any?>): JSONObject {
        val exerciseDesc = args["exercise_description"]?.toString() ?: "exercise"
        val frame = latestFrame
        if (frame == null) {
            _exerciseGuide.value = ExerciseGuideState(error = "Camera not active")
            return JSONObject().apply {
                put("success", false)
                put("error", "Camera not active, cannot capture frame")
            }
        }

        _exerciseGuide.value = ExerciseGuideState(isGenerating = true)

        return withContext(Dispatchers.IO) {
            try {
                val baos = ByteArrayOutputStream()
                frame.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                val imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", imageBase64)
                                })
                            })
                            put(JSONObject().apply {
                                put("text", """You are a professional fitness coach and image editor.
Look at this photo of a person near a gym machine/equipment.

Generate a NEW image that shows the SAME person in the SAME location, but now performing the exercise "$exerciseDesc" with CORRECT form on the machine/equipment visible in the photo.

The generated image should:
- Show proper body positioning and form for the exercise
- Keep the same gym environment/background
- Clearly demonstrate the correct posture and grip
- Be realistic and helpful as a visual guide

Also provide a brief 1-2 sentence text description of the key form cues.""")
                            })
                        })
                    }))
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply {
                            put("TEXT")
                            put("IMAGE")
                        })
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-05-20:generateContent?key=${GeminiConfig.apiKey}"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                response.close()

                if (response.code !in 200..299) {
                    Log.e(TAG, "Exercise guide API error: ${response.code} - ${responseBody.take(200)}")
                    _exerciseGuide.value = ExerciseGuideState(error = "API error: ${response.code}")
                    return@withContext JSONObject().apply {
                        put("success", false)
                        put("error", "Failed to generate exercise guide image")
                    }
                }

                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")

                var resultImage: String? = null
                var resultDesc = ""

                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("inlineData")) {
                            resultImage = part.getJSONObject("inlineData").optString("data", "")
                        }
                        if (part.has("text")) {
                            resultDesc += part.getString("text")
                        }
                    }
                }

                if (resultImage != null) {
                    _exerciseGuide.value = ExerciseGuideState(
                        imageBase64 = resultImage,
                        description = resultDesc
                    )
                    Log.d(TAG, "Exercise guide generated for: $exerciseDesc")
                    JSONObject().apply {
                        put("success", true)
                        put("message", "Exercise guide image generated for \"$exerciseDesc\". The user can see it on screen now. Describe the key form cues verbally.")
                    }
                } else {
                    _exerciseGuide.value = ExerciseGuideState(
                        description = resultDesc,
                        error = if (resultDesc.isEmpty()) "No image generated" else null
                    )
                    JSONObject().apply {
                        put("success", true)
                        put("message", resultDesc.ifEmpty { "Could not generate image, try a different description." })
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exercise guide error: ${e.message}")
                _exerciseGuide.value = ExerciseGuideState(error = e.message)
                JSONObject().apply {
                    put("success", false)
                    put("error", "Failed to generate: ${e.message}")
                }
            }
        }
    }
}
