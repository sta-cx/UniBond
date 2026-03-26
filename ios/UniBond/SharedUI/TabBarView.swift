import SwiftUI

struct TabBarView: View {
    @Binding var selectedTab: Int

    var body: some View {
        HStack {
            tabItem(icon: "house.fill", label: "首页", index: 0)
            Spacer()
            tabItem(icon: "chart.bar.fill", label: "统计", index: 1)
            Spacer()
            tabItem(icon: "person.fill", label: "我的", index: 2)
        }
        .padding(.horizontal, 40)
        .padding(.vertical, 12)
        .background(.ultraThinMaterial)
    }

    private func tabItem(icon: String, label: String, index: Int) -> some View {
        Button {
            selectedTab = index
        } label: {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                Text(label)
                    .font(.system(size: 11, weight: .medium))
            }
            .foregroundStyle(selectedTab == index ? AppColors.primaryPurple : AppColors.textSecondary)
        }
    }
}
