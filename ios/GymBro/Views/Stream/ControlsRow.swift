import SwiftUI

struct ControlsRow: View {
    @ObservedObject var streamViewModel: StreamViewModel
    @ObservedObject var geminiViewModel: GeminiSessionViewModel

    var body: some View {
        HStack(spacing: 16) {
            CircleButton(icon: "stop.fill", color: AppColor.error) {
                Task { await streamViewModel.stopStreaming() }; geminiViewModel.stopSession()
            }
            CircleButton(icon: geminiViewModel.isActive ? "waveform.circle.fill" : "waveform.circle", color: geminiViewModel.isActive ? AppColor.accentGreen : AppColor.accent) {
                if geminiViewModel.isActive { geminiViewModel.stopSession() } else { geminiViewModel.startSession() }
            }
            CircleButton(icon: "camera.fill", color: AppColor.accent) { streamViewModel.capturePhoto() }
            CircleButton(icon: streamViewModel.isRecording ? "record.circle.fill" : "record.circle", color: streamViewModel.isRecording ? AppColor.error : AppColor.accent) { streamViewModel.toggleRecording() }
            CircleButton(icon: streamViewModel.streamingMode == .glasses ? "iphone" : "eyeglasses", color: AppColor.accentPurple) { streamViewModel.switchMode() }
        }.padding(16).glassCard()
    }
}
