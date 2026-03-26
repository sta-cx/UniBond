import Foundation
import Observation
import SwiftUI

enum AppRoute: Hashable {
    case quizAnswer
    case quizWaiting
    case quizResult(date: String)
    case bindPartner
}

enum SheetRoute: String, Identifiable {
    case moodPicker
    case unbindConfirm

    var id: String { rawValue }
}

@Observable
class AppRouter {
    var homePath = NavigationPath()
    var statsPath = NavigationPath()
    var profilePath = NavigationPath()
    var activeSheet: SheetRoute?
    var showWidgetPermission = false
    var selectedTab = 0
    var pendingNotificationRoute: AppRoute?

    func navigateHome(to route: AppRoute) {
        selectedTab = 0
        homePath.append(route)
    }

    func navigateStats() {
        selectedTab = 1
    }

    func handleNotification(type: String) {
        switch type {
        case "QUIZ_REMINDER":
            selectedTab = 0
        case "PARTNER_ANSWERED":
            navigateHome(to: .quizWaiting)
        case "RESULT_REVEALED":
            navigateHome(to: .quizResult(date: Date().iso8601DateString))
        case "PARTNER_MOOD":
            selectedTab = 0
        case "ACHIEVEMENT_UNLOCKED", "STREAK_MILESTONE":
            navigateStats()
        default:
            break
        }
    }

    func reset() {
        homePath = NavigationPath()
        statsPath = NavigationPath()
        profilePath = NavigationPath()
        activeSheet = nil
        selectedTab = 0
    }
}
