import ActivityKit
import SwiftUI
import WidgetKit

struct MoodActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        var partnerEmoji: String
        var partnerText: String?
        var updatedAt: String
    }

    var partnerName: String
}

struct MoodLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: MoodActivityAttributes.self) { context in
            VStack(alignment: .leading, spacing: 8) {
                Text("\(context.attributes.partnerName) 的心情")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(context.state.partnerEmoji)
                    .font(.system(size: 40))
                if let text = context.state.partnerText, !text.isEmpty {
                    Text(text)
                        .font(.body)
                        .lineLimit(2)
                }
                if let updatedAt = Date.fromISO8601(context.state.updatedAt) {
                    Text(updatedAt.relativeTimeString)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            .padding()
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Text(context.state.partnerEmoji)
                        .font(.system(size: 28))
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.attributes.partnerName)
                        .font(.caption)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text(context.state.partnerText ?? "心情已更新")
                        .font(.caption)
                        .lineLimit(2)
                }
            } compactLeading: {
                Text(context.state.partnerEmoji)
            } compactTrailing: {
                Text("心情")
                    .font(.caption2)
            } minimal: {
                Text(context.state.partnerEmoji)
            }
        }
    }
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
