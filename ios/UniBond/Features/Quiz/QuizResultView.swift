import SwiftUI

struct QuizResultView: View {
    @Environment(AppRouter.self) private var router
    @State var viewModel: QuizViewModel
    let date: String

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                if viewModel.isLoading {
                    LoadingView()
                } else if let result = viewModel.result {
                    ZStack {
                        Circle()
                            .stroke(Color.gray.opacity(0.2), lineWidth: 8)
                            .frame(width: 120, height: 120)
                        Circle()
                            .trim(from: 0, to: Double(result.score) / 100.0)
                            .stroke(AppColors.primaryGradient, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                            .frame(width: 120, height: 120)
                            .rotationEffect(.degrees(-90))
                        VStack(spacing: 2) {
                            Text("\(result.score)%")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundStyle(AppColors.primaryPurple)
                            Text("默契分")
                                .font(.system(size: 13))
                                .foregroundStyle(AppColors.textSecondary)
                        }
                    }

                    CardView {
                        VStack(spacing: 0) {
                            HStack {
                                Text("题目").font(.system(size: 13, weight: .medium)).frame(maxWidth: .infinity, alignment: .leading)
                                Text("我").font(.system(size: 13, weight: .medium)).frame(width: 40)
                                Text("TA").font(.system(size: 13, weight: .medium)).frame(width: 40)
                                Text("").frame(width: 24)
                            }
                            .foregroundStyle(AppColors.textSecondary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)

                            Divider()

                            let myAnswers = decodeAnswers(result.myAnswers)
                            let partnerAnswers = decodeAnswers(result.partnerAnswers ?? "[]")
                            let labels = ["A", "B", "C", "D"]

                            ForEach(0..<myAnswers.count, id: \.self) { index in
                                let matched = index < partnerAnswers.count && myAnswers[index] == partnerAnswers[index]
                                HStack {
                                    Text("Q\(index + 1)")
                                        .font(.system(size: 15))
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                    Text(labels[safe: myAnswers[index]] ?? "?")
                                        .font(.system(size: 15))
                                        .frame(width: 40)
                                    Text(index < partnerAnswers.count ? (labels[safe: partnerAnswers[index]] ?? "?") : "-")
                                        .font(.system(size: 15))
                                        .frame(width: 40)
                                    Image(systemName: matched ? "checkmark.circle.fill" : "xmark.circle.fill")
                                        .foregroundStyle(matched ? AppColors.success : AppColors.error)
                                        .frame(width: 24)
                                }
                                .padding(.horizontal, 16)
                                .padding(.vertical, 10)
                                if index < myAnswers.count - 1 {
                                    Divider()
                                }
                            }
                        }
                    }

                    PrimaryButton("去看看 TA 的心情") {
                        router.activeSheet = .moodPicker
                    }
                }
            }
            .padding(20)
        }
        .gradientBackground()
        .navigationTitle("答题结果")
        .task { await viewModel.loadResult(date: date) }
    }

    private func decodeAnswers(_ json: String) -> [Int] {
        guard let data = json.data(using: .utf8) else { return [] }
        return (try? JSONDecoder().decode([Int].self, from: data)) ?? []
    }
}

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
