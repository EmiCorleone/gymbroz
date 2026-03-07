package com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * POV (first-person) rep counter using MediaPipe Hand Landmarker.
 *
 * Tracks the wrist landmark Y-position to detect bicep curl reps from a
 * first-person camera perspective (e.g. smart glasses or phone held in front).
 *
 * Rep detection logic:
 * - Wrist Y > UP_THRESHOLD (hand low in frame = arm extended) → "down" stage
 * - Wrist Y < DOWN_THRESHOLD (hand high in frame = arm curled) and was "down" → rep counted
 */
class PovRepCounter(context: Context) {
    companion object {
        private const val TAG = "PovRepCounter"
        private const val MODEL_ASSET = "hand_landmarker.task"

        // Y thresholds (normalized 0-1, where 0=top, 1=bottom of frame)
        // Hand moves UP in frame when curling (Y decreases)
        const val DOWN_THRESHOLD = 0.563f  // hand is high enough = curled position
        const val UP_THRESHOLD = 0.711f   // hand is low enough = extended position

        // Wrist landmark index in MediaPipe hand model
        const val WRIST_LANDMARK = 0
        // Middle finger MCP - useful for tracking hand position
        const val MIDDLE_MCP = 9
    }

    private var handLandmarker: HandLandmarker? = null
    private var isInitialized = false
    private var timestampMs: Long = 0

    // Rep counting state
    private var repCount = 0
    private var stage: String? = null // "up" (extended) or "down" (curled)
    private var wristY = 0f

    private val _overlayData = MutableStateFlow(PoseOverlayData())
    val overlayData: StateFlow<PoseOverlayData> = _overlayData.asStateFlow()

    private val landmarkPaint = Paint().apply {
        color = Color.parseColor("#00ff88")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val connectionPaint = Paint().apply {
        color = Color.parseColor("#00ff88")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val wristPaint = Paint().apply {
        color = Color.parseColor("#ff4400")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val hudBgPaint = Paint().apply {
        color = Color.argb(200, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val hudTextPaint = Paint().apply {
        color = Color.parseColor("#00ff88")
        textSize = 72f
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val hudSmallPaint = Paint().apply {
        color = Color.parseColor("#ffff00")
        textSize = 32f
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val stagePaint = Paint().apply {
        textSize = 28f
        isAntiAlias = true
    }

    // Hand connections for drawing skeleton
    private val handConnections = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,       // thumb
        0 to 5, 5 to 6, 6 to 7, 7 to 8,       // index
        0 to 9, 9 to 10, 10 to 11, 11 to 12,  // middle
        0 to 13, 13 to 14, 14 to 15, 15 to 16, // ring
        0 to 17, 17 to 18, 18 to 19, 19 to 20, // pinky
        5 to 9, 9 to 13, 13 to 17              // palm
    )

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET)
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.VIDEO)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            isInitialized = true
            Log.d(TAG, "HandLandmarker initialized for POV rep counting")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize HandLandmarker: ${e.message}")
        }
    }

    fun processFrame(bitmap: Bitmap): Bitmap? {
        if (!isInitialized || handLandmarker == null) return null

        val mpImage = BitmapImageBuilder(bitmap).build()
        timestampMs += 33 // ~30fps

        val result: HandLandmarkerResult
        try {
            result = handLandmarker!!.detectForVideo(mpImage, timestampMs)
        } catch (e: Exception) {
            Log.e(TAG, "Detection error: ${e.message}")
            return null
        }

        if (result.landmarks().isEmpty()) return null

        val overlayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(overlayBitmap)
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        val landmarks = result.landmarks()[0]

        // Draw hand skeleton
        for ((a, b) in handConnections) {
            if (a < landmarks.size && b < landmarks.size) {
                val la = landmarks[a]
                val lb = landmarks[b]
                canvas.drawLine(la.x() * w, la.y() * h, lb.x() * w, lb.y() * h, connectionPaint)
            }
        }

        // Draw landmarks
        for (lm in landmarks) {
            canvas.drawCircle(lm.x() * w, lm.y() * h, 6f, landmarkPaint)
        }

        // Track wrist position for rep counting
        val wristLm = landmarks[WRIST_LANDMARK]
        val middleMcp = landmarks[MIDDLE_MCP]
        wristY = wristLm.y()

        // Highlight wrist
        canvas.drawCircle(wristLm.x() * w, wristLm.y() * h, 16f, wristPaint)

        // Use average of wrist and middle MCP for more stable tracking
        val trackY = (wristLm.y() + middleMcp.y()) / 2f

        // Rep counting state machine
        if (trackY > UP_THRESHOLD) {
            // Hand is low in frame = arm extended
            stage = "up"
        } else if (trackY < DOWN_THRESHOLD && stage == "up") {
            // Hand moved high in frame = arm curled, and was previously extended
            stage = "down"
            repCount++
            Log.d(TAG, "Rep counted! Total: $repCount (trackY=$trackY)")
        }

        // Draw HUD
        val hudLeft = 20f
        val hudTop = 20f
        val hudWidth = 500f
        val hudHeight = 200f
        canvas.drawRoundRect(hudLeft, hudTop, hudLeft + hudWidth, hudTop + hudHeight, 24f, 24f, hudBgPaint)

        canvas.drawText("REPS: $repCount", hudLeft + 20, hudTop + 70, hudTextPaint)
        canvas.drawText(
            "Wrist Y: ${(trackY * 100).roundToInt()}%",
            hudLeft + 20, hudTop + 115, hudSmallPaint
        )

        val stageText = (stage ?: "wait").uppercase()
        val stageColor = when (stage) {
            "down" -> Color.parseColor("#ff4400")
            "up" -> Color.parseColor("#00ff88")
            else -> Color.parseColor("#888888")
        }
        stagePaint.color = stageColor
        canvas.drawText(
            "$stageText | curl<${(DOWN_THRESHOLD * 100).roundToInt()}% extend>${(UP_THRESHOLD * 100).roundToInt()}%",
            hudLeft + 20, hudTop + 160, stagePaint
        )

        _overlayData.value = PoseOverlayData(
            bitmap = overlayBitmap,
            repCount = repCount,
            currentAngle = trackY * 180f, // repurpose angle field for Y position
            stage = stage,
            activeArm = "POV"
        )

        return overlayBitmap
    }

    fun reset() {
        repCount = 0
        stage = null
        wristY = 0f
        _overlayData.value = PoseOverlayData()
    }

    fun getRepCount(): Int = repCount

    fun close() {
        handLandmarker?.close()
        handLandmarker = null
        isInitialized = false
    }
}
