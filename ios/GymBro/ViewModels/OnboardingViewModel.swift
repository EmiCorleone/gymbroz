import SwiftUI
import SwiftData
import Supabase

@MainActor
final class OnboardingViewModel: ObservableObject {
    // Auth
    @Published var email = ""
    @Published var password = ""
    @Published var isSignUp = true
    @Published var authError: String?
    @Published var isAuthLoading = false

    // Profile data
    @Published var name = ""
    @Published var gender = ""
    @Published var age = ""
    @Published var heightCm = ""
    @Published var weightKg = ""
    @Published var fitnessGoal = ""
    @Published var experienceLevel = ""
    @Published var weeklyWorkouts = ""
    @Published var mirrorPhoto: UIImage?
    @Published var mirrorPhotoPath: String?

    @Published var isSaving = false
    @Published var saveError: String?

    private var supabase: SupabaseClient { GymBroSupabaseClient.shared.client }

    func signInAnonymously() async {
        do {
            try await supabase.auth.signInAnonymously()
        } catch {
            print("[Onboarding] Anonymous sign-in failed: \(error)")
        }
    }

    func authenticate() async {
        isAuthLoading = true
        authError = nil

        do {
            if isSignUp {
                try await supabase.auth.signUp(email: email, password: password)
            } else {
                try await supabase.auth.signIn(email: email, password: password)
            }
        } catch {
            authError = error.localizedDescription
        }

        isAuthLoading = false
    }

    func saveProfile(modelContext: ModelContext) async -> Bool {
        isSaving = true
        saveError = nil

        // Ensure auth session
        do {
            _ = try await supabase.auth.session
        } catch {
            await signInAnonymously()
        }

        // Upload mirror photo if available
        var photoURL: String?
        if let photo = mirrorPhoto, let jpegData = photo.jpegData(compressionQuality: 0.8) {
            do {
                let fileName = "mirror_\(UUID().uuidString).jpg"
                try await supabase.storage.from("gymbro_assets").upload(
                    path: fileName,
                    file: jpegData,
                    options: FileOptions(contentType: "image/jpeg")
                )
                let publicURL = try supabase.storage.from("gymbro_assets").getPublicURL(path: fileName)
                photoURL = publicURL.absoluteString
            } catch {
                print("[Onboarding] Photo upload failed: \(error)")
            }
        }

        // Save to local SwiftData
        let profile = UserProfile(
            name: name,
            gender: gender,
            age: Int(age) ?? 0,
            heightCm: Int(heightCm) ?? 0,
            weightKg: Int(weightKg) ?? 0,
            fitnessGoal: fitnessGoal,
            experienceLevel: experienceLevel,
            weeklyWorkouts: weeklyWorkouts,
            mirrorPhotoPath: photoURL ?? mirrorPhotoPath
        )
        modelContext.insert(profile)
        try? modelContext.save()

        // Sync to Supabase
        do {
            guard let userId = try? await supabase.auth.session.user.id.uuidString else {
                isSaving = false
                return true // Local save succeeded
            }
            try await supabase.database.from("user_profiles").upsert([
                "id": userId,
                "name": name,
                "gender": gender,
                "age": age,
                "height_cm": heightCm,
                "weight_kg": weightKg,
                "fitness_goal": fitnessGoal,
                "experience_level": experienceLevel,
                "weekly_workouts": weeklyWorkouts,
                "mirror_photo_url": photoURL ?? ""
            ]).execute()
        } catch {
            print("[Onboarding] Supabase profile sync failed: \(error)")
        }

        if let photoURL {
            UserDefaults.standard.set(photoURL, forKey: "mirrorPhotoPath")
        }

        isSaving = false
        return true
    }
}
