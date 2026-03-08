import SwiftUI

struct AnimatedMeshGradientView: View {
    @State private var animating = false

    var body: some View {
        if #available(iOS 18.0, *) {
            MeshGradient(
                width: 3, height: 3,
                points: [
                    [0, 0], [0.5, 0], [1, 0],
                    [0, 0.5], [animating ? 0.6 : 0.4, 0.5], [1, 0.5],
                    [0, 1], [0.5, 1], [1, 1]
                ],
                colors: [
                    .black, AppColor.accentPurple.opacity(0.3), .black,
                    AppColor.accent.opacity(0.2), AppColor.accentGreen.opacity(0.2), AppColor.accentOrange.opacity(0.2),
                    .black, AppColor.accent.opacity(0.3), .black
                ]
            )
            .onAppear {
                withAnimation(.easeInOut(duration: 4).repeatForever(autoreverses: true)) {
                    animating = true
                }
            }
        } else {
            LinearGradient(
                colors: [.black, AppColor.accent.opacity(0.2), .black],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }
}
