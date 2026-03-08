import UIKit
import CoreGraphics

enum RepStage {
    case up
    case down
}

struct PoseOverlayData {
    var overlayImage: UIImage?
    var repCount: Int
    var leftAngle: Double
    var rightAngle: Double
    var leftStage: RepStage
    var rightStage: RepStage
}

@MainActor
final class PoseDetectionManager: ObservableObject {
    @Published var overlayData = PoseOverlayData(
        overlayImage: nil, repCount: 0,
        leftAngle: 0, rightAngle: 0,
        leftStage: .down, rightStage: .down
    )

    private var isActive = false
    private var repCount = 0
    private var leftStage: RepStage = .down
    private var rightStage: RepStage = .down

    // MediaPipe PoseLandmarker instance would go here
    // private var poseLandmarker: PoseLandmarker?

    func start(exercise: String) {
        repCount = 0
        leftStage = .down
        rightStage = .down
        isActive = true
        // Initialize MediaPipe PoseLandmarker
        // let options = PoseLandmarkerOptions()
        // options.baseOptions.modelAssetPath = Bundle.main.path(forResource: "pose_landmarker_full", ofType: "task")
        // options.runningMode = .image
        // poseLandmarker = try? PoseLandmarker(options: options)
    }

    func stop() -> Int {
        isActive = false
        let finalCount = repCount
        return finalCount
    }

    func getRepCount() -> Int {
        return repCount
    }

    func processFrame(_ image: UIImage) {
        guard isActive else { return }

        // In a real implementation, this would:
        // 1. Convert UIImage to MPImage
        // 2. Run poseLandmarker.detect(image:)
        // 3. Extract landmarks for shoulders (11,12), elbows (13,14), wrists (15,16)
        // 4. Calculate angles and count reps

        // Placeholder: The actual MediaPipe processing would look like:
        // guard let mpImage = try? MPImage(uiImage: image) else { return }
        // let result = try? poseLandmarker?.detect(image: mpImage)
        // guard let landmarks = result?.landmarks.first else { return }
        // processLandmarks(landmarks, imageSize: image.size)
    }

    func processLandmarks(_ landmarks: [(x: Float, y: Float, z: Float)], imageSize: CGSize) {
        guard landmarks.count >= 17 else { return }

        // Left arm: shoulder(11), elbow(13), wrist(15)
        let leftAngle = calculateAngle(
            a: landmarks[11], b: landmarks[13], c: landmarks[15]
        )
        // Right arm: shoulder(12), elbow(14), wrist(16)
        let rightAngle = calculateAngle(
            a: landmarks[12], b: landmarks[14], c: landmarks[16]
        )

        // Rep counting state machine
        if leftAngle < 80 { leftStage = .down }
        if leftAngle > 120 && leftStage == .down {
            leftStage = .up
            repCount += 1
        }

        if rightAngle < 80 { rightStage = .down }
        if rightAngle > 120 && rightStage == .down {
            rightStage = .up
            // Only count from one arm to avoid double-counting
        }

        // Draw overlay
        let overlay = drawOverlay(landmarks: landmarks, imageSize: imageSize)

        overlayData = PoseOverlayData(
            overlayImage: overlay,
            repCount: repCount,
            leftAngle: leftAngle,
            rightAngle: rightAngle,
            leftStage: leftStage,
            rightStage: rightStage
        )
    }

    private func calculateAngle(
        a: (x: Float, y: Float, z: Float),
        b: (x: Float, y: Float, z: Float),
        c: (x: Float, y: Float, z: Float)
    ) -> Double {
        let radians = atan2(Double(c.y - b.y), Double(c.x - b.x)) -
                      atan2(Double(a.y - b.y), Double(a.x - b.x))
        var angle = abs(radians * 180.0 / .pi)
        if angle > 180 { angle = 360 - angle }
        return angle
    }

    private func drawOverlay(landmarks: [(x: Float, y: Float, z: Float)], imageSize: CGSize) -> UIImage? {
        let renderer = UIGraphicsImageRenderer(size: imageSize)
        return renderer.image { context in
            let ctx = context.cgContext

            // Draw skeleton connections
            let connections: [(Int, Int)] = [
                (11, 13), (13, 15), (12, 14), (14, 16),  // Arms
                (11, 12), (11, 23), (12, 24), (23, 24),  // Torso
                (23, 25), (25, 27), (24, 26), (26, 28)   // Legs
            ]

            ctx.setStrokeColor(UIColor.green.cgColor)
            ctx.setLineWidth(3)

            for (start, end) in connections {
                guard start < landmarks.count, end < landmarks.count else { continue }
                let p1 = CGPoint(x: CGFloat(landmarks[start].x) * imageSize.width,
                                 y: CGFloat(landmarks[start].y) * imageSize.height)
                let p2 = CGPoint(x: CGFloat(landmarks[end].x) * imageSize.width,
                                 y: CGFloat(landmarks[end].y) * imageSize.height)
                ctx.move(to: p1)
                ctx.addLine(to: p2)
            }
            ctx.strokePath()

            // Draw joints
            ctx.setFillColor(UIColor.yellow.cgColor)
            for landmark in landmarks {
                let point = CGPoint(x: CGFloat(landmark.x) * imageSize.width,
                                    y: CGFloat(landmark.y) * imageSize.height)
                ctx.fillEllipse(in: CGRect(x: point.x - 4, y: point.y - 4, width: 8, height: 8))
            }
        }
    }
}
