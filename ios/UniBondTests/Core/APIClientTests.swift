import XCTest
@testable import UniBond

final class APIClientTests: XCTestCase {
    func testBuildURLRequest() async throws {
        let client = APIClient(baseURL: "https://example.com")
        let endpoint = APIEndpoint(path: "/api/v1/user/me", method: .GET)
        let request = try await client.buildRequest(for: endpoint)

        XCTAssertEqual(request.url?.absoluteString, "https://example.com/api/v1/user/me")
        XCTAssertEqual(request.httpMethod, "GET")
    }

    func testBuildURLRequestWithBody() async throws {
        let client = APIClient(baseURL: "https://example.com")
        let body = EmailSendRequest(email: "test@example.com")
        let endpoint = APIEndpoint.emailSend(body)
        let request = try await client.buildRequest(for: endpoint)

        XCTAssertEqual(request.httpMethod, "POST")
        XCTAssertNotNil(request.httpBody)
        let decoded = try JSONDecoder().decode(EmailSendRequest.self, from: request.httpBody ?? Data())
        XCTAssertEqual(decoded.email, "test@example.com")
    }
}
