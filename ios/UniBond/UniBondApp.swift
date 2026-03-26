import SwiftUI
import UIKit
import UserNotifications

final class PushCoordinator {
    static let shared = PushCoordinator()

    var apiClient: APIClient?
    weak var router: AppRouter?

    func requestAuthorization() {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            guard granted else { return }
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
    }

    func registerDeviceToken(_ deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        guard let apiClient else { return }
        Task {
            try? await apiClient.requestVoid(.registerDeviceToken(token))
        }
    }

    func handleNotification(userInfo: [AnyHashable: Any]) {
        guard let type = userInfo["type"] as? String else { return }
        Task { @MainActor in
            router?.handleNotification(type: type)
        }
    }
}

final class UniBondAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        PushCoordinator.shared.registerDeviceToken(deviceToken)
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        PushCoordinator.shared.handleNotification(userInfo: response.notification.request.content.userInfo)
        completionHandler()
    }
}

@main
struct UniBondApp: App {
    @UIApplicationDelegateAdaptor(UniBondAppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    @State private var appState = AppState()
    @State private var router = AppRouter()
    @State private var apiClient = APIClient(baseURL: "http://localhost:8080")
    @State private var didBootstrap = false

    var body: some Scene {
        WindowGroup {
            Group {
                if appState.isAuthenticated {
                    MainTabView(apiClient: apiClient)
                } else {
                    LoginView(viewModel: AuthViewModel(apiClient: apiClient, appState: appState))
                }
            }
            .environment(appState)
            .environment(router)
            .task {
                guard !didBootstrap else { return }
                didBootstrap = true
                appState.startNetworkMonitoring()
                PushCoordinator.shared.apiClient = apiClient
                PushCoordinator.shared.router = router
                PushCoordinator.shared.requestAuthorization()
                await bootstrapSession()
            }
            .onChange(of: scenePhase) { _, newPhase in
                guard newPhase == .active, appState.isAuthenticated else { return }
                let timeZoneIdentifier = TimeZone.current.identifier
                if timeZoneIdentifier != AppSettings.shared.lastTimezone {
                    AppSettings.shared.lastTimezone = timeZoneIdentifier
                    Task {
                        try? await apiClient.requestVoid(
                            .updateProfile(ProfileUpdateRequest(nickname: nil, avatarUrl: nil, timezone: timeZoneIdentifier))
                        )
                    }
                }
            }
        }
    }

    private func bootstrapSession() async {
        guard KeychainManager.shared.accessToken != nil else { return }
        do {
            let user: UserResponse = try await apiClient.request(.me)
            await MainActor.run {
                appState.authState = .authenticated(user)
            }
            if user.partnerId != nil {
                let couple: CoupleResponse = try await apiClient.request(.coupleInfo)
                await MainActor.run {
                    appState.coupleState = .bound(couple)
                }
            }
        } catch {
            await MainActor.run {
                appState.logout()
                router.reset()
            }
        }
    }
}

struct MainTabView: View {
    @Environment(AppState.self) private var appState
    @Environment(AppRouter.self) private var router
    let apiClient: APIClient

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                switch router.selectedTab {
                case 0:
                    NavigationStack(path: Binding(get: { router.homePath }, set: { router.homePath = $0 })) {
                        HomeView(viewModel: HomeViewModel(apiClient: apiClient, appState: appState))
                            .navigationDestination(for: AppRoute.self) { route in
                                homeDestination(route)
                            }
                    }
                case 1:
                    NavigationStack(path: Binding(get: { router.statsPath }, set: { router.statsPath = $0 })) {
                        StatsView(viewModel: StatsViewModel(apiClient: apiClient))
                    }
                default:
                    NavigationStack(path: Binding(get: { router.profilePath }, set: { router.profilePath = $0 })) {
                        ProfileView(viewModel: ProfileViewModel(apiClient: apiClient, appState: appState))
                            .navigationDestination(for: AppRoute.self) { route in
                                profileDestination(route)
                            }
                    }
                }
            }

            TabBarView(selectedTab: Binding(get: { router.selectedTab }, set: { router.selectedTab = $0 }))
        }
        .sheet(item: Binding(get: { router.activeSheet }, set: { router.activeSheet = $0 })) { route in
            switch route {
            case .moodPicker:
                MoodPickerView(
                    viewModel: MoodViewModel(apiClient: apiClient),
                    partnerName: appState.currentCouple?.partnerNickname ?? "TA"
                )
            case .unbindConfirm:
                UnbindConfirmView(viewModel: CoupleViewModel(apiClient: apiClient, appState: appState))
            }
        }
    }

    @ViewBuilder
    private func homeDestination(_ route: AppRoute) -> some View {
        switch route {
        case .quizAnswer:
            QuizAnswerView(viewModel: QuizViewModel(apiClient: apiClient))
        case .quizWaiting:
            QuizWaitingView()
        case .quizResult(let date):
            QuizResultView(viewModel: QuizViewModel(apiClient: apiClient), date: date)
        case .bindPartner:
            BindPartnerView(viewModel: CoupleViewModel(apiClient: apiClient, appState: appState))
        }
    }

    @ViewBuilder
    private func profileDestination(_ route: AppRoute) -> some View {
        switch route {
        case .bindPartner:
            BindPartnerView(viewModel: CoupleViewModel(apiClient: apiClient, appState: appState))
        case .quizAnswer:
            QuizAnswerView(viewModel: QuizViewModel(apiClient: apiClient))
        case .quizWaiting:
            QuizWaitingView()
        case .quizResult(let date):
            QuizResultView(viewModel: QuizViewModel(apiClient: apiClient), date: date)
        }
    }
}
