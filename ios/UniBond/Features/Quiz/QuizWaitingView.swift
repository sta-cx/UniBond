import SwiftUI

struct QuizWaitingView: View {
    @Environment(AppState.self) private var appState
    @Environment(AppRouter.self) private var router

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 64))
                .foregroundStyle(AppColors.success)
            Text("答题完成！")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(AppColors.textPrimary)

            CardView {
                VStack(spacing: 12) {
                    HStack {
                        Text("我").font(.system(size: 15, weight: .medium))
                        Spacer()
                        Text("已完成 ✅")
                            .foregroundStyle(AppColors.success)
                            .font(.system(size: 13))
                    }
                    Divider()
                    HStack {
                        Text(appState.currentCouple?.partnerNickname ?? "TA")
                            .font(.system(size: 15, weight: .medium))
                        Spacer()
                        HStack(spacing: 4) {
                            ProgressView().controlSize(.small)
                            Text("尚未作答")
                                .font(.system(size: 13))
                                .foregroundStyle(AppColors.textSecondary)
                        }
                    }
                }
                .padding(20)
            }

            Text("对方完成后即可查看结果")
                .font(.system(size: 13))
                .foregroundStyle(AppColors.textSecondary)

            Spacer()

            SecondaryButton("返回首页") {
                router.homePath = NavigationPath()
            }
        }
        .padding(20)
        .gradientBackground()
        .navigationBarBackButtonHidden()
    }
}
