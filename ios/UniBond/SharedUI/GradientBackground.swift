import SwiftUI

struct GradientBackground: ViewModifier {
    func body(content: Content) -> some View {
        content.background(AppColors.backgroundGradient.ignoresSafeArea())
    }
}

extension View {
    func gradientBackground() -> some View {
        modifier(GradientBackground())
    }
}
