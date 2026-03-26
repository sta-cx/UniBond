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
struct UserResponse: Codable {
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
}

// MARK: - Couple
struct CoupleResponse: Codable {
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
        KeychainManager.shared.deleteTokens()
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
        let request = try client.buildRequest(for: endpoint)
        XCTAssertEqual(request.url?.absoluteString, "https://example.com/api/v1/user/me")
        XCTAssertEqual(request.httpMethod, "GET")
    }

    func testBuildURLRequestWithBody() async throws {
        let client = APIClient(baseURL: "https://example.com")
        let body = EmailSendRequest(email: "test@example.com")
        let endpoint = APIEndpoint.emailSend(body)
        let request = try client.buildRequest(for: endpoint)
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
            .background(isDisabled ? Color.gray.opacity(0.4) : AnyShapeStyle(AppColors.primaryGradient))
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

@Observable
class AuthViewModel {
    var loginStep: LoginStep = .initial
    var email: String = ""
    var code: String = ""
    var isLoading = false
    var errorMessage: String?
    var countdown: Int = 0

    private let apiClient: APIClient
    private let appState: AppState
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
    @State private var viewModel: AuthViewModel
    @State private var authService = AuthService()

    init(apiClient: APIClient) {
        _viewModel = State(initialValue: AuthViewModel(apiClient: apiClient, appState: AppState()))
    }

    var body: some View {
        ZStack {
            AppColors.backgroundGradient.ignoresSafeArea()

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
        .onAppear { viewModel = AuthViewModel(apiClient: viewModel.apiClient, appState: appState) }
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
                            await viewModel.loginWithApple(identityToken: token, nickname: credential.fullName?.givenName)
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
                viewModel.loginStep = .emailInput
            }
        }
    }

    private var emailInputSection: some View {
        VStack(spacing: 16) {
            TextField("请输入邮箱", text: $viewModel.email)
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
                .autocapitalization(.none)
                .padding(16)
                .background(.white.opacity(0.8))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            PrimaryButton("获取验证码", isDisabled: !viewModel.isEmailValid || viewModel.isLoading) {
                Task { await viewModel.sendCode() }
            }
        }
    }

    private var codeInputSection: some View {
        VStack(spacing: 16) {
            TextField("请输入 6 位验证码", text: $viewModel.code)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .padding(16)
                .background(.white.opacity(0.8))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            PrimaryButton("登录", isDisabled: !viewModel.isCodeValid || viewModel.isLoading) {
                Task { await viewModel.loginWithEmail() }
            }

            if viewModel.countdown > 0 {
                Text("重新发送 (\(viewModel.countdown)s)")
                    .font(.system(size: 13))
                    .foregroundStyle(AppColors.textSecondary)
            } else {
                Button("重新发送验证码") { Task { await viewModel.sendCode() } }
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

HomeViewModel loads overview data, quiz status, and mood. It manages the 5 home screen states:
- Unbound → show bind prompt
- Quiz available → show "start quiz" card
- Answered, waiting → show "waiting for partner"
- Waiting to reveal → show "waiting to reveal" + nudge button
- Revealed → show score summary

Key methods:
- `loadData()` — calls overview, quiz today, partner mood
- `startPolling()` / `stopPolling()` — 30s poll for quiz result when waiting
- Quiz state derived from `QuizResponse` + `QuizResultResponse`

- [ ] **Step 2: Implement HomeView**

HomeView switches between 5 states based on ViewModel state. Includes:
- Greeting header with time-based emoji
- Quiz card (state-dependent)
- Mood sync section (both user's + partner's mood)
- 3 stat cards at bottom (today score, streak, achievements)
- Navigation to QuizAnswerView, MoodPicker sheet

Refer to prototype screens: `kT6Sk`, `hSqE6`, `QHBTJ`, `rPGgW`, `VSDcp`

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

Manages quiz state machine: load questions → select answers → submit → wait → result. Parses `questions` JSON string into `[QuizQuestion]`. Handles 30s polling when waiting.

- [ ] **Step 4: Implement QuizAnswerView**

5-question flow with progress bar (`问题 N/5`), question text, 4 option rows. Navigation bar with back + close. Submit when all 5 answered.

Refer to prototype: `noWcd`

- [ ] **Step 5: Implement QuizWaitingView**

Shows completion checkmark, partner name + "尚未作答" status, "返回首页" button.

Refer to prototype: `zWolL`

- [ ] **Step 6: Implement QuizResultView**

Score circle (0-100), answer comparison table (Q1-Q5 with 我/TA columns, match highlight), "去看看 TA 的心情" link.

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
@Observable
class MoodViewModel {
    var selectedEmoji: String?
    var moodText: String = ""
    var isLoading = false
    var myMood: MoodResponse?
    var partnerMood: MoodResponse?

    private let apiClient: APIClient

    let emojis = ["😊", "🥰", "😴", "😢", "😤", "🤗", "😎", "🤔", "😋"]

    init(apiClient: APIClient) { self.apiClient = apiClient }

    func updateMood() async { /* POST /api/v1/mood, cache response locally */ }
    func loadPartnerMood() async { /* GET /api/v1/mood/partner */ }
}
```

- [ ] **Step 2: Implement MoodPickerView**

Sheet with: partner mood display, 3x3 EmojiGrid, text input (50 char limit), "更新心情" PrimaryButton.

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

Loads weekly data (for Swift Charts bar chart), overview stats, and achievement list.

- [ ] **Step 2: Implement StatsView**

Layout: page title "默契统计" + week/all toggle, bar chart (Swift Charts `BarMark`), 3 StatCards, achievement badge grid.

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

Handles bind (POST invite code), unbind (DELETE + confirmation), and ShareLink for invite code.

- [ ] **Step 2: Implement BindPartnerView**

Shows my invite code (large text, copy + share buttons), divider "或", input field for partner's code, "绑定 TA" button.

Refer to prototype: `jNnXr`

- [ ] **Step 3: Implement UnbindConfirmView**

Confirmation sheet with broken heart icon, warning text, "确认解绑" destructive button, "再想想" cancel button.

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

Handles: profile edit (nickname), logout (clear tokens + reset AppState), account deletion (DELETE with confirmation), couple info display.

- [ ] **Step 2: Implement ProfileView**

Two states:
- **Bound**: avatar + name + email, couple info card (partner name, days together, invite code), settings list (notifications, privacy, feedback, about, delete account), logout button
- **Unbound**: avatar + name + email, bind CTA card, simplified settings list, logout button

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
- Handle `scenePhase` for timezone check
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
