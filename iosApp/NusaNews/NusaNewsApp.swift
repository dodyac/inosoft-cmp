import SwiftUI
import NusaNewsShared

@main
struct NusaNewsApp: App {
    init() {
        AppConfiguration.warnAboutMissingApiKey()
        SharedModule_iosKt.doInitKoinIos(
            apiKey: AppConfiguration.newsApiKey,
            enableNetworkLogs: AppConfiguration.isDebugBuild
        )
    }

    var body: some Scene {
        WindowGroup {
            ArticleListView()
        }
    }
}

enum AppConfiguration {
    /// Supplied by Configuration/Secrets.xcconfig at build time; never committed.
    static var newsApiKey: String {
        let value = Bundle.main.object(forInfoDictionaryKey: "NewsApiKey") as? String
        return value?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    }

    /// Configuration problems are a developer concern, so they are reported to the console
    /// instead of the UI. Readers only ever see a plain "news isn't available" message.
    static func warnAboutMissingApiKey() {
        guard newsApiKey.isEmpty else { return }
        print(
            """
            [NewsReaderSetup] NEWS_API_KEY is not configured. Copy \
            iosApp/Configuration/Secrets.xcconfig.example to Secrets.xcconfig and add your key, \
            then rebuild. The app will run with cached data only.
            """
        )
    }

    static var isDebugBuild: Bool {
        #if DEBUG
        return true
        #else
        return false
        #endif
    }
}
