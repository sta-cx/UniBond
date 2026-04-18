import SwiftUI
import Charts

struct StatsView: View {
    @State var viewModel: StatsViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                Text("默契统计")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(AppColors.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)

                if let weekly = viewModel.weekly {
                    CardView {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("本周趋势")
                                .font(.system(size: 17, weight: .semibold))
                            Chart(weekly.scores, id: \.date) { day in
                                BarMark(
                                    x: .value("日期", String(day.date.suffix(5))),
                                    y: .value("分数", day.score)
                                )
                                .foregroundStyle(AppColors.primaryGradient)
                                .cornerRadius(4)
                            }
                            .frame(height: 180)
                            .chartYScale(domain: 0...100)
                        }
                        .padding(20)
                    }
                }

                if let overview = viewModel.overview {
                    HStack(spacing: 12) {
                        StatCard(value: String(format: "%.0f", overview.avgScore), label: "平均默契分", color: AppColors.primaryPurple)
                        StatCard(value: "\(overview.streakDays)", label: "默契打卡", color: AppColors.success)
                        StatCard(value: "\(overview.totalQuizzes)", label: "累计答题", color: AppColors.primaryPink)
                    }
                }

                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("成就徽章")
                            .font(.system(size: 17, weight: .semibold))
                        Spacer()
                        if !viewModel.achievements.isEmpty {
                            let unlocked = viewModel.achievements.filter(\.unlocked).count
                            Text("\(unlocked)/\(viewModel.achievements.count)")
                                .font(.system(size: 13))
                                .foregroundStyle(AppColors.textSecondary)
                        }
                    }
                    if viewModel.achievements.isEmpty {
                        EmptyStateView(icon: "🏆", message: "继续答题解锁更多成就")
                    } else {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 16) {
                                ForEach(viewModel.achievements, id: \.type) { achievement in
                                    AchievementBadge(
                                        name: achievement.displayName,
                                        icon: achievementIcon(achievement.type),
                                        subtitle: achievement.unlocked ? "已解锁" : "未解锁",
                                        unlocked: achievement.unlocked
                                    )
                                }
                            }
                            .padding(.horizontal, 4)
                        }
                    }
                }

                if viewModel.isLoading {
                    LoadingView()
                }
                if let error = viewModel.errorMessage {
                    Text(error)
                        .font(.system(size: 13))
                        .foregroundStyle(AppColors.error)
                }
            }
            .padding(.horizontal, 20)
        }
        .gradientBackground()
        .task { await viewModel.loadData() }
        .refreshable { await viewModel.loadData() }
    }

    private func achievementIcon(_ type: String) -> String {
        switch type {
        case "FIRST_QUIZ":
            return "🎯"
        case "STREAK_7":
            return "🔥"
        case "STREAK_30":
            return "💎"
        case "PERFECT_SCORE":
            return "💯"
        case "QUIZ_50":
            return "📚"
        default:
            return "🏅"
        }
    }
}
