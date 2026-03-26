import Foundation
import Network
import Observation

enum AuthState: Equatable {
    case unauthenticated
    case authenticated(UserResponse)
}

enum CoupleState: Equatable {
    case unbound
    case bound(CoupleResponse)
}

@Observable
class AppState {
    var authState: AuthState = .unauthenticated
    var coupleState: CoupleState = .unbound
    var isOnline = true

    private let monitor = NWPathMonitor()
    private var didStartMonitoring = false

    var isAuthenticated: Bool {
        if case .authenticated = authState {
            return true
        }
        return false
    }

    var isBound: Bool {
        if case .bound = coupleState {
            return true
        }
        return false
    }

    var currentUser: UserResponse? {
        if case .authenticated(let user) = authState {
            return user
        }
        return nil
    }

    var currentCouple: CoupleResponse? {
        if case .bound(let couple) = coupleState {
            return couple
        }
        return nil
    }

    func startNetworkMonitoring() {
        guard !didStartMonitoring else { return }
        didStartMonitoring = true
        monitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor in
                self?.isOnline = path.status == .satisfied
            }
        }
        monitor.start(queue: DispatchQueue(label: "NetworkMonitor"))
    }

    func logout() {
        AuthInterceptor.clearTokens()
        authState = .unauthenticated
        coupleState = .unbound
    }
}
