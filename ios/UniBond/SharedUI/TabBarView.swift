import SwiftUI

struct TabBarView: View {
    @Binding var selectedTab: Int

    var body: some View {
        HStack(spacing: 0) {
            tabItem(icon: "house.fill", label: "首页", index: 0)
            tabItem(icon: "heart.text.square.fill", label: "统计", index: 1)
            tabItem(icon: "person.fill", label: "我的", index: 2)
        }
        .padding(.horizontal, 8)
        .padding(.top, 8)
        .padding(.bottom, 4)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 24))
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .shadow(color: .black.opacity(0.06), radius: 10, x: 0, y: 2)
        .padding(.horizontal, 20)
        .padding(.bottom, 4)
    }

    private func tabItem(icon: String, label: String, index: Int) -> some View {
        Button {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                selectedTab = index
            }
        } label: {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: selectedTab == index ? 23 : 21))
                    .symbolRenderingMode(.monochrome)
                    .contentTransition(.symbolEffect(.replace.byLayer.downUp))

                Text(label)
                    .font(.system(size: 10, weight: .semibold))
                    .transaction { $0.animation = nil }
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 20)
            .padding(.vertical, 8)
            .background(
                selectedTab == index
                    ? LinearGradient(
                        colors: [AppColors.primaryPurple.opacity(0.12), AppColors.primaryPink.opacity(0.12)],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    : nil
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .foregroundStyle(selectedTab == index ? AppColors.primaryPurple : .secondary.opacity(0.6))
            .animation(.spring(duration: 0.3), value: selectedTab)
        }
    }
}
