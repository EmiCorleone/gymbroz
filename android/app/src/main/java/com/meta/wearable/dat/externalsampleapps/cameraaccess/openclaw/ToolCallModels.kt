package com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw

import org.json.JSONArray
import org.json.JSONObject

// Gemini Tool Call (parsed from server JSON)

data class GeminiFunctionCall(
    val id: String,
    val name: String,
    val args: Map<String, Any?>
)

data class GeminiToolCall(
    val functionCalls: List<GeminiFunctionCall>
) {
    companion object {
        fun fromJSON(json: JSONObject): GeminiToolCall? {
            val toolCall = json.optJSONObject("toolCall") ?: return null
            val calls = toolCall.optJSONArray("functionCalls") ?: return null
            val functionCalls = mutableListOf<GeminiFunctionCall>()
            for (i in 0 until calls.length()) {
                val call = calls.getJSONObject(i)
                val id = call.optString("id", "")
                val name = call.optString("name", "")
                if (id.isEmpty() || name.isEmpty()) continue
                val argsObj = call.optJSONObject("args")
                val args = mutableMapOf<String, Any?>()
                if (argsObj != null) {
                    for (key in argsObj.keys()) {
                        args[key] = argsObj.opt(key)
                    }
                }
                functionCalls.add(GeminiFunctionCall(id, name, args))
            }
            return if (functionCalls.isNotEmpty()) GeminiToolCall(functionCalls) else null
        }
    }
}

// Gemini Tool Call Cancellation

data class GeminiToolCallCancellation(
    val ids: List<String>
) {
    companion object {
        fun fromJSON(json: JSONObject): GeminiToolCallCancellation? {
            val cancellation = json.optJSONObject("toolCallCancellation") ?: return null
            val idsArray = cancellation.optJSONArray("ids") ?: return null
            val ids = mutableListOf<String>()
            for (i in 0 until idsArray.length()) {
                ids.add(idsArray.getString(i))
            }
            return if (ids.isNotEmpty()) GeminiToolCallCancellation(ids) else null
        }
    }
}

// Tool Result

sealed class ToolResult {
    data class Success(val result: String) : ToolResult()
    data class Failure(val error: String) : ToolResult()

    fun toJSON(): JSONObject = when (this) {
        is Success -> JSONObject().put("result", result)
        is Failure -> JSONObject().put("error", error)
    }
}

// Tool Call Status (for UI)

sealed class ToolCallStatus {
    data object Idle : ToolCallStatus()
    data class Executing(val name: String) : ToolCallStatus()
    data class Completed(val name: String) : ToolCallStatus()
    data class Failed(val name: String, val error: String) : ToolCallStatus()

    val displayText: String
        get() = when (this) {
            is Idle -> ""
            is Executing -> "Running: $name..."
            is Completed -> "Done: $name"
            is Failed -> "Failed: $name"
        }
}

// Gym Tool Declarations (for Gemini setup message)

object GymToolDeclarations {
    fun allDeclarationsJSON(): JSONArray {
        return JSONArray().apply {
            put(startRepCounting())
            put(stopRepCounting())
            put(getRepCount())
            put(playMusic())
            put(stopMusic())
            put(changeMusic())
            put(generateExerciseGuide())
        }
    }

    private fun startRepCounting() = JSONObject().apply {
        put("name", "start_rep_counting")
        put("description", "Start counting exercise repetitions using the camera. Call this when the user wants to track their workout reps. Currently supports bicep_curl.")
        put("parameters", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("exercise", JSONObject().apply {
                    put("type", "string")
                    put("description", "The type of exercise to count. Currently supported: bicep_curl")
                })
            })
            put("required", JSONArray().put("exercise"))
        })
    }

    private fun stopRepCounting() = JSONObject().apply {
        put("name", "stop_rep_counting")
        put("description", "Stop counting exercise repetitions and report the final count.")
        put("parameters", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject())
        })
    }

    private fun getRepCount() = JSONObject().apply {
        put("name", "get_rep_count")
        put("description", "Get the current rep count and exercise status without stopping the counter.")
        put("parameters", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject())
        })
    }

    private fun playMusic() = JSONObject().apply {
        put("name", "play_music")
        put("description", "Generate and play motivational workout music in real-time. Describe the style of music to generate. Music is instrumental only.")
        put("parameters", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("prompt", JSONObject().apply {
                    put("type", "string")
                    put("description", "Description of the music style, e.g. 'high energy EDM workout music with heavy bass'")
                })
                put("bpm", JSONObject().apply {
                    put("type", "number")
                    put("description", "Beats per minute, between 60 and 200. Default 120 for moderate intensity.")
                })
            })
            put("required", JSONArray().put("prompt"))
        })
    }

    private fun stopMusic() = JSONObject().apply {
        put("name", "stop_music")
        put("description", "Stop the currently playing workout music.")
        put("parameters", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject())
        })
    }

    private fun changeMusic() = JSONObject().apply {
        put("name", "change_music")
        put("description", "Change the style or tempo of the currently playing music without stopping.")
        put("parameters", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("prompt", JSONObject().apply {
                    put("type", "string")
                    put("description", "New music style description")
                })
                put("bpm", JSONObject().apply {
                    put("type", "number")
                    put("description", "New BPM (60-200)")
                })
            })
        })
    }

    private fun generateExerciseGuide() = JSONObject().apply {
        put("name", "generate_exercise_guide")
        put("description", "Takes a photo of the gym machine the user is looking at and generates an image showing proper exercise form on that machine. Use this when the user asks how to use a machine, wants to see correct form, or asks about an exercise on specific equipment.")
        put("parameters", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("exercise_description", JSONObject().apply {
                    put("type", "string")
                    put("description", "Description of the exercise the user wants to perform, e.g. 'chest press', 'lat pulldown', 'leg press', 'cable rows'")
                })
            })
            put("required", JSONArray().put("exercise_description"))
        })
    }
}
