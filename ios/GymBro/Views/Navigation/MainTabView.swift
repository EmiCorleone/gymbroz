import SwiftUI
import MWDATCore

struct MainTabView: View {
    let wearables: WearablesInterface
    @ObservedObject var wearablesViewModel: WearablesViewModel
    @State private var selectedTab = 0
    @StateObject private var geminiViewModel = GeminiSessionViewModel()

    var body: some View {
        ZStack(alignment: .bottom) {
            AppColor.background.ignoresSafeArea()
            TabView(selection: $selectedTab) {
                DashboardView(onStartWorkout: { selectedTab = 1 }).tag(0)
                StreamView(wearables: wearables, wearablesViewModel: wearablesViewModel, geminiViewModel: geminiViewModel).tag(1)
                NavigationStack { ProfileView() }.tag(2)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))

            HStack(spacing: 0) {
                TabButton(icon: "house.fill", label: "Dashboard", isSelected: selectedTab == 0) { selectedTab = 0 }
                TabButton(icon: "figure.strengthtraining.traditional", label: "Workout", isSelected: selectedTab == 1) { selectedTab = 1 }
                TabButton(icon: "person.fill", label: "Profile", isSelected: selectedTab == 2) { selectedTab = 2 }
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            .glassCard()
            .padding(.horizontal, 24).padding(.bottom, 8)
        }
    }
}

private struct TabButton: View {
    let icon: String; let label: String; let isSelected: Bool; let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon).font(.system(size: 20))
                Text(label).font(.caption2)
            }.foregroundColor(isSelected ? AppColor.accent : AppColor.textSecondary).frame(maxWidth: .infinity)
        }
    }
}
