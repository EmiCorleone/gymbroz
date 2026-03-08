import UIKit

@MainActor
final class PovRepCounter: ObservableObject {
    struct OverlayData {
        var overlayImage: UIImage?
        var repCount: Int
        var wristYPercent: Double
        var isCalibrated: Bool
    }

    @Published var overlayData = OverlayData(overlayImage: nil, repCount: 0, wristYPercent: 0, isCalibrated: false)

    private var isActive = false
    private var repCount = 0
    private var calibrationFrames: [Double] = []
    private var curlThreshold: Double = 0.5
    private var isAboveThreshold = false
    private let hysteresisBuffer = 0.05
    private let calibrationFrameCount = 45

    func start() {
        repCount = 0
        calibrationFrames = []
        isAboveThreshold = false
        isActive = true
    }

    func stop() -> Int {
        isActive = false
        return repCount
    }

    func getRepCount() -> Int {
        return repCount
    }

    func processHandLandmarks(_ landmarks: [(x: Float, y: Float, z: Float)], imageSize: CGSize) {
        guard isActive, landmarks.count >= 21 else { return }

        // Wrist is landmark 0
        let wristY = Double(landmarks[0].y)

        // Calibration phase
        if calibrationFrames.count < calibrationFrameCount {
            calibrationFrames.append(wristY)
            if calibrationFrames.count == calibrationFrameCount {
                let minY = calibrationFrames.min() ?? 0
                let maxY = calibrationFrames.max() ?? 1
                curlThreshold = (minY + maxY) / 2
            }
            overlayData = OverlayData(
                overlayImage: drawOverlay(landmarks: landmarks, imageSize: imageSize),
                repCount: repCount,
                wristYPercent: wristY * 100,
                isCalibrated: false
            )
            return
        }

        // Rep counting with hysteresis
        if wristY < curlThreshold - hysteresisBuffer && !isAboveThreshold {
            isAboveThreshold = true
        } else if wristY > curlThreshold + hysteresisBuffer && isAboveThreshold {
            isAboveThreshold = false
            repCount += 1
        }

        overlayData = OverlayData(
            overlayImage: drawOverlay(landmarks: landmarks, imageSize: imageSize),
            repCount: repCount,
            wristYPercent: wristY * 100,
            isCalibrated: true
        )
    }

    private func drawOverlay(landmarks: [(x: Float, y: Float, z: Float)], imageSize: CGSize) -> UIImage? {
        let renderer = UIGraphicsImageRenderer(size: imageSize)
        return renderer.image { context in
            let ctx = context.cgContext

            // Draw hand connections
            let connections: [(Int, Int)] = [
                (0, 1), (1, 2), (2, 3), (3, 4),       // Thumb
                (0, 5), (5, 6), (6, 7), (7, 8),       // Index
                (0, 9), (9, 10), (10, 11), (11, 12),   // Middle
                (0, 13), (13, 14), (14, 15), (15, 16), // Ring
                (0, 17), (17, 18), (18, 19), (19, 20), // Pinky
                (5, 9), (9, 13), (13, 17)              // Palm
            ]

            ctx.setStrokeColor(UIColor.cyan.cgColor)
            ctx.setLineWidth(2)

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

            ctx.setFillColor(UIColor.white.cgColor)
            for landmark in landmarks {
                let point = CGPoint(x: CGFloat(landmark.x) * imageSize.width,
                                    y: CGFloat(landmark.y) * imageSize.height)
                ctx.fillEllipse(in: CGRect(x: point.x - 3, y: point.y - 3, width: 6, height: 6))
            }
        }
    }
}
