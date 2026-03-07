package com.meta.wearable.dat.externalsampleapps.cameraaccess.gemini

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.externalsampleapps.cameraaccess.data.WorkoutRepository
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.ExerciseGuideState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.GeminiFunctionCall
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.GymToolHandler
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.MusicState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.MusicStreamingService
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.PoseDetectionManager
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.PovRepCounter
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.PoseOverlayData
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.RepCounterState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.ToolCallStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.stream.StreamingMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GeminiUiState(
    val isGeminiActive: Boolean = false,
    val connectionState: GeminiConnectionState = GeminiConnectionState.Disconnected,
    val isModelSpeaking: Boolean = false,
    val errorMessage: String? = null,
    val userTranscript: String = "",
    val aiTranscript: String = "",
    val toolCallStatus: ToolCallStatus = ToolCallStatus.Idle,
    val repCounter: RepCounterState = RepCounterState(),
    val music: MusicState = MusicState(),
    val exerciseGuide: ExerciseGuideState = ExerciseGuideState(),
    val poseOverlay: PoseOverlayData = PoseOverlayData(),
)

class GeminiSessionViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "GeminiSessionVM"
    }

    private val _uiState = MutableStateFlow(GeminiUiState())
    val uiState: StateFlow<GeminiUiState> = _uiState.asStateFlow()

    private val geminiService = GeminiLiveService()
    private val audioManager = AudioManager()
    private var gymToolHandler: GymToolHandler? = null
    private var musicService: MusicStreamingService? = null
    var poseDetectionManager: PoseDetectionManager? = null
    var povRepCounter: PovRepCounter? = null
    // When true, use POV hand tracking instead of 3rd-person pose detection
    var usePovMode: Boolean = true
    private var lastVideoFrameTime: Long = 0
    private var stateObservationJob: Job? = null

    var streamingMode: StreamingMode = StreamingMode.GLASSES
    var currentSessionId: Long? = null
    private val repository by lazy { WorkoutRepository(application) }

    private suspend fun loadMirrorPhotoPath(): String? {
        return WorkoutRepository(getApplication()).getProfile()?.mirrorPhotoPath
    }

    fun startSession() {
        if (_uiState.value.isGeminiActive) return

        if (!GeminiConfig.isConfigured) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Gemini API key not configured. Open Settings and add your key from https://aistudio.google.com/apikey"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isGeminiActive = true)

        // Wire audio callbacks
        audioManager.onAudioCaptured = lambda@{ data ->
            // Phone mode: mute mic while model speaks to prevent echo
            if (streamingMode == StreamingMode.PHONE && geminiService.isModelSpeaking.value) return@lambda
            geminiService.sendAudio(data)
        }

        geminiService.onAudioReceived = { data ->
            audioManager.playAudio(data)
        }

        geminiService.onInterrupted = {
            audioManager.stopPlayback()
        }

        geminiService.onTurnComplete = {
            _uiState.value = _uiState.value.copy(userTranscript = "")
        }

        geminiService.onInputTranscription = { text ->
            _uiState.value = _uiState.value.copy(
                userTranscript = _uiState.value.userTranscript + text,
                aiTranscript = ""
            )
        }

        geminiService.onOutputTranscription = { text ->
            _uiState.value = _uiState.value.copy(
                aiTranscript = _uiState.value.aiTranscript + text
            )
        }

        geminiService.onDisconnected = { reason ->
            if (_uiState.value.isGeminiActive) {
                stopSession()
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Connection lost: ${reason ?: "Unknown error"}"
                )
            }
        }

        viewModelScope.launch {
            // Set up music service and gym tool handler
            val music = MusicStreamingService(viewModelScope)
            musicService = music
            val handler = GymToolHandler(viewModelScope, music)
            handler.mirrorPhotoPath = loadMirrorPhotoPath()
            gymToolHandler = handler

            geminiService.onToolCall = { toolCall ->
                for (call in toolCall.functionCalls) {
                    handler.handleToolCall(call) { response ->
                        geminiService.sendToolResponse(response)
                    }
                }
            }

            geminiService.onToolCallCancellation = { cancellation ->
                handler.cancelToolCalls(cancellation.ids)
            }

            // Observe service + tool state
            stateObservationJob = viewModelScope.launch {
                var wasSpeaking = false
                var lastRepCount = -1
                var lastRepInjectionTime = 0L
                while (isActive) {
                    delay(100)
                    val isSpeaking = geminiService.isModelSpeaking.value
                    // Duck music when model speaks, unduck when done
                    if (isSpeaking != wasSpeaking) {
                        if (isSpeaking) music.duck() else music.unduck()
                        wasSpeaking = isSpeaking
                    }
                    val poseData = if (usePovMode) {
                        povRepCounter?.overlayData?.value ?: PoseOverlayData()
                    } else {
                        poseDetectionManager?.overlayData?.value ?: PoseOverlayData()
                    }
                    // Inject rep count updates into Gemini context (throttled to every 5s)
                    val currentReps = poseData.repCount
                    val now = System.currentTimeMillis()
                    val repCounterActive = handler.repCounter.value.active
                    val exerciseName = handler.repCounter.value.exercise
                    
                    if (repCounterActive && currentReps != lastRepCount && currentReps > 0) {
                        lastRepCount = currentReps
                        
                        // Log to Supabase immediately in the background
                        currentSessionId?.let { sId ->
                            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                repository.logRepEvent(sId, exerciseName, currentReps)
                            }
                        }

                        if (now - lastRepInjectionTime > 5000) {
                            lastRepInjectionTime = now
                            geminiService.sendContextText(
                                "[System update — do NOT respond to this message verbally unless asked. " +
                                "Rep counter: ${exerciseName}, current count: $currentReps reps]"
                            )
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        connectionState = geminiService.connectionState.value,
                        isModelSpeaking = isSpeaking,
                        toolCallStatus = handler.toolCallStatus.value,
                        repCounter = handler.repCounter.value.copy(repCount = poseData.repCount),
                        music = handler.music.value,
                        exerciseGuide = handler.exerciseGuide.value,
                        poseOverlay = poseData,
                    )
                }
            }

            // Connect to Gemini
            geminiService.connect { setupOk ->
                if (!setupOk) {
                    val msg = when (val state = geminiService.connectionState.value) {
                        is GeminiConnectionState.Error -> state.message
                        else -> "Failed to connect to Gemini"
                    }
                    _uiState.value = _uiState.value.copy(errorMessage = msg)
                    geminiService.disconnect()
                    stateObservationJob?.cancel()
                    _uiState.value = _uiState.value.copy(
                        isGeminiActive = false,
                        connectionState = GeminiConnectionState.Disconnected
                    )
                    return@connect
                }

                // Start mic capture
                try {
                    audioManager.startCapture()
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Mic capture failed: ${e.message}"
                    )
                    geminiService.disconnect()
                    stateObservationJob?.cancel()
                    _uiState.value = _uiState.value.copy(
                        isGeminiActive = false,
                        connectionState = GeminiConnectionState.Disconnected
                    )
                }
            }
        }
    }

    fun stopSession() {
        gymToolHandler?.cancelAll()
        gymToolHandler = null
        musicService?.stop()
        musicService = null
        audioManager.stopCapture()
        geminiService.disconnect()
        stateObservationJob?.cancel()
        stateObservationJob = null
        povRepCounter?.reset()
        _uiState.value = GeminiUiState()
    }

    fun sendVideoFrameIfThrottled(bitmap: Bitmap) {
        latestFrame = bitmap
        gymToolHandler?.latestFrame = bitmap

        // Run rep counting on every frame when active (works without Gemini)
        if (_uiState.value.repCounter.active) {
            val overlay = if (usePovMode) {
                povRepCounter?.processFrame(bitmap)
            } else {
                poseDetectionManager?.processFrame(bitmap)
            }
            if (overlay != null) {
                val poseData = if (usePovMode) {
                    povRepCounter?.overlayData?.value ?: PoseOverlayData()
                } else {
                    poseDetectionManager?.overlayData?.value ?: PoseOverlayData()
                }
                _uiState.value = _uiState.value.copy(
                    repCounter = _uiState.value.repCounter.copy(repCount = poseData.repCount),
                    poseOverlay = poseData,
                )
            }
        }

        if (!_uiState.value.isGeminiActive) return
        if (_uiState.value.connectionState != GeminiConnectionState.Ready) return
        val now = System.currentTimeMillis()
        if (now - lastVideoFrameTime < GeminiConfig.VIDEO_FRAME_INTERVAL_MS) return
        lastVideoFrameTime = now
        geminiService.sendVideoFrame(bitmap)
    }

    fun toggleRepCounting() {
        val current = _uiState.value.repCounter
        if (current.active) {
            // Stop rep counting
            poseDetectionManager?.reset()
            povRepCounter?.reset()
            _uiState.value = _uiState.value.copy(
                repCounter = RepCounterState(),
                poseOverlay = PoseOverlayData(),
            )
        } else {
            // Start rep counting for bicep curls
            poseDetectionManager?.reset()
            povRepCounter?.reset()
            _uiState.value = _uiState.value.copy(
                repCounter = RepCounterState(active = true, exercise = "bicep_curl", repCount = 0),
            )
        }
    }

    // Store latest frame even without Gemini active (for test button)
    var latestFrame: Bitmap? = null
    private var testGuideJob: Job? = null
    private var testHandler: GymToolHandler? = null

    fun testExerciseGuide() {
        Log.d(TAG, "testExerciseGuide called, geminiActive=${_uiState.value.isGeminiActive}")
        if (_uiState.value.exerciseGuide.isGenerating) return // already in progress
        // When Gemini is active, the AI handles exercise guide via tool calls — don't duplicate
        if (_uiState.value.isGeminiActive) return

        _uiState.value = _uiState.value.copy(
            exerciseGuide = ExerciseGuideState(isGenerating = true)
        )

        testGuideJob?.cancel()
        testGuideJob = viewModelScope.launch {
            val handler = testHandler ?: GymToolHandler(viewModelScope, MusicStreamingService(viewModelScope))
            testHandler = handler
            // Always load the mirror photo path from the profile
            handler.mirrorPhotoPath = loadMirrorPhotoPath()

            Log.d(TAG, "testExerciseGuide: mirrorPhotoPath=${handler.mirrorPhotoPath}")

            handler.handleToolCall(
                GeminiFunctionCall(
                    id = "test_guide_${System.currentTimeMillis()}",
                    name = "generate_exercise_guide",
                    args = mapOf("exercise_description" to "bench press")
                )
            ) { /* ignore tool response */ }

            // Poll handler state until result arrives
            var pollCount = 0
            while (isActive) {
                delay(200)
                val guideState = handler.exerciseGuide.value
                _uiState.value = _uiState.value.copy(exerciseGuide = guideState)
                pollCount++
                if (pollCount % 10 == 0) {
                    Log.d(TAG, "testExerciseGuide poll #$pollCount: isGenerating=${guideState.isGenerating}, hasImage=${guideState.imageBase64 != null}, error=${guideState.error}")
                }
                if (!guideState.isGenerating && (guideState.imageBase64 != null || guideState.error != null)) {
                    Log.d(TAG, "testExerciseGuide: result arrived! hasImage=${guideState.imageBase64 != null}, error=${guideState.error}")
                    break
                }
            }
        }
    }

    fun dismissExerciseGuide() {
        testGuideJob?.cancel()
        gymToolHandler?.dismissExerciseGuide()
        testHandler?.dismissExerciseGuide()
        _uiState.value = _uiState.value.copy(exerciseGuide = ExerciseGuideState())
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}
