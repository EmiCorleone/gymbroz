import SwiftUI
import SwiftData
import MWDATCore

#if DEBUG
import MWDATMockDevice
#endif

@main
struct GymBroApp: App {
    #if DEBUG
    @StateObject private var debugMenuViewModel = DebugMenuViewModel(mockDeviceKit: MockDeviceKit.shared)
    #endif

    private let wearables: WearablesInterface
    @StateObject private var wearablesViewModel: WearablesViewModel
    @StateObject private var settingsManager = SettingsManager.shared

    init() {
        do {
            try Wearables.configure()
        } catch {
            #if DEBUG
            NSLog("[GymBro] Failed to configure Wearables SDK: \(error)")
            #endif
        }
        let wearables = Wearables.shared
        self.wearables = wearables
        self._wearablesViewModel = StateObject(wrappedValue: WearablesViewModel(wearables: wearables))
    }

    var body: some Scene {
        WindowGroup {
            ContentView(wearables: wearables, wearablesViewModel: wearablesViewModel)
                .modelContainer(for: [UserProfile.self, WorkoutSession.self, ExerciseSet.self])
                .environmentObject(settingsManager)
                #if DEBUG
                .sheet(isPresented: $debugMenuViewModel.showDebugMenu) {
                    MockDeviceKitView(viewModel: debugMenuViewModel.mockDeviceKitViewModel)
                }
                .overlay {
                    DebugMenuView(debugMenuViewModel: debugMenuViewModel)
                }
                #endif
        }
    }
}

struct ContentView: View {
    let wearables: WearablesInterface
    @ObservedObject var wearablesViewModel: WearablesViewModel
    @AppStorage("onboardingComplete") private var onboardingComplete = false

    var body: some View {
        if onboardingComplete {
            MainTabView(wearables: wearables, wearablesViewModel: wearablesViewModel)
        } else {
            OnboardingFlow(onComplete: {
                onboardingComplete = true
            })
        }
    }
}
