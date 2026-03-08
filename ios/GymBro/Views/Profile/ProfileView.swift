import SwiftUI
import SwiftData

struct ProfileView: View {
    @Environment(\.modelContext) private var modelContext
    @Query private var profiles: [UserProfile]
    @AppStorage("onboardingComplete") private var onboardingComplete = true
    private var profile: UserProfile? { profiles.first }

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Circle().fill(AppColor.accent.opacity(0.2)).frame(width: 100, height: 100)
                    .overlay(
                        Group {
                            if let path = profile?.mirrorPhotoPath, let url = URL(string: path) {
                                AsyncImage(url: url) { image in image.resizable().scaledToFill() } placeholder: { Image(systemName: "person.fill").font(.system(size: 40)).foregroundColor(AppColor.accent) }
                            } else { Image(systemName: "person.fill").font(.system(size: 40)).foregroundColor(AppColor.accent) }
                        }
                    ).clipShape(Circle()).overlay(Circle().stroke(AppColor.accent.opacity(0.5), lineWidth: 2)).glow(AppColor.accent, radius: 12)

                Text(profile?.name ?? "Athlete").font(.title2.bold()).foregroundColor(AppColor.textPrimary)

                if let goal = profile?.fitnessGoal, !goal.isEmpty {
                    Text(goal.replacingOccurrences(of: "_", with: " ").capitalized).font(.subheadline).foregroundColor(AppColor.accent)
                        .padding(.horizontal, 12).padding(.vertical, 4).background(AppColor.accent.opacity(0.15)).clipShape(Capsule())
                }

                if let p = profile {
                    VStack(spacing: 12) {
                        ProfileRow(label: "Gender", value: p.gender)
                        ProfileRow(label: "Age", value: "\(p.age)")
                        ProfileRow(label: "Height", value: "\(p.heightCm) cm")
                        ProfileRow(label: "Weight", value: "\(p.weightKg) kg")
                        ProfileRow(label: "Experience", value: p.experienceLevel.capitalized)
                        ProfileRow(label: "Weekly Goal", value: "\(p.weeklyWorkouts) days")
                    }.padding(16).glassCard()
                }

                NavigationLink(destination: SettingsView()) {
                    HStack {
                        Image(systemName: "gear").foregroundColor(AppColor.accent)
                        Text("Settings").foregroundColor(AppColor.textPrimary)
                        Spacer()
                        Image(systemName: "chevron.right").foregroundColor(AppColor.textSecondary)
                    }.padding(16).glassCard()
                }

                Button(action: { clearAndResetOnboarding() }) {
                    HStack {
                        Image(systemName: "arrow.counterclockwise").foregroundColor(AppColor.accentOrange)
                        Text("Restart Onboarding").foregroundColor(AppColor.accentOrange)
                    }.frame(maxWidth: .infinity).padding(16).glassCard()
                }

                Button(action: { Task { try? await GymBroSupabaseClient.shared.client.auth.signOut(); clearAndResetOnboarding() } }) {
                    Text("Log Out").foregroundColor(AppColor.error).frame(maxWidth: .infinity).padding(16).glassCard()
                }

                Spacer(minLength: 100)
            }.padding(.horizontal, 20).padding(.top, 40)
        }.background(AppColor.background).navigationTitle("Profile")
    }

    private func clearAndResetOnboarding() {
        let descriptor = FetchDescriptor<UserProfile>()
        if let existing = try? modelContext.fetch(descriptor) { for p in existing { modelContext.delete(p) } }
        try? modelContext.save()
        onboardingComplete = false
    }
}

private struct ProfileRow: View {
    let label: String; let value: String
    var body: some View { HStack { Text(label).foregroundColor(AppColor.textSecondary); Spacer(); Text(value).foregroundColor(AppColor.textPrimary).bold() } }
}
