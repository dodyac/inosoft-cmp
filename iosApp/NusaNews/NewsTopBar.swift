import SwiftUI

/// The shared top bar, matching Android's Material `TopAppBar`.
///
/// SwiftUI's navigation bar is deliberately not used: on iOS 26 it restyles toolbar items into
/// glass capsules and truncates a leading title, which drifts away from the Android layout. Drawing
/// the bar directly keeps one design on both platforms — a leading back arrow, a left-aligned
/// title, and an optional subtitle in the primary colour.
struct NewsTopBar: View {
    let title: String
    var subtitle: String?
    var onBack: (() -> Void)?

    var body: some View {
        HStack(spacing: NewsSpacing.sm) {
            if let onBack {
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundStyle(NewsColor.onBackground)
                        .frame(width: 44, height: 44)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(NewsStrings.navigateBack)
            }

            VStack(alignment: .leading, spacing: 0) {
                Text(title)
                    .font(.newsTitleLarge)
                    .foregroundStyle(NewsColor.onBackground)
                    .lineLimit(1)
                if let subtitle {
                    Text(subtitle)
                        .font(.newsLabelMedium)
                        .foregroundStyle(NewsColor.primary)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, onBack == nil ? NewsSpacing.lg : NewsSpacing.sm)
        .padding(.vertical, NewsSpacing.sm)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(NewsColor.background)
        .accessibilityElement(children: .contain)
    }
}
