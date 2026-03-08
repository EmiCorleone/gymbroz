import Foundation

struct GeminiFunctionCall: Codable, Identifiable {
    let id: String
    let name: String
    let args: [String: String]
}

struct GeminiToolCall {
    let functionCalls: [GeminiFunctionCall]

    static func parse(from json: [String: Any]) -> GeminiToolCall? {
        guard let parts = (json["serverContent"] as? [String: Any])?["modelTurn"]?["parts"] as? [[String: Any]] else {
            // Try toolCall format
            if let toolCallParts = (json["toolCall"] as? [String: Any])?["functionCalls"] as? [[String: Any]] {
                let calls = toolCallParts.compactMap { part -> GeminiFunctionCall? in
                    guard let id = part["id"] as? String,
                          let name = part["name"] as? String else { return nil }
                    let args = (part["args"] as? [String: String]) ?? [:]
                    return GeminiFunctionCall(id: id, name: name, args: args)
                }
                return calls.isEmpty ? nil : GeminiToolCall(functionCalls: calls)
            }
            return nil
        }

        let calls = parts.compactMap { part -> GeminiFunctionCall? in
            guard let fc = part["functionCall"] as? [String: Any],
                  let id = fc["id"] as? String,
                  let name = fc["name"] as? String else { return nil }
            let args = (fc["args"] as? [String: String]) ?? [:]
            return GeminiFunctionCall(id: id, name: name, args: args)
        }
        return calls.isEmpty ? nil : GeminiToolCall(functionCalls: calls)
    }
}

struct GeminiToolCallCancellation {
    let ids: [String]

    static func parse(from json: [String: Any]) -> GeminiToolCallCancellation? {
        guard let cancellation = json["toolCallCancellation"] as? [String: Any],
              let ids = cancellation["ids"] as? [String] else { return nil }
        return GeminiToolCallCancellation(ids: ids)
    }
}

enum ToolResult {
    case success(String)
    case failure(String)

    var jsonValue: [String: Any] {
        switch self {
        case .success(let value):
            return ["result": value]
        case .failure(let error):
            return ["error": error]
        }
    }
}

struct ToolCallStatus: Identifiable {
    let id: String
    let name: String
    var status: String
    var result: String?
}

enum GymToolDeclarations {
    static func toJSON() -> [[String: Any]] {
        return [
            [
                "function_declarations": [
                    [
                        "name": "start_rep_counting",
                        "description": "Start counting exercise repetitions using pose detection. Resets the counter each time.",
                        "parameters": [
                            "type": "OBJECT",
                            "properties": [
                                "exercise": [
                                    "type": "STRING",
                                    "description": "The exercise to count, e.g. 'bicep_curl'"
                                ]
                            ],
                            "required": ["exercise"]
                        ]
                    ],
                    [
                        "name": "stop_rep_counting",
                        "description": "Stop counting reps and return the final count.",
                        "parameters": [
                            "type": "OBJECT",
                            "properties": [:] as [String: Any]
                        ]
                    ],
                    [
                        "name": "get_rep_count",
                        "description": "Get the current rep count without stopping.",
                        "parameters": [
                            "type": "OBJECT",
                            "properties": [:] as [String: Any]
                        ]
                    ],
                    [
                        "name": "play_music",
                        "description": "Start playing workout music.",
                        "parameters": [
                            "type": "OBJECT",
                            "properties": [
                                "prompt": [
                                    "type": "STRING",
                                    "description": "Style description for the music"
                                ],
                                "bpm": [
                                    "type": "INTEGER",
                                    "description": "Beats per minute"
                                ]
                            ],
                            "required": ["prompt"]
                        ]
                    ],
                    [
                        "name": "stop_music",
                        "description": "Stop playing music.",
                        "parameters": [
                            "type": "OBJECT",
                            "properties": [:] as [String: Any]
                        ]
                    ],
                    [
                        "name": "change_music",
                        "description": "Change the current music style or BPM.",
                        "parameters": [
                            "type": "OBJECT",
                            "properties": [
                                "prompt": [
                                    "type": "STRING",
                                    "description": "New style description"
                                ],
                                "bpm": [
                                    "type": "INTEGER",
                                    "description": "New BPM"
                                ]
                            ]
                        ]
                    ],
                    [
                        "name": "generate_exercise_guide",
                        "description": "Generate an image showing the correct exercise form using the user's photo.",
                        "parameters": [
                            "type": "OBJECT",
                            "properties": [
                                "exercise_description": [
                                    "type": "STRING",
                                    "description": "Description of the exercise to demonstrate"
                                ]
                            ],
                            "required": ["exercise_description"]
                        ]
                    ]
                ]
            ]
        ]
    }
}
