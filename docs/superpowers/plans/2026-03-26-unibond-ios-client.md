# UniBond iOS Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete UniBond iOS client in SwiftUI, covering all 17 functional screens, Widget, and Live Activity, connecting to the existing Spring Boot backend.

**Architecture:** Modular feature-based structure with `@Observable` ViewModels, `actor`-based APIClient with JWT auto-refresh, and SwiftUI Navigation. Each feature module contains its own Views, ViewModels, and Service calls. Shared UI components ensure visual consistency across the app.

**Tech Stack:** Swift 5.9+, SwiftUI, iOS 17+, URLSession async/await, Keychain, UserDefaults, WidgetKit, ActivityKit, Swift Charts, APNs

**Spec:** `docs/superpowers/specs/2026-03-26-unibond-ios-client-design.md`

**Backend API Base:** All endpoints prefixed with `/api/v1`, responses wrapped in `ApiResponse<T>` with `data` field.

---

## File Map

### Core/Network/
| File | Responsibility |
|------|---------------|
| `APIClient.swift` | Actor-based HTTP client with request/decode/retry |
| `APIEndpoint.swift` | Enum defining all backend endpoints (path, method, body) |
| `AuthInterceptor.swift` | JWT token attach, 401 auto-refresh, token rotation |
| `APIError.swift` | Error enum mapping backend error codes |
| `APIModels.swift` | All Codable request/response structs |

### Core/Storage/
| File | Responsibility |
|------|---------------|
| `KeychainManager.swift` | Save/read/delete JWT tokens in Keychain |
| `AppSettings.swift` | UserDefaults wrapper for preferences + App Group shared data |

### Core/Extensions/
| File | Responsibility |
|------|---------------|
| `Date+Ext.swift` | Date formatting helpers |
| `Color+Ext.swift` | Hex color initializer + app color constants |

### Navigation/
| File | Responsibility |
|------|---------------|
| `AppState.swift` | Global observable state: auth + couple + network status |
| `AppRouter.swift` | Navigation path management, push notification routing |

### SharedUI/
| File | Responsibility |
|------|---------------|
| `GradientBackground.swift` | Pink-purple gradient background modifier |
| `CardView.swift` | White translucent rounded card container |
| `PrimaryButton.swift` | Gradient primary button (purple→pink) |
| `SecondaryButton.swift` | White bordered secondary button |
| `TabBarView.swift` | Custom 3-tab bottom navigation |
| `StatCard.swift` | Numeric stat display card |
| `EmojiGrid.swift` | 3x3 emoji selection grid |
| `QuizOptionRow.swift` | Quiz answer option row (A/B/C/D) |
| `AchievementBadge.swift` | Achievement badge (locked/unlocked) |
| `LoadingView.swift` | Loading spinner overlay |
| `OfflineBanner.swift` | Network status banner |
| `EmptyStateView.swift` | Generic empty state with illustration text |

### Features/Auth/
| File | Responsibility |
|------|---------------|
| `LoginView.swift` | Login screen: Apple Sign-in + email flow |
| `AuthViewModel.swift` | Login state, email countdown, API calls |
| `AuthService.swift` | Apple Sign-in ASAuthorizationController wrapper |

### Features/Home/
| File | Responsibility |
|------|---------------|
| `HomeView.swift` | Home screen with multi-state quiz card + mood + stats |
| `HomeViewModel.swift` | Load overview, quiz status, mood data, polling |

### Features/Quiz/
| File | Responsibility |
|------|---------------|
| `QuizAnswerView.swift` | 5-question answer flow with progress |
| `QuizResultView.swift` | Score + answer comparison display |
| `QuizWaitingView.swift` | Waiting for partner completion screen |
| `QuizViewModel.swift` | Quiz state machine, answer submission, polling |

### Features/Mood/
| File | Responsibility |
|------|---------------|
| `MoodPickerView.swift` | Emoji grid + text input sheet |
| `MoodViewModel.swift` | Mood update, partner mood fetch |

### Features/Stats/
| File | Responsibility |
|------|---------------|
| `StatsView.swift` | Weekly chart + stat cards + achievement grid |
| `StatsViewModel.swift` | Weekly data, achievements fetch |

### Features/Couple/
| File | Responsibility |
|------|---------------|
| `BindPartnerView.swift` | Invite code display/input, share/bind |
| `UnbindConfirmView.swift` | Unbind confirmation sheet |
| `CoupleViewModel.swift` | Bind/unbind API calls |

### Features/Profile/
| File | Responsibility |
|------|---------------|
| `ProfileView.swift` | Profile info + settings list (bound/unbound states) |
| `ProfileViewModel.swift` | Profile edit, logout, account deletion |

### App Entry/
| File | Responsibility |
|------|---------------|
| `UniBondApp.swift` | App entry, scene setup, push registration |

### Widget Extension/
| File | Responsibility |
|------|---------------|
| `UniBondWidget/WidgetSmall.swift` | Small widget view |
| `UniBondWidget/WidgetMedium.swift` | Medium widget view |
| `UniBondWidget/WidgetDataProvider.swift` | Timeline provider + shared data |
| `UniBondWidget/UniBondWidgetBundle.swift` | Widget bundle entry |

### Live Activity Extension/
| File | Responsibility |
|------|---------------|
| `UniBondLiveActivity/MoodLiveActivity.swift` | Live Activity UI + attributes |

---

## Task 1: Xcode Project Scaffolding

**Files:**
- Create: Xcode project `UniBond` with all directory structure
- Create: Widget extension target `UniBondWidget`
- Create: App Group capability

- [ ] **Step 1: Create Xcode project**

Create a new SwiftUI iOS App project named `UniBond`:
- Bundle ID: `com.unibond.app`
- Minimum deployment: iOS 17.0
- Swift Language
- Create the directory structure matching the File Map above (empty files with placeholder comments)

- [ ] **Step 2: Add Widget extension target**

Add a new target: Widget Extension named `UniBondWidget`
- Add to both targets: App Group `group.com.unibond.shared`

- [ ] **Step 3: Configure capabilities**

In the main app target, enable:
- Sign in with Apple
- Push Notifications
- App Groups (`group.com.unibond.shared`)
- Background Modes (Remote notifications)

- [ ] **Step 4: Create all empty files with module structure**

Create every file listed in the File Map with a minimal placeholder:

```swift
// [FileName].swift
// UniBond

import SwiftUI

// TODO: Implement
```

- [ ] **Step 5: Verify project builds**

Run: `xcodebuild -scheme UniBond -destination 'platform=iOS Simulator,name=iPhone 16' build`
Expected: BUILD SUCCEEDED

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: scaffold UniBond iOS project with module structure"
```

---

## Task 2: Core Data Models

**Files:**
- Create: `UniBond/Core/Network/APIModels.swift`
- Create: `UniBondTests/Core/APIModelsTests.swift`

- [ ] **Step 1: Write tests for JSON decoding**

```swift
import XCTest
@testable import UniBond

final class APIModelsTests: XCTestCase {

    func testAuthResponseDecoding() throws {
        let json = """
        {"data":{"accessToken":"abc","refreshToken":"def","userId":1,"isNew":false}}
        """
        let wrapper = try JSONDecoder().decode(ApiResponse<AuthResponse>.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(wrapper.data.accessToken, "abc")
        XCTAssertEqual(wrapper.data.userId, 1)
    }

    func testUserResponseDecoding() throws {
        let json = """
        {"data":{"id":1,"email":"a@b.com","nickname":"小明","avatarUrl":null,"authProvider":"EMAIL","inviteCode":"AB3K7X","partnerId":null,"createdAt":"2026-01-01T00:00:00Z"}}
        """
        let wrapper = try JSONDecoder().decode(ApiResponse<UserResponse>.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(wrapper.data.nickname, "小明")
        XCTAssertEqual(wrapper.data.inviteCode, "AB3K7X")
        XCTAssertNil(wrapper.data.partnerId)
    }

    func testQuizResponseDecoding() throws {
        let json = """
        {"data":{"id":1,"date":"2026-03-26","quizType":"BLIND","theme":null,"questions":"[{\\"index\\":0,\\"content\\":\\"Q1\\",\\"options\\":[\\"A\\",\\"B\\",\\"C\\",\\"D\\"]}]"}}
        """
        let wrapper = try JSONDecoder().decode(ApiResponse<QuizResponse>.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(wrapper.data.quizType, "BLIND")
    }

    func testOverviewResponseDecoding() throws {
        let json = """
        {"data":{"todayScore":92,"streakDays":7,"totalQuizzes":23,"avgScore":85.5,"recentAchievements":[]}}
        """
        let wrapper = try JSONDecoder().decode(ApiResponse<OverviewResponse>.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(wrapper.data.todayScore, 92)
        XCTAssertEqual(wrapper.data.streakDays, 7)
    }

    func testCursorPageDecoding() throws {
        let json = """
        {"data":{"data":[{"id":1,"date":"2026-03-26","quizType":"BLIND","theme":null,"questions":"[]"}],"cursor":"123","hasMore":true}}
        """
        let wrapper = try JSONDecoder().decode(ApiResponse<CursorPage<QuizResponse>>.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(wrapper.data.data.count, 1)
        XCTAssertTrue(wrapper.data.hasMore)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `xcodebuild test -scheme UniBond -destination 'platform=iOS Simulator,name=iPhone 16'`
Expected: FAIL (types not defined)

- [ ] **Step 3: Implement all models**

```swift
// APIModels.swift
import Foundation

// MARK: - API Wrapper
struct ApiResponse<T: Codable>: Codable {
    let data: T
}

struct ApiErrorResponse: Codable {
    let code: String
    let message: String
    let timestamp: String
}

// MARK: - Auth
struct EmailSendRequest: Codable {
    let email: String
}

struct EmailLoginRequest: Codable {
    let email: String
    let code: String
    let timezone: String?
}

struct AppleLoginRequest: Codable {
    let identityToken: String
    let nickname: String?
    let timezone: String?
}

struct AuthResponse: Codable {
    let accessToken: String
    let refreshToken: String
    let userId: Int64
    let isNew: Bool
}

// MARK: - User
struct UserResponse: Codable, Equatable {
    let id: Int64
    let email: String?
    let nickname: String
    let avatarUrl: String?
    let authProvider: String
    let inviteCode: String
    let partnerId: Int64?
    let createdAt: String
}

struct ProfileUpdateRequest: Codable {
    let nickname: String?
    let avatarUrl: String?
    let timezone: String?
}

// MARK: - Couple
struct CoupleResponse: Codable, Equatable {
    let id: Int64
    let partnerUserId: Int64
    let partnerNickname: String
    let anniversaryDate: String?
    let bindAt: String
}

struct BindRequest: Codable {
    let inviteCode: String
}

// MARK: - Quiz
struct QuizResponse: Codable {
    let id: Int64
    let date: String
    let quizType: String
    let theme: String?
    let questions: String  // JSON string, parsed client-side
}

struct AnswerRequest: Codable {
    let quizId: Int64
    let answers: String
    let partnerGuess: String?
}

struct QuizResultResponse: Codable {
    let score: Int
    let revealed: Bool
    let myAnswers: String
    let partnerAnswers: String?
    let questions: String
}

struct QuizQuestion: Codable {
    let index: Int
    let content: String
    let options: [String]
}

// MARK: - Mood
struct MoodUpdateRequest: Codable {
    let emoji: String
    let text: String?
}

struct MoodResponse: Codable {
    let emoji: String
    let text: String?
    let updatedAt: String
}

// MARK: - Stats
struct OverviewResponse: Codable {
    let todayScore: Int
    let streakDays: Int
    let totalQuizzes: Int
    let avgScore: Double
    let recentAchievements: [AchievementResponse]
}

struct WeeklyResponse: Codable {
    let scores: [DayScore]
    let avgScore: Double
    let quizzesCompleted: Int
}

struct DayScore: Codable {
    let date: String
    let score: Int
    let quizType: String?
}

struct AchievementResponse: Codable {
    let type: String
    let displayName: String
    let unlocked: Bool
    let unlockedAt: String?
}

// MARK: - Pagination
struct CursorPage<T: Codable>: Codable {
    let data: [T]
    let cursor: String?
    let hasMore: Bool
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `xcodebuild test -scheme UniBond -destination 'platform=iOS Simulator,name=iPhone 16'`
Expected: All 5 tests PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add all API data models with JSON decoding tests"
```

---

## Task 3: Keychain & Storage

**Files:**
- Create: `UniBond/Core/Storage/KeychainManager.swift`
- Create: `UniBond/Core/Storage/AppSettings.swift`
- Create: `UniBondTests/Core/KeychainManagerTests.swift`

- [ ] **Step 1: Write Keychain tests**

```swift
import XCTest
@testable import UniBond

final class KeychainManagerTests: XCTestCase {

    override func tearDown() {
        KeychainManager.shared.deleteTokens()
    }

    func testSaveAndReadTokens() {
        KeychainManager.shared.saveTokens(access: "access123", refresh: "refresh456")
        XCTAssertEqual(KeychainManager.shared.accessToken, "access123")
        XCTAssertEqual(KeychainManager.shared.refreshToken, "refresh456")
    }

    func testDeleteTokens() {
        KeychainManager.shared.saveTokens(access: "a", refresh: "r")
        KeychainManager.shared.deleteTokens()
        XCTAssertNil(KeychainManager.shared.accessToken)
        XCTAssertNil(KeychainManager.shared.refreshToken)
    }

    func testUpdateAccessToken() {
        KeychainManager.shared.saveTokens(access: "old", refresh: "r")
        KeychainManager.shared.saveTokens(access: "new", refresh: "r")
        XCTAssertEqual(KeychainManager.shared.accessToken, "new")
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

- [ ] **Step 3: Implement KeychainManager**

```swift
import Foundation
import Security

final class KeychainManager {
    static let shared = KeychainManager()
    private let service = "com.unibond.app"

    private init() {}

    var accessToken: String? {
        read(key: "accessToken")
    }

    var refreshToken: String? {
        read(key: "refreshToken")
    }

    func saveTokens(access: String, refresh: String) {
        save(key: "accessToken", value: access)
        save(key: "refreshToken", value: refresh)
    }

    func deleteTokens() {
        delete(key: "accessToken")
        delete(key: "refreshToken")
    }

    private func save(key: String, value: String) {
        let data = value.data(using: .utf8)!
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(query as CFDictionary)
        var addQuery = query
        addQuery[kSecValueData as String] = data
        SecItemAdd(addQuery as CFDictionary, nil)
    }

    private func read(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private func delete(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(query as CFDictionary)
    }
}
```

- [ ] **Step 4: Implement AppSettings**

```swift
import Foundation

final class AppSettings {
    static let shared = AppSettings()

    private let defaults = UserDefaults.standard
    private let sharedDefaults = UserDefaults(suiteName: "group.com.unibond.shared")!

    private init() {}

    var lastTimezone: String? {
        get { defaults.string(forKey: "lastTimezone") }
        set { defaults.set(newValue, forKey: "lastTimezone") }
    }

    var cachedUserId: Int64? {
        get { defaults.object(forKey: "cachedUserId") as? Int64 }
        set { defaults.set(newValue, forKey: "cachedUserId") }
    }

    // Shared with Widget via App Group
    var sharedTodayScore: Int {
        get { sharedDefaults.integer(forKey: "todayScore") }
        set { sharedDefaults.set(newValue, forKey: "todayScore") }
    }

    var sharedStreakDays: Int {
        get { sharedDefaults.integer(forKey: "streakDays") }
        set { sharedDefaults.set(newValue, forKey: "streakDays") }
    }

    var sharedQuizAnswered: Bool {
        get { sharedDefaults.bool(forKey: "quizAnswered") }
        set { sharedDefaults.set(newValue, forKey: "quizAnswered") }
    }

    var sharedQuizType: String? {
        get { sharedDefaults.string(forKey: "quizType") }
        set { sharedDefaults.set(newValue, forKey: "quizType") }
    }

    var sharedPartnerMoodEmoji: String? {
        get { sharedDefaults.string(forKey: "partnerMoodEmoji") }
        set { sharedDefaults.set(newValue, forKey: "partnerMoodEmoji") }
    }

    var sharedPartnerMoodText: String? {
        get { sharedDefaults.string(forKey: "partnerMoodText") }
        set { sharedDefaults.set(newValue, forKey: "partnerMoodText") }
    }

    func clearAll() {
        let domain = Bundle.main.bundleIdentifier!
        defaults.removePersistentDomain(forName: domain)
    }
}
```

- [ ] **Step 5: Run tests, verify pass**

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add KeychainManager and AppSettings with tests"
```

---

## Task 4: API Error & Endpoint Definitions

**Files:**
- Create: `UniBond/Core/Network/APIError.swift`
- Create: `UniBond/Core/Network/APIEndpoint.swift`

- [ ] **Step 1: Implement APIError**

```swift
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
        if let body, let errorResponse = try? JSONDecoder().decode(ApiErrorResponse.self, from: body) {
            switch errorResponse.code {
            case "COUPLE_NOT_BOUND": return .coupleNotBound
            case "QUIZ_ALREADY_ANSWERED": return .quizAlreadyAnswered
            default: break
            }
            return .badRequest(errorResponse.message)
        }
        switch statusCode {
        case 401: return .unauthorized
        case 403: return .forbidden
        case 404: return .notFound
        case 429: return .rateLimited
        default: return .serverError("服务器错误 (\(statusCode))")
        }
    }
}
```

- [ ] **Step 2: Implement APIEndpoint**

```swift
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
        .init(path: "/api/v1/quiz/result/\(date)", method: .GET)
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
```

- [ ] **Step 3: Verify build succeeds**

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add APIError and APIEndpoint definitions"
```

---

## Task 5: APIClient with JWT Auto-Refresh

**Files:**
- Create: `UniBond/Core/Network/APIClient.swift`
- Create: `UniBondTests/Core/APIClientTests.swift`

- [ ] **Step 1: Write APIClient tests**

```swift
import XCTest
@testable import UniBond

final class APIClientTests: XCTestCase {

    func testBuildURLRequest() async throws {
        let client = APIClient(baseURL: "https://example.com")
        let endpoint = APIEndpoint(path: "/api/v1/user/me", method: .GET)
        let request = try await client.buildRequest(for: endpoint)
        XCTAssertEqual(request.url?.absoluteString, "https://example.com/api/v1/user/me")
        XCTAssertEqual(request.httpMethod, "GET")
    }

    func testBuildURLRequestWithBody() async throws {
        let client = APIClient(baseURL: "https://example.com")
        let body = EmailSendRequest(email: "test@example.com")
        let endpoint = APIEndpoint.emailSend(body)
        let request = try await client.buildRequest(for: endpoint)
        XCTAssertEqual(request.httpMethod, "POST")
        XCTAssertNotNil(request.httpBody)
        let decoded = try JSONDecoder().decode(EmailSendRequest.self, from: request.httpBody!)
        XCTAssertEqual(decoded.email, "test@example.com")
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

- [ ] **Step 3: Implement APIClient**

```swift
import Foundation

actor APIClient {
    let baseURL: String
    private let session: URLSession
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder
    private var isRefreshing = false
    private var refreshContinuations: [CheckedContinuation<Void, Error>] = []

    init(baseURL: String, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session
        self.encoder = JSONEncoder()
        self.decoder = JSONDecoder()
    }

    func request<T: Decodable>(_ endpoint: APIEndpoint) async throws -> T {
        let (data, _) = try await performRequest(endpoint)
        do {
            let wrapper = try decoder.decode(ApiResponse<T>.self, from: data)
            return wrapper.data
        } catch {
            throw APIError.decodingError
        }
    }

    func requestVoid(_ endpoint: APIEndpoint) async throws {
        let _ = try await performRequest(endpoint)
    }

    private func performRequest(_ endpoint: APIEndpoint) async throws -> (Data, HTTPURLResponse) {
        var urlRequest = try buildRequest(for: endpoint)

        if endpoint.requiresAuth, let token = KeychainManager.shared.accessToken {
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try await session.data(for: urlRequest)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.networkError
        }

        if httpResponse.statusCode == 401 && endpoint.requiresAuth {
            try await refreshTokenIfNeeded()
            // Retry with new token
            var retryRequest = try buildRequest(for: endpoint)
            if let newToken = KeychainManager.shared.accessToken {
                retryRequest.setValue("Bearer \(newToken)", forHTTPHeaderField: "Authorization")
            }
            let (retryData, retryResponse) = try await session.data(for: retryRequest)
            guard let retryHttp = retryResponse as? HTTPURLResponse else {
                throw APIError.networkError
            }
            if retryHttp.statusCode >= 400 {
                throw APIError.from(statusCode: retryHttp.statusCode, body: retryData)
            }
            return (retryData, retryHttp)
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
        defer {
            isRefreshing = false
            let continuations = refreshContinuations
            refreshContinuations = []
            for c in continuations { c.resume() }
        }

        guard let refreshToken = KeychainManager.shared.refreshToken else {
            throw APIError.unauthorized
        }

        let endpoint = APIEndpoint.refreshToken(refreshToken)
        let urlRequest = try buildRequest(for: endpoint)
        let (data, response) = try await session.data(for: urlRequest)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            KeychainManager.shared.deleteTokens()
            throw APIError.unauthorized
        }

        let authResponse = try decoder.decode(ApiResponse<AuthResponse>.self, from: data)
        KeychainManager.shared.saveTokens(
            access: authResponse.data.accessToken,
            refresh: authResponse.data.refreshToken
        )
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

// Helper to erase Encodable type
struct AnyEncodable: Encodable {
    private let _encode: (Encoder) throws -> Void
    init(_ wrapped: Encodable) {
        _encode = wrapped.encode
    }
    func encode(to encoder: Encoder) throws {
        try _encode(encoder)
    }
}
```

- [ ] **Step 4: Run tests, verify pass**

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: add APIClient with JWT auto-refresh"
```

---

## Task 6: Extensions & Color System

**Files:**
- Create: `UniBond/Core/Extensions/Color+Ext.swift`
- Create: `UniBond/Core/Extensions/Date+Ext.swift`

- [ ] **Step 1: Implement Color+Ext**

```swift
import SwiftUI

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        self.init(red: r, green: g, blue: b)
    }
}

enum AppColors {
    static let primaryPurple = Color(hex: "A855F7")
    static let primaryPink = Color(hex: "EC4899")
    static let bgLight = Color(hex: "F3E8FF")
    static let bgLightPink = Color(hex: "FCE7F3")
    static let cardBg = Color.white.opacity(0.8)
    static let textPrimary = Color(hex: "1F1F1F")
    static let textSecondary = Color(hex: "6B7280")
    static let success = Color(hex: "10B981")
    static let error = Color(hex: "E11D48")

    static let primaryGradient = LinearGradient(
        colors: [primaryPurple, primaryPink],
        startPoint: .leading, endPoint: .trailing
    )
    static let backgroundGradient = LinearGradient(
        colors: [bgLight, bgLightPink],
        startPoint: .topLeading, endPoint: .bottomTrailing
    )
}
```

- [ ] **Step 2: Implement Date+Ext**

```swift
import Foundation

extension Date {
    var iso8601DateString: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: self)
    }

    static func fromISO8601(_ string: String) -> Date? {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.date(from: string) ?? {
            formatter.formatOptions = [.withInternetDateTime]
            return formatter.date(from: string)
        }()
    }

    var relativeTimeString: String {
        let formatter = RelativeDateTimeFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.unitsStyle = .short
        return formatter.localizedString(for: self, relativeTo: .now)
    }
}
```

- [ ] **Step 3: Verify build**

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add color system and date extensions"
```

---

## Task 7: AppState & AppRouter

**Files:**
- Create: `UniBond/Navigation/AppState.swift`
- Create: `UniBond/Navigation/AppRouter.swift`

- [ ] **Step 1: Implement AppState**

```swift
import SwiftUI
import Network

enum AuthState: Equatable {
    case unauthenticated
    case authenticated(UserResponse)
}

enum CoupleState: Equatable {
    case unbound
    case bound(CoupleResponse)
}

@Observable
class AppState {
    var authState: AuthState = .unauthenticated
    var coupleState: CoupleState = .unbound
    var isOnline: Bool = true

    private let monitor = NWPathMonitor()

    var isAuthenticated: Bool {
        if case .authenticated = authState { return true }
        return false
    }

    var isBound: Bool {
        if case .bound = coupleState { return true }
        return false
    }

    var currentUser: UserResponse? {
        if case .authenticated(let user) = authState { return user }
        return nil
    }

    var currentCouple: CoupleResponse? {
        if case .bound(let couple) = coupleState { return couple }
        return nil
    }

    func startNetworkMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor in
                self?.isOnline = path.status == .satisfied
            }
        }
        monitor.start(queue: DispatchQueue(label: "NetworkMonitor"))
    }

    func logout() {
        KeychainManager.shared.deleteTokens()
        authState = .unauthenticated
        coupleState = .unbound
    }
}
```

- [ ] **Step 2: Implement AppRouter**

```swift
import SwiftUI

enum AppRoute: Hashable {
    case quizAnswer
    case quizWaiting
    case quizResult(date: String)
    case bindPartner
}

enum SheetRoute: Identifiable {
    case moodPicker
    case unbindConfirm

    var id: String { String(describing: self) }
}

@Observable
class AppRouter {
    var homePath = NavigationPath()
    var statsPath = NavigationPath()
    var profilePath = NavigationPath()
    var activeSheet: SheetRoute?
    var showWidgetPermission = false
    var selectedTab: Int = 0

    // Push notification destination
    var pendingNotificationRoute: AppRoute?

    func navigateHome(to route: AppRoute) {
        selectedTab = 0
        homePath.append(route)
    }

    func navigateStats() {
        selectedTab = 1
    }

    func handleNotification(type: String) {
        switch type {
        case "QUIZ_REMINDER":
            selectedTab = 0
        case "PARTNER_ANSWERED":
            navigateHome(to: .quizWaiting)
        case "RESULT_REVEALED":
            navigateHome(to: .quizResult(date: Date().iso8601DateString))
        case "PARTNER_MOOD":
            selectedTab = 0
        case "ACHIEVEMENT_UNLOCKED", "STREAK_MILESTONE":
            navigateStats()
        default:
            break
        }
    }

    func reset() {
        homePath = NavigationPath()
        statsPath = NavigationPath()
        profilePath = NavigationPath()
        activeSheet = nil
        selectedTab = 0
    }
}
```

- [ ] **Step 3: Verify build**

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add AppState and AppRouter for navigation"
```

---

## Task 8: Shared UI Components

**Files:**
- Create: All files in `UniBond/SharedUI/`

- [ ] **Step 1: Implement GradientBackground**

```swift
import SwiftUI

struct GradientBackground: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(AppColors.backgroundGradient.ignoresSafeArea())
    }
}

extension View {
    func gradientBackground() -> some View {
        modifier(GradientBackground())
    }
}
```

- [ ] **Step 2: Implement CardView, PrimaryButton, SecondaryButton**

```swift
// CardView.swift
import SwiftUI

struct CardView<Content: View>: View {
    let content: Content
    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }
    var body: some View {
        content
            .background(.white.opacity(0.8))
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// PrimaryButton.swift
import SwiftUI

struct PrimaryButton: View {
    let title: String
    let icon: String?
    let action: () -> Void
    var isDisabled: Bool = false

    init(_ title: String, icon: String? = nil, isDisabled: Bool = false, action: @escaping () -> Void) {
        self.title = title
        self.icon = icon
        self.isDisabled = isDisabled
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon { Image(systemName: icon) }
                Text(title).fontWeight(.semibold)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .foregroundStyle(.white)
            .background(isDisabled ? AnyShapeStyle(Color.gray.opacity(0.4)) : AnyShapeStyle(AppColors.primaryGradient))
            .clipShape(RoundedRectangle(cornerRadius: 24))
        }
        .disabled(isDisabled)
    }
}

// SecondaryButton.swift
import SwiftUI

struct SecondaryButton: View {
    let title: String
    let icon: String?
    let action: () -> Void

    init(_ title: String, icon: String? = nil, action: @escaping () -> Void) {
        self.title = title
        self.icon = icon
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon { Image(systemName: icon) }
                Text(title).fontWeight(.medium)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .foregroundStyle(AppColors.primaryPurple)
            .background(.white)
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(AppColors.primaryPurple.opacity(0.3), lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 24))
        }
    }
}
```

- [ ] **Step 3: Implement TabBarView**

```swift
import SwiftUI

struct TabBarView: View {
    @Binding var selectedTab: Int

    var body: some View {
        HStack {
            tabItem(icon: "house.fill", label: "首页", index: 0)
            Spacer()
            tabItem(icon: "chart.bar.fill", label: "统计", index: 1)
            Spacer()
            tabItem(icon: "person.fill", label: "我的", index: 2)
        }
        .padding(.horizontal, 40)
        .padding(.vertical, 12)
        .background(.ultraThinMaterial)
    }

    private func tabItem(icon: String, label: String, index: Int) -> some View {
        Button {
            selectedTab = index
        } label: {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                Text(label)
                    .font(.system(size: 11, weight: .medium))
            }
            .foregroundStyle(selectedTab == index ? AppColors.primaryPurple : AppColors.textSecondary)
        }
    }
}
```

- [ ] **Step 4: Implement StatCard, QuizOptionRow, EmojiGrid, AchievementBadge**

```swift
// StatCard.swift
import SwiftUI

struct StatCard: View {
    let value: String
    let label: String
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(color)
            Text(label)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(AppColors.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(.white.opacity(0.8))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// QuizOptionRow.swift
import SwiftUI

struct QuizOptionRow: View {
    let label: String  // "A", "B", "C", "D"
    let text: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Text(label)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(isSelected ? .white : AppColors.primaryPurple)
                    .frame(width: 28, height: 28)
                    .background(isSelected ? AppColors.primaryPurple : .clear)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(AppColors.primaryPurple.opacity(0.3), lineWidth: isSelected ? 0 : 1))
                Text(text)
                    .font(.system(size: 15))
                    .foregroundStyle(AppColors.textPrimary)
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(AppColors.primaryPurple)
                }
            }
            .padding(16)
            .background(.white.opacity(isSelected ? 1 : 0.8))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isSelected ? AppColors.primaryPurple : Color.clear, lineWidth: 2)
            )
        }
    }
}

// EmojiGrid.swift
import SwiftUI

struct EmojiGrid: View {
    let emojis: [String]
    @Binding var selected: String?

    var body: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 12) {
            ForEach(emojis, id: \.self) { emoji in
                Button {
                    selected = emoji
                } label: {
                    Text(emoji)
                        .font(.system(size: 32))
                        .frame(width: 56, height: 56)
                        .background(selected == emoji ? AppColors.primaryPurple.opacity(0.15) : .white.opacity(0.6))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(selected == emoji ? AppColors.primaryPurple : .clear, lineWidth: 2)
                        )
                }
            }
        }
    }
}

// AchievementBadge.swift
import SwiftUI

struct AchievementBadge: View {
    let name: String
    let icon: String
    let subtitle: String
    let unlocked: Bool

    var body: some View {
        VStack(spacing: 6) {
            ZStack {
                Circle()
                    .fill(unlocked ? AppColors.primaryPurple.opacity(0.1) : Color.gray.opacity(0.1))
                    .frame(width: 56, height: 56)
                if unlocked {
                    Text(icon).font(.system(size: 28))
                } else {
                    Image(systemName: "lock.fill")
                        .foregroundStyle(.gray)
                }
            }
            Text(name)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(unlocked ? AppColors.textPrimary : .gray)
            Text(subtitle)
                .font(.system(size: 9))
                .foregroundStyle(AppColors.textSecondary)
        }
    }
}
```

- [ ] **Step 5: Implement LoadingView, OfflineBanner, EmptyStateView**

```swift
// LoadingView.swift
import SwiftUI

struct LoadingView: View {
    var body: some View {
        ProgressView()
            .tint(AppColors.primaryPurple)
    }
}

// OfflineBanner.swift
import SwiftUI

struct OfflineBanner: View {
    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "wifi.slash")
            Text("无网络连接")
        }
        .font(.system(size: 13, weight: .medium))
        .foregroundStyle(.white)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color.gray.opacity(0.8))
        .clipShape(Capsule())
    }
}

// EmptyStateView.swift
import SwiftUI

struct EmptyStateView: View {
    let icon: String
    let message: String

    var body: some View {
        VStack(spacing: 12) {
            Text(icon).font(.system(size: 48))
            Text(message)
                .font(.system(size: 15))
                .foregroundStyle(AppColors.textSecondary)
                .multilineTextAlignment(.center)
        }
        .padding(32)
    }
}
```

- [ ] **Step 6: Verify build**

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add all shared UI components"
```

---

## Task 9: Auth Module (Login)

**Files:**
- Create: `UniBond/Features/Auth/LoginView.swift`
- Create: `UniBond/Features/Auth/AuthViewModel.swift`
- Create: `UniBond/Features/Auth/AuthService.swift`
- Create: `UniBondTests/Features/AuthViewModelTests.swift`

- [ ] **Step 1: Write AuthViewModel tests**

Test email countdown timer logic and state transitions:

```swift
import XCTest
@testable import UniBond

final class AuthViewModelTests: XCTestCase {

    func testInitialState() {
        let vm = AuthViewModel(apiClient: APIClient(baseURL: "https://test.com"), appState: AppState())
        XCTAssertEqual(vm.loginStep, .initial)
        XCTAssertFalse(vm.isLoading)
        XCTAssertEqual(vm.countdown, 0)
    }

    func testEmailValidation() {
        let vm = AuthViewModel(apiClient: APIClient(baseURL: "https://test.com"), appState: AppState())
        vm.email = ""
        XCTAssertFalse(vm.isEmailValid)
        vm.email = "bad"
        XCTAssertFalse(vm.isEmailValid)
        vm.email = "test@example.com"
        XCTAssertTrue(vm.isEmailValid)
    }

    func testCodeValidation() {
        let vm = AuthViewModel(apiClient: APIClient(baseURL: "https://test.com"), appState: AppState())
        vm.code = "123"
        XCTAssertFalse(vm.isCodeValid)
        vm.code = "123456"
        XCTAssertTrue(vm.isCodeValid)
    }
}
```

- [ ] **Step 2: Run tests, verify fail**

- [ ] **Step 3: Implement AuthViewModel**

```swift
import SwiftUI

enum LoginStep {
    case initial       // Show Apple + email button
    case emailInput    // Enter email
    case codeInput     // Enter verification code
}

@MainActor @Observable
class AuthViewModel {
    var loginStep: LoginStep = .initial
    var email: String = ""
    var code: String = ""
    var isLoading = false
    var errorMessage: String?
    var countdown: Int = 0

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
                .appleLogin(AppleLoginRequest(identityToken: identityToken, nickname: nickname, timezone: TimeZone.current.identifier))
            )
            await handleLoginSuccess(response)
        } catch let error as APIError {
            errorMessage = error.errorDescription
        } catch {
            errorMessage = "Apple 登录失败，请重试"
        }
        isLoading = false
    }

    @MainActor
    private func handleLoginSuccess(_ response: AuthResponse) async {
        KeychainManager.shared.saveTokens(access: response.accessToken, refresh: response.refreshToken)
        AppSettings.shared.cachedUserId = response.userId
        // Fetch user info
        do {
            let user: UserResponse = try await apiClient.request(.me)
            appState.authState = .authenticated(user)
            // Check couple status
            if user.partnerId != nil {
                let couple: CoupleResponse = try await apiClient.request(.coupleInfo)
                appState.coupleState = .bound(couple)
            }
        } catch {
            appState.authState = .unauthenticated
        }
    }

    private func startCountdown() {
        countdown = 60
        countdownTimer?.invalidate()
        countdownTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] timer in
            guard let self else { timer.invalidate(); return }
            if self.countdown > 0 {
                self.countdown -= 1
            } else {
                timer.invalidate()
            }
        }
    }
}
```

- [ ] **Step 4: Implement AuthService (Apple Sign-in wrapper)**

```swift
import AuthenticationServices

class AuthService: NSObject, ASAuthorizationControllerDelegate {
    private var continuation: CheckedContinuation<(String, String?), Error>?

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
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let token = String(data: tokenData, encoding: .utf8) else {
            continuation?.resume(throwing: APIError.serverError("Apple 登录凭证无效"))
            return
        }
        let nickname = credential.fullName?.givenName
        continuation?.resume(returning: (token, nickname))
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        continuation?.resume(throwing: error)
    }
}
```

- [ ] **Step 5: Implement LoginView**

```swift
import SwiftUI
import AuthenticationServices

struct LoginView: View {
    @Environment(AppState.self) private var appState
    @State private var viewModel: AuthViewModel?
    @State private var authService = AuthService()
    let apiClient: APIClient

    var body: some View {
        ZStack {
            AppColors.backgroundGradient.ignoresSafeArea()

            if let viewModel {
            VStack(spacing: 24) {
                Spacer()
                // Logo
                Circle()
                    .fill(AppColors.primaryGradient)
                    .frame(width: 80, height: 80)
                    .overlay(Image(systemName: "heart.fill").font(.system(size: 36)).foregroundStyle(.white))

                Text("UniBond")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(AppColors.textPrimary)
                Text("每日默契问答，让爱更近")
                    .font(.system(size: 15))
                    .foregroundStyle(AppColors.textSecondary)

                Spacer()

                switch viewModel.loginStep {
                case .initial:
                    initialButtons
                case .emailInput:
                    emailInputSection
                case .codeInput:
                    codeInputSection
                }

                if let error = viewModel.errorMessage {
                    Text(error).font(.system(size: 13)).foregroundStyle(AppColors.error)
                }

                Spacer().frame(height: 40)

                Text("登录即表示同意《用户协议》和《隐私政策》")
                    .font(.system(size: 11))
                    .foregroundStyle(AppColors.textSecondary)
            }
            .padding(.horizontal, 32)
            }
        }
        .onAppear { viewModel = AuthViewModel(apiClient: apiClient, appState: appState) }
    }

    private var initialButtons: some View {
        VStack(spacing: 16) {
            SignInWithAppleButton(.signIn) { request in
                request.requestedScopes = [.fullName, .email]
            } onCompletion: { result in
                Task {
                    switch result {
                    case .success(let auth):
                        if let credential = auth.credential as? ASAuthorizationAppleIDCredential,
                           let tokenData = credential.identityToken,
                           let token = String(data: tokenData, encoding: .utf8) {
                            await viewModel?.loginWithApple(identityToken: token, nickname: credential.fullName?.givenName)
                        }
                    case .failure:
                        break
                    }
                }
            }
            .signInWithAppleButtonStyle(.black)
            .frame(height: 50)
            .clipShape(RoundedRectangle(cornerRadius: 24))

            divider

            SecondaryButton("邮箱验证码登录", icon: "envelope.fill") {
                viewModel?.loginStep = .emailInput
            }
        }
    }

    private var emailInputSection: some View {
        VStack(spacing: 16) {
            TextField("请输入邮箱", text: Binding(get: { viewModel?.email ?? "" }, set: { viewModel?.email = $0 }))
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
                .autocapitalization(.none)
                .padding(16)
                .background(.white.opacity(0.8))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            PrimaryButton("获取验证码", isDisabled: !(viewModel?.isEmailValid ?? false) || (viewModel?.isLoading ?? false)) {
                Task { await viewModel?.sendCode() }
            }
        }
    }

    private var codeInputSection: some View {
        VStack(spacing: 16) {
            TextField("请输入 6 位验证码", text: Binding(get: { viewModel?.code ?? "" }, set: { viewModel?.code = $0 }))
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .padding(16)
                .background(.white.opacity(0.8))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            PrimaryButton("登录", isDisabled: !(viewModel?.isCodeValid ?? false) || (viewModel?.isLoading ?? false)) {
                Task { await viewModel?.loginWithEmail() }
            }

            if let countdown = viewModel?.countdown, countdown > 0 {
                Text("重新发送 (\(countdown)s)")
                    .font(.system(size: 13))
                    .foregroundStyle(AppColors.textSecondary)
            } else {
                Button("重新发送验证码") { Task { await viewModel?.sendCode() } }
                    .font(.system(size: 13))
                    .foregroundStyle(AppColors.primaryPurple)
            }
        }
    }

    private var divider: some View {
        HStack {
            Rectangle().fill(AppColors.textSecondary.opacity(0.3)).frame(height: 0.5)
            Text("或").font(.system(size: 13)).foregroundStyle(AppColors.textSecondary)
            Rectangle().fill(AppColors.textSecondary.opacity(0.3)).frame(height: 0.5)
        }
    }
}
```

- [ ] **Step 6: Run tests, verify pass**

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: implement Auth module with Apple Sign-in and email login"
```

---

## Task 10: Home Module

**Files:**
- Create: `UniBond/Features/Home/HomeView.swift`
- Create: `UniBond/Features/Home/HomeViewModel.swift`

- [ ] **Step 1: Implement HomeViewModel**

```swift
import SwiftUI
import WidgetKit

enum QuizCardState {
    case unbound
    case available(QuizResponse)
    case answeredWaiting(date: String)
    case waitingReveal(date: String)
    case revealed(QuizResultResponse)
}

@MainActor @Observable
class HomeViewModel {
    var quizCardState: QuizCardState = .unbound
    var overview: OverviewResponse?
    var myMood: MoodResponse?
    var partnerMood: MoodResponse?
    var isLoading = false
    var errorMessage: String?

    private let apiClient: APIClient
    private let appState: AppState
    private var pollingTask: Task<Void, Never>?

    init(apiClient: APIClient, appState: AppState) {
        self.apiClient = apiClient
        self.appState = appState
    }

    func loadData() async {
        guard appState.isBound else {
            quizCardState = .unbound
            return
        }
        isLoading = true
        do {
            // Load overview, quiz, partner mood in parallel
            async let overviewResult: OverviewResponse = apiClient.request(.overview)
            async let partnerMoodResult: MoodResponse? = try? apiClient.request(.partnerMood)

            overview = try await overviewResult
            partnerMood = try await partnerMoodResult

            // Update shared data for Widget
            if let ov = overview {
                AppSettings.shared.sharedTodayScore = ov.todayScore
                AppSettings.shared.sharedStreakDays = ov.streakDays
            }

            // Determine quiz state
            await loadQuizState()
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "加载失败"
        }
        isLoading = false
    }

    private func loadQuizState() async {
        let today = Date().iso8601DateString
        do {
            let quiz: QuizResponse = try await apiClient.request(.quizToday)
            // Check if already answered by fetching result
            do {
                let result: QuizResultResponse = try await apiClient.request(.quizResult(date: today))
                if result.revealed {
                    quizCardState = .revealed(result)
                    AppSettings.shared.sharedQuizAnswered = true
                } else if result.partnerAnswers != nil {
                    quizCardState = .waitingReveal(date: today)
                    startPolling(date: today)
                } else {
                    quizCardState = .answeredWaiting(date: today)
                    startPolling(date: today)
                }
            } catch {
                // No result yet — quiz is available
                quizCardState = .available(quiz)
                AppSettings.shared.sharedQuizAnswered = false
            }
            AppSettings.shared.sharedQuizType = quiz.quizType
        } catch {
            quizCardState = .unbound // fallback
        }
    }

    func startPolling(date: String) {
        stopPolling()
        pollingTask = Task {
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(30))
                guard !Task.isCancelled else { break }
                do {
                    let result: QuizResultResponse = try await apiClient.request(.quizResult(date: date))
                    if result.revealed {
                        quizCardState = .revealed(result)
                        AppSettings.shared.sharedQuizAnswered = true
                        WidgetCenter.shared.reloadAllTimelines()
                        break
                    } else if result.partnerAnswers != nil {
                        quizCardState = .waitingReveal(date: date)
                    }
                } catch { }
            }
        }
    }

    func stopPolling() {
        pollingTask?.cancel()
        pollingTask = nil
    }

    var greetingEmoji: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 6..<12: return "🌅"
        case 12..<18: return "☀️"
        default: return "🌙"
        }
    }

    var greetingText: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 6..<12: return "早上好"
        case 12..<18: return "下午好"
        default: return "晚上好"
        }
    }
}
```

- [ ] **Step 2: Implement HomeView**

```swift
import SwiftUI

struct HomeView: View {
    @Environment(AppState.self) private var appState
    @Environment(AppRouter.self) private var router
    @State var viewModel: HomeViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Greeting
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(viewModel.greetingEmoji) \(viewModel.greetingText)")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundStyle(AppColors.textPrimary)
                        if let user = appState.currentUser {
                            Text(user.nickname)
                                .font(.system(size: 15))
                                .foregroundStyle(AppColors.textSecondary)
                        }
                    }
                    Spacer()
                }
                .padding(.top, 8)

                // Offline banner
                if !appState.isOnline {
                    OfflineBanner()
                }

                // Quiz card
                quizCard

                // Mood section
                if appState.isBound {
                    moodSection
                }

                // Stats summary
                if let overview = viewModel.overview {
                    HStack(spacing: 12) {
                        StatCard(value: "\(overview.todayScore)", label: "今日默契", color: AppColors.primaryPurple)
                        StatCard(value: "\(overview.streakDays)天", label: "连续打卡", color: AppColors.success)
                        StatCard(value: "\(overview.totalQuizzes)", label: "累计答题", color: AppColors.primaryPink)
                    }
                }
            }
            .padding(.horizontal, 20)
        }
        .gradientBackground()
        .task { await viewModel.loadData() }
        .onDisappear { viewModel.stopPolling() }
        .refreshable { await viewModel.loadData() }
    }

    @ViewBuilder
    private var quizCard: some View {
        switch viewModel.quizCardState {
        case .unbound:
            CardView {
                VStack(spacing: 16) {
                    EmptyStateView(icon: "💑", message: "绑定伴侣后开始每日默契挑战")
                    PrimaryButton("去绑定", icon: "link") {
                        router.navigateHome(to: .bindPartner)
                    }
                }
                .padding(20)
            }
        case .available:
            CardView {
                VStack(spacing: 12) {
                    HStack {
                        Text("🎯").font(.system(size: 28))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("今日默契挑战").font(.system(size: 17, weight: .semibold))
                            Text("5 道趣味问题，看看你们有多默契").font(.system(size: 13)).foregroundStyle(AppColors.textSecondary)
                        }
                        Spacer()
                    }
                    PrimaryButton("开始答题") {
                        router.navigateHome(to: .quizAnswer)
                    }
                }
                .padding(20)
            }
        case .answeredWaiting:
            CardView {
                VStack(spacing: 12) {
                    Text("✅").font(.system(size: 36))
                    Text("你已完成答题").font(.system(size: 17, weight: .semibold))
                    Text("等待 TA 完成答题...").font(.system(size: 15)).foregroundStyle(AppColors.textSecondary)
                    ProgressView().tint(AppColors.primaryPurple)
                }
                .padding(20)
            }
        case .waitingReveal(let date):
            CardView {
                VStack(spacing: 12) {
                    Text("🎉").font(.system(size: 36))
                    Text("双方已完成答题").font(.system(size: 17, weight: .semibold))
                    Text("等待系统揭晓结果...").font(.system(size: 15)).foregroundStyle(AppColors.textSecondary)
                    PrimaryButton("查看结果") {
                        router.navigateHome(to: .quizResult(date: date))
                    }
                }
                .padding(20)
            }
        case .revealed(let result):
            CardView {
                VStack(spacing: 12) {
                    Text("💕").font(.system(size: 36))
                    Text("\(result.score)%").font(.system(size: 40, weight: .bold)).foregroundStyle(AppColors.primaryPurple)
                    Text("今日默契分").font(.system(size: 15)).foregroundStyle(AppColors.textSecondary)
                    PrimaryButton("查看详情") {
                        router.navigateHome(to: .quizResult(date: Date().iso8601DateString))
                    }
                }
                .padding(20)
            }
        }
    }

    private var moodSection: some View {
        CardView {
            VStack(spacing: 12) {
                HStack {
                    Text("心情同步").font(.system(size: 17, weight: .semibold))
                    Spacer()
                    Button("更新心情") { router.activeSheet = .moodPicker }
                        .font(.system(size: 13)).foregroundStyle(AppColors.primaryPurple)
                }
                HStack(spacing: 24) {
                    // My mood
                    VStack(spacing: 4) {
                        Text(viewModel.myMood?.emoji ?? "😊").font(.system(size: 32))
                        Text("我").font(.system(size: 11)).foregroundStyle(AppColors.textSecondary)
                    }
                    // Partner mood
                    VStack(spacing: 4) {
                        if let mood = viewModel.partnerMood {
                            Text(mood.emoji).font(.system(size: 32))
                        } else {
                            Text("❓").font(.system(size: 32))
                        }
                        Text("TA").font(.system(size: 11)).foregroundStyle(AppColors.textSecondary)
                    }
                    Spacer()
                    if let mood = viewModel.partnerMood, let text = mood.text, !text.isEmpty {
                        Text(text)
                            .font(.system(size: 13))
                            .foregroundStyle(AppColors.textSecondary)
                            .lineLimit(2)
                    }
                }
            }
            .padding(20)
        }
    }
}
```

- [ ] **Step 3: Verify build and visual check in simulator**

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: implement Home module with multi-state quiz card"
```

---

## Task 11: Quiz Module

**Files:**
- Create: `UniBond/Features/Quiz/QuizViewModel.swift`
- Create: `UniBond/Features/Quiz/QuizAnswerView.swift`
- Create: `UniBond/Features/Quiz/QuizResultView.swift`
- Create: `UniBond/Features/Quiz/QuizWaitingView.swift`
- Create: `UniBondTests/Features/QuizViewModelTests.swift`

- [ ] **Step 1: Write QuizViewModel tests**

Test answer selection, submission readiness, and score display:

```swift
import XCTest
@testable import UniBond

final class QuizViewModelTests: XCTestCase {

    func testSelectAnswer() {
        let vm = QuizViewModel(apiClient: APIClient(baseURL: "https://test.com"))
        vm.selectAnswer(questionIndex: 0, optionIndex: 2)
        XCTAssertEqual(vm.selectedAnswers[0], 2)
    }

    func testAllAnswered() {
        let vm = QuizViewModel(apiClient: APIClient(baseURL: "https://test.com"))
        for i in 0..<5 { vm.selectAnswer(questionIndex: i, optionIndex: 0) }
        XCTAssertTrue(vm.allAnswered)
    }

    func testNotAllAnswered() {
        let vm = QuizViewModel(apiClient: APIClient(baseURL: "https://test.com"))
        vm.selectAnswer(questionIndex: 0, optionIndex: 1)
        XCTAssertFalse(vm.allAnswered)
    }
}
```

- [ ] **Step 2: Run tests, verify fail**

- [ ] **Step 3: Implement QuizViewModel**

```swift
import SwiftUI
import WidgetKit

@MainActor @Observable
class QuizViewModel {
    var questions: [QuizQuestion] = []
    var selectedAnswers: [Int: Int] = [:]  // questionIndex -> optionIndex
    var currentQuestionIndex = 0
    var isLoading = false
    var isSubmitting = false
    var errorMessage: String?
    var quizId: Int64 = 0
    var result: QuizResultResponse?

    private let apiClient: APIClient

    init(apiClient: APIClient) {
        self.apiClient = apiClient
    }

    var allAnswered: Bool {
        questions.count > 0 && selectedAnswers.count == questions.count
    }

    var progress: Double {
        guard questions.count > 0 else { return 0 }
        return Double(selectedAnswers.count) / Double(questions.count)
    }

    func selectAnswer(questionIndex: Int, optionIndex: Int) {
        selectedAnswers[questionIndex] = optionIndex
    }

    func loadQuiz() async {
        isLoading = true
        do {
            let quiz: QuizResponse = try await apiClient.request(.quizToday)
            quizId = quiz.id
            // Parse questions JSON string
            if let data = quiz.questions.data(using: .utf8) {
                questions = try JSONDecoder().decode([QuizQuestion].self, from: data)
            }
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "加载题目失败"
        }
        isLoading = false
    }

    func submitAnswers() async -> Bool {
        guard allAnswered else { return false }
        isSubmitting = true
        do {
            let answersArray = (0..<questions.count).map { selectedAnswers[$0] ?? 0 }
            let answersJson = try String(data: JSONEncoder().encode(answersArray), encoding: .utf8) ?? "[]"
            try await apiClient.requestVoid(.submitAnswer(AnswerRequest(
                quizId: quizId,
                answers: answersJson,
                partnerGuess: nil
            )))
            AppSettings.shared.sharedQuizAnswered = true
            WidgetCenter.shared.reloadAllTimelines()
            isSubmitting = false
            return true
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "提交失败"
            isSubmitting = false
            return false
        }
    }

    func loadResult(date: String) async {
        isLoading = true
        do {
            result = try await apiClient.request(.quizResult(date: date))
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "加载结果失败"
        }
        isLoading = false
    }
}
```

- [ ] **Step 4: Implement QuizAnswerView**

```swift
import SwiftUI

struct QuizAnswerView: View {
    @Environment(AppRouter.self) private var router
    @State var viewModel: QuizViewModel
    let labels = ["A", "B", "C", "D"]

    var body: some View {
        VStack(spacing: 20) {
            // Progress
            HStack {
                Text("问题 \(viewModel.currentQuestionIndex + 1)/\(viewModel.questions.count)")
                    .font(.system(size: 15, weight: .semibold))
                Spacer()
            }
            ProgressView(value: viewModel.progress)
                .tint(AppColors.primaryPurple)

            if viewModel.isLoading {
                Spacer()
                LoadingView()
                Spacer()
            } else if viewModel.currentQuestionIndex < viewModel.questions.count {
                let question = viewModel.questions[viewModel.currentQuestionIndex]

                // Question
                Text(question.content)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(AppColors.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.top, 8)

                // Options
                VStack(spacing: 10) {
                    ForEach(Array(question.options.enumerated()), id: \.offset) { index, option in
                        QuizOptionRow(
                            label: labels[index],
                            text: option,
                            isSelected: viewModel.selectedAnswers[viewModel.currentQuestionIndex] == index
                        ) {
                            viewModel.selectAnswer(questionIndex: viewModel.currentQuestionIndex, optionIndex: index)
                        }
                    }
                }

                Spacer()

                // Navigation buttons
                HStack(spacing: 12) {
                    if viewModel.currentQuestionIndex > 0 {
                        SecondaryButton("上一题") {
                            viewModel.currentQuestionIndex -= 1
                        }
                    }
                    if viewModel.currentQuestionIndex < viewModel.questions.count - 1 {
                        PrimaryButton("下一题", isDisabled: viewModel.selectedAnswers[viewModel.currentQuestionIndex] == nil) {
                            viewModel.currentQuestionIndex += 1
                        }
                    } else {
                        PrimaryButton("提交答案", isDisabled: !viewModel.allAnswered || viewModel.isSubmitting) {
                            Task {
                                if await viewModel.submitAnswers() {
                                    router.homePath.append(AppRoute.quizWaiting)
                                }
                            }
                        }
                    }
                }
            }

            if let error = viewModel.errorMessage {
                Text(error).font(.system(size: 13)).foregroundStyle(AppColors.error)
            }
        }
        .padding(20)
        .gradientBackground()
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.loadQuiz() }
    }
}
```

Refer to prototype: `noWcd`

- [ ] **Step 5: Implement QuizWaitingView**

```swift
import SwiftUI

struct QuizWaitingView: View {
    @Environment(AppState.self) private var appState
    @Environment(AppRouter.self) private var router

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 64))
                .foregroundStyle(AppColors.success)
            Text("答题完成！")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(AppColors.textPrimary)

            CardView {
                VStack(spacing: 12) {
                    HStack {
                        Text("我").font(.system(size: 15, weight: .medium))
                        Spacer()
                        Text("已完成 ✅").foregroundStyle(AppColors.success).font(.system(size: 13))
                    }
                    Divider()
                    HStack {
                        Text(appState.currentCouple?.partnerNickname ?? "TA")
                            .font(.system(size: 15, weight: .medium))
                        Spacer()
                        HStack(spacing: 4) {
                            ProgressView().controlSize(.small)
                            Text("尚未作答").font(.system(size: 13)).foregroundStyle(AppColors.textSecondary)
                        }
                    }
                }
                .padding(20)
            }

            Text("对方完成后即可查看结果")
                .font(.system(size: 13))
                .foregroundStyle(AppColors.textSecondary)

            Spacer()

            SecondaryButton("返回首页") {
                router.homePath = NavigationPath()
            }
        }
        .padding(20)
        .gradientBackground()
        .navigationBarBackButtonHidden()
    }
}
```

Refer to prototype: `zWolL`

- [ ] **Step 6: Implement QuizResultView**

```swift
import SwiftUI

struct QuizResultView: View {
    @Environment(AppRouter.self) private var router
    @State var viewModel: QuizViewModel
    let date: String

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                if viewModel.isLoading {
                    LoadingView()
                } else if let result = viewModel.result {
                    // Score circle
                    ZStack {
                        Circle()
                            .stroke(Color.gray.opacity(0.2), lineWidth: 8)
                            .frame(width: 120, height: 120)
                        Circle()
                            .trim(from: 0, to: Double(result.score) / 100.0)
                            .stroke(AppColors.primaryGradient, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                            .frame(width: 120, height: 120)
                            .rotationEffect(.degrees(-90))
                        VStack(spacing: 2) {
                            Text("\(result.score)%")
                                .font(.system(size: 32, weight: .bold))
                                .foregroundStyle(AppColors.primaryPurple)
                            Text("默契分")
                                .font(.system(size: 13))
                                .foregroundStyle(AppColors.textSecondary)
                        }
                    }

                    // Answer comparison
                    CardView {
                        VStack(spacing: 0) {
                            // Header
                            HStack {
                                Text("题目").font(.system(size: 13, weight: .medium)).frame(maxWidth: .infinity, alignment: .leading)
                                Text("我").font(.system(size: 13, weight: .medium)).frame(width: 40)
                                Text("TA").font(.system(size: 13, weight: .medium)).frame(width: 40)
                                Text("").frame(width: 24)
                            }
                            .foregroundStyle(AppColors.textSecondary)
                            .padding(.horizontal, 16).padding(.vertical, 8)

                            Divider()

                            // Parse answers
                            let myAnswers = (try? JSONDecoder().decode([Int].self, from: result.myAnswers.data(using: .utf8) ?? Data())) ?? []
                            let partnerAnswers = (try? JSONDecoder().decode([Int].self, from: (result.partnerAnswers ?? "[]").data(using: .utf8) ?? Data())) ?? []
                            let labels = ["A", "B", "C", "D"]

                            ForEach(0..<myAnswers.count, id: \.self) { i in
                                let match = i < partnerAnswers.count && myAnswers[i] == partnerAnswers[i]
                                HStack {
                                    Text("Q\(i + 1)").font(.system(size: 15)).frame(maxWidth: .infinity, alignment: .leading)
                                    Text(labels[safe: myAnswers[i]] ?? "?").font(.system(size: 15)).frame(width: 40)
                                    Text(i < partnerAnswers.count ? (labels[safe: partnerAnswers[i]] ?? "?") : "-").font(.system(size: 15)).frame(width: 40)
                                    Image(systemName: match ? "checkmark.circle.fill" : "xmark.circle.fill")
                                        .foregroundStyle(match ? AppColors.success : AppColors.error)
                                        .frame(width: 24)
                                }
                                .padding(.horizontal, 16).padding(.vertical, 10)
                                if i < myAnswers.count - 1 { Divider() }
                            }
                        }
                    }

                    // Action
                    PrimaryButton("去看看 TA 的心情") {
                        router.activeSheet = .moodPicker
                    }
                }
            }
            .padding(20)
        }
        .gradientBackground()
        .navigationTitle("答题结果")
        .task { await viewModel.loadResult(date: date) }
    }
}

// Safe array subscript helper
extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
```

Refer to prototype: `azWdO`

- [ ] **Step 7: Run tests, verify pass**

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: implement Quiz module with answer flow and result display"
```

---

## Task 12: Mood Module

**Files:**
- Create: `UniBond/Features/Mood/MoodViewModel.swift`
- Create: `UniBond/Features/Mood/MoodPickerView.swift`

- [ ] **Step 1: Implement MoodViewModel**

```swift
import SwiftUI
import WidgetKit

@MainActor @Observable
class MoodViewModel {
    var selectedEmoji: String?
    var moodText: String = ""
    var isLoading = false
    var errorMessage: String?
    var myMood: MoodResponse?
    var partnerMood: MoodResponse?

    private let apiClient: APIClient

    let emojis = ["😊", "🥰", "😴", "😢", "😤", "🤗", "😎", "🤔", "😋"]

    init(apiClient: APIClient) { self.apiClient = apiClient }

    func updateMood() async -> Bool {
        guard let emoji = selectedEmoji else { return false }
        isLoading = true
        do {
            let response: MoodResponse = try await apiClient.request(
                .updateMood(MoodUpdateRequest(emoji: emoji, text: moodText.isEmpty ? nil : moodText))
            )
            myMood = response
            // Update widget
            WidgetCenter.shared.reloadAllTimelines()
            isLoading = false
            return true
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "更新心情失败"
            isLoading = false
            return false
        }
    }

    func loadPartnerMood() async {
        do {
            partnerMood = try await apiClient.request(.partnerMood)
            if let mood = partnerMood {
                AppSettings.shared.sharedPartnerMoodEmoji = mood.emoji
                AppSettings.shared.sharedPartnerMoodText = mood.text
            }
        } catch { }
    }
}
```

- [ ] **Step 2: Implement MoodPickerView**

```swift
import SwiftUI

struct MoodPickerView: View {
    @Environment(\.dismiss) private var dismiss
    @State var viewModel: MoodViewModel

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // Partner mood display
                    if let mood = viewModel.partnerMood {
                        CardView {
                            VStack(spacing: 8) {
                                Text("TA 的心情").font(.system(size: 13)).foregroundStyle(AppColors.textSecondary)
                                Text(mood.emoji).font(.system(size: 40))
                                if let text = mood.text, !text.isEmpty {
                                    Text(text).font(.system(size: 15)).foregroundStyle(AppColors.textPrimary)
                                }
                                if let time = Date.fromISO8601(mood.updatedAt) {
                                    Text(time.relativeTimeString).font(.system(size: 11)).foregroundStyle(AppColors.textSecondary)
                                }
                            }
                            .padding(20)
                        }
                    }

                    // Emoji grid
                    VStack(alignment: .leading, spacing: 12) {
                        Text("选择你的心情").font(.system(size: 17, weight: .semibold))
                        EmojiGrid(emojis: viewModel.emojis, selected: $viewModel.selectedEmoji)
                    }

                    // Text input
                    VStack(alignment: .leading, spacing: 8) {
                        Text("心情短语（选填）").font(.system(size: 13)).foregroundStyle(AppColors.textSecondary)
                        TextField("今天想说点什么...", text: $viewModel.moodText)
                            .padding(12)
                            .background(.white.opacity(0.8))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .onChange(of: viewModel.moodText) { _, new in
                                if new.count > 50 { viewModel.moodText = String(new.prefix(50)) }
                            }
                        Text("\(viewModel.moodText.count)/50")
                            .font(.system(size: 11))
                            .foregroundStyle(AppColors.textSecondary)
                            .frame(maxWidth: .infinity, alignment: .trailing)
                    }

                    if let error = viewModel.errorMessage {
                        Text(error).font(.system(size: 13)).foregroundStyle(AppColors.error)
                    }

                    PrimaryButton("更新心情", isDisabled: viewModel.selectedEmoji == nil || viewModel.isLoading) {
                        Task {
                            if await viewModel.updateMood() { dismiss() }
                        }
                    }
                }
                .padding(20)
            }
            .gradientBackground()
            .navigationTitle("心情同步")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
            .task { await viewModel.loadPartnerMood() }
        }
    }
}
```

Refer to prototype: `7ZH9p`

- [ ] **Step 3: Verify build**

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: implement Mood module with emoji picker"
```

---

## Task 13: Stats Module

**Files:**
- Create: `UniBond/Features/Stats/StatsViewModel.swift`
- Create: `UniBond/Features/Stats/StatsView.swift`

- [ ] **Step 1: Implement StatsViewModel**

```swift
import SwiftUI

@MainActor @Observable
class StatsViewModel {
    var weekly: WeeklyResponse?
    var overview: OverviewResponse?
    var achievements: [AchievementResponse] = []
    var isLoading = false
    var errorMessage: String?

    private let apiClient: APIClient

    init(apiClient: APIClient) { self.apiClient = apiClient }

    func loadData() async {
        isLoading = true
        do {
            async let weeklyResult: WeeklyResponse = apiClient.request(.weekly)
            async let overviewResult: OverviewResponse = apiClient.request(.overview)
            async let achievementsResult: [AchievementResponse] = apiClient.request(.achievements)
            weekly = try await weeklyResult
            overview = try await overviewResult
            achievements = try await achievementsResult
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "加载统计失败"
        }
        isLoading = false
    }
}
```

- [ ] **Step 2: Implement StatsView**

```swift
import SwiftUI
import Charts

struct StatsView: View {
    @State var viewModel: StatsViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                Text("默契统计")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(AppColors.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)

                // Weekly chart
                if let weekly = viewModel.weekly {
                    CardView {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("本周趋势").font(.system(size: 17, weight: .semibold))
                            Chart(weekly.scores, id: \.date) { day in
                                BarMark(
                                    x: .value("日期", String(day.date.suffix(5))),
                                    y: .value("分数", day.score)
                                )
                                .foregroundStyle(AppColors.primaryGradient)
                                .cornerRadius(4)
                            }
                            .frame(height: 180)
                            .chartYScale(domain: 0...100)
                        }
                        .padding(20)
                    }
                }

                // Stat cards
                if let overview = viewModel.overview {
                    HStack(spacing: 12) {
                        StatCard(value: String(format: "%.0f", overview.avgScore), label: "平均默契分", color: AppColors.primaryPurple)
                        StatCard(value: "\(overview.streakDays)天", label: "连续天数", color: AppColors.success)
                        StatCard(value: "\(overview.totalQuizzes)", label: "累计答题", color: AppColors.primaryPink)
                    }
                }

                // Achievements
                VStack(alignment: .leading, spacing: 12) {
                    Text("成就徽章").font(.system(size: 17, weight: .semibold))
                    if viewModel.achievements.isEmpty {
                        EmptyStateView(icon: "🏆", message: "继续答题解锁更多成就")
                    } else {
                        let columns = Array(repeating: GridItem(.flexible()), count: 4)
                        LazyVGrid(columns: columns, spacing: 16) {
                            ForEach(viewModel.achievements, id: \.type) { achievement in
                                AchievementBadge(
                                    name: achievement.displayName,
                                    icon: achievementIcon(achievement.type),
                                    subtitle: achievement.unlocked ? "已解锁" : "未解锁",
                                    unlocked: achievement.unlocked
                                )
                            }
                        }
                    }
                }

                if viewModel.isLoading { LoadingView() }
            }
            .padding(.horizontal, 20)
        }
        .gradientBackground()
        .task { await viewModel.loadData() }
        .refreshable { await viewModel.loadData() }
    }

    private func achievementIcon(_ type: String) -> String {
        switch type {
        case "FIRST_QUIZ": return "🎯"
        case "STREAK_7": return "🔥"
        case "STREAK_30": return "💎"
        case "PERFECT_SCORE": return "💯"
        case "QUIZ_50": return "📚"
        default: return "🏅"
        }
    }
}
```

Refer to prototype: `uyP5a`

- [ ] **Step 3: Verify build**

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: implement Stats module with charts and achievements"
```

---

## Task 14: Couple Module

**Files:**
- Create: `UniBond/Features/Couple/CoupleViewModel.swift`
- Create: `UniBond/Features/Couple/BindPartnerView.swift`
- Create: `UniBond/Features/Couple/UnbindConfirmView.swift`

- [ ] **Step 1: Implement CoupleViewModel**

```swift
import SwiftUI

@MainActor @Observable
class CoupleViewModel {
    var partnerCode: String = ""
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
        do {
            try await apiClient.requestVoid(.unbind)
            appState.coupleState = .unbound
            isLoading = false
            return true
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? "解绑失败"
            isLoading = false
            return false
        }
    }
}
```

- [ ] **Step 2: Implement BindPartnerView**

```swift
import SwiftUI

struct BindPartnerView: View {
    @Environment(AppRouter.self) private var router
    @State var viewModel: CoupleViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // My invite code
                CardView {
                    VStack(spacing: 12) {
                        Text("我的邀请码").font(.system(size: 13)).foregroundStyle(AppColors.textSecondary)
                        Text(viewModel.myInviteCode)
                            .font(.system(size: 32, weight: .bold, design: .monospaced))
                            .foregroundStyle(AppColors.primaryPurple)
                            .kerning(4)

                        HStack(spacing: 16) {
                            Button {
                                UIPasteboard.general.string = viewModel.myInviteCode
                            } label: {
                                Label("复制", systemImage: "doc.on.doc").font(.system(size: 13))
                            }
                            ShareLink(item: "来 UniBond 和我一起答题吧！我的邀请码：\(viewModel.myInviteCode)") {
                                Label("分享", systemImage: "square.and.arrow.up").font(.system(size: 13))
                            }
                        }
                        .foregroundStyle(AppColors.primaryPurple)
                    }
                    .padding(20)
                }

                // Divider
                HStack {
                    Rectangle().fill(AppColors.textSecondary.opacity(0.3)).frame(height: 0.5)
                    Text("或").font(.system(size: 13)).foregroundStyle(AppColors.textSecondary)
                    Rectangle().fill(AppColors.textSecondary.opacity(0.3)).frame(height: 0.5)
                }

                // Input partner code
                CardView {
                    VStack(spacing: 12) {
                        Text("输入对方邀请码").font(.system(size: 15, weight: .medium))
                        TextField("6 位邀请码", text: $viewModel.partnerCode)
                            .font(.system(size: 20, weight: .semibold, design: .monospaced))
                            .multilineTextAlignment(.center)
                            .textInputAutocapitalization(.characters)
                            .padding(12)
                            .background(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(AppColors.primaryPurple.opacity(0.3), lineWidth: 1))
                            .onChange(of: viewModel.partnerCode) { _, new in
                                viewModel.partnerCode = String(new.prefix(6)).uppercased()
                            }

                        if let error = viewModel.errorMessage {
                            Text(error).font(.system(size: 13)).foregroundStyle(AppColors.error)
                        }

                        PrimaryButton("绑定 TA", icon: "link", isDisabled: viewModel.partnerCode.count != 6 || viewModel.isLoading) {
                            Task {
                                if await viewModel.bind() {
                                    router.homePath = NavigationPath()
                                }
                            }
                        }
                    }
                    .padding(20)
                }
            }
            .padding(20)
        }
        .gradientBackground()
        .navigationTitle("绑定伴侣")
    }
}
```

Refer to prototype: `jNnXr`

- [ ] **Step 3: Implement UnbindConfirmView**

```swift
import SwiftUI

struct UnbindConfirmView: View {
    @Environment(\.dismiss) private var dismiss
    @State var viewModel: CoupleViewModel

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Text("💔").font(.system(size: 56))
            Text("确认解绑？")
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(AppColors.textPrimary)
            Text("解绑后，你们的答题记录和统计数据将被清除，此操作不可恢复。")
                .font(.system(size: 15))
                .foregroundStyle(AppColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 20)

            if let error = viewModel.errorMessage {
                Text(error).font(.system(size: 13)).foregroundStyle(AppColors.error)
            }

            Spacer()

            VStack(spacing: 12) {
                Button {
                    Task {
                        if await viewModel.unbind() { dismiss() }
                    }
                } label: {
                    Text("确认解绑")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(AppColors.error)
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                }
                .disabled(viewModel.isLoading)

                SecondaryButton("再想想") { dismiss() }
            }
        }
        .padding(32)
        .gradientBackground()
    }
}
```

Refer to prototype: `vIsnc`

- [ ] **Step 4: Verify build**

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: implement Couple module with bind/unbind flow"
```

---

## Task 15: Profile Module

**Files:**
- Create: `UniBond/Features/Profile/ProfileViewModel.swift`
- Create: `UniBond/Features/Profile/ProfileView.swift`

- [ ] **Step 1: Implement ProfileViewModel**

```swift
import SwiftUI

@MainActor @Observable
class ProfileViewModel {
    var nickname: String = ""
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
        guard let couple = appState.currentCouple,
              let date = Date.fromISO8601(couple.bindAt) else { return nil }
        return Calendar.current.dateComponents([.day], from: date, to: Date()).day
    }
}
```

- [ ] **Step 2: Implement ProfileView**

```swift
import SwiftUI

struct ProfileView: View {
    @Environment(AppState.self) private var appState
    @Environment(AppRouter.self) private var router
    @State var viewModel: ProfileViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Avatar + Name
                VStack(spacing: 12) {
                    Circle()
                        .fill(AppColors.primaryGradient)
                        .frame(width: 72, height: 72)
                        .overlay(
                            Text(String(appState.currentUser?.nickname.prefix(1) ?? "U"))
                                .font(.system(size: 28, weight: .bold))
                                .foregroundStyle(.white)
                        )

                    if viewModel.isEditing {
                        HStack {
                            TextField("昵称", text: $viewModel.nickname)
                                .textFieldStyle(.roundedBorder)
                                .frame(width: 150)
                            Button("保存") { Task { await viewModel.updateNickname() } }
                                .font(.system(size: 13, weight: .medium))
                                .foregroundStyle(AppColors.primaryPurple)
                        }
                    } else {
                        HStack(spacing: 6) {
                            Text(appState.currentUser?.nickname ?? "")
                                .font(.system(size: 20, weight: .semibold))
                            Button { viewModel.isEditing = true } label: {
                                Image(systemName: "pencil").font(.system(size: 13)).foregroundStyle(AppColors.textSecondary)
                            }
                        }
                    }

                    if let email = appState.currentUser?.email {
                        Text(email).font(.system(size: 13)).foregroundStyle(AppColors.textSecondary)
                    }
                }
                .padding(.top, 12)

                // Couple info (bound only)
                if appState.isBound, let couple = appState.currentCouple {
                    CardView {
                        VStack(spacing: 12) {
                            HStack {
                                Text("💑").font(.system(size: 24))
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("与 \(couple.partnerNickname) 已绑定")
                                        .font(.system(size: 15, weight: .medium))
                                    if let days = viewModel.daysTogether {
                                        Text("在一起 \(days) 天")
                                            .font(.system(size: 13))
                                            .foregroundStyle(AppColors.textSecondary)
                                    }
                                }
                                Spacer()
                            }
                            HStack {
                                Text("邀请码：\(appState.currentUser?.inviteCode ?? "")")
                                    .font(.system(size: 13, design: .monospaced))
                                    .foregroundStyle(AppColors.textSecondary)
                                Spacer()
                            }
                        }
                        .padding(16)
                    }
                } else {
                    // Unbound — bind CTA
                    CardView {
                        VStack(spacing: 12) {
                            Text("💕").font(.system(size: 32))
                            Text("绑定伴侣，开启默契之旅")
                                .font(.system(size: 15))
                                .foregroundStyle(AppColors.textSecondary)
                            PrimaryButton("去绑定", icon: "link") {
                                router.profilePath.append(AppRoute.bindPartner)
                            }
                        }
                        .padding(16)
                    }
                }

                // Settings list
                CardView {
                    VStack(spacing: 0) {
                        settingsRow(icon: "bell.fill", title: "通知设置") { }
                        Divider().padding(.leading, 44)

                        if appState.isBound {
                            settingsRow(icon: "lock.shield.fill", title: "隐私") { }
                            Divider().padding(.leading, 44)
                            settingsRow(icon: "bubble.left.fill", title: "意见反馈") { }
                            Divider().padding(.leading, 44)
                        }

                        settingsRow(icon: "info.circle.fill", title: "关于 UniBond") { }
                        Divider().padding(.leading, 44)

                        if appState.isBound {
                            settingsRow(icon: "heart.slash.fill", title: "解绑伴侣", color: .orange) {
                                router.activeSheet = .unbindConfirm
                            }
                            Divider().padding(.leading, 44)
                        }

                        settingsRow(icon: "trash.fill", title: "删除账号", color: AppColors.error) {
                            viewModel.showDeleteConfirm = true
                        }
                    }
                }

                if let error = viewModel.errorMessage {
                    Text(error).font(.system(size: 13)).foregroundStyle(AppColors.error)
                }

                // Logout
                SecondaryButton("退出登录") {
                    Task { await viewModel.logout() }
                }
                .padding(.top, 8)
            }
            .padding(.horizontal, 20)
        }
        .gradientBackground()
        .alert("确认删除账号？", isPresented: $viewModel.showDeleteConfirm) {
            Button("取消", role: .cancel) { }
            Button("删除", role: .destructive) {
                Task { await viewModel.deleteAccount() }
            }
        } message: {
            Text("删除后所有数据将被永久清除，无法恢复。")
        }
    }

    private func settingsRow(icon: String, title: String, color: Color = AppColors.textPrimary, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 16))
                    .foregroundStyle(color)
                    .frame(width: 24)
                Text(title)
                    .font(.system(size: 15))
                    .foregroundStyle(color)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundStyle(AppColors.textSecondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
    }
}
```

Refer to prototypes: `XiJiH` (bound), `tgTZX` (unbound)

- [ ] **Step 3: Verify build**

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: implement Profile module with bound/unbound states"
```

---

## Task 16: App Entry & Main Tab

**Files:**
- Create: `UniBond/UniBondApp.swift`

- [ ] **Step 1: Implement UniBondApp and MainTabView**

App entry point:
- Creates `AppState`, `APIClient`, `AppRouter` as `@State`
- Injects into environment
- Root view switches: `LoginView` vs `MainTabView` based on `appState.authState`
- `MainTabView` has 3 tabs with `NavigationStack` each
- Handle `scenePhase` for timezone check: when `.active`, compare `TimeZone.current.identifier` with `AppSettings.shared.lastTimezone`; if different, call `PUT /api/v1/user/profile` with new timezone and update cached value

```swift
// In UniBondApp, add to .onChange(of: scenePhase):
if scenePhase == .active, appState.isAuthenticated {
    let tz = TimeZone.current.identifier
    if tz != AppSettings.shared.lastTimezone {
        AppSettings.shared.lastTimezone = tz
        Task {
            try? await apiClient.requestVoid(.updateProfile(ProfileUpdateRequest(nickname: nil, avatarUrl: nil, timezone: tz)))
        }
    }
}
```
- Register for push notifications on launch
- Implement `UNUserNotificationCenterDelegate` for notification routing

- [ ] **Step 2: Wire up all navigation routes**

Ensure all `AppRoute` cases map to correct destination views within NavigationStack `.navigationDestination`.

- [ ] **Step 3: Full app run in simulator — test login → home → quiz → mood → stats → profile flow**

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: implement app entry point and main tab navigation"
```

---

## Task 17: Widget Extension

**Files:**
- Create: `UniBondWidget/WidgetSmall.swift`
- Create: `UniBondWidget/WidgetMedium.swift`
- Create: `UniBondWidget/WidgetDataProvider.swift`
- Create: `UniBondWidget/UniBondWidgetBundle.swift`

- [ ] **Step 1: Implement WidgetDataProvider**

`TimelineProvider` that reads from App Group `UserDefaults`. Returns `.after(Date().addingTimeInterval(7200))` (2 hours).

- [ ] **Step 2: Implement WidgetSmall**

170x170: score number, streak days, quiz answered badge. Match prototype `tyXJ5`.

- [ ] **Step 3: Implement WidgetMedium**

364x170: score, quiz type label, partner mood, answered status. Match prototype `FzmeK`.

- [ ] **Step 4: Implement WidgetBundle**

Register both widget families.

- [ ] **Step 5: Build widget target, verify in simulator**

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: implement Widget extension with small and medium sizes"
```

---

## Task 18: Live Activity

**Files:**
- Create: `UniBondLiveActivity/MoodLiveActivity.swift`

- [ ] **Step 1: Define ActivityAttributes**

```swift
import ActivityKit

struct MoodActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        var partnerEmoji: String
        var partnerText: String?
        var updatedAt: String
    }
    var partnerName: String
}
```

- [ ] **Step 2: Implement Live Activity UI**

Lock screen widget showing partner mood with emoji + text + relative time.

- [ ] **Step 3: Add start/update/end logic in MoodViewModel**

Start Live Activity when couple is bound, update on mood changes, end on unbind.

- [ ] **Step 4: Verify build**

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: implement Live Activity for mood sync"
```

---

## Task 19: Push Notification Registration & Handling

**Files:**
- Modify: `UniBond/UniBondApp.swift`

- [ ] **Step 1: Add APNs registration in AppDelegate**

Request push permission, get device token, send to backend via `POST /api/v1/user/device-token`.

- [ ] **Step 2: Implement notification tap handling**

Parse notification payload `type` field, route via `AppRouter.handleNotification(type:)`.

- [ ] **Step 3: Test notification registration in simulator**

Note: Actual push delivery requires physical device + APNs certificate.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add push notification registration and routing"
```

---

## Task 20: Final Integration & Polish

- [ ] **Step 1: Run full app flow test in simulator**

Test complete user journey:
1. Launch → Login screen
2. Apple/email login → Home (unbound)
3. Enter invite code → bind → Home (bound)
4. Start quiz → answer 5 questions → waiting → result
5. Update mood → see partner mood
6. Check stats → see chart + achievements
7. Profile → edit nickname → unbind → logout

- [ ] **Step 2: Verify offline banner shows when network disconnected**

- [ ] **Step 3: Verify empty states for new couples**

- [ ] **Step 4: Run all unit tests**

```bash
xcodebuild test -scheme UniBond -destination 'platform=iOS Simulator,name=iPhone 16'
```
Expected: All tests pass

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "feat: complete UniBond iOS client integration"
```
