import Foundation
import Observation

@MainActor
@Observable
class CoupleViewModel {
    var partnerCode = ""
    var isLoading = false
    var errorMessage: String?
    var bindSucceeded = false

    private let apiClient: APIClient
    private let appState: AppState
    private var pollTask: Task<Void, Never>?

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

    // MARK: - Bind Polling

    func startBindPolling() {
        guard pollTask == nil else { return }
        pollTask = Task {
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(5))
                guard !Task.isCancelled else { return }
                if await checkBindStatus() {
                    bindSucceeded = true
                    return
                }
            }
        }
    }

    func stopBindPolling() {
        pollTask?.cancel()
        pollTask = nil
    }

    private func checkBindStatus() async -> Bool {
        do {
            let user: UserResponse = try await apiClient.request(.me)
            if user.partnerId != nil {
                let couple: CoupleResponse = try await apiClient.request(.coupleInfo)
                appState.authState = .authenticated(user)
                appState.coupleState = .bound(couple)
                return true
            }
        } catch {
            // Silently ignore polling errors
        }
        return false
    }
}
