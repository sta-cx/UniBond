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
                            Text(user.nickname ?? "用户")
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
        .onAppear {
            if !viewModel.isLoading {
                Task { await viewModel.loadData() }
            }
        }
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
        case .noQuiz:
            CardView {
                VStack(spacing: 12) {
                    Text("🎯").font(.system(size: 28))
                    Text("今日默契挑战").font(.system(size: 17, weight: .semibold))
                    Text("今日题目还未生成，请稍后再来")
                        .font(.system(size: 13))
                        .foregroundStyle(AppColors.textSecondary)
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
            VStack(spacing: 16) {
                HStack {
                    Image(systemName: "heart.fill")
                        .font(.system(size: 14))
                        .foregroundStyle(AppColors.primaryPink)
                    Text("心情同步")
                        .font(.system(size: 17, weight: .semibold))
                    Spacer()
                }

                HStack(spacing: 0) {
                    Spacer()
                    VStack(spacing: 8) {
                        ZStack {
                            Circle()
                                .fill(AppColors.bgLight)
                                .frame(width: 64, height: 64)
                            Text(viewModel.myMood?.emoji ?? "😊")
                                .font(.system(size: 32))
                        }
                        Text(viewModel.myMood != nil ? moodText(viewModel.myMood?.emoji) : "我心情好")
                            .font(.system(size: 13))
                            .foregroundStyle(AppColors.textSecondary)
                    }

                    Spacer()

                    VStack(spacing: 8) {
                        ZStack {
                            Circle()
                                .fill(AppColors.bgLightPink)
                                .frame(width: 64, height: 64)
                            Text(viewModel.partnerMood?.emoji ?? "❓")
                                .font(.system(size: 32))
                        }
                        Text(viewModel.partnerMood != nil ? moodText(viewModel.partnerMood?.emoji) : "TA心情好")
                            .font(.system(size: 13))
                            .foregroundStyle(AppColors.textSecondary)
                    }

                    Spacer()
                }

                PrimaryButton("更新心情") {
                    router.activeSheet = .moodPicker
                }
            }
            .padding(20)
        }
    }

    private func moodText(_ emoji: String?) -> String {
        guard let emoji else { return "" }
        switch emoji {
        case "😊", "😄", "😁": return "我心情好"
        case "😢", "😭": return "有点难过"
        case "😡", "😤": return "有点生气"
        case "🥰", "😍": return "超级开心"
        case "😴", "😪": return "有点困"
        default: return "我心情好"
        }
    }
}
