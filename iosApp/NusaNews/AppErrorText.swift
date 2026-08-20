import Foundation
import NusaNewsShared

/// Mirrors `res/values/strings.xml` on Android so both platforms explain identical shared errors
/// with identical wording.
///
/// These strings are written for readers, not for developers: configuration problems such as a
/// missing or rejected API key are reported to the console instead, since a reader can do nothing
/// about them and should not be shown internal setup instructions.
extension AppError {
    var displayMessage: String {
        switch self {
        case is AppErrorMissingApiKey, is AppErrorUnauthorized:
            return "News isn’t available right now. Please try again later."
        case is AppErrorNetworkUnavailable:
            return "You appear to be offline. Check your connection and try again."
        case is AppErrorTimeout:
            return "This is taking longer than usual. Please try again."
        case is AppErrorRateLimited:
            return "You’ve refreshed a lot recently. Please try again in a few minutes."
        case is AppErrorRequestRejected:
            return "We couldn’t load the news right now. Please try again."
        case is AppErrorServerUnavailable:
            return "The news is temporarily unavailable. Please try again shortly."
        case is AppErrorMalformedResponse:
            return "Something went wrong while loading the news. Please try again."
        case is AppErrorDatabaseUnavailable:
            return "Your saved stories aren’t available right now. Please try again."
        case is AppErrorArticleNotFound:
            return "This story isn’t available anymore."
        default:
            return "Something went wrong. Please try again."
        }
    }
}

extension NewsFeed {
    /// Matches the Android chip labels so both platforms name the same shared feeds identically.
    var title: String {
        switch self {
        case NewsFeed.everything: return NewsStrings.feedEverything
        default: return NewsStrings.feedTopHeadlines
        }
    }

    static var all: [NewsFeed] { NewsFeed.entries }
}
