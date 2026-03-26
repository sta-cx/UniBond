import Foundation
import Observation

@MainActor
@Observable
class ProfileViewModel {
    var nickname = ""
    var isEditing = false
    var isLoading = false
    var errorMessage: String?
    var showDeleteConfirm = false

    private let apiClient: APIClient
    private let appState: AppState

    init(apiClient: APIClient, appState: AppState) {
        self.apiClient = apiClient
        self.appState = appState
        self.nickname = appState.currentUser?.nickname ?? ""
    }

    func updateNickname() async {
        guard !nickname.isEmpty else { return }
        isLoading = true
        errorMessage = nil
        do {
            let user: UserResponse = try await apiClient.request(
                .updateProfile(ProfileUpdateRequest(nickname: nickname, avatarUrl: nil, timezone: nil))
            )
            appState.authState = .authenticated(user)
            isEditing = false
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "更新失败"
        }
        isLoading = false
    }

    func logout() async {
        if let refreshToken = KeychainManager.shared.refreshToken {
            try? await apiClient.requestVoid(.logout(refreshToken))
        }
        appState.logout()
        AppSettings.shared.clearAll()
    }

    func deleteAccount() async {
        isLoading = true
        errorMessage = nil
        do {
            try await apiClient.requestVoid(.deleteAccount)
            appState.logout()
            AppSettings.shared.clearAll()
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "删除账号失败"
        }
        isLoading = false
    }

    var daysTogether: Int? {
        guard let couple = appState.currentCouple, let date = Date.fromISO8601(couple.bindAt) else {
            return nil
        }
        return Calendar.current.dateComponents([.day], from: date, to: Date()).day
    }
}
