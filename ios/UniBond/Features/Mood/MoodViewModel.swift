import Foundation
import Observation
import WidgetKit

@MainActor
@Observable
class MoodViewModel {
    var selectedEmoji: String?
    var moodText = ""
    var isLoading = false
    var errorMessage: String?
    var myMood: MoodResponse?
    var partnerMood: MoodResponse?

    private let apiClient: APIClient

    let emojis = ["😊", "🥰", "😴", "😢", "😤", "🤗", "😎", "🤔", "😋"]

    init(apiClient: APIClient) {
        self.apiClient = apiClient
    }

    func updateMood() async -> Bool {
        guard let emoji = selectedEmoji else { return false }
        isLoading = true
        errorMessage = nil
        do {
            let response: MoodResponse = try await apiClient.request(
                .updateMood(MoodUpdateRequest(emoji: emoji, text: moodText.isEmpty ? nil : moodText))
            )
            myMood = response
            WidgetCenter.shared.reloadAllTimelines()
            isLoading = false
            return true
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "更新心情失败"
            isLoading = false
            return false
        }
    }

    func loadPartnerMood(partnerName: String = "TA") async {
        do {
            partnerMood = try await apiClient.request(.partnerMood)
            if let partnerMood {
                AppSettings.shared.sharedPartnerMoodEmoji = partnerMood.emoji
                AppSettings.shared.sharedPartnerMoodText = partnerMood.text
                await MoodLiveActivityManager.startOrUpdate(
                    partnerName: partnerName,
                    emoji: partnerMood.emoji,
                    text: partnerMood.text,
                    updatedAt: partnerMood.updatedAt
                )
            }
        } catch {
        }
    }
}
