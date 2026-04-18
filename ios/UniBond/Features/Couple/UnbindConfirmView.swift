import SwiftUI

struct UnbindConfirmView: View {
    @Environment(\.dismiss) private var dismiss
    @State var viewModel: CoupleViewModel

    var body: some View {
        ZStack {
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .onTapGesture { dismiss() }

            VStack(spacing: 20) {
                Text("💔").font(.system(size: 40))

                Text("确认解绑？")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(AppColors.textPrimary)

                Text("解绑后，你们的答题记录和统计数据将被清除，此操作不可恢复。")
                    .font(.system(size: 14))
                    .foregroundStyle(AppColors.textSecondary)
                    .multilineTextAlignment(.center)

                if let error = viewModel.errorMessage {
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundStyle(AppColors.error)
                }

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
                            .background(AppColors.primaryPink)
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                    }
                    .disabled(viewModel.isLoading)

                    Button {
                        dismiss()
                    } label: {
                        Text("再想想")
                            .font(.system(size: 15))
                            .foregroundStyle(AppColors.textSecondary)
                    }
                }
            }
            .padding(28)
            .background(.white)
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .shadow(color: .black.opacity(0.15), radius: 20, y: 10)
            .padding(.horizontal, 40)
        }
    }
}
