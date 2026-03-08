import SwiftUI
import MWDATCore

struct StreamView: View {
    let wearables: WearablesInterface
    @ObservedObject var wearablesViewModel: WearablesViewModel
    @ObservedObject var geminiViewModel: GeminiSessionViewModel
    @StateObject private var streamViewModel: StreamViewModel

    init(wearables: WearablesInterface, wearablesViewModel: WearablesViewModel, geminiViewModel: GeminiSessionViewModel) {
        self.wearables = wearables
        self.wearablesViewModel = wearablesViewModel
        self.geminiViewModel = geminiViewModel
        self._streamViewModel = StateObject(wrappedValue: StreamViewModel(wearables: wearables, geminiViewModel: geminiViewModel))
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if let frame = streamViewModel.currentFrame {
                GeometryReader { geo in
                    Image(uiImage: frame).resizable().aspectRatio(contentMode: .fill)
                        .frame(width: geo.size.width, height: geo.size.height).clipped()
                }.ignoresSafeArea()
            } else if streamViewModel.isStreaming {
                ProgressView().scaleEffect(1.5).foregroundColor(.white)
            } else {
                VStack(spacing: 24) {
                    Spacer()
                    Image(systemName: "video.fill").font(.system(size: 60)).foregroundColor(AppColor.accent)
                    Text(wearablesViewModel.registrationState == .registered ? "Ready to stream" : "Connect your glasses first").foregroundColor(AppColor.textSecondary)
                    HStack(spacing: 16) {
                        if wearablesViewModel.registrationState == .registered {
                            Button(action: { streamViewModel.streamingMode = .glasses; Task { await streamViewModel.startStreaming() } }) {
                                Label("Glasses", systemImage: "eyeglasses").padding().background(AppColor.accent).foregroundColor(.white).clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        } else {
                            Button(action: { wearablesViewModel.connectGlasses() }) {
                                Label("Connect Glasses", systemImage: "eyeglasses").padding().background(AppColor.surface).foregroundColor(AppColor.textPrimary).clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        }
                        Button(action: { streamViewModel.streamingMode = .phone; Task { await streamViewModel.startStreaming() } }) {
                            Label("Phone", systemImage: "iphone").padding().background(AppColor.surface).foregroundColor(AppColor.textPrimary).clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }
                    Spacer()
                }
            }

            if geminiViewModel.isActive { GeminiOverlayView(viewModel: geminiViewModel) }

            if streamViewModel.isStreaming {
                VStack { Spacer(); ControlsRow(streamViewModel: streamViewModel, geminiViewModel: geminiViewModel) }.padding(24)
            }
        }
        .onOpenURL { url in wearablesViewModel.handleURL(url) }
        .alert("Error", isPresented: Binding(
            get: { streamViewModel.wearablesManager.showError },
            set: { streamViewModel.wearablesManager.showError = $0 }
        )) {
            Button("OK") { streamViewModel.wearablesManager.dismissError() }
        } message: { Text(streamViewModel.wearablesManager.errorMessage) }
    }
}
