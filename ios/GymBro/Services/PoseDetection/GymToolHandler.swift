import Foundation
import UIKit

@MainActor
final class GymToolHandler {
    private let poseDetectionManager: PoseDetectionManager
    private var currentExercise: String?
    private var exerciseGuideTask: Task<Void, Never>?

    var onExerciseGuideGenerated: ((UIImage, String) -> Void)?
    var onToolStatus: ((ToolCallStatus) -> Void)?

    init(poseDetectionManager: PoseDetectionManager) {
        self.poseDetectionManager = poseDetectionManager
    }

    func handleToolCall(_ call: GeminiFunctionCall) async -> ToolResult {
        switch call.name {
        case "start_rep_counting":
            let exercise = call.args["exercise"] ?? "bicep_curl"
            currentExercise = exercise
            poseDetectionManager.start(exercise: exercise)
            return .success("Started counting reps for \(exercise). Counter reset to 0.")

        case "stop_rep_counting":
            let finalCount = poseDetectionManager.stop()
            let exercise = currentExercise ?? "exercise"
            currentExercise = nil
            return .success("Stopped counting. Final count: \(finalCount) reps of \(exercise).")

        case "get_rep_count":
            let count = poseDetectionManager.getRepCount()
            return .success("Current rep count: \(count)")

        case "play_music":
            let prompt = call.args["prompt"] ?? "energetic workout"
            let bpm = call.args["bpm"] ?? "120"
            return .success("Music playback started: \(prompt) at \(bpm) BPM")

        case "stop_music":
            return .success("Music stopped.")

        case "change_music":
            let prompt = call.args["prompt"] ?? ""
            let bpm = call.args["bpm"] ?? ""
            return .success("Music changed. Style: \(prompt), BPM: \(bpm)")

        case "generate_exercise_guide":
            let description = call.args["exercise_description"] ?? ""
            return await generateExerciseGuide(description: description)

        default:
            return .failure("Unknown tool: \(call.name)")
        }
    }

    private func generateExerciseGuide(description: String) async -> ToolResult {
        // Load user's mirror photo for exercise guide generation
        // Uses Gemini Image Gen API
        guard let profilePhotoURL = UserDefaults.standard.string(forKey: "mirrorPhotoPath"),
              let url = URL(string: profilePhotoURL),
              let imageData = try? Data(contentsOf: url),
              let image = UIImage(data: imageData) else {
            return .failure("No profile photo available for exercise guide generation.")
        }

        guard let jpegData = image.jpegData(compressionQuality: 0.8) else {
            return .failure("Failed to process profile photo.")
        }

        let base64Image = jpegData.base64EncodedString()

        let requestBody: [String: Any] = [
            "contents": [
                [
                    "parts": [
                        [
                            "inlineData": [
                                "mimeType": "image/jpeg",
                                "data": base64Image
                            ]
                        ],
                        [
                            "text": "Edit this photo to show the person performing the following exercise with correct form: \(description). Keep the person's appearance the same but adjust their pose to demonstrate proper technique."
                        ]
                    ]
                ]
            ],
            "generationConfig": [
                "responseModalities": ["TEXT", "IMAGE"]
            ]
        ]

        do {
            let url = URL(string: "\(GeminiConfig.imageGenEndpoint)?key=\(GeminiConfig.apiKey)")!
            var request = URLRequest(url: url)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)

            let (data, _) = try await URLSession.shared.data(for: request)

            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let candidates = json["candidates"] as? [[String: Any]],
               let content = candidates.first?["content"] as? [String: Any],
               let parts = content["parts"] as? [[String: Any]] {

                var guideText = ""
                for part in parts {
                    if let text = part["text"] as? String {
                        guideText = text
                    }
                    if let inlineData = part["inlineData"] as? [String: Any],
                       let b64 = inlineData["data"] as? String,
                       let imgData = Data(base64Encoded: b64),
                       let guideImage = UIImage(data: imgData) {
                        onExerciseGuideGenerated?(guideImage, guideText)
                    }
                }
                return .success("Exercise guide generated for: \(description). \(guideText)")
            }
            return .failure("Failed to parse exercise guide response.")
        } catch {
            return .failure("Exercise guide generation failed: \(error.localizedDescription)")
        }
    }
}
