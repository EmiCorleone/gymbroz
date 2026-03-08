import SwiftUI
import SwiftData

struct DashboardState {
    var userName: String = ""
    var totalWorkouts: Int = 0
    var totalReps: Int = 0
    var totalMinutes: Int = 0
    var currentStreak: Int = 0
    var recentSessions: [WorkoutSession] = []
    var weeklyActivity: [Bool] = Array(repeating: false, count: 7)
    var mirrorPhotoPath: String?
}

@MainActor
final class DashboardViewModel: ObservableObject {
    @Published var state = DashboardState()

    private var repository: WorkoutRepository?

    func setup(modelContext: ModelContext) {
        repository = WorkoutRepository(modelContext: modelContext)
        loadData()
    }

    func loadData() {
        guard let repo = repository else { return }

        if let profile = repo.getProfile() {
            state.userName = profile.name
            state.mirrorPhotoPath = profile.mirrorPhotoPath
        }

        state.totalWorkouts = repo.getTotalSessionCount()
        state.totalReps = repo.getTotalReps()
        state.totalMinutes = repo.getTotalMinutes()
        state.currentStreak = repo.getCurrentStreak()
        state.recentSessions = repo.getRecentSessions(limit: 5)
        state.weeklyActivity = repo.getWeeklyActivity()

        // Mock data if empty
        if state.totalWorkouts == 0 {
            state.totalWorkouts = 12
            state.totalReps = 3450
            state.totalMinutes = 540
            state.currentStreak = 2
            state.weeklyActivity = [true, false, true, true, false, true, false]
        }
    }
}
