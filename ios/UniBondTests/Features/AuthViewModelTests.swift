import XCTest
@testable import UniBond

final class AuthViewModelTests: XCTestCase {
    func testInitialState() {
        let viewModel = AuthViewModel(apiClient: APIClient(baseURL: "https://test.com"), appState: AppState())
        XCTAssertEqual(viewModel.loginStep, .initial)
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertEqual(viewModel.countdown, 0)
    }

    func testEmailValidation() {
        let viewModel = AuthViewModel(apiClient: APIClient(baseURL: "https://test.com"), appState: AppState())
        viewModel.email = ""
        XCTAssertFalse(viewModel.isEmailValid)
        viewModel.email = "bad"
        XCTAssertFalse(viewModel.isEmailValid)
        viewModel.email = "test@example.com"
        XCTAssertTrue(viewModel.isEmailValid)
    }

    func testCodeValidation() {
        let viewModel = AuthViewModel(apiClient: APIClient(baseURL: "https://test.com"), appState: AppState())
        viewModel.code = "123"
        XCTAssertFalse(viewModel.isCodeValid)
        viewModel.code = "123456"
        XCTAssertTrue(viewModel.isCodeValid)
    }
}
