import SwiftUI

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        let red = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let green = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let blue = Double(rgbValue & 0x0000FF) / 255.0
        self.init(red: red, green: green, blue: blue)
    }
}

enum AppColors {
    static let primaryPurple = Color(hex: "A855F7")
    static let primaryPink = Color(hex: "EC4899")
    static let bgLight = Color(hex: "F3E8FF")
    static let bgLightPink = Color(hex: "FCE7F3")
    static let cardBackground = Color.white.opacity(0.8)
    static let textPrimary = Color(hex: "1F1F1F")
    static let textSecondary = Color(hex: "6B7280")
    static let success = Color(hex: "10B981")
    static let error = Color(hex: "E11D48")

    static let primaryGradient = LinearGradient(
        colors: [primaryPurple, primaryPink],
        startPoint: .leading,
        endPoint: .trailing
    )

    static let backgroundGradient = LinearGradient(
        colors: [bgLight, bgLightPink],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
}
