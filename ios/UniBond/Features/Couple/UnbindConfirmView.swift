import SwiftUI

struct UnbindConfirmView: View {
    @Environment(\.dismiss) private var dismiss
    @State var viewModel: CoupleViewModel

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Text("💔").font(.system(size: 56))
            Text("确认解绑？")
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(AppColors.textPrimary)
            Text("解绑后，你们的答题记录和统计数据将被清除，此操作不可恢复。")
                .font(.system(size: 15))
                .foregroundStyle(AppColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 20)

            if let error = viewModel.errorMessage {
                Text(error)
                    .font(.system(size: 13))
                    .foregroundStyle(AppColors.error)
            }

            Spacer()

            VStack(spacing: 12) {
                Button {
                    Task {
                        if await viewModel.unbind() {
                            dismiss()
                        }
                    }
                } label: {
                    Text("确认解绑")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(AppColors.error)
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                }
                .disabled(viewModel.isLoading)

                SecondaryButton("再想想") {
                    dismiss()
                }
            }
        }
        .padding(32)
        .gradientBackground()
    }
}
