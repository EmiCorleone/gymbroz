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
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.acos
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PoseOverlayData(
    val bitmap: Bitmap? = null,
    val repCount: Int = 0,
    val currentAngle: Float = 0f,
    val wristY: Float = 0f,
    val stage: String? = null,
    val activeArm: String = ""
)

data class PoseFrameData(
    val frameIndex: Int,
    val timestampMs: Long,
    val landmarks: List<List<Float>>,
    val leftElbowAngle: Float?,
    val rightElbowAngle: Float?,
    val activeArm: String,
    val stage: String?,
    val repCount: Int
)

class PoseDetectionManager(context: Context) {
    companion object {
        private const val TAG = "PoseDetection"
        private const val MODEL_ASSET = "pose_landmarker_full.task"

        // MediaPipe pose landmark connections for skeleton drawing
        val POSE_CONNECTIONS = listOf(
            11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16,
            11 to 23, 12 to 24, 23 to 24, 23 to 25, 24 to 26,
            25 to 27, 26 to 28
        )

        // Bicep curl keypoint sets: [shoulder, elbow, wrist]
        val LEFT_ARM = Triple(11, 13, 15)
        val RIGHT_ARM = Triple(12, 14, 16)

        const val UP_ANGLE = 120f
        const val DOWN_ANGLE = 80f
    }

    private var poseLandmarker: PoseLandmarker? = null
    private var isInitialized = false
    private var timestampMs: Long = 0

    // Rep counting state
    private var repCount = 0
    private var stage: String? = null // "up" or "down"
    private var currentAngle = 0f
    private var activeArm = ""

    private val _overlayData = MutableStateFlow(PoseOverlayData())
    val overlayData: StateFlow<PoseOverlayData> = _overlayData.asStateFlow()

    private var frameIndex = 0
    private val _frameBuffer = mutableListOf<PoseFrameData>()
    val frameBuffer: List<PoseFrameData> get() = _frameBuffer

    fun drainFrameBuffer(): List<PoseFrameData> {
        val frames = _frameBuffer.toList()
        _frameBuffer.clear()
        return frames
    }

    private val skeletonPaint = Paint().apply {
        color = Color.parseColor("#00ff88")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val jointPaint = Paint().apply {
        color = Color.parseColor("#00ff88")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val highlightPaint = Paint().apply {
        color = Color.parseColor("#ff4400")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val highlightLinePaint = Paint().apply {
        color = Color.parseColor("#ff4400")
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#ffff00")
        textSize = 36f
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val hudBgPaint = Paint().apply {
        color = Color.argb(200, 0, 0, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
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

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.VIDEO)
                .setNumPoses(1)
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            isInitialized = true
            Log.d(TAG, "PoseLandmarker initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PoseLandmarker: ${e.message}")
        }
    }

    fun processFrame(bitmap: Bitmap): Bitmap? {
        if (!isInitialized || poseLandmarker == null) return null

        val mpImage = BitmapImageBuilder(bitmap).build()
        timestampMs += 33 // ~30fps

        val result: PoseLandmarkerResult
        try {
            result = poseLandmarker!!.detectForVideo(mpImage, timestampMs)
        } catch (e: Exception) {
            return null
        }

        if (result.landmarks().isEmpty()) return null

        // Draw overlay on a transparent bitmap
        val overlayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(overlayBitmap)
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        val landmarks = result.landmarks()[0]

        // Draw skeleton connections
        for ((a, b) in POSE_CONNECTIONS) {
            if (a < landmarks.size && b < landmarks.size) {
                val la = landmarks[a]
                val lb = landmarks[b]
                canvas.drawLine(la.x() * w, la.y() * h, lb.x() * w, lb.y() * h, skeletonPaint)
            }
        }

        // Draw all landmark dots
        for (lm in landmarks) {
            canvas.drawCircle(lm.x() * w, lm.y() * h, 8f, jointPaint)
        }

        // Process both arms for rep counting
        var bestAngle = -1f
        var bestArmLabel = ""

        for ((armLabel, armKps) in listOf("L" to LEFT_ARM, "R" to RIGHT_ARM)) {
            val (kpA, kpB, kpC) = armKps
            if (kpA >= landmarks.size || kpB >= landmarks.size || kpC >= landmarks.size) continue

            val a = landmarks[kpA]
            val b = landmarks[kpB]
            val c = landmarks[kpC]

            // Highlight arm keypoints
            for (kp in listOf(a, b, c)) {
                canvas.drawCircle(kp.x() * w, kp.y() * h, 16f, highlightPaint)
            }

            // Draw highlighted arm lines
            canvas.drawLine(a.x() * w, a.y() * h, b.x() * w, b.y() * h, highlightLinePaint)
            canvas.drawLine(b.x() * w, b.y() * h, c.x() * w, c.y() * h, highlightLinePaint)

            // Calculate angle
            val angle = calculateAngle(
                a.x(), a.y(), a.z(),
                b.x(), b.y(), b.z(),
                c.x(), c.y(), c.z()
            )

            // Draw angle label at elbow
            canvas.drawText(
                "$armLabel: ${angle.roundToInt()}°",
                b.x() * w + 20,
                b.y() * h - 15,
                textPaint
            )

            if (bestAngle < 0 || angle < bestAngle) {
                bestAngle = angle
                bestArmLabel = armLabel
            }
        }

        // Update rep counting state
        if (bestAngle >= 0) {
            currentAngle = bestAngle
            activeArm = bestArmLabel

            if (bestAngle < DOWN_ANGLE) {
                stage = "down"
            } else if (bestAngle > UP_ANGLE && stage == "down") {
                stage = "up"
                repCount++
            }
        }

        // Capture frame data for RL export (every 3rd frame = ~10fps)
        if (frameIndex % 3 == 0) {
            val landmarkData = landmarks.map { lm ->
                listOf(lm.x(), lm.y(), lm.z(), if (lm.visibility().isPresent) lm.visibility().get() else 0f)
            }
            val leftAngle = run {
                val (a, b, c) = LEFT_ARM
                if (a < landmarks.size && b < landmarks.size && c < landmarks.size) {
                    calculateAngle(
                        landmarks[a].x(), landmarks[a].y(), landmarks[a].z(),
                        landmarks[b].x(), landmarks[b].y(), landmarks[b].z(),
                        landmarks[c].x(), landmarks[c].y(), landmarks[c].z()
                    )
                } else null
            }
            val rightAngle = run {
                val (a, b, c) = RIGHT_ARM
                if (a < landmarks.size && b < landmarks.size && c < landmarks.size) {
                    calculateAngle(
                        landmarks[a].x(), landmarks[a].y(), landmarks[a].z(),
                        landmarks[b].x(), landmarks[b].y(), landmarks[b].z(),
                        landmarks[c].x(), landmarks[c].y(), landmarks[c].z()
                    )
                } else null
            }
            _frameBuffer.add(PoseFrameData(
                frameIndex = frameIndex,
                timestampMs = timestampMs,
                landmarks = landmarkData,
                leftElbowAngle = leftAngle,
                rightElbowAngle = rightAngle,
                activeArm = activeArm,
                stage = stage,
                repCount = repCount
            ))
        }
        frameIndex++

        // Draw HUD
        val hudLeft = 20f
        val hudTop = 20f
        val hudWidth = 500f
        val hudHeight = 200f
        canvas.drawRoundRect(hudLeft, hudTop, hudLeft + hudWidth, hudTop + hudHeight, 24f, 24f, hudBgPaint)

        canvas.drawText("REPS: $repCount", hudLeft + 20, hudTop + 70, hudTextPaint)
        canvas.drawText(
            "Angle: ${currentAngle.roundToInt()}° ($activeArm)",
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
            "$stageText | curl<${DOWN_ANGLE.roundToInt()}° extend>${UP_ANGLE.roundToInt()}°",
            hudLeft + 20, hudTop + 160, stagePaint
        )

        _overlayData.value = PoseOverlayData(
            bitmap = overlayBitmap,
            repCount = repCount,
            currentAngle = currentAngle,
            stage = stage,
            activeArm = activeArm
        )

        return overlayBitmap
    }

    fun reset() {
        repCount = 0
        stage = null
        currentAngle = 0f
        activeArm = ""
        frameIndex = 0
        _frameBuffer.clear()
        _overlayData.value = PoseOverlayData()
    }

    fun getRepCount(): Int = repCount

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
        isInitialized = false
    }

    private fun calculateAngle(
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float
    ): Float {
        val baX = ax - bx; val baY = ay - by; val baZ = az - bz
        val bcX = cx - bx; val bcY = cy - by; val bcZ = cz - bz

        val dot = baX * bcX + baY * bcY + baZ * bcZ
        val magBA = sqrt((baX * baX + baY * baY + baZ * baZ).toDouble())
        val magBC = sqrt((bcX * bcX + bcY * bcY + bcZ * bcZ).toDouble())

        if (magBA == 0.0 || magBC == 0.0) return 0f

        val cosAngle = (dot / (magBA * magBC)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosAngle)).toFloat()
    }
}
