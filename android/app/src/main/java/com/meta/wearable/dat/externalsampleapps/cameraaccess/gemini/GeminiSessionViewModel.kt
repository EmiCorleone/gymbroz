package com.meta.wearable.dat.externalsampleapps.cameraaccess.gemini

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.ExerciseGuideState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.GymToolHandler
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.MusicState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.MusicStreamingService
import com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.PoseDetectionManager
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

class GeminiSessionViewModel : ViewModel() {
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
    private var lastVideoFrameTime: Long = 0
    private var stateObservationJob: Job? = null

    var streamingMode: StreamingMode = StreamingMode.GLASSES

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
                while (isActive) {
                    delay(100)
                    val isSpeaking = geminiService.isModelSpeaking.value
                    // Duck music when model speaks, unduck when done
                    if (isSpeaking != wasSpeaking) {
                        if (isSpeaking) music.duck() else music.unduck()
                        wasSpeaking = isSpeaking
                    }
                    val poseData = poseDetectionManager?.overlayData?.value ?: PoseOverlayData()
                    // Inject rep count updates into Gemini context
                    val currentReps = poseData.repCount
                    if (handler.repCounter.value.active && currentReps != lastRepCount && currentReps > 0) {
                        lastRepCount = currentReps
                        geminiService.sendContextText(
                            "[System: Rep counter update — ${handler.repCounter.value.exercise}: $currentReps reps completed]"
                        )
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
        _uiState.value = GeminiUiState()
    }

    fun sendVideoFrameIfThrottled(bitmap: Bitmap) {
        if (!_uiState.value.isGeminiActive) return
        if (_uiState.value.connectionState != GeminiConnectionState.Ready) return

        // Run pose detection on every frame (not throttled)
        if (_uiState.value.repCounter.active) {
            poseDetectionManager?.processFrame(bitmap)
        }

        val now = System.currentTimeMillis()
        if (now - lastVideoFrameTime < GeminiConfig.VIDEO_FRAME_INTERVAL_MS) return
        lastVideoFrameTime = now
        geminiService.sendVideoFrame(bitmap)
        // Keep latest frame for exercise guide
        gymToolHandler?.latestFrame = bitmap
    }

    fun dismissExerciseGuide() {
        gymToolHandler?.dismissExerciseGuide()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}
