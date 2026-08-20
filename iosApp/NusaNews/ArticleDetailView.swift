import SwiftUI
import NusaNewsShared

struct ArticleDetailView: View {
    let articleId: String
    @StateObject private var model: ArticleDetailModel
    @State private var showImageViewer = false
    @Environment(\.dismiss) private var dismiss

    init(articleId: String) {
        self.articleId = articleId
        _model = StateObject(wrappedValue: ArticleDetailModel(articleId: articleId))
    }

    var body: some View {
        VStack(spacing: 0) {
            NewsTopBar(title: NewsStrings.detailTitle, onBack: { dismiss() })
            content
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(NewsColor.background)
        .toolbar(.hidden, for: .navigationBar)
        .tint(NewsColor.primary)
        .task { model.start() }
        .onDisappear { model.stop() }
    }

    @ViewBuilder
    private var content: some View {
        Group {
            if model.isLoading {
                ProgressView()
                    .tint(NewsColor.primary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let article = model.article {
                detail(article)
            } else if let error = model.error {
                VStack(spacing: NewsSpacing.lg) {
                    Image(systemName: "newspaper")
                        .font(.system(size: NewsSize.emptyIcon * 0.62))
                        .frame(width: NewsSize.emptyIcon, height: NewsSize.emptyIcon)
                        .foregroundStyle(NewsColor.onSurfaceVariant)
                    Text(error.displayMessage)
                        .font(.newsBodyLarge)
                        .foregroundStyle(NewsColor.onBackground)
                        .multilineTextAlignment(.center)
                        .lineSpacing(NewsLineSpacing.bodyLarge)
                }
                .padding(NewsSpacing.xl)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func detail(_ article: Article) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                ArticleThumbnail(article: article)
                    .frame(maxWidth: .infinity)
                    .frame(height: NewsSize.detailImageHeight)
                    .clipShape(RoundedRectangle(cornerRadius: NewsRadius.card))
                    .contentShape(Rectangle())
                    .onTapGesture {
                        if article.imageUrl != nil { showImageViewer = true }
                    }
                    .accessibilityAddTraits(article.imageUrl == nil ? [] : .isButton)
                    .accessibilityHint(
                        article.imageUrl == nil ? "" : NewsStrings.imageViewerOpen
                    )
                    .fullScreenCover(isPresented: $showImageViewer) {
                        if let imageUrl = article.imageUrl {
                            ImageViewer(
                                imageUrl: imageUrl,
                                imageLabel: NewsStrings.imageDescription(for: article.title),
                                onDismiss: { showImageViewer = false }
                            )
                        }
                    }

                Spacer().frame(height: NewsSpacing.xl)
                Text(article.sourceName ?? NewsStrings.sourceUnknown)
                    .font(.newsLabelMedium)
                    .foregroundStyle(NewsColor.primary)

                Spacer().frame(height: NewsSpacing.sm)
                Text(article.title)
                    .font(.newsHeadline)
                    .foregroundStyle(NewsColor.onBackground)
                    .lineSpacing(NewsLineSpacing.headline)

                Spacer().frame(height: NewsSpacing.md)
                Text(NewsFacadeKt.formatPublicationDateForDisplay(isoTimestamp: article.publishedAt))
                    .font(.newsLabelMedium)
                    .foregroundStyle(NewsColor.onSurfaceVariant)

                Spacer().frame(height: NewsSpacing.xl)
                Text(article.description_ ?? NewsStrings.descriptionUnavailable)
                    .font(.newsBodyLarge)
                    .foregroundStyle(NewsColor.onBackground)
                    .lineSpacing(NewsLineSpacing.bodyLarge)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, NewsSpacing.lg)
            .padding(.bottom, NewsSpacing.xxl)
        }
    }
}

/// iOS counterpart of the Android `ArticleDetailViewModel`: observe one cached article, never refetch.
@MainActor
final class ArticleDetailModel: ObservableObject {
    @Published private(set) var isLoading = true
    @Published private(set) var article: Article?
    @Published private(set) var error: AppError?

    private let articleId: String
    private let facade: NewsFacade
    private var subscription: SubscriptionHandle?

    init(articleId: String, facade: NewsFacade = NewsFacadeKt.newsFacade()) {
        self.articleId = articleId
        self.facade = facade
    }

    func start() {
        guard subscription == nil else { return }
        subscription = facade.observeArticle(
            articleId: articleId,
            onArticle: { [weak self] article in
                guard let self else { return }
                self.isLoading = false
                self.article = article
                self.error = article == nil ? AppErrorArticleNotFound.shared : nil
            },
            onError: { [weak self] error in
                guard let self else { return }
                self.isLoading = false
                self.error = error
            }
        )
    }

    func stop() {
        subscription?.cancel()
        subscription = nil
    }
}
