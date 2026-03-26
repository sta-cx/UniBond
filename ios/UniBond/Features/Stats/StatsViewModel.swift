import Foundation
import Observation

@MainActor
@Observable
class StatsViewModel {
    var weekly: WeeklyResponse?
    var overview: OverviewResponse?
    var achievements: [AchievementResponse] = []
    var isLoading = false
    var errorMessage: String?

    private let apiClient: APIClient

    init(apiClient: APIClient) {
        self.apiClient = apiClient
    }

    func loadData() async {
        isLoading = true
        errorMessage = nil
        do {
            async let weeklyResult: WeeklyResponse = apiClient.request(.weekly)
            async let overviewResult: OverviewResponse = apiClient.request(.overview)
            async let achievementsResult: [AchievementResponse] = apiClient.request(.achievements)
            weekly = try await weeklyResult
            overview = try await overviewResult
            achievements = try await achievementsResult
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "加载统计失败"
        }
        isLoading = false
    }
}
