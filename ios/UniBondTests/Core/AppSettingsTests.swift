import XCTest
@testable import UniBond

final class AppSettingsTests: XCTestCase {
    override func tearDown() {
        AppSettings.shared.lastTimezone = nil
        AppSettings.shared.cachedUserId = nil
        AppSettings.shared.sharedTodayScore = 0
        AppSettings.shared.sharedStreakDays = 0
        AppSettings.shared.sharedQuizAnswered = false
        AppSettings.shared.sharedQuizType = nil
        AppSettings.shared.sharedPartnerMoodEmoji = nil
        AppSettings.shared.sharedPartnerMoodText = nil
        super.tearDown()
    }

    func testStandardDefaultsPropertiesRoundTrip() {
        AppSettings.shared.lastTimezone = "Asia/Shanghai"
        AppSettings.shared.cachedUserId = 42

        XCTAssertEqual(AppSettings.shared.lastTimezone, "Asia/Shanghai")
        XCTAssertEqual(AppSettings.shared.cachedUserId, 42)
    }

    func testSharedDefaultsPropertiesRoundTrip() {
        AppSettings.shared.sharedTodayScore = 99
        AppSettings.shared.sharedStreakDays = 7
        AppSettings.shared.sharedQuizAnswered = true
        AppSettings.shared.sharedQuizType = "BLIND"
        AppSettings.shared.sharedPartnerMoodEmoji = ":)"
        AppSettings.shared.sharedPartnerMoodText = "Good day"

        XCTAssertEqual(AppSettings.shared.sharedTodayScore, 99)
        XCTAssertEqual(AppSettings.shared.sharedStreakDays, 7)
        XCTAssertTrue(AppSettings.shared.sharedQuizAnswered)
        XCTAssertEqual(AppSettings.shared.sharedQuizType, "BLIND")
        XCTAssertEqual(AppSettings.shared.sharedPartnerMoodEmoji, ":)")
        XCTAssertEqual(AppSettings.shared.sharedPartnerMoodText, "Good day")
    }
}
