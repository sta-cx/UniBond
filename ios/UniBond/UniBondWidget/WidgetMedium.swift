import SwiftUI
import WidgetKit

struct WidgetMedium: Widget {
    let kind = "UniBondWidgetMedium"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetDataProvider()) { entry in
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("今日默契分")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(entry.todayScore)")
                        .font(.system(size: 40, weight: .bold))
                        .foregroundStyle(.pink)
                    Text("题型：\(entry.quizType)")
                        .font(.caption)
                }
                Spacer()
                VStack(alignment: .leading, spacing: 8) {
                    Text("TA 的心情")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(entry.partnerMoodEmoji)
                        .font(.system(size: 32))
                    Text(entry.partnerMoodText)
                        .font(.caption)
                        .lineLimit(2)
                    Text(entry.quizAnswered ? "今日已完成答题" : "今日待答题")
                        .font(.caption2)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(entry.quizAnswered ? Color.green.opacity(0.2) : Color.orange.opacity(0.2))
                        .clipShape(Capsule())
                }
            }
            .padding()
            .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("UniBond 中组件")
        .description("查看默契分、题型和伴侣心情。")
        .supportedFamilies([.systemMedium])
    }
}
