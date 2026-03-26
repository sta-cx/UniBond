import SwiftUI
import WidgetKit

struct WidgetSmall: Widget {
    let kind = "UniBondWidgetSmall"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: WidgetDataProvider()) { entry in
            VStack(alignment: .leading, spacing: 12) {
                Text("今日默契")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text("\(entry.todayScore)")
                    .font(.system(size: 36, weight: .bold))
                    .foregroundStyle(.pink)
                HStack {
                    Label("\(entry.streakDays)天", systemImage: "flame.fill")
                        .font(.caption)
                    Spacer()
                    Text(entry.quizAnswered ? "已答题" : "待答题")
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
        .configurationDisplayName("UniBond 小组件")
        .description("查看今日默契分和打卡状态。")
        .supportedFamilies([.systemSmall])
    }
}
