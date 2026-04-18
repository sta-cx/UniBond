# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

UniBond is a couples social app with daily quizzes and mood sync. Monorepo containing:
- **unibond-server/** — Spring Boot 3.4.3 / Java 17 REST API backend
- **ios/** — SwiftUI iOS client (iOS 17+), built with XcodeGen (`project.yml`)

## Build & Run Commands

### Backend (unibond-server)

```bash
# Start infrastructure (PostgreSQL 15 + Redis 7)
cd unibond-server && docker compose up -d

# Run with dev profile (default, connects to PostgreSQL/Redis on 39.105.121.125)
cd unibond-server && mvn spring-boot:run

# Build JAR
cd unibond-server && mvn package -DskipTests

# Run all tests (H2 in-memory DB, profile=test, Flyway disabled)
cd unibond-server && mvn test

# Run a single test class or method
cd unibond-server && mvn test -Dtest=QuizServiceTest
cd unibond-server && mvn test -Dtest=QuizServiceTest#testGetTodayQuiz

# Docker build
cd unibond-server && docker build -t unibond-server .
```

### iOS Client

Uses XcodeGen — if `UniBond.xcodeproj` is missing or stale, regenerate it:
```bash
cd ios && xcodegen generate
```

```bash
# Build (adjust simulator name to your available runtimes)
cd ios && xcodebuild build -scheme UniBond -destination 'platform=iOS Simulator,name=iPhone 17 Pro'

# Run tests
cd ios && xcodebuild test -scheme UniBond -destination 'platform=iOS Simulator,name=iPhone 17 Pro'
```

## Architecture

### Backend

Single Spring Boot monolith with feature-based package structure under `com.unibond`:

| Package | Purpose |
|---------|---------|
| `config/` | SecurityConfig, RedisConfig, ApnsConfig, WebConfig (CORS, timezone), JacksonConfig |
| `auth/` | Apple Sign-In + email code auth, JWT tokens (access 2h + refresh 30d in Redis) |
| `user/` | User CRUD, device token registration |
| `couple/` | Couple binding via 6-char invite code |
| `quiz/` | Daily quiz generation (scheduled task `Day % 3` rotates BLIND/GUESS/THEME) + answer/scoring |
| `mood/` | Real-time mood status between partners |
| `stats/` | Weekly stats + achievement system |
| `push/` | APNs push via Pushy 0.15.4, Live Activity pushes |
| `common/` | ApiResponse wrapper, ErrorCode enum, GlobalExceptionHandler, JwtProvider/JwtAuthFilter, InputSanitizer, RateLimitInterceptor |

**API conventions:**
- Base path: `/api/v1/`
- Success: `{data: T}`, Error: `{code, message, timestamp}`
- Auth: Bearer JWT in header, `@CurrentUserId` annotation injects user ID into controllers
- All timestamps are UTC (Jackson + server configured)
- Jackson `non_null` inclusion — nullable fields are omitted from responses; iOS models must use `Optional` for fields that can be null
- Error codes: centralized in `ErrorCode` enum with Chinese messages
- Rate limiting: Bucket4j with in-memory `ConcurrentHashMap` (per-IP on auth, per-user on authenticated)
- Flyway migrations in `src/main/resources/db/migration/`
- Hibernate naming: `CamelCaseToUnderscoresNamingStrategy` — use `@Column` annotations when column names differ from field names

### iOS Client

MVVM with SwiftUI, targeting iOS 17+:

| Layer | Description |
|-------|-------------|
| `Core/Network/` | `actor`-based `APIClient` with async/await, automatic token refresh with continuation coalescing, `APIEndpoint` static factory |
| `Core/Storage/` | `KeychainManager` (tokens), `AppSettings` (UserDefaults + App Group shared defaults for widget data) |
| `Core/LiveActivity/` | ActivityKit `MoodActivityTypes` + `MoodLiveActivityManager` |
| `Features/` | Auth, Couple, Quiz, Mood, Stats, Profile, Home — each with `@Observable` ViewModel |
| `SharedUI/` | Reusable components (GradientBackground, CardView, PrimaryButton, EmojiGrid, etc.) |
| `Navigation/` | `AppState` (auth/couple/network state) + `AppRouter` (3 NavigationPaths, tab selection, sheet routing) |
| `UniBondWidget/` | WidgetKit Small + Medium widgets, reads from App Group UserDefaults |
| `UniBondLiveActivity/` | Lock screen / Dynamic Island mood display |

**Key patterns:**
- **Observation framework** (`@Observable` macro) — not `ObservableObject`/`@Published`
- **Environment injection** — `AppState` and `AppRouter` passed via `.environment()`, not dependency injection
- **Tab architecture** — all 3 tab NavigationStacks are kept alive in a ZStack with opacity control (avoids view destruction on tab switch)
- **State machine** — `QuizCardState` enum with 6 cases (unbound/noQuiz/available/answeredWaiting/waitingRevealed/revealed) drives the home view quiz card
- **Atomic state updates** — always set `authState` and `coupleState` together; setting `authState` alone triggers immediate UI re-render before `coupleState` is ready
- **Widget data flow** — ViewModel writes to `AppSettings.shared` (App Group UserDefaults) → Widget reads via `WidgetDataProvider` → call `WidgetCenter.shared.reloadAllTimelines()` after data changes
- **Push notifications** — `PushCoordinator` singleton handles token registration and notification routing; custom `type` field injected into APNs payload via string manipulation (Pushy's `SimpleApnsPayloadBuilder` has no `addCustomData` method)

## Configuration

- **Backend profiles:** `dev` (hardcoded DB/Redis at 39.105.121.125), `prod` (env vars), `test` (H2 in-memory, Flyway disabled, create-drop DDL)
- **Maven mirrors:** `maven-settings.xml` configures Aliyun mirror for dependency resolution in China
- **Production env vars:** See `unibond-server/.env.example` (DB, Redis, JWT, Mail, APNs)

## Common Gotchas

- **Jackson `non_null` + iOS Codable:** If backend field is null, it's omitted from JSON. iOS `Codable` models must declare these as `Optional` (`String?`) or decoding fails silently.
- **`RateLimitInterceptor` ObjectMapper:** Creates its own `ObjectMapper()` — must register `JavaTimeModule` or `Instant` serialization fails, causing 500 instead of 429.
- **Quiz not generated:** New test accounts have no quiz yet. `loadQuizState()` must handle quiz-not-found gracefully (`.noQuiz` state), not fall back to `.unbound`.
- **H2 test vs PostgreSQL:** Test profile uses H2 which doesn't support PostgreSQL-specific features. Migrations with PostgreSQL-specific syntax will fail in tests.
