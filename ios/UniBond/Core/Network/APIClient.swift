import Foundation

actor APIClient {
    let baseURL: String
    private let session: URLSession
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder
    private var isRefreshing = false
    private var refreshContinuations: [CheckedContinuation<Void, Error>] = []

    init(baseURL: String, session: URLSession? = nil) {
        self.baseURL = baseURL
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        self.session = session ?? URLSession(configuration: config)
        self.encoder = JSONEncoder()
        self.decoder = JSONDecoder()
    }

    func request<T: Codable>(_ endpoint: APIEndpoint) async throws -> T {
        let (data, _) = try await performRequest(endpoint)
        do {
            let wrapper = try decoder.decode(ApiResponse<T>.self, from: data)
            return wrapper.data
        } catch {
            throw APIError.decodingError
        }
    }

    func requestVoid(_ endpoint: APIEndpoint) async throws {
        _ = try await performRequest(endpoint)
    }

    private func performRequest(_ endpoint: APIEndpoint) async throws -> (Data, HTTPURLResponse) {
        var urlRequest = try buildRequest(for: endpoint)
        if endpoint.requiresAuth {
            urlRequest = AuthInterceptor.attachToken(to: urlRequest)
        }

        let (data, response) = try await session.data(for: urlRequest)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.networkError
        }

        if httpResponse.statusCode == 401 && endpoint.requiresAuth {
            try await refreshTokenIfNeeded()
            var retryRequest = try buildRequest(for: endpoint)
            retryRequest = AuthInterceptor.attachToken(to: retryRequest)
            let (retryData, retryResponse) = try await session.data(for: retryRequest)
            guard let retryHTTPResponse = retryResponse as? HTTPURLResponse else {
                throw APIError.networkError
            }
            if retryHTTPResponse.statusCode >= 400 {
                throw APIError.from(statusCode: retryHTTPResponse.statusCode, body: retryData)
            }
            return (retryData, retryHTTPResponse)
        }

        if httpResponse.statusCode >= 400 {
            throw APIError.from(statusCode: httpResponse.statusCode, body: data)
        }

        return (data, httpResponse)
    }

    private func refreshTokenIfNeeded() async throws {
        if isRefreshing {
            try await withCheckedThrowingContinuation { continuation in
                refreshContinuations.append(continuation)
            }
            return
        }

        isRefreshing = true
        var refreshError: Error?

        do {
            guard let refreshToken = KeychainManager.shared.refreshToken else {
                KeychainManager.shared.deleteTokens()
                throw APIError.unauthorized
            }

            let refreshRequest = try buildRequest(for: .refreshToken(refreshToken))
            let (data, response) = try await session.data(for: refreshRequest)
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                KeychainManager.shared.deleteTokens()
                throw APIError.unauthorized
            }

            let authResponse = try decoder.decode(ApiResponse<AuthResponse>.self, from: data)
            AuthInterceptor.storeTokens(access: authResponse.data.accessToken, refresh: authResponse.data.refreshToken)

            // Resume waiting continuations with success
            let continuations = refreshContinuations
            refreshContinuations = []
            for continuation in continuations {
                continuation.resume()
            }
        } catch {
            refreshError = error
            // Resume waiting continuations with error
            let continuations = refreshContinuations
            refreshContinuations = []
            for continuation in continuations {
                continuation.resume(throwing: error)
            }
        }

        isRefreshing = false
        if let error = refreshError {
            throw error
        }
    }

    func buildRequest(for endpoint: APIEndpoint) throws -> URLRequest {
        guard let url = URL(string: baseURL + endpoint.path) else {
            throw APIError.networkError
        }

        var request = URLRequest(url: url)
        request.httpMethod = endpoint.method.rawValue
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        if let body = endpoint.body {
            request.httpBody = try encoder.encode(AnyEncodable(body))
        }

        return request
    }
}

struct AnyEncodable: Encodable {
    private let encodeClosure: (Encoder) throws -> Void

    init(_ wrapped: Encodable) {
        encodeClosure = wrapped.encode
    }

    func encode(to encoder: Encoder) throws {
        try encodeClosure(encoder)
    }
}
