import ActivityKit

struct MoodActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        var partnerEmoji: String
        var partnerText: String?
        var updatedAt: String
    }

    var partnerName: String
}

enum MoodLiveActivityManager {
    static func startOrUpdate(partnerName: String, emoji: String, text: String?, updatedAt: String) async {
        let contentState = MoodActivityAttributes.ContentState(
            partnerEmoji: emoji,
            partnerText: text,
            updatedAt: updatedAt
        )

        if let activity = Activity<MoodActivityAttributes>.activities.first {
            await activity.update(ActivityContent(state: contentState, staleDate: nil))
            return
        }

        let attributes = MoodActivityAttributes(partnerName: partnerName)
        try? Activity.request(attributes: attributes, content: ActivityContent(state: contentState, staleDate: nil))
    }

    static func endAll() async {
        for activity in Activity<MoodActivityAttributes>.activities {
            await activity.end(nil, dismissalPolicy: .immediate)
        }
    }
}
