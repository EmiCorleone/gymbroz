import Foundation

enum GeminiConfig {
    static let wsBaseURL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    static let modelName = "gemini-2.5-flash-native-audio-preview-12-2025"
    static let imageGenModel = "gemini-2.5-flash-image"
    static let imageGenEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/\(imageGenModel):generateContent"

    // Audio settings
    static let inputSampleRate = 16000
    static let outputSampleRate = 24000
    static let channels = 1
    static let bitsPerSample = 16

    // Video settings
    static let frameIntervalSeconds: TimeInterval = 1.0
    static let jpegQuality: CGFloat = 0.5

    static var apiKey: String {
        SettingsManager.shared.effectiveGeminiAPIKey
    }

    static var systemPrompt: String {
        SettingsManager.shared.geminiSystemPrompt
    }

    static var wsURL: URL {
        URL(string: "\(wsBaseURL)?key=\(apiKey)")!
    }
}
