import Foundation
import Observation

enum LoginStep: Equatable {
    case initial
    case emailInput
    case codeInput
}

@MainActor
@Observable
class AuthViewModel {
    var loginStep: LoginStep = .initial
    var email = ""
    var code = ""
    var isLoading = false
    var errorMessage: String?
    var countdown = 0

    let apiClient: APIClient
    let appState: AppState
    private var countdownTimer: Timer?

    var isEmailValid: Bool {
        email.contains("@") && email.contains(".")
    }

    var isCodeValid: Bool {
        code.count == 6
    }

    init(apiClient: APIClient, appState: AppState) {
        self.apiClient = apiClient
        self.appState = appState
    }

    deinit {
        countdownTimer?.invalidate()
    }

    func sendCode() async {
        guard isEmailValid else { return }
        isLoading = true
        errorMessage = nil
        do {
            try await apiClient.requestVoid(.emailSend(EmailSendRequest(email: email)))
            loginStep = .codeInput
            startCountdown()
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = "发送失败，请重试"
        }
        isLoading = false
    }

    func loginWithEmail() async {
        guard isCodeValid else { return }
        isLoading = true
        errorMessage = nil
        do {
            let response: AuthResponse = try await apiClient.request(
                .emailLogin(EmailLoginRequest(email: email, code: code, timezone: TimeZone.current.identifier))
            )
            await handleLoginSuccess(response)
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = "登录失败，请重试"
        }
        isLoading = false
    }

    func loginWithApple(identityToken: String, nickname: String?) async {
        isLoading = true
        errorMessage = nil
        do {
            let response: AuthResponse = try await apiClient.request(
                .appleLogin(
                    AppleLoginRequest(
                        identityToken: identityToken,
                        nickname: nickname,
                        timezone: TimeZone.current.identifier
                    )
                )
            )
            await handleLoginSuccess(response)
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = "Apple 登录失败，请重试"
        }
        isLoading = false
    }

    private func handleLoginSuccess(_ response: AuthResponse) async {
        AuthInterceptor.storeTokens(access: response.accessToken, refresh: response.refreshToken)
        AppSettings.shared.cachedUserId = response.userId
        AppSettings.shared.lastTimezone = TimeZone.current.identifier

        do {
            let user: UserResponse = try await apiClient.request(.me)
            appState.authState = .authenticated(user)
            if user.partnerId != nil {
                let couple: CoupleResponse = try await apiClient.request(.coupleInfo)
                appState.coupleState = .bound(couple)
            } else {
                appState.coupleState = .unbound
            }
        } catch {
            appState.authState = .unauthenticated
            appState.coupleState = .unbound
            AuthInterceptor.clearTokens()
        }
    }

    private func startCountdown() {
        countdown = 60
        countdownTimer?.invalidate()
        countdownTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] timer in
            guard let self else {
                timer.invalidate()
                return
            }
            if countdown > 0 {
                countdown -= 1
            } else {
                timer.invalidate()
            }
        }
    }
}
