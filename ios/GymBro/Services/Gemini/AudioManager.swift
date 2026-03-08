import AVFoundation
import Foundation

final class AudioManager {
    var onAudioCaptured: ((Data) -> Void)?

    private let audioEngine = AVAudioEngine()
    private let playerNode = AVAudioPlayerNode()
    private var isCapturing = false
    private var isPlaying = false

    private let outputFormat: AVAudioFormat

    init() {
        // Output format for Gemini audio: 24kHz mono 16-bit PCM
        outputFormat = AVAudioFormat(
            commonFormat: .pcmFormatInt16,
            sampleRate: Double(GeminiConfig.outputSampleRate),
            channels: 1,
            interleaved: true
        )!
    }

    func setupAudioSession() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playAndRecord, mode: .voiceChat, options: [.defaultToSpeaker, .allowBluetooth, .duckOthers])
            try session.setPreferredSampleRate(Double(GeminiConfig.inputSampleRate))
            try session.setActive(true)
        } catch {
            print("[AudioManager] Audio session setup failed: \(error)")
        }
    }

    func startCapture() {
        guard !isCapturing else { return }

        setupAudioSession()

        let inputNode = audioEngine.inputNode
        let inputFormat = inputNode.outputFormat(forBus: 0)

        // Target format: 16kHz mono 16-bit PCM
        guard let targetFormat = AVAudioFormat(
            commonFormat: .pcmFormatInt16,
            sampleRate: Double(GeminiConfig.inputSampleRate),
            channels: 1,
            interleaved: true
        ) else { return }

        // Install converter if sample rates differ
        guard let converter = AVAudioConverter(from: inputFormat, to: targetFormat) else {
            print("[AudioManager] Cannot create audio converter")
            return
        }

        inputNode.installTap(onBus: 0, bufferSize: 1600, format: inputFormat) { [weak self] buffer, _ in
            guard let self else { return }

            let frameCount = AVAudioFrameCount(
                Double(buffer.frameLength) * Double(GeminiConfig.inputSampleRate) / inputFormat.sampleRate
            )
            guard let convertedBuffer = AVAudioPCMBuffer(pcmFormat: targetFormat, frameCapacity: frameCount) else { return }

            var error: NSError?
            let status = converter.convert(to: convertedBuffer, error: &error) { _, outStatus in
                outStatus.pointee = .haveData
                return buffer
            }

            if status == .haveData, let channelData = convertedBuffer.int16ChannelData {
                let data = Data(bytes: channelData[0], count: Int(convertedBuffer.frameLength) * 2)
                if data.count >= 3200 {
                    self.onAudioCaptured?(data)
                }
            }
        }

        audioEngine.attach(playerNode)
        audioEngine.connect(playerNode, to: audioEngine.mainMixerNode, format: outputFormat)

        do {
            try audioEngine.start()
            isCapturing = true
        } catch {
            print("[AudioManager] Engine start failed: \(error)")
        }
    }

    func stopCapture() {
        guard isCapturing else { return }
        audioEngine.inputNode.removeTap(onBus: 0)
        playerNode.stop()
        audioEngine.stop()
        isCapturing = false
    }

    func playAudio(_ data: Data) {
        guard isCapturing else { return }

        let frameCount = UInt32(data.count / 2)  // 16-bit = 2 bytes per sample
        guard let buffer = AVAudioPCMBuffer(pcmFormat: outputFormat, frameCapacity: frameCount) else { return }
        buffer.frameLength = frameCount

        data.withUnsafeBytes { rawBuffer in
            if let src = rawBuffer.baseAddress {
                memcpy(buffer.int16ChannelData![0], src, data.count)
            }
        }

        if !playerNode.isPlaying {
            playerNode.play()
        }
        playerNode.scheduleBuffer(buffer)
    }
}
