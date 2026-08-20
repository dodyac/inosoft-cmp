import SwiftUI
import NusaNewsShared

struct ArticleListView: View {
    @StateObject private var model = ArticleFeedModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                NewsTopBar(title: NewsStrings.listTitle, subtitle: NewsStrings.listSubtitle)
                FeedSelector(
                    selectedFeed: model.selectedFeed,
                    onSelect: model.select(feed:)
                )
                content
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(NewsColor.background)
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: String.self) { articleId in
                ArticleDetailView(articleId: articleId)
            }
        }
        .tint(NewsColor.primary)
        .task { model.start() }
        .onDisappear { model.stop() }
    }

    @ViewBuilder
    private var content: some View {
        if model.isInitialLoading {
            LoadingState()
        } else if let blockingError = model.blockingError {
            StatusState(
                systemImage: "wifi.exclamationmark",
                title: NewsStrings.errorTitle,
                message: blockingError.displayMessage,
                onRetry: model.retry
            )
        } else if model.articles.isEmpty {
            StatusState(
                systemImage: "newspaper",
                title: NewsStrings.emptyTitle,
                message: NewsStrings.emptyDescription,
                onRetry: model.retry
            )
        } else {
            feed
        }
    }

    private var feed: some View {
        ScrollView {
            LazyVStack(spacing: NewsSpacing.md) {
                if model.showingSavedArticles {
                    SavedArticlesNotice()
                }

                ForEach(model.articles, id: \.id) { article in
                    NavigationLink(value: article.id) {
                        ArticleCard(article: article)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, NewsSpacing.lg)
            .padding(.top, NewsSpacing.sm)
            .padding(.bottom, NewsSpacing.xxl)
        }
        .refreshable { await model.refresh() }
        .overlay(alignment: .bottom) {
            if let messageText = model.messageText {
                MessageSnackbar(text: messageText, onRetry: model.retry)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.2), value: model.messageText)
        .task(id: model.messageText) {
            guard model.messageText != nil else { return }
            // SnackbarDuration.Long on Android is ten seconds.
            try? await Task.sleep(for: .seconds(10))
            guard !Task.isCancelled else { return }
            model.consumeMessage()
        }
    }
}

/// The counterpart of Android's `FilterChip` row.
private struct FeedSelector: View {
    let selectedFeed: NewsFeed
    let onSelect: (NewsFeed) -> Void

    var body: some View {
        HStack(spacing: NewsSpacing.sm) {
            ForEach(NewsFeed.all, id: \.self) { feed in
                let isSelected = feed == selectedFeed
                Button {
                    onSelect(feed)
                } label: {
                    Text(feed.title)
                        .font(.newsBodyMedium)
                        .padding(.horizontal, NewsSpacing.lg)
                        .padding(.vertical, NewsSpacing.sm)
                        .background(
                            RoundedRectangle(cornerRadius: NewsRadius.card)
                                .fill(isSelected ? NewsColor.primaryContainer : Color.clear)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: NewsRadius.card)
                                .stroke(isSelected ? Color.clear : NewsColor.outline)
                        )
                        .foregroundStyle(
                            isSelected ? NewsColor.onPrimaryContainer : NewsColor.onSurfaceVariant
                        )
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, NewsSpacing.lg)
        .padding(.bottom, NewsSpacing.sm)
        .background(NewsColor.background)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(NewsStrings.feedSelector)
    }
}

private struct ArticleCard: View {
    let article: Article

    var body: some View {
        HStack(alignment: .top, spacing: NewsSpacing.md) {
            ArticleThumbnail(article: article)
                .frame(width: NewsSize.listImageWidth, height: NewsSize.listImageHeight)
                .clipShape(RoundedRectangle(cornerRadius: NewsRadius.image))

            VStack(alignment: .leading, spacing: NewsSpacing.xs) {
                Text(article.sourceName ?? NewsStrings.sourceUnknown)
                    .font(.newsLabelMedium)
                    .foregroundStyle(NewsColor.primary)
                    .lineLimit(1)
                Text(article.title)
                    .font(.newsTitleMedium)
                    .foregroundStyle(NewsColor.onBackground)
                    .lineSpacing(NewsLineSpacing.titleMedium)
                    .lineLimit(3)
                Text(article.description_ ?? NewsStrings.descriptionUnavailable)
                    .font(.newsBodyMedium)
                    .foregroundStyle(NewsColor.onSurfaceVariant)
                    .lineLimit(2)
                Text(NewsFacadeKt.formatPublicationDateForDisplay(isoTimestamp: article.publishedAt))
                    .font(.newsLabelMedium)
                    .foregroundStyle(NewsColor.onSurfaceVariant)
                    .padding(.top, NewsSpacing.xs)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(NewsSpacing.md)
        .background(NewsColor.cardSurface, in: RoundedRectangle(cornerRadius: NewsRadius.card))
        .shadow(color: .black.opacity(0.06), radius: 6, y: 2)
    }
}

struct ArticleThumbnail: View {
    let article: Article

    var body: some View {
        if let imageUrl = article.imageUrl, let url = URL(string: imageUrl) {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().scaledToFill()
                case .failure:
                    placeholder
                default:
                    NewsColor.surfaceVariant
                }
            }
            .background(NewsColor.surfaceVariant)
            .clipped()
            .accessibilityLabel(NewsStrings.imageDescription(for: article.title))
        } else {
            placeholder
        }
    }

    private var placeholder: some View {
        NewsColor.surfaceVariant
            .overlay {
                Image(systemName: "photo")
                    .foregroundStyle(NewsColor.onSurfaceVariant)
            }
            .accessibilityLabel(NewsStrings.imageUnavailable)
    }
}

private struct SavedArticlesNotice: View {
    var body: some View {
        HStack(alignment: .top, spacing: NewsSpacing.md) {
            Image(systemName: "icloud.slash")
            VStack(alignment: .leading, spacing: NewsSpacing.xs) {
                Text(NewsStrings.savedTitle).font(.newsTitleMedium)
                Text(NewsStrings.savedDescription)
                    .font(.newsBodyMedium)
                    .lineSpacing(NewsLineSpacing.bodyMedium)
            }
        }
        .padding(NewsSpacing.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .foregroundStyle(NewsColor.onPrimaryContainer)
        .background(
            NewsColor.primaryContainer,
            in: RoundedRectangle(cornerRadius: NewsRadius.card)
        )
    }
}

/// Styled after the Material 3 snackbar Android shows for a recoverable refresh failure.
private struct MessageSnackbar: View {
    let text: String
    let onRetry: () -> Void

    var body: some View {
        HStack(spacing: NewsSpacing.md) {
            Text(text)
                .font(.newsBodyMedium)
                .foregroundStyle(NewsColor.inverseOnSurface)
            Spacer(minLength: NewsSpacing.sm)
            Button(NewsStrings.retry, action: onRetry)
                .font(.newsLabelMedium)
                .foregroundStyle(NewsColor.inversePrimary)
        }
        .padding(NewsSpacing.lg)
        .background(NewsColor.inverseSurface, in: RoundedRectangle(cornerRadius: NewsRadius.image))
        .padding(.horizontal, NewsSpacing.lg)
        .padding(.bottom, NewsSpacing.md)
    }
}

private struct LoadingState: View {
    var body: some View {
        VStack(spacing: NewsSpacing.lg) {
            ProgressView().tint(NewsColor.primary)
            Text(NewsStrings.loading)
                .font(.newsBodyLarge)
                .foregroundStyle(NewsColor.onBackground)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(NewsColor.background)
    }
}

private struct StatusState: View {
    let systemImage: String
    let title: String
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Image(systemName: systemImage)
                .font(.system(size: NewsSize.emptyIcon * 0.62))
                .frame(width: NewsSize.emptyIcon, height: NewsSize.emptyIcon)
                .foregroundStyle(NewsColor.onSurfaceVariant)

            Spacer().frame(height: NewsSpacing.lg)
            Text(title)
                .font(.newsTitleLarge)
                .foregroundStyle(NewsColor.onBackground)

            Spacer().frame(height: NewsSpacing.sm)
            Text(message)
                .font(.newsBodyLarge)
                .foregroundStyle(NewsColor.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .lineSpacing(NewsLineSpacing.bodyLarge)

            Spacer().frame(height: NewsSpacing.xl)
            Button(action: onRetry) {
                Text(NewsStrings.retry)
                    .font(.newsBodyMedium)
                    .padding(.horizontal, NewsSpacing.xl)
                    .padding(.vertical, NewsSpacing.md)
                    .background(NewsColor.primary, in: Capsule())
                    .foregroundStyle(NewsColor.onPrimary)
            }
            .buttonStyle(.plain)
        }
        .padding(NewsSpacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(NewsColor.background)
    }
}
