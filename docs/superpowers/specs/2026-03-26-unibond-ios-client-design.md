# UniBond iOS Client Design Specification

## Overview

UniBond iOS 客户端，使用 SwiftUI 实现，对接已有 Spring Boot 后端 API。覆盖全部 MVP 功能：登录、情侣绑定、每日问答、心情同步、统计成就、iOS Widget 和 Live Activity。

## Technical Stack

- **Language:** Swift 5.9+
- **UI Framework:** SwiftUI, iOS 17+
- **State Management:** `@Observable` / `@Environment`
- **Networking:** 原生 `URLSession` + `async/await`
- **Local Storage:** `Keychain` (JWT Token) + `UserDefaults` (偏好设置)
- **Push:** APNs (直连，后端已实现)
- **Widgets:** WidgetKit (Small + Medium)
- **Live Activity:** ActivityKit (心情同步)
- **Charts:** Swift Charts (iOS 17 原生)

## Project Structure

```
UniBond/
├── UniBondApp.swift                # App 入口、路由
├── Core/
│   ├── Network/
│   │   ├── APIClient.swift         # 统一请求客户端 (async/await, actor)
│   │   ├── APIEndpoint.swift       # 接口定义枚举
│   │   ├── AuthInterceptor.swift   # JWT 自动附加 & Token 刷新
│   │   └── APIError.swift          # 错误类型映射
│   ├── Storage/
│   │   ├── KeychainManager.swift   # Token 安全存储
│   │   └── AppSettings.swift       # UserDefaults 封装
│   └── Extensions/                 # Date, String 等通用扩展
├── Features/
│   ├── Auth/
│   │   ├── LoginView.swift
│   │   ├── AuthViewModel.swift
│   │   └── AuthService.swift       # Apple Sign-in + 邮箱登录
│   ├── Couple/
│   │   ├── BindPartnerView.swift
│   │   ├── UnbindConfirmView.swift
│   │   └── CoupleViewModel.swift
│   ├── Quiz/
│   │   ├── QuizAnswerView.swift
│   │   ├── QuizResultView.swift
│   │   ├── QuizWaitingView.swift
│   │   └── QuizViewModel.swift
│   ├── Mood/
│   │   ├── MoodPickerView.swift
│   │   └── MoodViewModel.swift
│   ├── Stats/
│   │   ├── StatsView.swift
│   │   └── StatsViewModel.swift
│   ├── Profile/
│   │   ├── ProfileView.swift
│   │   └── ProfileViewModel.swift
│   └── Home/
│       ├── HomeView.swift          # 首页（多状态切换）
│       └── HomeViewModel.swift
├── SharedUI/
│   ├── GradientBackground.swift    # 粉紫渐变背景
│   ├── TabBarView.swift            # 自定义底部 Tab
│   ├── CardView.swift              # 通用卡片容器
│   ├── PrimaryButton.swift         # 渐变主按钮
│   ├── SecondaryButton.swift       # 白底描边次按钮
│   ├── StatCard.swift              # 统计数字卡片
│   ├── EmojiGrid.swift             # 心情 Emoji 选择网格
│   ├── QuizOptionRow.swift         # 答题选项行
│   ├── AchievementBadge.swift      # 成就徽章
│   └── LoadingView.swift
├── Navigation/
│   ├── AppRouter.swift             # 全局路由/导航
│   └── AppState.swift              # 全局状态 (登录态、绑定态)
├── UniBondWidget/                  # Widget Extension Target
│   ├── WidgetSmall.swift
│   ├── WidgetMedium.swift
│   └── WidgetDataProvider.swift
└── UniBondLiveActivity/            # Live Activity Extension Target
    └── MoodLiveActivity.swift
```

## Navigation & App State

### Global State Machine

应用有 3 个顶层状态，决定用户看到的界面：

```
┌──────────┐    登录成功    ┌──────────┐    绑定成功    ┌──────────┐
│  未登录   │ ──────────→ │  未绑定   │ ──────────→ │  已绑定   │
│ (Login)  │             │ (Unbound) │             │ (Bound)  │
└──────────┘             └──────────┘             └──────────┘
     ↑                        ↑                        │
     │         退出登录        │        解绑伴侣         │
     ←────────────────────────←────────────────────────┘
```

### AppState Design

```swift
@Observable
class AppState {
    var authState: AuthState = .unauthenticated  // .unauthenticated / .authenticated(User)
    var coupleState: CoupleState = .unbound      // .unbound / .bound(Couple)
}
```

### Navigation Structure

- **未登录** → `LoginView`（全屏）
- **已登录未绑定** → `MainTabView`，首页显示绑定引导卡片，问答/统计锁定
- **已登录已绑定** → `MainTabView`，全部功能可用

### Tab Bar (3 Tabs)

| Tab | 页面 | 说明 |
|-----|------|------|
| 首页 | HomeView | 每日概览、问答入口、心情同步 |
| 统计 | StatsView | 趋势图、成就 |
| 我的 | ProfileView | 个人信息、设置 |

> 注：问答流程通过首页 NavigationStack push 进入，不作为独立 Tab。

### Page Navigation (NavigationStack)

- 首页 → 答题页 → 等待页 → 结果页（push 链）
- 首页 → 心情选择页（sheet 弹出）
- 个人页 → 绑定伴侣页（push）
- 个人页 → 解绑确认（sheet / alert）
- 首页 → Widget 权限弹窗（overlay）

## Core Data Flow & Networking

### APIClient

```swift
actor APIClient {
    func request<T: Decodable>(_ endpoint: APIEndpoint) async throws -> T
    func requestVoid(_ endpoint: APIEndpoint) async throws
}
```

### JWT Token Auto-Management

```
请求发出 → AuthInterceptor 附加 Access Token
    ↓
  401 响应？
    ├─ 是 → 用 Refresh Token 刷新 → 重试原请求
    │         ↓
    │       刷新也失败？→ 清除 Token → 跳转登录页
    └─ 否 → 返回数据
```

- Access Token 存 Keychain，每次请求自动附加到 `Authorization: Bearer xxx`
- Refresh Token 存 Keychain，401 时自动刷新（后端 2 小时过期，30 天 Refresh）
- Token 刷新使用 `actor` 隔离，保证并发请求只触发一次刷新

### API Endpoints

| 模块 | 接口 | HTTP |
|------|------|------|
| Auth | 邮箱发送验证码 / 邮箱登录 / Apple 登录 / 刷新 Token / 登出 | POST |
| User | 获取我的信息 / 更新昵称头像 | GET / PUT |
| Couple | 绑定伴侣 / 解绑 / 获取情侣信息 | POST / DELETE / GET |
| Quiz | 获取今日问答 / 提交答案 / 获取结果 | GET / POST |
| Mood | 获取伴侣心情 / 更新我的心情 | GET / POST |
| Stats | 获取统计 / 获取成就列表 | GET |
| Push | 注册 Device Token | POST |
| User | 删除账号 | DELETE |

### Error Handling

后端返回统一格式 `{code, message, timestamp}`，iOS 端映射为：

```swift
enum APIError: Error {
    case unauthorized          // 401 → 跳转登录
    case forbidden             // 403 → 无权限，刷新状态
    case coupleNotBound        // COUPLE_NOT_BOUND → 引导绑定
    case quizAlreadyAnswered   // QUIZ_ALREADY_ANSWERED → 显示已答
    case rateLimited           // 429 → 提示稍后重试
    case serverError(String)   // 500 → 通用错误提示
    case networkError          // 无网络
}
```

## Feature Module Details

### Auth Module

**Apple Sign-in:**
1. 调用 `AuthenticationServices` 获取 `identityToken` + `authorizationCode`
2. 发送到后端 `/api/auth/apple`，附带 `TimeZone.current.identifier` → 返回 JWT Token 对
3. 存入 Keychain，更新 AppState → 进入主界面

**邮箱验证码登录：**
1. 输入邮箱 → 调用发送验证码接口（60 秒倒计时）
2. 输入 6 位验证码 → 调用登录接口，附带 `TimeZone.current.identifier` → 返回 JWT Token
3. 同上存储并进入主界面

**时区同步：** 每次 App 进入前台（`scenePhase == .active`），检查时区是否变化，若变化则调用 `PUT /api/v1/user/profile` 更新。

LoginView 内部包含两个子状态：Apple 登录按钮 + 邮箱验证码流程（输入邮箱 → 输入验证码，作为 LoginView 内的视图切换，不需要独立页面）。

### Couple Module

**绑定流程：**
- 未绑定用户看到自己的 6 位邀请码（可复制/分享）
- 输入对方邀请码 → 调用绑定接口 → 成功后刷新 AppState.coupleState
- 使用 `ShareLink(item:)` 做系统分享（SwiftUI 原生）

**解绑流程：**
- ProfileView 点击解绑 → 弹出确认弹窗
- 确认后调用解绑接口 → 清除情侣状态 → 回到未绑定首页

### Quiz Module

**状态机：**

```
今日问答可用 → 答题中(5题逐题) → 已提交等待对方 → 双方完成可揭晓 → 查看结果
```

| 状态 | 对应屏幕 | 数据来源 |
|------|---------|---------|
| 可答题 | HomeView 显示"开始答题"卡片 | GET /api/quiz/today |
| 答题中 | QuizAnswerView (逐题切换) | 本地状态 |
| 已提交等待 | QuizWaitingView | 后端返回 revealed=false |
| 等待揭晓 | HomeView "等待开奖" | 轮询 GET /api/v1/quiz/result/{date}，每 30 秒一次；推送通知作为辅助触发 |
| 已揭晓 | QuizResultView (分数+对比) | GET /api/quiz/result |

**答题交互：** 选择选项后高亮，5 题答完一次性提交。

### Mood Module

- 9 个 Emoji 网格选择
- 可选填心情短语（50 字以内）
- 点击"更新心情"调用 POST /api/v1/mood 接口
- 用户自己的心情从 POST 响应中获取并本地缓存
- 伴侣心情通过 GET /api/v1/mood/partner 获取
- 首页实时显示双方心情状态
- Live Activity 展示伴侣最新心情（ActivityKit）

### Stats Module

- 周趋势柱状图（Swift Charts）
- 3 个统计卡片：平均默契分、连续天数、累计答题
- 成就徽章网格（已解锁高亮，未解锁灰色+锁图标）

### Profile Module

**已绑定状态：** 头像昵称编辑、伴侣信息（天数计算）、邀请码展示、解绑入口、设置列表（通知/隐私/反馈/关于/删除账号）、退出登录

**未绑定状态：** 头像昵称编辑、绑定引导卡片、精简设置列表（通知/关于/删除账号）、退出登录

**删除账号流程：** 设置中点击"删除账号" → 二次确认弹窗（destructive 样式）→ 调用 `DELETE /api/v1/user/account` → 清除本地数据 → 跳转登录页。此功能为 App Store 审核必需。

### Widget & Live Activity

**Widget Small (170x170)：** 默契分 + 连续天数 + 答题状态

**Widget Medium (364x170)：** 默契分 + 今日模式 + 伴侣心情 + 答题状态

**数据更新：** 通过 App Group 共享 UserDefaults，答题/心情更新后调用 `WidgetCenter.shared.reloadAllTimelines()`

**Widget 时间线策略：** `.after(Date)` 返回每 2 小时刷新一次的时间线，答题提交和心情更新后额外触发 `reloadAllTimelines()`。

**Live Activity：** 心情更新后通过 APNs 推送更新锁屏上的心情状态展示

## UI Design System

### Color Palette

| 用途 | 颜色 | 说明 |
|------|------|------|
| 主色 | `#A855F7` → `#EC4899` | 紫到粉渐变，按钮和强调 |
| 背景 | `#F3E8FF` → `#FCE7F3` | 浅紫到浅粉渐变，全局背景 |
| 卡片背景 | `#FFFFFF` (80% opacity) | 毛玻璃/半透明白 |
| 文字主色 | `#1F1F1F` | 标题、正文 |
| 文字次色 | `#6B7280` | 副标题、说明 |
| 成功 | `#10B981` | 答对、匹配 |
| 错误/强调 | `#E11D48` | 解绑、不匹配 |

### Corner Radius

- 卡片：16px
- 按钮：12px（大按钮全圆角 24px）
- 输入框：12px
- 头像：圆形

### Typography

- 大标题：28pt Bold
- 页面标题：20pt Semibold
- 卡片标题：17pt Semibold
- 正文：15pt Regular
- 辅助文字：13pt Regular
- 标签：11pt Medium

### Shared UI Components

| 组件 | 说明 |
|------|------|
| `GradientBackground` | 全局粉紫渐变背景 |
| `CardView` | 白色半透明圆角卡片容器 |
| `PrimaryButton` | 渐变主按钮（紫→粉） |
| `SecondaryButton` | 白底描边次按钮 |
| `TabBarView` | 自定义底部 3 Tab 导航 |
| `StatCard` | 统计数字卡片 |
| `EmojiGrid` | 心情 Emoji 选择网格 |
| `QuizOptionRow` | 答题选项行（A/B/C/D） |
| `AchievementBadge` | 成就徽章 |
| `LoadingView` | 加载状态 |

## Screen Inventory (18 Screens from Prototype)

| ID | 屏幕名 | 说明 |
|----|--------|------|
| 1Fopb | Login Screen | Apple 登录 + 邮箱验证码登录 |
| kT6Sk | Home Screen (已绑定，可答题) | 每日问答卡片 + 心情同步 + 统计摘要 |
| noWcd | Quiz Answer Screen | 答题页，4选1，进度 3/5 |
| azWdO | Quiz Result Screen | 默契分 + 答案对比 |
| 7ZH9p | Mood Picker Screen | Emoji 网格 + 心情短语 |
| uyP5a | Stats Screen | 周趋势图 + 成就徽章 |
| XiJiH | Profile Screen (已绑定) | 个人信息 + 伴侣信息 + 设置 |
| jNnXr | Bind Partner Screen | 邀请码展示/输入 |
| tyXJ5 | Widget Small | iOS 小组件 |
| FzmeK | Widget Medium | iOS 中组件 |
| vIsnc | Unbind Confirm | 解绑确认弹窗 |
| tgTZX | Profile Screen (未绑定) | 绑定引导 + 精简设置 |
| hSqE6 | Home Screen (未绑定) | 绑定引导 + 锁定功能 |
| QHBTJ | Home Screen (已答等待) | 等待伴侣 + 查看进度 |
| zWolL | Quiz Complete (等待对方) | 等待伴侣完成答题 |
| rPGgW | Home Screen (等待揭晓) | 催一下 TA |
| VSDcp | Home Screen (Widget 弹窗) | 开启桌面小组件权限 |

> 注：屏幕 4fcdY 为空白占位帧，不对应实际功能页面。

## Data Models

iOS 端 Codable 结构体，对应后端 DTO：

```swift
// Auth
struct AuthResponse: Codable {
    let accessToken: String
    let refreshToken: String
}

// User
struct UserResponse: Codable {
    let id: Int64
    let email: String?
    let nickname: String
    let avatarUrl: String?
    let inviteCode: String
    let partnerId: Int64?
    let timezone: String
    let createdAt: String
}

// Couple
struct CoupleResponse: Codable {
    let id: Int64
    let partnerNickname: String
    let partnerAvatarUrl: String?
    let anniversaryDate: String?
    let status: String  // ACTIVE / DISSOLVED
    let createdAt: String
}

// Quiz
struct DailyQuizResponse: Codable {
    let id: Int64
    let date: String
    let quizType: String  // BLIND / GUESS / THEME
    let questions: [QuizQuestion]
    let myAnswer: QuizAnswerResponse?
    let partnerAnswer: QuizAnswerResponse?
    let revealed: Bool
}

struct QuizQuestion: Codable {
    let index: Int
    let content: String
    let options: [String]
}

struct QuizAnswerResponse: Codable {
    let answers: [Int]
    let score: Int?
}

// Mood
struct MoodResponse: Codable {
    let emoji: String
    let text: String?
    let updatedAt: String
}

// Stats
struct WeeklyStatsResponse: Codable {
    let dailyStats: [DailyStat]
    let averageScore: Int
    let currentStreak: Int
    let totalQuizzes: Int
}

struct DailyStat: Codable {
    let date: String
    let matchScore: Int?
    let quizType: String?
}

struct AchievementResponse: Codable {
    let type: String
    let unlockedAt: String?
}

// Pagination
struct CursorPage<T: Codable>: Codable {
    let items: [T]
    let nextCursor: String?
    let hasMore: Bool
}
```

## Offline Behavior

- 无网络时显示非侵入式顶部横幅提示"无网络连接"
- 禁用需要网络的操作（答题、更新心情、绑定/解绑等），按钮显示为禁用状态
- 展示本地缓存的最后数据：统计数据、心情状态、用户信息
- 网络恢复后自动隐藏横幅，恢复按钮交互
- 不做离线队列（MVP 阶段不缓存未提交操作）

## Empty States

| 场景 | 显示内容 |
|------|---------|
| 新绑定情侣，无统计数据 | 插图 + "开始你们的第一次默契挑战吧！" |
| 无成就解锁 | 所有徽章灰色 + 锁图标 + "继续答题解锁更多成就" |
| 伴侣未设置心情 | 心情位置显示默认占位 "TA 还没有更新心情" |
| 今日无问答（异常） | "今天的题目正在准备中，请稍后再来" |

## Push Notification Handling

用户点击推送通知时的目标页面映射：

| 通知类型 | 目标页面 |
|---------|---------|
| 每日问答提醒 | HomeView（开始答题卡片） |
| 伴侣已答题 | QuizWaitingView → 自动检查是否可揭晓 |
| 结果已揭晓 | QuizResultView |
| 伴侣更新心情 | HomeView（心情同步区域） |
| 成就解锁 | StatsView（成就区域） |
| 连续打卡里程碑 | StatsView |

**实现方式：** 通过 `UNUserNotificationCenterDelegate` 的 `userNotificationCenter(_:didReceive:)` 解析通知 payload 中的 `type` 字段，更新 `AppRouter` 的导航目标。
