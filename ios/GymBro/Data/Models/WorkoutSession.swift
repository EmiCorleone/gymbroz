import Foundation
import SwiftData

@Model
final class WorkoutSession {
    var startTime: Date
    var endTime: Date?
    var durationMinutes: Int
    var totalReps: Int
    var totalExercises: Int
    var isPhoneMode: Bool
    var videoUrl: String?
    @Relationship(deleteRule: .cascade, inverse: \ExerciseSet.session)
    var exercises: [ExerciseSet]

    init(
        startTime: Date = Date(),
        endTime: Date? = nil,
        durationMinutes: Int = 0,
        totalReps: Int = 0,
        totalExercises: Int = 0,
        isPhoneMode: Bool = false,
        videoUrl: String? = nil,
        exercises: [ExerciseSet] = []
    ) {
        self.startTime = startTime
        self.endTime = endTime
        self.durationMinutes = durationMinutes
        self.totalReps = totalReps
        self.totalExercises = totalExercises
        self.isPhoneMode = isPhoneMode
        self.videoUrl = videoUrl
        self.exercises = exercises
    }
}
