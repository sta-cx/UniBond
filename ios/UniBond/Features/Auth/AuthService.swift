import AuthenticationServices
import Foundation

@MainActor
final class AuthService: NSObject, ASAuthorizationControllerDelegate {
    private var continuation: CheckedContinuation<(identityToken: String, nickname: String?), Error>?

    func signInWithApple() async throws -> (identityToken: String, nickname: String?) {
        try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation
            let provider = ASAuthorizationAppleIDProvider()
            let request = provider.createRequest()
            request.requestedScopes = [.fullName, .email]
            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.performRequests()
        }
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard
            let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
            let tokenData = credential.identityToken,
            let token = String(data: tokenData, encoding: .utf8)
        else {
            continuation?.resume(throwing: APIError.serverError("Apple 登录凭证无效"))
            continuation = nil
            return
        }

        continuation?.resume(returning: (token, credential.fullName?.givenName))
        continuation = nil
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        continuation?.resume(throwing: error)
        continuation = nil
    }
}
