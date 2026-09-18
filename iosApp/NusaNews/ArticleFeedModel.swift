import Foundation
import NusaNewsShared

/// iOS counterpart of the Android `ArticleListViewModel`.
///
/// All policy (what counts as a usable cache, which failure blocks the screen) already lives in
/// shared Kotlin; this type only turns the shared results into observable view state.
@MainActor
final class ArticleFeedModel: ObservableObject {
    @Published private(set) var articles: [Article] = []
    @Published private(set) var isInitialLoading = true
    @Published private(set) var isRefreshing = false
    @Published private(set) var showingSavedArticles = false
    @Published private(set) var blockingError: AppError?
    /// Kotlin's `AppError` is a protocol type, which cannot be `Equatable`; SwiftUI needs an
    /// equatable value to drive `task(id:)` and `animation(value:)`, so the view observes the text.
    @Published private(set) var messageText: String?
    @Published private(set) var selectedFeed: NewsFeed = NewsFeed.Companion.shared.Default

    private let facade: NewsFacade
    private var subscription: SubscriptionHandle?
    private var syncStates: [NewsFeed: SyncState] = [:]
    private var localError: AppError?
    private var feedsInFlight: Set<NewsFeed> = []
    private var feedsWithConsumedMessage: Set<NewsFeed> = []

    private var syncState: SyncState { syncStates[selectedFeed] ?? .loading }

    init(facade: NewsFacade = NewsFacadeKt.newsFacade()) {
        self.facade = facade
    }

    func start() {
        guard subscription == nil else { return }
        facade.trackScreenViewed(screenName: Screens.shared.ArticleList)
        observeSelectedFeed()
        Task { await refresh() }
    }

    /// Switching tabs shows that feed's cache immediately and syncs it only the first time,
    /// so returning to a tab does not spend a request the free NewsAPI plan may not have.
    func select(feed: NewsFeed) {
        guard feed != selectedFeed else { return }
        selectedFeed = feed
        facade.trackFeedSelected(feed: feed)
        articles = []
        observeSelectedFeed()
        recomputeState()
        if syncStates[feed] == nil {
            Task { await refresh() }
        }
    }

    private func observeSelectedFeed() {
        subscription?.cancel()
        subscription = facade.observeArticles(
            feed: selectedFeed,
            onArticles: { [weak self] articles in
                guard let self else { return }
                self.localError = nil
                self.articles = articles
                self.recomputeState()
            },
            onError: { [weak self] error in
                guard let self else { return }
                self.localError = error
                self.recomputeState()
            }
        )
    }

    func stop() {
        subscription?.cancel()
        subscription = nil
    }

    func refresh() async {
        // Mirrors the Android guard: a second pull while a sync is in flight is a no-op.
        let feed = selectedFeed
        guard !feedsInFlight.contains(feed) else { return }
        facade.trackRefreshRequested(feed: feed)
        feedsInFlight.insert(feed)
        defer { feedsInFlight.remove(feed) }

        syncStates[feed] = .loading
        feedsWithConsumedMessage.remove(feed)
        recomputeState()

        do {
            let result = try await facade.refresh(feed: feed)
            syncStates[feed] = SyncState(result)
        } catch {
            // The shared layer already maps expected failures; a throw here means cancellation.
            return
        }
        recomputeState()
    }

    func retry() {
        Task { await refresh() }
    }

    /// Mirrors the Android snackbar, which disappears on its own instead of blocking the feed.
    func consumeMessage() {
        feedsWithConsumedMessage.insert(selectedFeed)
        recomputeState()
    }

    private func recomputeState() {
        let hasArticles = !articles.isEmpty
        let failure = syncState.failure

        if let localError {
            isInitialLoading = false
            isRefreshing = false
            showingSavedArticles = false
            blockingError = localError
            messageText = nil
            return
        }

        // Room has not delivered the rows a successful sync just wrote yet.
        let awaitingCommittedArticles = syncState.isSuccessWithContent && !hasArticles
        let awaitingKnownCache = failure?.hasUsableCache == true && !hasArticles

        isInitialLoading = (syncState.isLoading || awaitingCommittedArticles || awaitingKnownCache)
            && !hasArticles
        isRefreshing = syncState.isLoading && hasArticles
        showingSavedArticles = failure != nil && hasArticles
        blockingError = (failure?.hasUsableCache == false && !hasArticles) ? failure?.error : nil
        let messageAlreadySeen = feedsWithConsumedMessage.contains(selectedFeed)
        let showMessage = failure != nil && hasArticles && !messageAlreadySeen
        messageText = showMessage ? failure?.error.displayMessage : nil
    }

    private enum SyncState {
        case loading
        case success(updatedCount: Int32)
        case failure(error: AppError, hasUsableCache: Bool)

        init(_ result: RefreshResult) {
            switch result {
            case let success as RefreshResultSuccess:
                self = .success(updatedCount: success.updatedCount)
            case let failure as RefreshResultFailure:
                self = .failure(error: failure.error, hasUsableCache: failure.hasUsableCache)
            default:
                self = .failure(error: AppErrorUnknown.shared, hasUsableCache: false)
            }
        }

        var isLoading: Bool {
            if case .loading = self { return true }
            return false
        }

        var isSuccessWithContent: Bool {
            if case let .success(updatedCount) = self { return updatedCount > 0 }
            return false
        }

        var failure: (error: AppError, hasUsableCache: Bool)? {
            if case let .failure(error, hasUsableCache) = self { return (error, hasUsableCache) }
            return nil
        }
    }
}
