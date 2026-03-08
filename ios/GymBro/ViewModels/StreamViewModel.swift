import SwiftUI
import MWDATCore

enum StreamingMode {
    case glasses
    case phone
}

@MainActor
final class StreamViewModel: ObservableObject {
    @Published var currentFrame: UIImage?
    @Published var capturedPhoto: UIImage?
    @Published var showShareDialog = false
    @Published var streamingMode: StreamingMode = .glasses
    @Published var isStreaming = false
    @Published var isRecording = false
    @Published var currentSessionId: String?

    let wearablesManager: WearablesManager
    let phoneCameraManager = PhoneCameraManager()
    let geminiViewModel: GeminiSessionViewModel
    private let streamingManager = StreamingManager()

    init(wearables: WearablesInterface, geminiViewModel: GeminiSessionViewModel) {
        self.wearablesManager = WearablesManager(wearables: wearables)
        self.geminiViewModel = geminiViewModel

        // Route wearables frames
        wearablesManager.onVideoFrame = { [weak self] image in
            Task { @MainActor in
                self?.currentFrame = image
                self?.geminiViewModel.processVideoFrame(image)
                if self?.isRecording == true {
                    self?.streamingManager.appendFrame(image)
                }
            }
        }

        // Route phone camera frames
        phoneCameraManager.onFrameCaptured = { [weak self] image in
            Task { @MainActor in
                self?.currentFrame = image
                self?.geminiViewModel.processVideoFrame(image)
                if self?.isRecording == true {
                    self?.streamingManager.appendFrame(image)
                }
            }
        }
    }

    func startStreaming() async {
        isStreaming = true
        switch streamingMode {
        case .glasses:
            await wearablesManager.startStreaming()
        case .phone:
            phoneCameraManager.startCamera()
        }
    }

    func stopStreaming() async {
        isStreaming = false
        switch streamingMode {
        case .glasses:
            await wearablesManager.stopStreaming()
        case .phone:
            phoneCameraManager.stopCamera()
        }
    }

    func toggleRecording() {
        if isRecording {
            isRecording = false
            Task {
                _ = await streamingManager.stopRecording()
            }
        } else {
            isRecording = true
            streamingManager.startRecording()
        }
    }

    func switchMode() {
        Task {
            await stopStreaming()
            streamingMode = streamingMode == .glasses ? .phone : .glasses
            await startStreaming()
        }
    }

    func capturePhoto() {
        if streamingMode == .glasses {
            wearablesManager.capturePhoto()
        } else if let frame = currentFrame {
            capturedPhoto = frame
            showShareDialog = true
        }
    }
}
