import SwiftUI

struct GeminiOverlayView: View {
    @ObservedObject var viewModel: GeminiSessionViewModel

    var body: some View {
        VStack {
            HStack {
                Circle().fill(statusColor).frame(width: 8, height: 8)
                Text(statusText).font(.caption).foregroundColor(AppColor.textSecondary)
                Spacer()
                if viewModel.isModelSpeaking {
                    HStack(spacing: 2) { ForEach(0..<3, id: \.self) { _ in Capsule().fill(AppColor.accent).frame(width: 3, height: CGFloat.random(in: 8...16)) } }
                }
            }.padding(.horizontal, 20).padding(.top, 60)

            if viewModel.isRepCountingActive {
                HStack {
                    VStack(alignment: .leading) {
                        Text(viewModel.currentExercise.replacingOccurrences(of: "_", with: " ").capitalized).font(.caption).foregroundColor(AppColor.textSecondary)
                        Text("\(viewModel.currentRepCount)").font(.system(size: 48, weight: .black, design: .monospaced)).foregroundColor(AppColor.accentGreen)
                        Text("REPS").font(.caption.bold()).foregroundColor(AppColor.accentGreen.opacity(0.7))
                    }.padding().glassCard().glow(AppColor.accentGreen, radius: 12)
                    Spacer()
                }.padding(.horizontal, 20)
            }

            Spacer()

            if !viewModel.modelTranscript.isEmpty {
                Text(viewModel.modelTranscript).font(.subheadline).foregroundColor(.white).padding(12)
                    .background(Color.black.opacity(0.6)).clipShape(RoundedRectangle(cornerRadius: 8))
                    .padding(.horizontal, 20).padding(.bottom, 100)
            }
        }
    }

    private var statusColor: Color {
        switch viewModel.connectionState {
        case .connected: return AppColor.accentGreen
        case .connecting: return AppColor.accentOrange
        case .disconnected: return AppColor.error
        }
    }
    private var statusText: String {
        switch viewModel.connectionState {
        case .connected: return "AI Coach Connected"
        case .connecting: return "Connecting..."
        case .disconnected: return "Disconnected"
        }
    }
}
