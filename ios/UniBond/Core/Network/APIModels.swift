// APIModels.swift
// UniBond

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
    // Backend returns serialized JSON string for questions; parse separately when needed.
    let questions: String
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
