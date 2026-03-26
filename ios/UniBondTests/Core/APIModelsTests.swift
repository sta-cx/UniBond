import XCTest
@testable import UniBond

final class APIModelsTests: XCTestCase {
    func testAuthResponseDecoding() throws {
        let json = """
        {"data":{"accessToken":"abc","refreshToken":"def","userId":1,"isNew":false}}
        """
        let wrapper = try JSONDecoder().decode(ApiResponse<AuthResponse>.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(wrapper.data.accessToken, "abc")
        XCTAssertEqual(wrapper.data.refreshToken, "def")
        XCTAssertEqual(wrapper.data.userId, 1)
        XCTAssertFalse(wrapper.data.isNew)
    }

    func testUserResponseDecoding() throws {
        let json = """
        {"data":{"id":1,"email":"a@b.com","nickname":"\u5C0F\u660E","avatarUrl":null,"authProvider":"EMAIL","inviteCode":"AB3K7X","partnerId":null,"createdAt":"2026-01-01T00:00:00Z"}}
        """
        let wrapper = try JSONDecoder().decode(ApiResponse<UserResponse>.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(wrapper.data.id, 1)
        XCTAssertEqual(wrapper.data.email, "a@b.com")
        XCTAssertEqual(wrapper.data.nickname, "\u{5C0F}\u{660E}")
        XCTAssertNil(wrapper.data.avatarUrl)
        XCTAssertEqual(wrapper.data.authProvider, "EMAIL")
        XCTAssertEqual(wrapper.data.inviteCode, "AB3K7X")
        XCTAssertNil(wrapper.data.partnerId)
        XCTAssertEqual(wrapper.data.createdAt, "2026-01-01T00:00:00Z")
    }

    func testQuizResponseDecoding() throws {
        let json = """
        {"data":{"id":1,"date":"2026-03-26","quizType":"BLIND","theme":null,"questions":"[{\\"index\\":0,\\"content\\":\\"Q1\\",\\"options\\":[\\"A\\",\\"B\\",\\"C\\",\\"D\\"]}]"}}
        """
        let wrapper = try JSONDecoder().decode(ApiResponse<QuizResponse>.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(wrapper.data.id, 1)
        XCTAssertEqual(wrapper.data.date, "2026-03-26")
        XCTAssertEqual(wrapper.data.quizType, "BLIND")
        XCTAssertNil(wrapper.data.theme)
        XCTAssertTrue(wrapper.data.questions.contains("\"content\":\"Q1\""))
    }

    func testOverviewResponseDecoding() throws {
        let json = """
        {"data":{"todayScore":92,"streakDays":7,"totalQuizzes":23,"avgScore":85.5,"recentAchievements":[]}}
        """
        let wrapper = try JSONDecoder().decode(ApiResponse<OverviewResponse>.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(wrapper.data.todayScore, 92)
        XCTAssertEqual(wrapper.data.streakDays, 7)
        XCTAssertEqual(wrapper.data.totalQuizzes, 23)
        XCTAssertEqual(wrapper.data.avgScore, 85.5, accuracy: 0.0001)
        XCTAssertTrue(wrapper.data.recentAchievements.isEmpty)
    }

    func testCursorPageDecoding() throws {
        let json = """
        {"data":{"data":[{"id":1,"date":"2026-03-26","quizType":"BLIND","theme":null,"questions":"[]"}],"cursor":"123","hasMore":true}}
        """
        let wrapper = try JSONDecoder().decode(ApiResponse<CursorPage<QuizResponse>>.self, from: json.data(using: .utf8)!)
        XCTAssertEqual(wrapper.data.data.count, 1)
        XCTAssertEqual(wrapper.data.data[0].id, 1)
        XCTAssertEqual(wrapper.data.cursor, "123")
        XCTAssertTrue(wrapper.data.hasMore)
    }
}
