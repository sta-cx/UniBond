import Foundation

enum APIError: Error, LocalizedError {
    case unauthorized
    case forbidden
    case coupleNotBound
    case quizAlreadyAnswered
    case rateLimited
    case notFound
    case badRequest(String)
    case serverError(String)
    case networkError
    case decodingError

    var errorDescription: String? {
        switch self {
        case .unauthorized: return "登录已过期，请重新登录"
        case .forbidden: return "没有权限执行此操作"
        case .coupleNotBound: return "请先绑定伴侣"
        case .quizAlreadyAnswered: return "今日已答题"
        case .rateLimited: return "操作太频繁，请稍后再试"
        case .notFound: return "请求的资源不存在"
        case .badRequest(let msg): return msg
        case .serverError(let msg): return msg
        case .networkError: return "网络连接失败，请检查网络"
        case .decodingError: return "数据解析失败"
        }
    }

    static func from(statusCode: Int, body: Data?) -> APIError {
        let errorResponse = body.flatMap { try? JSONDecoder().decode(ApiErrorResponse.self, from: $0) }
        if let code = errorResponse?.code {
            switch code {
            case "COUPLE_NOT_BOUND": return .coupleNotBound
            case "QUIZ_ALREADY_ANSWERED": return .quizAlreadyAnswered
            default: break
            }
        }

        switch statusCode {
        case 401:
            return .unauthorized
        case 403:
            return .forbidden
        case 404:
            return .notFound
        case 429:
            return .rateLimited
        case 400..<500:
            return .badRequest(errorResponse?.message ?? "请求错误 (\(statusCode))")
        default:
            return .serverError(errorResponse?.message ?? "服务器错误 (\(statusCode))")
        }
    }
}
