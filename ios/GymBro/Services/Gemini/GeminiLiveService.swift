import Foundation
import UIKit

enum GeminiConnectionState {
    case disconnected
    case connecting
    case connected
}

protocol GeminiLiveServiceDelegate: AnyObject {
    func geminiDidReceiveAudio(_ audioData: Data)
    func geminiDidReceiveToolCall(_ toolCall: GeminiToolCall)
    func geminiDidReceiveToolCallCancellation(_ cancellation: GeminiToolCallCancellation)
    func geminiDidChangeConnectionState(_ state: GeminiConnectionState)
    func geminiDidReceiveTranscript(_ text: String, isFinal: Bool)
    func geminiModelSpeakingChanged(_ isSpeaking: Bool)
}

final class GeminiLiveService {
    weak var delegate: GeminiLiveServiceDelegate?

    private var webSocketTask: URLSessionWebSocketTask?
    private let session = URLSession(configuration: .default)
    private var isSetupComplete = false
    private var connectionState: GeminiConnectionState = .disconnected {
        didSet { delegate?.geminiDidChangeConnectionState(connectionState) }
    }

    func connect() {
        guard connectionState == .disconnected else { return }
        connectionState = .connecting

        let request = URLRequest(url: GeminiConfig.wsURL)
        webSocketTask = session.webSocketTask(with: request)
        webSocketTask?.resume()

        sendSetupMessage()
        receiveMessages()
    }

    func disconnect() {
        webSocketTask?.cancel(with: .goingAway, reason: nil)
        webSocketTask = nil
        isSetupComplete = false
        connectionState = .disconnected
    }

    func sendAudio(_ audioData: Data) {
        guard isSetupComplete else { return }
        let base64Audio = audioData.base64EncodedString()
        let message: [String: Any] = [
            "realtimeInput": [
                "mediaChunks": [
                    [
                        "mimeType": "audio/pcm;rate=\(GeminiConfig.inputSampleRate)",
                        "data": base64Audio
                    ]
                ]
            ]
        ]
        sendJSON(message)
    }

    func sendVideoFrame(_ image: UIImage) {
        guard isSetupComplete else { return }
        guard let jpegData = image.jpegData(compressionQuality: GeminiConfig.jpegQuality) else { return }
        let base64 = jpegData.base64EncodedString()
        let message: [String: Any] = [
            "realtimeInput": [
                "mediaChunks": [
                    [
                        "mimeType": "image/jpeg",
                        "data": base64
                    ]
                ]
            ]
        ]
        sendJSON(message)
    }

    func sendToolResponse(functionCallId: String, result: ToolResult) {
        let message: [String: Any] = [
            "toolResponse": [
                "functionResponses": [
                    [
                        "id": functionCallId,
                        "name": "",
                        "response": result.jsonValue
                    ]
                ]
            ]
        ]
        sendJSON(message)
    }

    // MARK: - Private

    private func sendSetupMessage() {
        let setup: [String: Any] = [
            "setup": [
                "model": "models/\(GeminiConfig.modelName)",
                "generationConfig": [
                    "responseModalities": ["AUDIO"],
                    "speechConfig": [
                        "voiceConfig": [
                            "prebuiltVoiceConfig": [
                                "voiceName": "Kore"
                            ]
                        ]
                    ]
                ],
                "systemInstruction": [
                    "parts": [
                        ["text": GeminiConfig.systemPrompt]
                    ]
                ],
                "tools": GymToolDeclarations.toJSON()
            ]
        ]
        sendJSON(setup)
    }

    private func receiveMessages() {
        webSocketTask?.receive { [weak self] result in
            guard let self else { return }
            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    self.handleMessage(text)
                case .data(let data):
                    if let text = String(data: data, encoding: .utf8) {
                        self.handleMessage(text)
                    }
                @unknown default:
                    break
                }
                self.receiveMessages()

            case .failure(let error):
                print("[GeminiLive] WebSocket error: \(error)")
                self.connectionState = .disconnected
            }
        }
    }

    private func handleMessage(_ text: String) {
        guard let data = text.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return }

        // Setup complete
        if json["setupComplete"] != nil {
            isSetupComplete = true
            connectionState = .connected
            return
        }

        // Tool calls
        if let toolCall = GeminiToolCall.parse(from: json) {
            delegate?.geminiDidReceiveToolCall(toolCall)
            return
        }

        // Tool call cancellation
        if let cancellation = GeminiToolCallCancellation.parse(from: json) {
            delegate?.geminiDidReceiveToolCallCancellation(cancellation)
            return
        }

        // Server content (audio / text)
        if let serverContent = json["serverContent"] as? [String: Any] {
            let turnComplete = serverContent["turnComplete"] as? Bool ?? false

            if let modelTurn = serverContent["modelTurn"] as? [String: Any],
               let parts = modelTurn["parts"] as? [[String: Any]] {
                for part in parts {
                    // Audio response
                    if let inlineData = part["inlineData"] as? [String: Any],
                       let b64 = inlineData["data"] as? String,
                       let audioData = Data(base64Encoded: b64) {
                        delegate?.geminiModelSpeakingChanged(true)
                        delegate?.geminiDidReceiveAudio(audioData)
                    }
                    // Text transcript
                    if let text = part["text"] as? String {
                        delegate?.geminiDidReceiveTranscript(text, isFinal: turnComplete)
                    }
                }
            }

            if turnComplete {
                delegate?.geminiModelSpeakingChanged(false)
            }
        }
    }

    private func sendJSON(_ dict: [String: Any]) {
        guard let data = try? JSONSerialization.data(withJSONObject: dict),
              let string = String(data: data, encoding: .utf8) else { return }
        webSocketTask?.send(.string(string)) { error in
            if let error {
                print("[GeminiLive] Send error: \(error)")
            }
        }
    }
}
