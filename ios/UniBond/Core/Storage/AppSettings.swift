// AppSettings.swift
// UniBond

import Foundation

final class AppSettings {
    static let shared = AppSettings()

    private let defaults = UserDefaults.standard
    private let sharedDefaults: UserDefaults

    private init() {
        sharedDefaults = UserDefaults(suiteName: "group.com.unibond.shared") ?? .standard
    }

    var lastTimezone: String? {
        get { defaults.string(forKey: "lastTimezone") }
        set { defaults.set(newValue, forKey: "lastTimezone") }
    }

    var cachedUserId: Int64? {
        get { defaults.object(forKey: "cachedUserId") as? Int64 }
        set { defaults.set(newValue, forKey: "cachedUserId") }
    }

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
        if let domain = Bundle.main.bundleIdentifier {
            defaults.removePersistentDomain(forName: domain)
        }
        sharedDefaults.removePersistentDomain(forName: "group.com.unibond.shared")
    }
}
