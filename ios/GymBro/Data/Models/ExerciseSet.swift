import Foundation
import SwiftData

@Model
final class ExerciseSet {
    var exerciseName: String
    var repCount: Int
    var startTime: Date
    var endTime: Date?
    var guideImagePath: String?
    var session: WorkoutSession?

    init(
        exerciseName: String = "",
        repCount: Int = 0,
        startTime: Date = Date(),
        endTime: Date? = nil,
        guideImagePath: String? = nil,
        session: WorkoutSession? = nil
    ) {
        self.exerciseName = exerciseName
        self.repCount = repCount
        self.startTime = startTime
        self.endTime = endTime
        self.guideImagePath = guideImagePath
        self.session = session
    }
}
