import SwiftUI
import UIKit

/// The iOS half of the shared visual language.
///
/// Every value here mirrors the Android theme so the two clients read as one product:
/// colors come from `presentation/theme/Theme.kt`, metrics from `presentation/common/NewsDimens.kt`,
/// and text styles from `presentation/theme/Type.kt`. Changing a token on one platform means
/// changing it on the other.
enum NewsColor {
    static let primary = dynamic(light: 0x006C74, dark: 0x80D4D9)
    static let onPrimary = dynamic(light: 0xFFFFFF, dark: 0x00373B)
    static let primaryContainer = dynamic(light: 0x9CF1F5, dark: 0x005057)
    static let onPrimaryContainer = dynamic(light: 0x002021, dark: 0x9CF1F5)
    static let background = dynamic(light: 0xFCFDFC, dark: 0x101416)
    static let onBackground = dynamic(light: 0x17223B, dark: 0xE0E3E5)
    static let surfaceVariant = dynamic(light: 0xF2F7F7, dark: 0x253033)
    static let onSurfaceVariant = dynamic(light: 0x4A5453, dark: 0xBFC8C7)
    static let error = dynamic(light: 0xB3261E, dark: 0xFFB4AB)

    /// Material 3 derives `surfaceContainerLow` and `inverseSurface` from the tonal palette;
    /// these are the resolved values for the Nusa News scheme.
    static let cardSurface = dynamic(light: 0xF4F7F7, dark: 0x171C1E)
    static let outline = dynamic(light: 0xD3DEDD, dark: 0x2F3B3D)
    static let inverseSurface = dynamic(light: 0x2B3133, dark: 0xE0E3E5)
    static let inverseOnSurface = dynamic(light: 0xEFF1F1, dark: 0x2B3133)
    static let inversePrimary = dynamic(light: 0x80D4D9, dark: 0x006C74)

    private static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { traits in
            UIColor(rgb: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
}

/// Mirrors `NewsDimens` on Android.
enum NewsSpacing {
    static let xs: CGFloat = 4
    static let sm: CGFloat = 8
    static let md: CGFloat = 12
    static let lg: CGFloat = 16
    static let xl: CGFloat = 24
    static let xxl: CGFloat = 32
}

enum NewsRadius {
    static let card: CGFloat = 20
    static let image: CGFloat = 14
}

enum NewsSize {
    static let listImageWidth: CGFloat = 124
    static let listImageHeight: CGFloat = 108
    static let detailImageHeight: CGFloat = 240
    static let emptyIcon: CGFloat = 56
}

/// Mirrors `NewsTypography` on Android. `lineSpacing` is the Compose line height minus the font
/// size, which is how SwiftUI expresses the same leading.
extension Font {
    static let newsHeadline = Font.system(size: 28, weight: .bold)
    static let newsTitleLarge = Font.system(size: 22, weight: .bold)
    static let newsTitleMedium = Font.system(size: 17, weight: .semibold)
    static let newsBodyLarge = Font.system(size: 16)
    static let newsBodyMedium = Font.system(size: 14)
    static let newsLabelMedium = Font.system(size: 12, weight: .medium)
}

enum NewsLineSpacing {
    static let headline: CGFloat = 6
    static let bodyLarge: CGFloat = 9
    static let bodyMedium: CGFloat = 6
    static let titleMedium: CGFloat = 5
}

/// The copy shown on both platforms. Android keeps the same strings in `res/values/strings.xml`.
enum NewsStrings {
    static let listTitle = "Top stories"
    static let listSubtitle = "Indonesia"
    static let detailTitle = "News Detail"
    static let retry = "Try again"
    static let loading = "Finding today’s stories…"
    static let savedTitle = "You’re reading saved stories"
    static let savedDescription = "A refresh failed, but your last downloaded news is still available."
    static let emptyTitle = "No stories yet"
    static let emptyDescription = "Pull down to refresh, or try again in a moment."
    static let errorTitle = "We couldn’t load the news"
    static let descriptionUnavailable = "No description is available for this story."
    static let sourceUnknown = "Unknown source"
    static let imageUnavailable = "Image unavailable"
    static let imageViewerOpen = "Open image full screen"
    static let imageViewerClose = "Close image"
    static let navigateBack = "Navigate back"
    static let feedSelector = "News feed"
    static let feedTopHeadlines = "Top headlines"
    static let feedEverything = "Everything"

    static func imageDescription(for title: String) -> String { "Image for \(title)" }
}

private extension UIColor {
    convenience init(rgb: UInt32) {
        self.init(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1
        )
    }
}
