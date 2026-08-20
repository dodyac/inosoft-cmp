import SwiftUI
import NusaNewsShared

private let minScale: CGFloat = 1
private let maxScale: CGFloat = 5
private let doubleTapScale: CGFloat = 2.5

/// Full-screen viewer for the article image, matching Android's `ZoomableImageViewer`:
/// pinch to zoom, drag to pan, double-tap to toggle.
///
/// Panning is clamped to the scaled image bounds so the picture cannot be flung off-screen, and
/// zooming back out snaps to centre.
struct ImageViewer: View {
    let imageUrl: String
    let imageLabel: String
    let onDismiss: () -> Void

    @State private var scale: CGFloat = minScale
    @State private var committedScale: CGFloat = minScale
    @State private var offset: CGSize = .zero
    @State private var committedOffset: CGSize = .zero

    var body: some View {
        // Only the backdrop and the image bleed into the safe area; the close button stays inside
        // it, otherwise it lands under the Dynamic Island.
        ZStack(alignment: .topLeading) {
            Color.black.ignoresSafeArea()

            GeometryReader { proxy in
                zoomableImage(in: proxy.size)
                    .frame(width: proxy.size.width, height: proxy.size.height)
            }
            .ignoresSafeArea()

            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
                    .background(Color.black.opacity(0.35), in: Circle())
            }
            .buttonStyle(.plain)
            .padding(NewsSpacing.sm)
            .accessibilityLabel(NewsStrings.imageViewerClose)
        }
        .animation(.easeInOut(duration: 0.2), value: scale)
    }

    private func zoomableImage(in size: CGSize) -> some View {
        AsyncImage(url: URL(string: imageUrl)) { phase in
            switch phase {
            case .success(let image):
                image.resizable().scaledToFit()
            case .failure:
                Image(systemName: "photo")
                    .font(.system(size: NewsSize.emptyIcon))
                    .foregroundStyle(.white.opacity(0.6))
            default:
                ProgressView().tint(.white)
            }
        }
        .frame(width: size.width, height: size.height)
        .scaleEffect(scale)
        .offset(offset)
        .gesture(
            SimultaneousGesture(
                magnifyGesture(in: size),
                panGesture(in: size)
            )
        )
        .onTapGesture(count: 2) { toggleZoom() }
        .accessibilityLabel(imageLabel)
    }

    private func magnifyGesture(in size: CGSize) -> some Gesture {
        MagnifyGesture()
            .onChanged { value in
                scale = (committedScale * value.magnification).clamped(to: minScale...maxScale)
                offset = clamp(offset, in: size, scale: scale)
            }
            .onEnded { _ in
                committedScale = scale
                if scale <= minScale { resetPan() } else { committedOffset = offset }
            }
    }

    private func panGesture(in size: CGSize) -> some Gesture {
        DragGesture()
            .onChanged { value in
                guard scale > minScale else { return }
                let candidate = CGSize(
                    width: committedOffset.width + value.translation.width,
                    height: committedOffset.height + value.translation.height
                )
                offset = clamp(candidate, in: size, scale: scale)
            }
            .onEnded { _ in committedOffset = offset }
    }

    private func toggleZoom() {
        if scale > minScale {
            scale = minScale
            committedScale = minScale
            resetPan()
        } else {
            scale = doubleTapScale
            committedScale = doubleTapScale
        }
    }

    private func resetPan() {
        offset = .zero
        committedOffset = .zero
    }

    private func clamp(_ candidate: CGSize, in size: CGSize, scale: CGFloat) -> CGSize {
        guard scale > minScale else { return .zero }
        let maxX = size.width * (scale - 1) / 2
        let maxY = size.height * (scale - 1) / 2
        return CGSize(
            width: candidate.width.clamped(to: -maxX...maxX),
            height: candidate.height.clamped(to: -maxY...maxY)
        )
    }
}

private extension CGFloat {
    func clamped(to range: ClosedRange<CGFloat>) -> CGFloat {
        Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}
