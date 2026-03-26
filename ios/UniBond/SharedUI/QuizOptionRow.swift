import SwiftUI

struct QuizOptionRow: View {
    let label: String
    let text: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Text(label)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(isSelected ? .white : AppColors.primaryPurple)
                    .frame(width: 28, height: 28)
                    .background(isSelected ? AppColors.primaryPurple : .clear)
                    .clipShape(Circle())
                    .overlay(
                        Circle().stroke(
                            AppColors.primaryPurple.opacity(0.3),
                            lineWidth: isSelected ? 0 : 1
                        )
                    )
                Text(text)
                    .font(.system(size: 15))
                    .foregroundStyle(AppColors.textPrimary)
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(AppColors.primaryPurple)
                }
            }
            .padding(16)
            .background(.white.opacity(isSelected ? 1 : 0.8))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? AppColors.primaryPurple : Color.clear, lineWidth: 2)
            )
        }
        .buttonStyle(.plain)
    }
}
