import Foundation

enum AuthInterceptor {
    static func attachToken(to request: URLRequest) -> URLRequest {
        var request = request
        if let token = KeychainManager.shared.accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        return request
    }

    static func storeTokens(access: String, refresh: String) {
        KeychainManager.shared.saveTokens(access: access, refresh: refresh)
    }

    static func clearTokens() {
        KeychainManager.shared.deleteTokens()
    }
}
