import SwiftUI

struct CircleButton: View {
    let icon: String
    let color: Color
    let size: CGFloat
    let action: () -> Void

    init(icon: String, color: Color = AppColor.accent, size: CGFloat = 56, action: @escaping () -> Void) {
        self.icon = icon
        self.color = color
        self.size = size
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: size * 0.36))
                .foregroundColor(.white)
                .frame(width: size, height: size)
                .background(color.opacity(0.2))
                .clipShape(Circle())
                .overlay(Circle().stroke(color.opacity(0.4), lineWidth: 1))
        }
    }
}
