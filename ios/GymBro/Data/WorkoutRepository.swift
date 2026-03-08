import Foundation
import SwiftData
import Supabase

@MainActor
final class WorkoutRepository: ObservableObject {
    private let modelContext: ModelContext
    private var supabase: SupabaseClient { GymBroSupabaseClient.shared.client }

    init(modelContext: ModelContext) {
        self.modelContext = modelContext
    }

    // MARK: - Profile

    func saveProfile(_ profile: UserProfile) {
        modelContext.insert(profile)
        try? modelContext.save()
    }

    func getProfile() -> UserProfile? {
        let descriptor = FetchDescriptor<UserProfile>()
        return try? modelContext.fetch(descriptor).first
    }

    func isOnboardingComplete() -> Bool {
        let descriptor = FetchDescriptor<UserProfile>()
        return (try? modelContext.fetchCount(descriptor)) ?? 0 > 0
    }

    func clearProfile() {
        let descriptor = FetchDescriptor<UserProfile>()
        if let profiles = try? modelContext.fetch(descriptor) {
            for profile in profiles {
                modelContext.delete(profile)
            }
        }
        try? modelContext.save()
    }

    // MARK: - Sessions

    func startSession(isPhoneMode: Bool = false) -> WorkoutSession {
        let session = WorkoutSession(isPhoneMode: isPhoneMode)
        modelContext.insert(session)
        try? modelContext.save()

        Task {
            await syncSessionToSupabase(session)
        }
        return session
    }

    func endSession(_ session: WorkoutSession, totalReps: Int, totalExercises: Int, videoUrl: String? = nil) {
        let now = Date()
        let duration = Int(now.timeIntervalSince(session.startTime) / 60)
        session.endTime = now
        session.durationMinutes = duration
        session.totalReps = totalReps
        session.totalExercises = totalExercises
        session.videoUrl = videoUrl
        try? modelContext.save()

        Task {
            await syncSessionToSupabase(session)
        }
    }

    func getRecentSessions(limit: Int = 5) -> [WorkoutSession] {
        var descriptor = FetchDescriptor<WorkoutSession>(
            sortBy: [SortDescriptor(\.startTime, order: .reverse)]
        )
        descriptor.fetchLimit = limit
        return (try? modelContext.fetch(descriptor)) ?? []
    }

    func getAllSessions() -> [WorkoutSession] {
        let descriptor = FetchDescriptor<WorkoutSession>(
            sortBy: [SortDescriptor(\.startTime, order: .reverse)]
        )
        return (try? modelContext.fetch(descriptor)) ?? []
    }

    // MARK: - Exercise Sets

    func logExerciseSet(session: WorkoutSession, exerciseName: String, repCount: Int, guideImagePath: String? = nil) -> ExerciseSet {
        let set = ExerciseSet(
            exerciseName: exerciseName,
            repCount: repCount,
            endTime: Date(),
            guideImagePath: guideImagePath,
            session: session
        )
        modelContext.insert(set)
        try? modelContext.save()

        Task {
            await syncExerciseSetToSupabase(set, session: session)
        }
        return set
    }

    func logRepEvent(sessionId: String, exerciseName: String, repNumber: Int) {
        Task {
            do {
                guard let userId = try await supabase.auth.session.user.id.uuidString as String? else { return }
                try await supabase.database
                    .from("rep_events")
                    .insert([
                        "session_id": sessionId,
                        "user_id": userId,
                        "exercise_name": exerciseName,
                        "rep_number": "\(repNumber)"
                    ])
                    .execute()
            } catch {
                print("[WorkoutRepository] Failed to log rep event: \(error)")
            }
        }
    }

    // MARK: - Stats

    func getTotalSessionCount() -> Int {
        let descriptor = FetchDescriptor<WorkoutSession>()
        return (try? modelContext.fetchCount(descriptor)) ?? 0
    }

    func getTotalReps() -> Int {
        let sessions = getAllSessions()
        return sessions.reduce(0) { $0 + $1.totalReps }
    }

    func getTotalMinutes() -> Int {
        let sessions = getAllSessions()
        return sessions.reduce(0) { $0 + $1.durationMinutes }
    }

    func getCurrentStreak() -> Int {
        let sessions = getAllSessions()
        if sessions.isEmpty { return 0 }

        let calendar = Calendar.current
        let activeDays = Set(sessions.map { calendar.startOfDay(for: $0.startTime) }).sorted(by: >)

        var streak = 0
        var checkDate = calendar.startOfDay(for: Date())

        for day in activeDays {
            if day == checkDate {
                streak += 1
                checkDate = calendar.date(byAdding: .day, value: -1, to: checkDate)!
            } else if day < checkDate {
                break
            }
        }
        return streak
    }

    func getActiveDaysSince(_ since: Date) -> Int {
        let sessions = getAllSessions().filter { $0.startTime >= since }
        let calendar = Calendar.current
        let uniqueDays = Set(sessions.map { calendar.startOfDay(for: $0.startTime) })
        return uniqueDays.count
    }

    func getWeeklyActivity() -> [Bool] {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let weekAgo = calendar.date(byAdding: .day, value: -6, to: today)!
        let sessions = getAllSessions().filter { $0.startTime >= weekAgo }
        let activeDays = Set(sessions.map { calendar.startOfDay(for: $0.startTime) })

        return (0..<7).map { offset in
            let day = calendar.date(byAdding: .day, value: offset, to: weekAgo)!
            return activeDays.contains(day)
        }
    }

    // MARK: - Supabase Sync

    private func syncSessionToSupabase(_ session: WorkoutSession) async {
        do {
            guard let userId = try? await supabase.auth.session.user.id.uuidString else { return }
            let data: [String: String] = [
                "id": session.persistentModelID.hashValue.description,
                "user_id": userId,
                "is_phone_mode": session.isPhoneMode ? "true" : "false",
                "total_reps": "\(session.totalReps)",
                "total_exercises": "\(session.totalExercises)",
                "duration_minutes": "\(session.durationMinutes)"
            ]
            try await supabase.database.from("workout_sessions").upsert(data).execute()
        } catch {
            print("[WorkoutRepository] Supabase session sync failed: \(error)")
        }
    }

    private func syncExerciseSetToSupabase(_ set: ExerciseSet, session: WorkoutSession) async {
        do {
            guard let userId = try? await supabase.auth.session.user.id.uuidString else { return }
            let data: [String: String] = [
                "id": set.persistentModelID.hashValue.description,
                "session_id": session.persistentModelID.hashValue.description,
                "user_id": userId,
                "exercise_name": set.exerciseName,
                "rep_count": "\(set.repCount)"
            ]
            try await supabase.database.from("exercise_sets").insert(data).execute()
        } catch {
            print("[WorkoutRepository] Supabase exercise set sync failed: \(error)")
        }
    }
}
