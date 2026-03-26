import SwiftUI

struct HomeView: View {
    @Environment(AppState.self) private var appState
    @Environment(AppRouter.self) private var router
    @State var viewModel: HomeViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(viewModel.greetingEmoji) \(viewModel.greetingText)")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundStyle(AppColors.textPrimary)
                        if let user = appState.currentUser {
                            Text(user.nickname)
                                .font(.system(size: 15))
                                .foregroundStyle(AppColors.textSecondary)
                        }
                    }
                    Spacer()
                }
                .padding(.top, 8)

                if !appState.isOnline {
                    OfflineBanner()
                }

                quizCard

                if appState.isBound {
                    moodSection
                }

                if let overview = viewModel.overview {
                    HStack(spacing: 12) {
                        StatCard(value: "\(overview.todayScore)", label: "今日默契", color: AppColors.primaryPurple)
                        StatCard(value: "\(overview.streakDays)天", label: "连续打卡", color: AppColors.success)
                        StatCard(value: "\(overview.totalQuizzes)", label: "累计答题", color: AppColors.primaryPink)
                    }
                }
            }
            .padding(.horizontal, 20)
        }
        .gradientBackground()
        .task { await viewModel.loadData() }
        .onDisappear { viewModel.stopPolling() }
        .refreshable { await viewModel.loadData() }
    }

    @ViewBuilder
    private var quizCard: some View {
        switch viewModel.quizCardState {
        case .unbound:
            CardView {
                VStack(spacing: 16) {
                    EmptyStateView(icon: "💑", message: "绑定伴侣后开始每日默契挑战")
                    PrimaryButton("去绑定", icon: "link") {
                        router.navigateHome(to: .bindPartner)
                    }
                }
                .padding(20)
            }
        case .available:
            CardView {
                VStack(spacing: 12) {
                    HStack {
                        Text("🎯").font(.system(size: 28))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("今日默契挑战").font(.system(size: 17, weight: .semibold))
                            Text("5 道趣味问题，看看你们有多默契")
                                .font(.system(size: 13))
                                .foregroundStyle(AppColors.textSecondary)
                        }
                        Spacer()
                    }
                    PrimaryButton("开始答题") {
                        router.navigateHome(to: .quizAnswer)
                    }
                }
                .padding(20)
            }
        case .answeredWaiting:
            CardView {
                VStack(spacing: 12) {
                    Text("✅").font(.system(size: 36))
                    Text("你已完成答题").font(.system(size: 17, weight: .semibold))
                    Text("等待 TA 完成答题...")
                        .font(.system(size: 15))
                        .foregroundStyle(AppColors.textSecondary)
                    ProgressView().tint(AppColors.primaryPurple)
                }
                .padding(20)
            }
        case .waitingReveal(let date):
            CardView {
                VStack(spacing: 12) {
                    Text("🎉").font(.system(size: 36))
                    Text("双方已完成答题").font(.system(size: 17, weight: .semibold))
                    Text("等待系统揭晓结果...")
                        .font(.system(size: 15))
                        .foregroundStyle(AppColors.textSecondary)
                    PrimaryButton("查看结果") {
                        router.navigateHome(to: .quizResult(date: date))
                    }
                }
                .padding(20)
            }
        case .revealed(let result):
            CardView {
                VStack(spacing: 12) {
                    Text("💕").font(.system(size: 36))
                    Text("\(result.score)%")
                        .font(.system(size: 40, weight: .bold))
                        .foregroundStyle(AppColors.primaryPurple)
                    Text("今日默契分")
                        .font(.system(size: 15))
                        .foregroundStyle(AppColors.textSecondary)
                    PrimaryButton("查看详情") {
                        router.navigateHome(to: .quizResult(date: Date().iso8601DateString))
                    }
                }
                .padding(20)
            }
        }
    }

    private var moodSection: some View {
        CardView {
            VStack(spacing: 12) {
                HStack {
                    Text("心情同步").font(.system(size: 17, weight: .semibold))
                    Spacer()
                    Button("更新心情") {
                        router.activeSheet = .moodPicker
                    }
                    .font(.system(size: 13))
                    .foregroundStyle(AppColors.primaryPurple)
                }
                HStack(spacing: 24) {
                    VStack(spacing: 4) {
                        Text(viewModel.myMood?.emoji ?? "😊").font(.system(size: 32))
                        Text("我").font(.system(size: 11)).foregroundStyle(AppColors.textSecondary)
                    }
                    VStack(spacing: 4) {
                        Text(viewModel.partnerMood?.emoji ?? "❓").font(.system(size: 32))
                        Text("TA").font(.system(size: 11)).foregroundStyle(AppColors.textSecondary)
                    }
                    Spacer()
                    if let text = viewModel.partnerMood?.text, !text.isEmpty {
                        Text(text)
                            .font(.system(size: 13))
                            .foregroundStyle(AppColors.textSecondary)
                            .lineLimit(2)
                    }
                }
            }
            .padding(20)
        }
    }
}
