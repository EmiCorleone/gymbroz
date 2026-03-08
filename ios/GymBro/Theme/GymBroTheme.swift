import SwiftUI

struct GlassCard: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(AppColor.glassBackground)
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(AppColor.cardBorder, lineWidth: 1)
            )
    }
}

struct GlowModifier: ViewModifier {
    let color: Color
    let radius: CGFloat

    func body(content: Content) -> some View {
        content.shadow(color: color.opacity(0.5), radius: radius)
    }
}

extension View {
    func glassCard() -> some View {
        modifier(GlassCard())
    }

    func glow(_ color: Color = AppColor.accent, radius: CGFloat = 10) -> some View {
        modifier(GlowModifier(color: color, radius: radius))
    }
}
