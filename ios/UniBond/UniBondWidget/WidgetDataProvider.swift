import Foundation
import WidgetKit

struct UniBondWidgetEntry: TimelineEntry {
    let date: Date
    let todayScore: Int
    let streakDays: Int
    let quizAnswered: Bool
    let quizType: String
    let partnerMoodEmoji: String
    let partnerMoodText: String
}

struct WidgetDataProvider: TimelineProvider {
    private let defaults = UserDefaults(suiteName: "group.com.unibond.shared") ?? .standard

    func placeholder(in context: Context) -> UniBondWidgetEntry {
        sampleEntry
    }

    func getSnapshot(in context: Context, completion: @escaping (UniBondWidgetEntry) -> Void) {
        completion(loadEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<UniBondWidgetEntry>) -> Void) {
        let entry = loadEntry()
        completion(Timeline(entries: [entry], policy: .after(Date().addingTimeInterval(7200))))
    }

    private var sampleEntry: UniBondWidgetEntry {
        UniBondWidgetEntry(
            date: Date(),
            todayScore: 88,
            streakDays: 7,
            quizAnswered: true,
            quizType: "BLIND",
            partnerMoodEmoji: "😊",
            partnerMoodText: "今天很开心"
        )
    }

    private func loadEntry() -> UniBondWidgetEntry {
        UniBondWidgetEntry(
            date: Date(),
            todayScore: defaults.integer(forKey: "todayScore"),
            streakDays: defaults.integer(forKey: "streakDays"),
            quizAnswered: defaults.bool(forKey: "quizAnswered"),
            quizType: defaults.string(forKey: "quizType") ?? "DAILY",
            partnerMoodEmoji: defaults.string(forKey: "partnerMoodEmoji") ?? "😊",
            partnerMoodText: defaults.string(forKey: "partnerMoodText") ?? "分享一下今天的心情"
        )
    }
}
