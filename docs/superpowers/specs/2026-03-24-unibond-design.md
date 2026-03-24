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

| 组件 | 内存 |
|------|------|
| Spring Boot (JVM -Xmx512m) | ~600MB |
| PostgreSQL | ~400MB |
| Redis (maxmemory 128mb) | ~150MB |
| OS + Nginx + 其他 | ~900MB |

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
| couple_id | BIGINT FK | 所属情侣 |
| date | DATE | 日期 |
| match_score | INT | 默契分 |
| streak_days | INT | 连续天数 |
| quiz_type_played | ENUM | 当日玩法 |

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

### 2. 问答三种模式

**双人盲答 (blind)**

系统推送5道题 → A/B 各自独立作答 → 双方都提交后揭晓对比 → 计算默契分 (相同答案数/总题数 × 100)

**猜对方 (guess)**

系统出题"B最喜欢的X是？" → A猜B的答案，B回答自己的真实答案 → 反向同理 → 猜中数/总题数 × 100

**主题挑战 (theme)**

系统选定主题(美食/旅行/回忆等) → 围绕主题出5-8道题 → 双人盲答流程 → 生成主题默契报告卡片

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

### 情侣绑定

- 注册后生成唯一6位邀请码
- 输入对方邀请码完成绑定

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
