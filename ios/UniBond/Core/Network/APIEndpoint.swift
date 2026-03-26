import Foundation

enum HTTPMethod: String {
    case GET, POST, PUT, DELETE
}

struct APIEndpoint {
    let path: String
    let method: HTTPMethod
    let body: Encodable?
    let requiresAuth: Bool

    init(path: String, method: HTTPMethod, body: Encodable? = nil, requiresAuth: Bool = true) {
        self.path = path
        self.method = method
        self.body = body
        self.requiresAuth = requiresAuth
    }

    // MARK: - Auth (no auth required)
    static func emailSend(_ request: EmailSendRequest) -> APIEndpoint {
        .init(path: "/api/v1/auth/email/send", method: .POST, body: request, requiresAuth: false)
    }

    static func emailLogin(_ request: EmailLoginRequest) -> APIEndpoint {
        .init(path: "/api/v1/auth/email/login", method: .POST, body: request, requiresAuth: false)
    }

    static func appleLogin(_ request: AppleLoginRequest) -> APIEndpoint {
        .init(path: "/api/v1/auth/apple", method: .POST, body: request, requiresAuth: false)
    }

    static func refreshToken(_ token: String) -> APIEndpoint {
        .init(path: "/api/v1/auth/refresh", method: .POST, body: ["refreshToken": token], requiresAuth: false)
    }

    static func logout(_ refreshToken: String) -> APIEndpoint {
        .init(path: "/api/v1/auth/logout", method: .POST, body: ["refreshToken": refreshToken], requiresAuth: false)
    }

    // MARK: - User
    static var me: APIEndpoint {
        .init(path: "/api/v1/user/me", method: .GET)
    }

    static func updateProfile(_ request: ProfileUpdateRequest) -> APIEndpoint {
        .init(path: "/api/v1/user/profile", method: .PUT, body: request)
    }

    static func registerDeviceToken(_ token: String) -> APIEndpoint {
        .init(path: "/api/v1/user/device-token", method: .POST, body: ["deviceToken": token])
    }

    static var deleteAccount: APIEndpoint {
        .init(path: "/api/v1/user/account", method: .DELETE)
    }

    // MARK: - Couple
    static var coupleInfo: APIEndpoint {
        .init(path: "/api/v1/couple/info", method: .GET)
    }

    static func bind(_ request: BindRequest) -> APIEndpoint {
        .init(path: "/api/v1/couple/bind", method: .POST, body: request)
    }

    static var unbind: APIEndpoint {
        .init(path: "/api/v1/couple/unbind", method: .DELETE)
    }

    // MARK: - Quiz
    static var quizToday: APIEndpoint {
        .init(path: "/api/v1/quiz/today", method: .GET)
    }

    static func submitAnswer(_ request: AnswerRequest) -> APIEndpoint {
        .init(path: "/api/v1/quiz/answer", method: .POST, body: request)
    }

    static func quizResult(date: String) -> APIEndpoint {
        let allowed = CharacterSet.urlPathAllowed.subtracting(CharacterSet(charactersIn: "/"))
        let encodedDate = date.addingPercentEncoding(withAllowedCharacters: allowed) ?? date
        return .init(path: "/api/v1/quiz/result/\(encodedDate)", method: .GET)
    }

    // MARK: - Mood
    static func updateMood(_ request: MoodUpdateRequest) -> APIEndpoint {
        .init(path: "/api/v1/mood", method: .POST, body: request)
    }

    static var partnerMood: APIEndpoint {
        .init(path: "/api/v1/mood/partner", method: .GET)
    }

    // MARK: - Stats
    static var overview: APIEndpoint {
        .init(path: "/api/v1/stats/overview", method: .GET)
    }

    static var achievements: APIEndpoint {
        .init(path: "/api/v1/stats/achievements", method: .GET)
    }

    static var weekly: APIEndpoint {
        .init(path: "/api/v1/stats/weekly", method: .GET)
    }
}
