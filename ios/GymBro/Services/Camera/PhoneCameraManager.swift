import AVFoundation
import UIKit

final class PhoneCameraManager: NSObject, ObservableObject {
    @Published var currentFrame: UIImage?

    var onFrameCaptured: ((UIImage) -> Void)?

    private var captureSession: AVCaptureSession?
    private let videoOutput = AVCaptureVideoDataOutput()
    private let sessionQueue = DispatchQueue(label: "com.gymbro.camera")
    private var isUsingFrontCamera = true

    func startCamera() {
        sessionQueue.async { [weak self] in
            self?.setupSession()
        }
    }

    func stopCamera() {
        sessionQueue.async { [weak self] in
            self?.captureSession?.stopRunning()
        }
    }

    func switchCamera() {
        isUsingFrontCamera.toggle()
        sessionQueue.async { [weak self] in
            guard let self, let session = self.captureSession else { return }
            session.beginConfiguration()
            if let currentInput = session.inputs.first as? AVCaptureDeviceInput {
                session.removeInput(currentInput)
            }
            let position: AVCaptureDevice.Position = self.isUsingFrontCamera ? .front : .back
            guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: position),
                  let input = try? AVCaptureDeviceInput(device: device) else {
                session.commitConfiguration()
                return
            }
            if session.canAddInput(input) {
                session.addInput(input)
            }
            session.commitConfiguration()
        }
    }

    private func setupSession() {
        let session = AVCaptureSession()
        session.sessionPreset = .medium

        let position: AVCaptureDevice.Position = isUsingFrontCamera ? .front : .back
        guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: position),
              let input = try? AVCaptureDeviceInput(device: device) else { return }

        if session.canAddInput(input) {
            session.addInput(input)
        }

        videoOutput.setSampleBufferDelegate(self, queue: sessionQueue)
        videoOutput.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]

        if session.canAddOutput(videoOutput) {
            session.addOutput(videoOutput)
        }

        session.startRunning()
        captureSession = session
    }
}

extension PhoneCameraManager: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
        let context = CIContext()
        guard let cgImage = context.createCGImage(ciImage, from: ciImage.extent) else { return }
        let image = UIImage(cgImage: cgImage)

        DispatchQueue.main.async { [weak self] in
            self?.currentFrame = image
            self?.onFrameCaptured?(image)
        }
    }
}
