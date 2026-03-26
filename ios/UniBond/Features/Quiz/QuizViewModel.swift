import Foundation
import Observation
import WidgetKit

@MainActor
@Observable
class QuizViewModel {
    var questions: [QuizQuestion] = []
    var selectedAnswers: [Int: Int] = [:]
    var currentQuestionIndex = 0
    var isLoading = false
    var isSubmitting = false
    var errorMessage: String?
    var quizId: Int64 = 0
    var result: QuizResultResponse?

    private let apiClient: APIClient

    init(apiClient: APIClient) {
        self.apiClient = apiClient
    }

    var allAnswered: Bool {
        questions.count > 0 && selectedAnswers.count == questions.count
    }

    var progress: Double {
        guard questions.count > 0 else { return 0 }
        return Double(selectedAnswers.count) / Double(questions.count)
    }

    func selectAnswer(questionIndex: Int, optionIndex: Int) {
        selectedAnswers[questionIndex] = optionIndex
    }

    func loadQuiz() async {
        isLoading = true
        errorMessage = nil
        do {
            let quiz: QuizResponse = try await apiClient.request(.quizToday)
            quizId = quiz.id
            if let data = quiz.questions.data(using: .utf8) {
                questions = try JSONDecoder().decode([QuizQuestion].self, from: data)
            }
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "加载题目失败"
        }
        isLoading = false
    }

    func submitAnswers() async -> Bool {
        guard allAnswered else { return false }
        isSubmitting = true
        errorMessage = nil
        do {
            let answersArray = (0..<questions.count).map { selectedAnswers[$0] ?? 0 }
            let data = try JSONEncoder().encode(answersArray)
            let answersJSON = String(data: data, encoding: .utf8) ?? "[]"
            try await apiClient.requestVoid(
                .submitAnswer(AnswerRequest(quizId: quizId, answers: answersJSON, partnerGuess: nil))
            )
            AppSettings.shared.sharedQuizAnswered = true
            WidgetCenter.shared.reloadAllTimelines()
            isSubmitting = false
            return true
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "提交失败"
            isSubmitting = false
            return false
        }
    }

    func loadResult(date: String) async {
        isLoading = true
        errorMessage = nil
        do {
            result = try await apiClient.request(.quizResult(date: date))
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "加载结果失败"
        }
        isLoading = false
    }
}
