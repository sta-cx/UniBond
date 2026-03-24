# UniBond - 情侣每日默契问答 App 设计文档

## 概述

UniBond 是一款面向情侣的 iOS App，核心功能是每日 AI 生成的默契问答和实时心情同步。通过答题互动增进了解，通过心情组件保持情感连接。

## 目标用户

情侣/伴侣，希望通过轻量级日常互动增进默契和情感联系。

## MVP 功能范围

1. 用户体系 + 情侣绑定
2. 每日默契问答（混合模式：盲答/猜对方/主题挑战）
3. 心情同步（Live Activity 实时 + Widget 每日概览）
4. 默契统计 + 成就系统

---

## 系统架构

```
┌─────────────────┐         ┌──────────────────────────────┐
│   iOS App       │         │   ECS 2C2G                   │
│  (SwiftUI)      │◄─REST──►│  Spring Boot                 │
│                 │         │   ├─ Auth Module              │
│  ┌───────────┐  │         │   ├─ Quiz Module              │
│  │WidgetKit  │  │         │   ├─ Mood Module              │
│  │Extension  │  │         │   ├─ Stats Module             │
│  │           │  │         │   └─ Push Module (APNs)       │
│  │LiveActivity│  │         │                              │
│  │Extension  │  │         │  PostgreSQL (数据持久化)       │
│  └───────────┘  │         │  Redis (缓存+会话+每日题目)    │
└─────────────────┘         └──────────────────────────────┘
```

**架构选型：单体 REST API**

- Spring Boot 单体应用，2G 内存足够运行
- 情侣 App 并发量低（每对情侣2人），单体架构完全胜任
- 部署简单，Docker Compose 一键启动
- 后续需要可平滑升级为微服务

### 内存分配策略 (2G ECS)

| 组件 | 配置 | 实际占用 |
|------|------|---------|
| Spring Boot (JVM -Xmx384m) | -Xmx384m -Xms256m | ~450MB |
| PostgreSQL (shared_buffers=128MB) | shared_buffers=128MB, work_mem=4MB, max_connections=30 | ~200MB |
| Redis (maxmemory 64mb) | maxmemory 64mb, maxmemory-policy allkeys-lru | ~80MB |
| OS + Docker + Nginx + sshd | — | ~500MB |
| **预留缓冲** | — | ~818MB |

注意：2048MB 总内存，实际可用约 1800MB（内核保留）。以上配置留有充足余量，避免 OOM。

---

## 数据模型

### User (用户)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| email | VARCHAR | 邮箱（可选） |
| nickname | VARCHAR | 昵称 |
| avatar_url | VARCHAR | 头像URL |
| auth_provider | ENUM | apple / email |
| apple_sub | VARCHAR | Apple用户唯一标识 |
| partner_id | BIGINT FK | 关联的另一半 |
| invite_code | VARCHAR(6) | 唯一邀请码 |
| device_token | VARCHAR | APNs设备Token |
| created_at | TIMESTAMP | 创建时间 |

### Couple (情侣关系)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_a_id | BIGINT FK | 用户A |
| user_b_id | BIGINT FK | 用户B |
| anniversary_date | DATE | 纪念日（可选） |
| bind_at | TIMESTAMP | 绑定时间 |
| status | ENUM | active / dissolved |

### DailyQuiz (每日题目)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| date | DATE | 日期 |
| couple_id | BIGINT FK | 所属情侣 |
| quiz_type | ENUM | blind / guess / theme |
| theme | VARCHAR | 主题（主题挑战时） |
| questions | JSONB | LLM生成的结构化题目 |
| generation_source | ENUM | ai / fallback_pool |
| prompt_context | JSONB | 生成时的上下文摘要 |
| created_at | TIMESTAMP | 创建时间 |

### QuizAnswer (答题记录)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| daily_quiz_id | BIGINT FK | 关联题目 |
| user_id | BIGINT FK | 答题用户 |
| couple_id | BIGINT FK | 所属情侣 |
| answers | JSONB | 用户答案 |
| partner_guess | JSONB | 猜对方模式时的猜测 |
| score | INT | 得分 |
| completed_at | TIMESTAMP | 完成时间 |
| revealed | BOOLEAN | 是否已揭晓 |

### QuestionPool (兜底题库)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| category | VARCHAR | 分类 |
| quiz_type | ENUM | blind / guess / theme |
| question | TEXT | 题目 |
| options | JSONB | 选项 |
| difficulty | INT | 难度 |
| used_count | INT | 使用次数 |

### MoodStatus (心情状态)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT FK | 用户 |
| couple_id | BIGINT FK | 所属情侣 |
| mood_emoji | VARCHAR | 心情表情 |
| mood_text | VARCHAR | 心情短语（可选） |
| updated_at | TIMESTAMP | 更新时间 |

Redis 同步存储: `mood:{userId} → { emoji, text, updatedAt, version }`

### Achievement (成就)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| couple_id | BIGINT FK | 所属情侣 |
| type | VARCHAR | 成就类型标识 |
| unlocked_at | TIMESTAMP | 解锁时间 |

### DailyStats (每日统计)

| 字段 | 类型 | 说明 |
|------|------|------|
| couple_id | BIGINT FK | 复合主键之一 |
| date | DATE | 复合主键之一 |
| match_score | INT | 默契分 |
| streak_days | INT | 连续天数 |
| quiz_type_played | ENUM | 当日玩法 |

主键：`PRIMARY KEY (couple_id, date)`

---

## 核心功能设计

### 1. AI 题目生成

```
每日凌晨定时任务 (Cron)
        │
        ▼
QuizGenerationService
  1. 从DB读取情侣历史数据：
     - 过往答题记录 & 默契分趋势
     - 最近心情状态
     - 已出过的题目（去重）
     - 纪念日/特殊日期
  2. 构造 Prompt → 调用云端 LLM API
  3. 解析返回的结构化 JSON 题目
  4. 存入 DailyQuiz 表 + Redis 缓存
```

- **异步预生成**：凌晨批量为所有情侣生成当日题目
- **降级兜底**：LLM API 失败时从 QuestionPool 抽题
- **费用控制**：每对情侣每天1次调用，约 500-800 token
- **输入净化**：所有用户输入（心情短语、历史答案）在构造 Prompt 前进行转义和截断，防止 Prompt 注入
- **输出校验**：LLM 返回的 JSON 必须通过严格 Schema 验证（题目数量、选项格式、内容长度），校验失败则降级到兜底题库

### 题目数量标准

所有模式统一为 **5 题**，确保分数可比较（默契分 = 匹配数 / 5 × 100）。

### 2. 问答三种模式

**双人盲答 (blind)**

系统推送5道题 → A/B 各自独立作答 → 双方都提交后揭晓对比 → 计算默契分 (相同答案数/5 × 100)

**猜对方 (guess)**

系统出题"B最喜欢的X是？" → A猜B的答案，B回答自己的真实答案 → 反向同理 → 猜中数/5 × 100

**主题挑战 (theme)**

系统选定主题(美食/旅行/回忆等) → 围绕主题出5道题 → 双人盲答流程 → 生成主题默契报告卡片

**答题并发控制：**
- 答题提交幂等：同一用户对同一 DailyQuiz 重复提交返回已有答案，不覆盖
- 揭晓触发：提交答案时在事务内 `SELECT COUNT(*) FROM quiz_answer WHERE daily_quiz_id = ? FOR UPDATE`，等于2时设 `revealed = true`
- UNIQUE 约束：`(daily_quiz_id, user_id)` 防止重复插入

**每日轮转规则**

```
Day % 3 == 0 → blind
Day % 3 == 1 → guess
Day % 3 == 2 → theme
```

AI 生成时可根据上下文覆盖（如纪念日强制切为回忆主题挑战）。

### 3. 心情同步

**采用 Live Activity + Widget 双轨方案：**

- **Live Activity (锁屏+灵动岛)**：实时心情，APNs 推送驱动，秒级更新
- **WidgetKit (桌面小组件)**：每日概览（今日默契分/连续天数/答题状态），每天刷新1-2次

**心情更新流程：**

```
A 更新心情 → POST /api/v1/mood → DB + Redis
    │
    └─► APNs Push (Live Activity update) → B 的锁屏/灵动岛秒级更新
```

**Live Activity 显示：**

- 灵动岛紧凑态：emoji + 昵称
- 灵动岛展开态：emoji + 昵称 + 短语 + 时间
- 锁屏横幅：App名 + emoji + 昵称 + 短语 + 更新时间

**Live Activity 生命周期：**

- **启动时机**：用户打开 App 且已绑定情侣时自动启动（如无活跃的 Live Activity）
- **更新方式**：服务端通过 APNs push-to-update 推送 content-state（包含 emoji、text、updatedAt）
- **8小时到期**：iOS 强制 Live Activity 最长存活8小时。到期后进入 `dismissed` 或 `ended` 状态
- **重启策略**：App 每次进入前台时检查，若无活跃 Live Activity 则重新启动
- **APNs Payload 格式**：`{ "aps": { "timestamp": epoch, "event": "update", "content-state": { "emoji": "😊", "text": "心情好", "updatedAt": "..." } } }`

**Widget 显示：**

- Small：默契分 + 连续天数
- Medium：默契分 + 连续天数 + 答题状态 + 今日主题

### 4. 成就系统

**连续打卡系列：**
- 初识默契 — 连续3天
- 默契升温 — 连续7天
- 心有灵犀 — 连续30天
- 灵魂伴侣 — 连续100天

**默契分系列：**
- 心意相通 — 单次100分
- 默契之星 — 累计10次90分以上

**主题挑战系列：**
- 美食知己 / 旅行搭档 / 回忆收藏家

**特殊成就：**
- 命中注定 — 绑定情侣关系
- 甜蜜纪念 — 在纪念日完成答题

**检测机制：** 答题完成事件触发 → AchievementService.check() → 新成就解锁时存DB + 推送通知

---

## 用户体系

### 登录方式 (MVP)

- **Sign in with Apple**：主要登录方式，零成本
- **邮箱 + 验证码**：补充方式，Spring Mail + SMTP 自发邮件，零成本

后续扩展：手机号+短信、微信登录（需企业资质）

### 认证机制

- JWT (Access Token 2小时 + Refresh Token 30天)
- Token 存 iOS Keychain
- 401 时自动用 Refresh Token 换新
- **Refresh Token 服务端存储**：存入 Redis（key: `refresh:{userId}`, TTL 30天），支持主动吊销
- **Token 轮转**：每次刷新时签发新 Refresh Token，旧 Token 立即失效
- **吊销场景**：登出、解绑情侣、账号删除时删除 Redis 中的 Refresh Token

### 情侣绑定

- 注册后生成唯一6位邀请码（大写字母+数字，排除易混淆字符 0/O/I/L，字符集共32个字符）
- 生成方式：SecureRandom 生成 + DB UNIQUE 约束，冲突时重试（最多3次）
- 输入对方邀请码完成绑定
- 绑定尝试限流：每用户每分钟最多5次

### 解绑流程

1. A 请求解绑 → 服务端将 Couple.status 设为 `dissolved`
2. 清除双方 User.partner_id
3. 取消当日未完成的 DailyQuiz（标记为 cancelled）
4. 终止对方的 Live Activity（发送 APNs end 事件）
5. 推送通知 B："你的情侣关系已被解除"
6. **历史数据保留**：QuizAnswer、DailyStats、Achievement 保留不删除，但解绑后不可查看对方数据
7. **重新绑定**：解绑后可立即与新的邀请码绑定，无冷却期

---

## 推送策略

| 事件 | 推送类型 | 内容 |
|------|---------|------|
| 今日题目已生成 | 普通推送 | "今日默契挑战来了，快来答题~" |
| 对方已答完 | 普通推送 | "TA已经答完了，就等你啦!" |
| 双方都答完 | 普通推送 | "默契结果出炉!今天你们得了92分" |
| 对方更新心情 | Live Activity更新 | 秒级更新锁屏/灵动岛 |
| 连续打卡里程碑 | 普通推送 | "你们已连续默契7天!解锁新成就" |
| 长时间未答题 | 定时推送 | "想TA了吗？今天的默契题还没答哦" |

APNs 集成：Spring Boot + Pushy 库

---

## API 清单

### Auth 认证
```
POST   /api/v1/auth/apple            Apple登录
POST   /api/v1/auth/email/send       发送邮箱验证码
POST   /api/v1/auth/email/login      邮箱验证码登录/注册
POST   /api/v1/auth/refresh          刷新Token
POST   /api/v1/auth/logout           登出（吊销Refresh Token）
DELETE /api/v1/user/account          删除账号（Apple审核要求）
```

### User 用户
```
GET    /api/v1/user/me               我的信息
PUT    /api/v1/user/profile          更新昵称头像
POST   /api/v1/user/device-token     注册APNs设备Token
```

### Couple 情侣
```
GET    /api/v1/couple/info           情侣信息
POST   /api/v1/couple/bind           邀请码绑定
DELETE /api/v1/couple/unbind         解除绑定
```

### Quiz 问答
```
GET    /api/v1/quiz/today            今日题目
POST   /api/v1/quiz/answer           提交答案
GET    /api/v1/quiz/result/{date}    某天结果
GET    /api/v1/quiz/history          历史记录(分页)
```

### Mood 心情
```
POST   /api/v1/mood                  更新我的心情
GET    /api/v1/mood/partner          获取对方心情
```

### Stats 统计+成就
```
GET    /api/v1/stats/overview        总览
GET    /api/v1/stats/achievements    成就列表
GET    /api/v1/stats/weekly          周报数据
```

---

## 技术栈

### iOS 客户端
- Swift 5.9+ / SwiftUI
- WidgetKit (每日概览Widget)
- ActivityKit (Live Activity心情)
- Sign in with Apple (AuthServices)
- Keychain (Token存储)
- URLSession / async-await 网络层

### 后端
- Java 17 + Spring Boot 3.x
- Spring Security + JWT
- Spring Data JPA + PostgreSQL
- Spring Data Redis
- Spring Mail (邮箱验证码)
- Pushy (APNs推送库)
- OpenAI/Claude API (题目生成)
- Flyway (数据库迁移)
- Scheduled Tasks (定时题目生成)

### 基础设施 (ECS 2C2G)
- PostgreSQL 15
- Redis 7 (maxmemory 128mb)
- Nginx (反向代理 + HTTPS)
- Docker Compose (一键部署)

---

## 项目结构

### 后端

```
unibond-server/
├── src/main/java/com/unibond/
│   ├── UnibondApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── RedisConfig.java
│   │   └── ApnsConfig.java
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── service/EmailService.java
│   │   ├── service/AppleAuthService.java
│   │   └── jwt/JwtProvider.java
│   ├── user/
│   │   ├── controller/UserController.java
│   │   ├── service/UserService.java
│   │   ├── entity/User.java
│   │   └── repository/UserRepository.java
│   ├── couple/
│   │   ├── controller/CoupleController.java
│   │   ├── service/CoupleService.java
│   │   ├── entity/Couple.java
│   │   └── repository/CoupleRepository.java
│   ├── quiz/
│   │   ├── controller/QuizController.java
│   │   ├── service/QuizService.java
│   │   ├── service/QuizGenerationService.java
│   │   ├── entity/DailyQuiz.java
│   │   ├── entity/QuizAnswer.java
│   │   ├── entity/QuestionPool.java
│   │   └── repository/...
│   ├── mood/
│   │   ├── controller/MoodController.java
│   │   ├── service/MoodService.java
│   │   ├── entity/MoodStatus.java
│   │   └── repository/MoodRepository.java
│   ├── stats/
│   │   ├── controller/StatsController.java
│   │   ├── service/StatsService.java
│   │   ├── service/AchievementService.java
│   │   ├── entity/Achievement.java
│   │   ├── entity/DailyStats.java
│   │   └── repository/...
│   ├── push/
│   │   ├── service/PushService.java
│   │   └── service/LiveActivityPushService.java
│   └── common/
│       ├── exception/GlobalExceptionHandler.java
│       ├── dto/ApiResponse.java
│       └── util/...
├── src/main/resources/
│   ├── application.yml
│   ├── db/migration/ (Flyway)
│   └── templates/ (邮件模板)
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

### iOS 客户端

```
UniBond/
├── UniBondApp.swift
├── Core/
│   ├── Network/ApiClient.swift
│   ├── Auth/KeychainManager.swift
│   ├── Auth/AuthManager.swift
│   └── Push/PushManager.swift
├── Features/
│   ├── Auth/
│   │   ├── LoginView.swift
│   │   ├── EmailLoginView.swift
│   │   └── BindPartnerView.swift
│   ├── Quiz/
│   │   ├── TodayQuizView.swift
│   │   ├── QuizAnswerView.swift
│   │   └── QuizResultView.swift
│   ├── Mood/
│   │   ├── MoodPickerView.swift
│   │   └── PartnerMoodView.swift
│   ├── Stats/
│   │   ├── StatsOverviewView.swift
│   │   └── AchievementsView.swift
│   └── Home/
│       └── HomeView.swift
├── Models/
├── UniBondWidget/          (WidgetKit Extension)
│   ├── DailyOverviewWidget.swift
│   └── WidgetTimelineProvider.swift
├── UniBondLiveActivity/    (Live Activity Extension)
│   └── MoodLiveActivity.swift
└── Shared/                 (App Group共享)
    └── SharedDefaults.swift
```

---

## 视觉风格

温暖玻璃拟态 (Warm Glassmorphism) / 极简柔光风 (Minimal Soft UI)

- 柔和渐变背景（暖粉 → 淡紫）
- 毛玻璃半透明卡片
- 圆角设计，柔和阴影
- 清晰的信息层级

---

## 安全与数据隔离

### 接口限流

| 端点 | 限制 |
|------|------|
| POST /api/v1/auth/email/send | 同一邮箱：1次/60秒，5次/小时 |
| POST /api/v1/auth/email/login | 同一IP：10次/分钟 |
| POST /api/v1/couple/bind | 同一用户：5次/分钟 |
| 其他认证接口 | 同一IP：30次/分钟 |
| 业务接口（已认证） | 同一用户：60次/分钟 |

实现方式：Spring Boot + Redis 令牌桶（Bucket4j 或自定义拦截器）

### 数据隔离

- 每个 API 请求在 Service 层校验：`当前用户.coupleId == 请求资源.coupleId`
- 通过 Spring Security 自定义 `@PreAuthorize` 或 Service 层 guard 实现
- 未绑定情侣的用户只能访问 Auth/User/Couple 相关接口

### 输入校验

- nickname：最长20字符，过滤特殊字符
- mood_text：最长50字符，过滤特殊字符
- mood_emoji：仅允许 Unicode emoji 字符
- 所有用户输入在存储前进行 XSS 过滤

---

## 错误处理

### 标准错误响应格式

```json
{
  "code": "QUIZ_ALREADY_ANSWERED",
  "message": "你已经回答过今天的题目了",
  "timestamp": "2026-03-24T10:30:00Z"
}
```

### HTTP 状态码约定

| 状态码 | 用途 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功（绑定、答题提交） |
| 304 | 未修改（心情轮询带 version 时） |
| 400 | 请求参数错误 |
| 401 | 未认证 / Token 过期 |
| 403 | 无权限（访问非自己情侣的数据） |
| 404 | 资源不存在 |
| 409 | 冲突（重复绑定、重复答题） |
| 429 | 请求过于频繁（限流） |
| 500 | 服务端内部错误 |

### 应用错误码

| 错误码 | 说明 |
|--------|------|
| AUTH_CODE_EXPIRED | 验证码已过期 |
| AUTH_CODE_INVALID | 验证码错误 |
| AUTH_TOKEN_EXPIRED | Token已过期 |
| COUPLE_ALREADY_BOUND | 已绑定情侣 |
| COUPLE_NOT_BOUND | 未绑定情侣 |
| INVITE_CODE_INVALID | 邀请码无效 |
| INVITE_CODE_SELF | 不能绑定自己 |
| QUIZ_NOT_AVAILABLE | 今日题目尚未生成 |
| QUIZ_ALREADY_ANSWERED | 已回答过 |
| QUIZ_NOT_REVEALED | 结果未揭晓（对方未答完） |
| RATE_LIMIT_EXCEEDED | 请求频率超限 |

---

## 时区处理

- 服务端统一使用 **UTC** 存储所有时间戳
- "每日"的定义基于 **用户注册时设置的时区**（User 表增加 `timezone` 字段，如 `Asia/Shanghai`）
- 凌晨定时任务按各情侣的时区分批生成题目（按时区分组，在各时区的 00:05 生成）
- `Day % 3` 轮转规则中的 Day 基于情侣所在时区的本地日期
- iOS 端在注册和每次 App 启动时上报当前时区

---

## 分页约定

列表接口统一使用游标分页：

```
请求：GET /api/v1/quiz/history?cursor={lastId}&size=20
响应：
{
  "data": [...],
  "cursor": "下一页游标，null表示没有更多",
  "hasMore": true
}
```

默认 size = 20，最大 size = 50。

---

## Widget 数据共享机制

- Main App 和 WidgetKit Extension 通过 **App Group** 共享数据
- App Group ID: `group.com.unibond.shared`
- 共享方式：`UserDefaults(suiteName: "group.com.unibond.shared")`
- 写入时机：App 前台时拉取到新数据后立刻写入
- 共享数据：

| Key | 类型 | 说明 |
|-----|------|------|
| match_score_today | Int | 今日默契分 |
| streak_days | Int | 连续天数 |
| quiz_answered_today | Bool | 今天是否已答题 |
| quiz_type_today | String | 今日模式 |
| partner_nickname | String | 对方昵称 |
