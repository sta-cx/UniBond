import XCTest
@testable import UniBond

final class KeychainManagerTests: XCTestCase {
    private var manager: KeychainManager!

    override func setUp() {
        super.setUp()
        manager = KeychainManager(service: "com.unibond.app.tests.\(UUID().uuidString)")
    }

    override func tearDown() {
        manager.deleteTokens()
        manager = nil
        super.tearDown()
    }

    func testSaveAndReadTokens() {
        manager.saveTokens(access: "access123", refresh: "refresh456")

        XCTAssertEqual(manager.accessToken, "access123")
        XCTAssertEqual(manager.refreshToken, "refresh456")
    }

    func testDeleteTokens() {
        manager.saveTokens(access: "a", refresh: "r")
        manager.deleteTokens()

        XCTAssertNil(manager.accessToken)
        XCTAssertNil(manager.refreshToken)
    }

    func testUpdateAccessToken() {
        manager.saveTokens(access: "old", refresh: "r")
        manager.saveTokens(access: "new", refresh: "r")

        XCTAssertEqual(manager.accessToken, "new")
    }
}
