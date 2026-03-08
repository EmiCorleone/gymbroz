package com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * POV (first-person) rep counter using MediaPipe Hand Landmarker.
 *
 * Uses multiple signals from hand landmarks:
 * - Wrist Y position (vertical position in frame)
 * - Forearm angle (wrist→palm direction vs horizontal)
 * - Arm segmentation overlay (estimated from hand convex hull + projected arm)
 *
 * Rep detection: hand crosses above curl threshold = 1 rep.
 * Must drop back below before counting again (hysteresis).
 * Auto-calibrates thresholds from first ~1.5s of movement.
 */
class PovRepCounter(context: Context) {
    companion object {
        private const val TAG = "PovRepCounter"
        private const val MODEL_ASSET = "hand_landmarker.task"

        // Default Y thresholds (normalized 0-1, 0=top 1=bottom)
        const val DEFAULT_CURL_THRESH = 0.50f
        // Hysteresis: must drop this much below curl threshold before counting again
        const val HYSTERESIS = 0.05f

        // Landmark indices
        const val WRIST = 0
        const val INDEX_MCP = 5
        const val MIDDLE_MCP = 9
        const val RING_MCP = 13
        const val PINKY_MCP = 17

        // Calibration
        const val CALIBRATION_FRAMES = 45 // ~1.5s at 30fps
    }

    private var handLandmarker: HandLandmarker? = null
    private var isInitialized = false
    private var timestampMs: Long = 0

    // Rep counting state
    private var repCount = 0
    private var stage: String? = null // "curled" or "ready"
    private var trackY = 0f
    private var forearmAngle = 0f

    // Threshold (auto-calibrated or manual)
    var curlThreshold = DEFAULT_CURL_THRESH
        private set

    // Smoothing buffers
    private val ySmooth = ArrayDeque<Float>(4)
    private val angleSmooth = ArrayDeque<Float>(4)

    // Auto-calibration
    private val calibrationY = mutableListOf<Float>()
    private val calibrationAngle = mutableListOf<Float>()
    var isCalibrated = false
        private set

    // Arm segmentation toggle
    var showArmSegmentation = true
    // Debug HUD toggle (off by default — Compose UI shows rep count)
    var showDebugHud = false

    private val _overlayData = MutableStateFlow(PoseOverlayData())
    val overlayData: StateFlow<PoseOverlayData> = _overlayData.asStateFlow()

    // Paints
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

    private val armSegPaint = Paint().apply {
        color = Color.argb(50, 0, 255, 136)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val thresholdPaint = Paint().apply {
        color = Color.parseColor("#ff4400")
        strokeWidth = 2f
        style = Paint.Style.STROKE
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

    private val handConnections = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        0 to 5, 5 to 6, 6 to 7, 7 to 8,
        0 to 9, 9 to 10, 10 to 11, 11 to 12,
        0 to 13, 13 to 14, 14 to 15, 15 to 16,
        0 to 17, 17 to 18, 18 to 19, 19 to 20,
        5 to 9, 9 to 13, 13 to 17
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
                .setMinHandDetectionConfidence(0.4f)
                .setMinTrackingConfidence(0.4f)
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
        timestampMs += 33

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

        // --- Arm segmentation overlay ---
        if (showArmSegmentation) {
            drawArmSegment(canvas, landmarks, w, h)
        }

        // --- Draw hand skeleton ---
        for ((a, b) in handConnections) {
            if (a < landmarks.size && b < landmarks.size) {
                val la = landmarks[a]
                val lb = landmarks[b]
                canvas.drawLine(la.x() * w, la.y() * h, lb.x() * w, lb.y() * h, connectionPaint)
            }
        }
        for (lm in landmarks) {
            canvas.drawCircle(lm.x() * w, lm.y() * h, 6f, landmarkPaint)
        }

        // --- Compute signals ---
        val wristLm = landmarks[WRIST]
        val middleMcp = landmarks[MIDDLE_MCP]

        // Wrist Y (smoothed)
        val rawY = (wristLm.y() + middleMcp.y()) / 2f
        ySmooth.addLast(rawY)
        if (ySmooth.size > 3) ySmooth.removeFirst()
        trackY = ySmooth.average().toFloat()

        // Forearm angle (smoothed)
        val rawAngle = computeForearmAngle(landmarks)
        angleSmooth.addLast(rawAngle)
        if (angleSmooth.size > 3) angleSmooth.removeFirst()
        forearmAngle = angleSmooth.average().toFloat()

        // Highlight wrist
        canvas.drawCircle(wristLm.x() * w, wristLm.y() * h, 16f, wristPaint)

        // --- Auto-calibration ---
        if (!isCalibrated) {
            calibrationY.add(trackY)
            calibrationAngle.add(forearmAngle)
            if (calibrationY.size >= CALIBRATION_FRAMES) {
                autoCalibrate()
            }
        }

        // --- Rep counting (single curl threshold + hysteresis) ---
        if (trackY < curlThreshold && stage != "curled") {
            stage = "curled"
            repCount++
            Log.d(TAG, "REP $repCount! (Y=$trackY, angle=$forearmAngle)")
        } else if (trackY > curlThreshold + HYSTERESIS) {
            stage = "ready"
        }

        // --- Draw curl threshold band (always visible) ---
        val bandHeight = 28f
        val bandY = curlThreshold * h
        // Gradient band: green-tinted zone above the threshold
        canvas.drawRect(0f, bandY - bandHeight, w, bandY, Paint().apply {
            color = Color.argb(80, 0, 255, 100)
            style = Paint.Style.FILL
        })
        // Solid edge lines
        canvas.drawLine(0f, bandY, w, bandY, Paint().apply {
            color = Color.argb(180, 0, 255, 100)
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        })
        canvas.drawLine(0f, bandY - bandHeight, w, bandY - bandHeight, Paint().apply {
            color = Color.argb(100, 0, 255, 100)
            strokeWidth = 2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        })
        // Arrow + label
        val labelPaint = Paint().apply {
            color = Color.argb(220, 255, 255, 255)
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CURL ABOVE HERE", w / 2f, bandY - bandHeight / 2f + 7f, labelPaint)

        if (showDebugHud) {
            // --- Draw forearm direction line ---
            val palmX = (landmarks[INDEX_MCP].x() + landmarks[MIDDLE_MCP].x() +
                    landmarks[RING_MCP].x() + landmarks[PINKY_MCP].x()) / 4f
            val palmY = (landmarks[INDEX_MCP].y() + landmarks[MIDDLE_MCP].y() +
                    landmarks[RING_MCP].y() + landmarks[PINKY_MCP].y()) / 4f
            val dx = wristLm.x() - palmX
            val dy = wristLm.y() - palmY
            val armEndX = wristLm.x() + dx * 3f
            val armEndY = wristLm.y() + dy * 3f
            canvas.drawLine(
                wristLm.x() * w, wristLm.y() * h,
                armEndX * w, armEndY * h,
                Paint().apply {
                    color = Color.parseColor("#ff6600")
                    strokeWidth = 6f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
            )

            // --- HUD ---
            val hudLeft = 20f
            val hudTop = 20f
            val hudWidth = 520f
            val hudHeight = 200f
            canvas.drawRoundRect(hudLeft, hudTop, hudLeft + hudWidth, hudTop + hudHeight, 24f, 24f, hudBgPaint)

            canvas.drawText("REPS: $repCount", hudLeft + 20, hudTop + 70, hudTextPaint)
            canvas.drawText(
                "Y:${(trackY * 100).roundToInt()}% | Angle:${forearmAngle.roundToInt()}°",
                hudLeft + 20, hudTop + 115, hudSmallPaint
            )

            val stageText = (stage ?: "wait").uppercase()
            val stageColor = when (stage) {
                "curled" -> Color.parseColor("#ff4400")
                "ready" -> Color.parseColor("#00ff88")
                else -> Color.parseColor("#888888")
            }
            stagePaint.color = stageColor
            val calibText = if (isCalibrated) "calibrated" else "calibrating..."
            canvas.drawText(
                "$stageText | curl<${(curlThreshold * 100).roundToInt()}% | $calibText",
                hudLeft + 20, hudTop + 160, stagePaint
            )
        }

        _overlayData.value = PoseOverlayData(
            bitmap = overlayBitmap,
            repCount = repCount,
            currentAngle = forearmAngle,
            wristY = trackY,
            stage = stage,
            activeArm = "POV"
        )

        return overlayBitmap
    }

    private fun computeForearmAngle(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        val wrist = landmarks[WRIST]
        val palmX = (landmarks[INDEX_MCP].x() + landmarks[MIDDLE_MCP].x() +
                landmarks[RING_MCP].x() + landmarks[PINKY_MCP].x()) / 4f
        val palmY = (landmarks[INDEX_MCP].y() + landmarks[MIDDLE_MCP].y() +
                landmarks[RING_MCP].y() + landmarks[PINKY_MCP].y()) / 4f
        val dx = palmX - wrist.x()
        val dy = palmY - wrist.y()
        return Math.toDegrees(atan2(abs(dy).toDouble(), abs(dx).toDouble())).toFloat()
    }

    private fun drawArmSegment(
        canvas: Canvas,
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        w: Float, h: Float
    ) {
        val points = landmarks.map { Pair(it.x() * w, it.y() * h) }
        val wrist = points[WRIST]
        val centerX = points.map { it.first }.average().toFloat()
        val centerY = points.map { it.second }.average().toFloat()

        val dx = wrist.first - centerX
        val dy = wrist.second - centerY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

        val armLen = 250f
        val armEndX = wrist.first + dx / dist * armLen
        val armEndY = wrist.second + dy / dist * armLen

        val wristWidth = 50f
        val perpX = -dy / dist * wristWidth
        val perpY = dx / dist * wristWidth

        val path = Path().apply {
            moveTo(wrist.first - perpX, wrist.second - perpY)
            lineTo(wrist.first + perpX, wrist.second + perpY)
            lineTo(armEndX + perpX, armEndY + perpY)
            lineTo(armEndX - perpX, armEndY - perpY)
            close()
        }
        canvas.drawPath(path, armSegPaint)
    }

    private fun autoCalibrate() {
        val yMin = calibrationY.min()
        val yMax = calibrationY.max()
        val yRange = yMax - yMin

        if (yRange > 0.05f) {
            curlThreshold = yMin + yRange * 0.30f
            isCalibrated = true
            Log.d(TAG, "Auto-calibrated: curlThreshold=$curlThreshold (Y range: $yMin-$yMax)")
        } else {
            // Not enough movement yet, keep collecting
            calibrationY.clear()
            calibrationAngle.clear()
        }
    }

    fun recalibrate() {
        calibrationY.clear()
        calibrationAngle.clear()
        isCalibrated = false
        Log.d(TAG, "Recalibrating...")
    }

    fun reset() {
        repCount = 0
        stage = null
        trackY = 0f
        forearmAngle = 0f
        ySmooth.clear()
        angleSmooth.clear()
        calibrationY.clear()
        calibrationAngle.clear()
        isCalibrated = false
        curlThreshold = DEFAULT_CURL_THRESH
        _overlayData.value = PoseOverlayData()
    }

    fun getRepCount(): Int = repCount

    fun close() {
        handLandmarker?.close()
        handLandmarker = null
        isInitialized = false
    }
}
