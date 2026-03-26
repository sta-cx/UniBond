import SwiftUI

struct AchievementBadge: View {
    let name: String
    let icon: String
    let subtitle: String
    let unlocked: Bool

    var body: some View {
        VStack(spacing: 6) {
            ZStack {
                Circle()
                    .fill(unlocked ? AppColors.primaryPurple.opacity(0.1) : Color.gray.opacity(0.1))
                    .frame(width: 56, height: 56)
                if unlocked {
                    Text(icon)
                        .font(.system(size: 28))
                } else {
                    Image(systemName: "lock.fill")
                        .foregroundStyle(.gray)
                }
            }
            Text(name)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(unlocked ? AppColors.textPrimary : .gray)
            Text(subtitle)
                .font(.system(size: 9))
                .foregroundStyle(AppColors.textSecondary)
        }
    }
}
