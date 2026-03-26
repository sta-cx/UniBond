import Foundation
import Observation

@MainActor
@Observable
class CoupleViewModel {
    var partnerCode = ""
    var isLoading = false
    var errorMessage: String?

    private let apiClient: APIClient
    private let appState: AppState

    init(apiClient: APIClient, appState: AppState) {
        self.apiClient = apiClient
        self.appState = appState
    }

    var myInviteCode: String {
        appState.currentUser?.inviteCode ?? ""
    }

    func bind() async -> Bool {
        guard partnerCode.count == 6 else { return false }
        isLoading = true
        errorMessage = nil
        do {
            let couple: CoupleResponse = try await apiClient.request(.bind(BindRequest(inviteCode: partnerCode)))
            appState.coupleState = .bound(couple)
            isLoading = false
            return true
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "绑定失败"
            isLoading = false
            return false
        }
    }

    func unbind() async -> Bool {
        isLoading = true
        errorMessage = nil
        do {
            try await apiClient.requestVoid(.unbind)
            appState.coupleState = .unbound
            await MoodLiveActivityManager.endAll()
            isLoading = false
            return true
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "解绑失败"
            isLoading = false
            return false
        }
    }
}
