import Foundation

enum Secrets {
    // REQUIRED: Get your key at https://aistudio.google.com/apikey
    static let geminiAPIKey: String = {
        ProcessInfo.processInfo.environment["GEMINI_API_KEY"] ?? ""
    }()

    // Supabase
    static let supabaseURL: String = {
        ProcessInfo.processInfo.environment["SUPABASE_URL"] ?? ""
    }()

    static let supabaseAnonKey: String = {
        ProcessInfo.processInfo.environment["SUPABASE_ANON_KEY"] ?? ""
    }()

    // Optional: OpenClaw
    static let openClawHost = "http://YOUR_MAC_HOSTNAME.local"
    static let openClawPort = 18789
    static let openClawHookToken = ""
    static let openClawGatewayToken = ""

    // Optional: WebRTC signaling
    static let webrtcSignalingURL = "wss://YOUR_SIGNALING_SERVER"
}
