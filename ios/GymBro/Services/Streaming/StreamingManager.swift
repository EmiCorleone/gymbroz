import AVFoundation
import UIKit

final class StreamingManager {
    private var assetWriter: AVAssetWriter?
    private var videoInput: AVAssetWriterInput?
    private var pixelBufferAdaptor: AVAssetWriterInputPixelBufferAdaptor?
    private var isRecording = false
    private var frameCount: Int64 = 0
    private var outputURL: URL?
    private let fps: Int32 = 24

    var recordingURL: URL? { outputURL }

    func startRecording() {
        let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let fileName = "gymbro_\(Int(Date().timeIntervalSince1970)).mp4"
        outputURL = documentsPath.appendingPathComponent(fileName)

        guard let url = outputURL else { return }

        do {
            assetWriter = try AVAssetWriter(outputURL: url, fileType: .mp4)

            let settings: [String: Any] = [
                AVVideoCodecKey: AVVideoCodecType.h264,
                AVVideoWidthKey: 432,
                AVVideoHeightKey: 768
            ]
            videoInput = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
            videoInput?.expectsMediaDataInRealTime = true

            let attrs: [String: Any] = [
                kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
                kCVPixelBufferWidthKey as String: 432,
                kCVPixelBufferHeightKey as String: 768
            ]
            pixelBufferAdaptor = AVAssetWriterInputPixelBufferAdaptor(
                assetWriterInput: videoInput!,
                sourcePixelBufferAttributes: attrs
            )

            if assetWriter!.canAdd(videoInput!) {
                assetWriter!.add(videoInput!)
            }

            assetWriter!.startWriting()
            assetWriter!.startSession(atSourceTime: .zero)
            isRecording = true
            frameCount = 0
        } catch {
            print("[StreamingManager] Failed to start recording: \(error)")
        }
    }

    func appendFrame(_ image: UIImage) {
        guard isRecording,
              let input = videoInput, input.isReadyForMoreMediaData,
              let adaptor = pixelBufferAdaptor,
              let cgImage = image.cgImage else { return }

        let time = CMTime(value: frameCount, timescale: fps)
        frameCount += 1

        guard let pool = adaptor.pixelBufferPool else { return }
        var pixelBuffer: CVPixelBuffer?
        CVPixelBufferPoolCreatePixelBuffer(nil, pool, &pixelBuffer)
        guard let buffer = pixelBuffer else { return }

        CVPixelBufferLockBaseAddress(buffer, [])
        let context = CGContext(
            data: CVPixelBufferGetBaseAddress(buffer),
            width: cgImage.width,
            height: cgImage.height,
            bitsPerComponent: 8,
            bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue
        )
        context?.draw(cgImage, in: CGRect(x: 0, y: 0, width: cgImage.width, height: cgImage.height))
        CVPixelBufferUnlockBaseAddress(buffer, [])

        adaptor.append(buffer, withPresentationTime: time)
    }

    func stopRecording() async -> URL? {
        guard isRecording else { return nil }
        isRecording = false
        videoInput?.markAsFinished()

        return await withCheckedContinuation { continuation in
            assetWriter?.finishWriting { [weak self] in
                continuation.resume(returning: self?.outputURL)
            }
        }
    }
}
