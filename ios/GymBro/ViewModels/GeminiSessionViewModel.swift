import SwiftUI
import Combine

@MainActor
final class GeminiSessionViewModel: ObservableObject {
    @Published var isActive = false
    @Published var connectionState: GeminiConnectionState = .disconnected
    @Published var isModelSpeaking = false
    @Published var modelTranscript = ""
    @Published var isRepCountingActive = false
    @Published var currentRepCount = 0
    @Published var currentExercise = ""
    @Published var exerciseGuideImage: UIImage?
    @Published var exerciseGuideText = ""
    @Published var toolStatuses: [ToolCallStatus] = []

    private let geminiService = GeminiLiveService()
    private let audioManager = AudioManager()
    let poseDetectionManager = PoseDetectionManager()
    let povRepCounter = PovRepCounter()
    private lazy var toolHandler = GymToolHandler(poseDetectionManager: poseDetectionManager)
    private var videoFrameTimer: Timer?
    private var pendingFrame: UIImage?

    func startSession() {
        isActive = true
        geminiService.delegate = self
        geminiService.connect()
        audioManager.onAudioCaptured = { [weak self] data in
            self?.geminiService.sendAudio(data)
        }
        audioManager.startCapture()

        toolHandler.onExerciseGuideGenerated = { [weak self] image, text in
            Task { @MainActor in
                self?.exerciseGuideImage = image
                self?.exerciseGuideText = text
            }
        }

        // Start video frame timer (1 frame per second)
        videoFrameTimer = Timer.scheduledTimer(withTimeInterval: GeminiConfig.frameIntervalSeconds, repeats: true) { [weak self] _ in
            Task { @MainActor in
                if let frame = self?.pendingFrame {
                    self?.geminiService.sendVideoFrame(frame)
                }
            }
        }
    }

    func stopSession() {
        isActive = false
        videoFrameTimer?.invalidate()
        videoFrameTimer = nil
        audioManager.stopCapture()
        geminiService.disconnect()
        connectionState = .disconnected
    }

    func processVideoFrame(_ image: UIImage) {
        pendingFrame = image

        if isRepCountingActive {
            poseDetectionManager.processFrame(image)
            currentRepCount = poseDetectionManager.getRepCount()
        }
    }
}

extension GeminiSessionViewModel: GeminiLiveServiceDelegate {
    nonisolated func geminiDidReceiveAudio(_ audioData: Data) {
        Task { @MainActor in
            audioManager.playAudio(audioData)
        }
    }

    nonisolated func geminiDidReceiveToolCall(_ toolCall: GeminiToolCall) {
        Task { @MainActor in
            for call in toolCall.functionCalls {
                let status = ToolCallStatus(id: call.id, name: call.name, status: "executing")
                toolStatuses.append(status)

                let result = await toolHandler.handleToolCall(call)
                geminiService.sendToolResponse(functionCallId: call.id, result: result)

                // Update rep counting state
                if call.name == "start_rep_counting" {
                    isRepCountingActive = true
                    currentExercise = call.args["exercise"] ?? "bicep_curl"
                    currentRepCount = 0
                } else if call.name == "stop_rep_counting" {
                    isRepCountingActive = false
                } else if call.name == "get_rep_count" {
                    currentRepCount = poseDetectionManager.getRepCount()
                }

                if let idx = toolStatuses.firstIndex(where: { $0.id == call.id }) {
                    toolStatuses[idx].status = "completed"
                }
            }
        }
    }

    nonisolated func geminiDidReceiveToolCallCancellation(_ cancellation: GeminiToolCallCancellation) {
        Task { @MainActor in
            toolStatuses.removeAll { cancellation.ids.contains($0.id) }
        }
    }

    nonisolated func geminiDidChangeConnectionState(_ state: GeminiConnectionState) {
        Task { @MainActor in
            connectionState = state
        }
    }

    nonisolated func geminiDidReceiveTranscript(_ text: String, isFinal: Bool) {
        Task { @MainActor in
            if isFinal {
                modelTranscript = text
            } else {
                modelTranscript += text
            }
        }
    }

    nonisolated func geminiModelSpeakingChanged(_ isSpeaking: Bool) {
        Task { @MainActor in
            isModelSpeaking = isSpeaking
        }
    }
}
