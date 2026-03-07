package com.meta.wearable.dat.externalsampleapps.cameraaccess.settings

import android.content.Context
import android.content.SharedPreferences
import com.meta.wearable.dat.externalsampleapps.cameraaccess.Secrets

object SettingsManager {
    private const val PREFS_NAME = "visionclaw_settings"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var geminiAPIKey: String
        get() = prefs.getString("geminiAPIKey", null) ?: Secrets.geminiAPIKey
        set(value) = prefs.edit().putString("geminiAPIKey", value).apply()

    var geminiSystemPrompt: String
        get() = prefs.getString("geminiSystemPrompt", null) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit().putString("geminiSystemPrompt", value).apply()

    var openClawHost: String
        get() = prefs.getString("openClawHost", null) ?: Secrets.openClawHost
        set(value) = prefs.edit().putString("openClawHost", value).apply()

    var openClawPort: Int
        get() {
            val stored = prefs.getInt("openClawPort", 0)
            return if (stored != 0) stored else Secrets.openClawPort
        }
        set(value) = prefs.edit().putInt("openClawPort", value).apply()

    var openClawHookToken: String
        get() = prefs.getString("openClawHookToken", null) ?: Secrets.openClawHookToken
        set(value) = prefs.edit().putString("openClawHookToken", value).apply()

    var openClawGatewayToken: String
        get() = prefs.getString("openClawGatewayToken", null) ?: Secrets.openClawGatewayToken
        set(value) = prefs.edit().putString("openClawGatewayToken", value).apply()

    var webrtcSignalingURL: String
        get() = prefs.getString("webrtcSignalingURL", null) ?: Secrets.webrtcSignalingURL
        set(value) = prefs.edit().putString("webrtcSignalingURL", value).apply()

    var supabaseUrl: String
        get() = prefs.getString("supabaseUrl", null) ?: Secrets.supabaseUrl
        set(value) = prefs.edit().putString("supabaseUrl", value).apply()

    var supabaseAnonKey: String
        get() = prefs.getString("supabaseAnonKey", null) ?: Secrets.supabaseAnonKey
        set(value) = prefs.edit().putString("supabaseAnonKey", value).apply()

    fun resetAll() {
        prefs.edit().clear().apply()
    }

    const val DEFAULT_SYSTEM_PROMPT = """You are a motivational AI gym assistant on smart glasses. You can see through the user's camera and hear them in real time.

CORE BEHAVIOR:
1. AUTOMATICALLY start counting reps when you see the user performing an exercise — do NOT wait for them to ask. As soon as you see repetitive exercise movement (like bicep curls, arm movements, etc.), immediately call start_rep_counting with exercise "bicep_curl".
2. Use get_rep_count frequently (every few seconds) while counting is active to give verbal encouragement and updates like "Great form! That's 5 reps!"
3. When the user clearly stops exercising or says they're done, call stop_rep_counting and announce the final count.
4. If they start a new set, call start_rep_counting again — the tool can be called multiple times, it resets each time.
5. When they ask for music, use play_music with an energetic style matching the workout intensity.

EXERCISE GUIDE:
6. When the user asks how to use a gym machine, wants to see proper form, or says things like "how do I use this?", "show me how to do this exercise", or "what exercise can I do here" — call generate_exercise_guide with a description of the exercise. This will capture the current camera view and generate an image showing correct form on that machine.
7. While the image is generating, explain the key form cues verbally so the user can start getting ready.

IMPORTANT RULES:
- Be PROACTIVE — don't wait to be told to count. If you see exercise happening, start counting immediately.
- You can always restart rep counting by calling start_rep_counting again. It will reset and begin fresh.
- Be concise and energetic in your voice responses — this is real-time conversation.
- Give form feedback based on what you see through the camera.
- Provide encouragement and motivation throughout the workout.
- When near a machine, if the user seems unsure, proactively offer to show them the correct form using generate_exercise_guide."""
}
