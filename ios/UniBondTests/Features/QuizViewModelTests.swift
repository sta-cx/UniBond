import XCTest
@testable import UniBond

final class QuizViewModelTests: XCTestCase {
    func testSelectAnswer() {
        let viewModel = QuizViewModel(apiClient: APIClient(baseURL: "https://test.com"))
        viewModel.selectAnswer(questionIndex: 0, optionIndex: 2)
        XCTAssertEqual(viewModel.selectedAnswers[0], 2)
    }

    func testAllAnswered() {
        let viewModel = QuizViewModel(apiClient: APIClient(baseURL: "https://test.com"))
        for index in 0..<5 {
            viewModel.selectAnswer(questionIndex: index, optionIndex: 0)
        }
        XCTAssertTrue(viewModel.allAnswered)
    }

    func testNotAllAnswered() {
        let viewModel = QuizViewModel(apiClient: APIClient(baseURL: "https://test.com"))
        viewModel.selectAnswer(questionIndex: 0, optionIndex: 1)
        XCTAssertFalse(viewModel.allAnswered)
    }
}
