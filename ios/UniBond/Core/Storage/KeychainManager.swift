// KeychainManager.swift
// UniBond

import Foundation
import Security

final class KeychainManager {
    static let shared = KeychainManager()

    private enum Keys {
        static let accessToken = "accessToken"
        static let refreshToken = "refreshToken"
    }
    private let service: String

    init(service: String = "com.unibond.app") {
        self.service = service
    }

    var accessToken: String? {
        read(key: Keys.accessToken)
    }

    var refreshToken: String? {
        read(key: Keys.refreshToken)
    }

    func saveTokens(access: String, refresh: String) {
        save(key: Keys.accessToken, value: access)
        save(key: Keys.refreshToken, value: refresh)
    }

    func deleteTokens() {
        delete(key: Keys.accessToken)
        delete(key: Keys.refreshToken)
    }

    private func save(key: String, value: String) {
        let data = Data(value.utf8)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]

        let deleteStatus = SecItemDelete(query as CFDictionary)
        if deleteStatus != errSecSuccess, deleteStatus != errSecItemNotFound {
            assertionFailure("Failed to delete existing keychain item (\(deleteStatus)) for key: \(key)")
        }

        var addQuery = query
        addQuery[kSecValueData as String] = data
        addQuery[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        let addStatus = SecItemAdd(addQuery as CFDictionary, nil)
        if addStatus != errSecSuccess {
            assertionFailure("Failed to save keychain item (\(addStatus)) for key: \(key)")
        }
    }

    private func read(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private func delete(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]

        let status = SecItemDelete(query as CFDictionary)
        if status != errSecSuccess, status != errSecItemNotFound {
            assertionFailure("Failed to delete keychain item (\(status)) for key: \(key)")
        }
    }
}
