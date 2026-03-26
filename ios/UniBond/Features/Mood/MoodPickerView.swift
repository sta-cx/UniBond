import SwiftUI

struct MoodPickerView: View {
    @Environment(\.dismiss) private var dismiss
    @State var viewModel: MoodViewModel
    let partnerName: String

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    if let mood = viewModel.partnerMood {
                        CardView {
                            VStack(spacing: 8) {
                                Text("TA 的心情")
                                    .font(.system(size: 13))
                                    .foregroundStyle(AppColors.textSecondary)
                                Text(mood.emoji)
                                    .font(.system(size: 40))
                                if let text = mood.text, !text.isEmpty {
                                    Text(text)
                                        .font(.system(size: 15))
                                        .foregroundStyle(AppColors.textPrimary)
                                }
                                if let time = Date.fromISO8601(mood.updatedAt) {
                                    Text(time.relativeTimeString)
                                        .font(.system(size: 11))
                                        .foregroundStyle(AppColors.textSecondary)
                                }
                            }
                            .padding(20)
                        }
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("选择你的心情")
                            .font(.system(size: 17, weight: .semibold))
                        EmojiGrid(emojis: viewModel.emojis, selected: Binding(get: { viewModel.selectedEmoji }, set: { viewModel.selectedEmoji = $0 }))
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("心情短语（选填）")
                            .font(.system(size: 13))
                            .foregroundStyle(AppColors.textSecondary)
                        TextField("今天想说点什么...", text: Binding(get: { viewModel.moodText }, set: { viewModel.moodText = String($0.prefix(50)) }))
                            .padding(12)
                            .background(.white.opacity(0.8))
                            .clipShape(RoundedRectangle(cornerRadius: 12))

                        Text("\(viewModel.moodText.count)/50")
                            .font(.system(size: 11))
                            .foregroundStyle(AppColors.textSecondary)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                    }

                    if let error = viewModel.errorMessage {
                        Text(error)
                            .font(.system(size: 13))
                            .foregroundStyle(AppColors.error)
                    }

                    PrimaryButton("更新心情", isDisabled: viewModel.selectedEmoji == nil || viewModel.isLoading) {
                        Task {
                            if await viewModel.updateMood() {
                                dismiss()
                            }
                        }
                    }
                }
                .padding(20)
            }
            .gradientBackground()
            .navigationTitle("心情同步")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
            .task { await viewModel.loadPartnerMood(partnerName: partnerName) }
        }
    }
}
