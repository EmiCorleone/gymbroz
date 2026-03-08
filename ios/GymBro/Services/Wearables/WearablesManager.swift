import Foundation
import UIKit
import MWDATCore
import MWDATCamera

@MainActor
final class WearablesManager: ObservableObject {
    enum StreamingStatus {
        case stopped, waiting, streaming
    }

    @Published var currentVideoFrame: UIImage?
    @Published var streamingStatus: StreamingStatus = .stopped
    @Published var hasActiveDevice = false
    @Published var capturedPhoto: UIImage?
    @Published var showPhotoPreview = false
    @Published var errorMessage = ""
    @Published var showError = false

    var onVideoFrame: ((UIImage) -> Void)?

    private var streamSession: StreamSession
    private var stateListenerToken: AnyListenerToken?
    private var videoFrameListenerToken: AnyListenerToken?
    private var errorListenerToken: AnyListenerToken?
    private var photoDataListenerToken: AnyListenerToken?
    private let wearables: WearablesInterface
    private let deviceSelector: AutoDeviceSelector
    private var deviceMonitorTask: Task<Void, Never>?

    init(wearables: WearablesInterface) {
        self.wearables = wearables
        self.deviceSelector = AutoDeviceSelector(wearables: wearables)
        let config = StreamSessionConfig(
            videoCodec: VideoCodec.raw,
            resolution: StreamingResolution.low,
            frameRate: 24
        )
        streamSession = StreamSession(streamSessionConfig: config, deviceSelector: deviceSelector)

        deviceMonitorTask = Task { @MainActor in
            for await device in deviceSelector.activeDeviceStream() {
                self.hasActiveDevice = device != nil
            }
        }

        stateListenerToken = streamSession.statePublisher.listen { [weak self] state in
            Task { @MainActor [weak self] in
                self?.updateStatus(from: state)
            }
        }

        videoFrameListenerToken = streamSession.videoFramePublisher.listen { [weak self] videoFrame in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let image = videoFrame.makeUIImage() {
                    self.currentVideoFrame = image
                    self.onVideoFrame?(image)
                }
            }
        }

        errorListenerToken = streamSession.errorPublisher.listen { [weak self] error in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.errorMessage = self.formatError(error)
                self.showError = true
            }
        }

        photoDataListenerToken = streamSession.photoDataPublisher.listen { [weak self] photoData in
            Task { @MainActor [weak self] in
                guard let self else { return }
                if let image = UIImage(data: photoData.data) {
                    self.capturedPhoto = image
                    self.showPhotoPreview = true
                }
            }
        }
    }

    func startStreaming() async {
        do {
            let status = try await wearables.checkPermissionStatus(.camera)
            if status != .granted {
                let newStatus = try await wearables.requestPermission(.camera)
                guard newStatus == .granted else {
                    errorMessage = "Camera permission denied"
                    showError = true
                    return
                }
            }
            await streamSession.start()
        } catch {
            errorMessage = "Permission error: \(error.localizedDescription)"
            showError = true
        }
    }

    func stopStreaming() async {
        await streamSession.stop()
    }

    func capturePhoto() {
        streamSession.capturePhoto(format: .jpeg)
    }

    func dismissPhotoPreview() {
        showPhotoPreview = false
        capturedPhoto = nil
    }

    func dismissError() {
        showError = false
        errorMessage = ""
    }

    private func updateStatus(from state: StreamSessionState) {
        switch state {
        case .stopped:
            currentVideoFrame = nil
            streamingStatus = .stopped
        case .waitingForDevice, .starting, .stopping, .paused:
            streamingStatus = .waiting
        case .streaming:
            streamingStatus = .streaming
        }
    }

    private func formatError(_ error: StreamSessionError) -> String {
        switch error {
        case .internalError: return "Internal error. Please try again."
        case .deviceNotFound: return "Device not found. Check connection."
        case .deviceNotConnected: return "Device not connected."
        case .timeout: return "Operation timed out."
        case .videoStreamingError: return "Video streaming failed."
        case .audioStreamingError: return "Audio streaming failed."
        case .permissionDenied: return "Camera permission denied."
        case .hingesClosed: return "Glasses hinges are closed."
        @unknown default: return "Unknown streaming error."
        }
    }
}
