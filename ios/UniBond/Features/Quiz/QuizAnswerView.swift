import SwiftUI

struct QuizAnswerView: View {
    @Environment(AppRouter.self) private var router
    @State var viewModel: QuizViewModel

    private let labels = ["A", "B", "C", "D"]

    var body: some View {
        VStack(spacing: 20) {
            if viewModel.isLoading {
                Spacer()
                LoadingView()
                Spacer()
            } else if !viewModel.questions.isEmpty {
                HStack {
                    Text("问题 \(viewModel.currentQuestionIndex + 1)/\(viewModel.questions.count)")
                        .font(.system(size: 15, weight: .semibold))
                    Spacer()
                }
                ProgressView(value: viewModel.progress)
                    .tint(AppColors.primaryPurple)

                if viewModel.currentQuestionIndex < viewModel.questions.count {
                    let question = viewModel.questions[viewModel.currentQuestionIndex]

                    CardView {
                        VStack(spacing: 16) {
                            HStack {
                                Text("答案模式")
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundStyle(AppColors.primaryPurple)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 4)
                                    .background(AppColors.primaryPurple.opacity(0.1))
                                    .clipShape(Capsule())
                                Spacer()
                            }

                            Text(question.content)
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundStyle(AppColors.textPrimary)
                                .frame(maxWidth: .infinity, alignment: .leading)

                            VStack(spacing: 10) {
                                ForEach(Array(question.options.enumerated()), id: \.offset) { index, option in
                                    QuizOptionRow(
                                        label: labels[index],
                                        text: option,
                                        isSelected: viewModel.selectedAnswers[viewModel.currentQuestionIndex] == index
                                    ) {
                                        viewModel.selectAnswer(questionIndex: viewModel.currentQuestionIndex, optionIndex: index)
                                    }
                                }
                            }
                        }
                        .padding(24)
                    }

                    Spacer()

                    HStack(spacing: 12) {
                        if viewModel.currentQuestionIndex > 0 {
                            SecondaryButton("上一题") {
                                viewModel.currentQuestionIndex -= 1
                            }
                        }
                        if viewModel.currentQuestionIndex < viewModel.questions.count - 1 {
                            PrimaryButton("下一题", isDisabled: viewModel.selectedAnswers[viewModel.currentQuestionIndex] == nil) {
                                viewModel.currentQuestionIndex += 1
                            }
                        } else {
                            PrimaryButton("提交答案", isDisabled: !viewModel.allAnswered || viewModel.isSubmitting) {
                                Task {
                                    if await viewModel.submitAnswers() {
                                        router.homePath.append(AppRoute.quizWaiting)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if let error = viewModel.errorMessage {
                Spacer()
                VStack(spacing: 16) {
                    Text("😵").font(.system(size: 48))
                    Text(error)
                        .font(.system(size: 15))
                        .foregroundStyle(AppColors.textSecondary)
                        .multilineTextAlignment(.center)
                    PrimaryButton("重试") {
                        Task { await viewModel.loadQuiz() }
                    }
                }
                .padding(.horizontal, 20)
                Spacer()
            }
        }
        .padding(20)
        .gradientBackground()
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.loadQuiz() }
    }
}
