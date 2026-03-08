import Foundation
import SwiftData

@Model
final class UserProfile {
    var name: String
    var gender: String           // "Male", "Female", "Other"
    var age: Int
    var heightCm: Int
    var weightKg: Int
    var fitnessGoal: String      // "build_muscle", "lose_weight", "stay_active", "improve_health"
    var experienceLevel: String  // "beginner", "intermediate", "advanced"
    var weeklyWorkouts: String   // "0-2", "3-5", "6+"
    var mirrorPhotoPath: String?
    var createdAt: Date
    var updatedAt: Date

    init(
        name: String = "",
        gender: String = "",
        age: Int = 0,
        heightCm: Int = 0,
        weightKg: Int = 0,
        fitnessGoal: String = "",
        experienceLevel: String = "",
        weeklyWorkouts: String = "",
        mirrorPhotoPath: String? = nil,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.name = name
        self.gender = gender
        self.age = age
        self.heightCm = heightCm
        self.weightKg = weightKg
        self.fitnessGoal = fitnessGoal
        self.experienceLevel = experienceLevel
        self.weeklyWorkouts = weeklyWorkouts
        self.mirrorPhotoPath = mirrorPhotoPath
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}
