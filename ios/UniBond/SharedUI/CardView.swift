import SwiftUI

struct CardView<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .background(AppColors.cardBackground)
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
