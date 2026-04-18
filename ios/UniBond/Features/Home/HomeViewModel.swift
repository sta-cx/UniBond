import Foundation
import Observation
import WidgetKit

enum QuizCardState {
    case unbound
    case noQuiz
    case available(QuizResponse)
    case answeredWaiting(date: String)
    case waitingReveal(date: String)
    case revealed(QuizResultResponse)
}

@MainActor
@Observable
class HomeViewModel {
    var quizCardState: QuizCardState = .unbound
    var overview: OverviewResponse?
    var myMood: MoodResponse?
    var partnerMood: MoodResponse?
    var isLoading = false
    var errorMessage: String?

    private let apiClient: APIClient
    private let appState: AppState
    private var pollingTask: Task<Void, Never>?

    init(apiClient: APIClient, appState: AppState) {
        self.apiClient = apiClient
        self.appState = appState
    }

    func loadData() async {
        guard appState.isBound else {
            quizCardState = .unbound
            return
        }

        isLoading = true
        errorMessage = nil
        do {
            async let overviewResult: OverviewResponse = apiClient.request(.overview)
            async let partnerMoodResult: MoodResponse? = try? apiClient.request(.partnerMood)

            overview = try await overviewResult
            partnerMood = await partnerMoodResult

            if let overview {
                AppSettings.shared.sharedTodayScore = overview.todayScore
                AppSettings.shared.sharedStreakDays = overview.streakDays
            }
            if let partnerMood {
                AppSettings.shared.sharedPartnerMoodEmoji = partnerMood.emoji
                AppSettings.shared.sharedPartnerMoodText = partnerMood.text
            }

            WidgetCenter.shared.reloadAllTimelines()
            await loadQuizState()
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "加载失败"
        }
        isLoading = false
    }

    private func loadQuizState() async {
        let today = Date().iso8601DateString
        do {
            let quiz: QuizResponse = try await apiClient.request(.quizToday)
            AppSettings.shared.sharedQuizType = quiz.quizType

            do {
                let result: QuizResultResponse = try await apiClient.request(.quizResult(date: today))
                if result.revealed {
                    quizCardState = .revealed(result)
                    AppSettings.shared.sharedQuizAnswered = true
                } else if result.partnerAnswers != nil {
                    quizCardState = .waitingReveal(date: today)
                    startPolling(date: today)
                } else {
                    quizCardState = .answeredWaiting(date: today)
                    startPolling(date: today)
                }
            } catch {
                quizCardState = .available(quiz)
                AppSettings.shared.sharedQuizAnswered = false
            }
        } catch {
            quizCardState = .noQuiz
        }
    }

    func startPolling(date: String) {
        stopPolling()
        pollingTask = Task {
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(30))
                guard !Task.isCancelled else { break }
                do {
                    let result: QuizResultResponse = try await apiClient.request(.quizResult(date: date))
                    if result.revealed {
                        quizCardState = .revealed(result)
                        AppSettings.shared.sharedQuizAnswered = true
                        WidgetCenter.shared.reloadAllTimelines()
                        break
                    }
                    if result.partnerAnswers != nil {
                        quizCardState = .waitingReveal(date: date)
                    }
                } catch {
                }
            }
        }
    }

    func stopPolling() {
        pollingTask?.cancel()
        pollingTask = nil
    }

    var greetingEmoji: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 6..<12:
            return "🌅"
        case 12..<18:
            return "☀️"
        default:
            return "🌙"
        }
    }

    var greetingText: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 6..<12:
            return "早上好"
        case 12..<18:
            return "下午好"
        default:
            return "晚上好"
        }
    }
}
