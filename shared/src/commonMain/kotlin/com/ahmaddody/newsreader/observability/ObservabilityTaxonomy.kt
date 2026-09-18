package com.ahmaddody.newsreader.observability

/**
 * The trace, event and key names are declared once here because Firebase name limits are real
 * limits, not style guidance: trace and event names are case-sensitive, attribute names cap at 32
 * characters, a custom trace allows 5 attributes, and an app allows 500 distinct event types.
 * Improvised names produce dashboards that cannot be compared across releases or platforms.
 */
object Traces {
    /** Full offline-first refresh: network fetch, mapping and the local write. */
    const val RefreshArticles = "refresh_articles"

    /** Remote fetch only, so network cost can be separated from database cost. */
    const val FetchArticles = "fetch_articles"

    /** Delete + insert of a feed's cached articles. */
    const val CacheArticles = "cache_articles"

    object Attribute {
        const val Feed = "feed"
        const val Outcome = "outcome"
        const val Error = "error"
    }

    object Metric {
        const val ArticlesFetched = "articles_fetched"
        const val ArticlesCached = "articles_cached"
        const val ArticlesDropped = "articles_dropped"
    }

    object Outcome {
        const val Success = "success"
        const val Failure = "failure"
    }
}

/** snake_case, object_action. Firebase allows 500 distinct event types per app. */
object AnalyticsEvents {
    const val ScreenViewed = "screen_viewed"
    const val FeedSelected = "feed_selected"
    const val FeedRefreshRequested = "feed_refresh_requested"
    const val FeedRefreshSucceeded = "feed_refresh_succeeded"
    const val FeedRefreshFailed = "feed_refresh_failed"
    const val ArticleOpened = "article_opened"
    const val CachedContentShown = "cached_content_shown"
}

/** Bounded and low-cardinality. Never an article id, url, title or any free text. */
object AnalyticsParams {
    const val ScreenName = "screen_name"
    const val Feed = "feed"
    const val Error = "error"
    const val ArticleCount = "article_count"
    const val Source = "source"
}

object Screens {
    const val ArticleList = "article_list"
    const val ArticleDetail = "article_detail"
}

/** Crashlytics custom keys. [CrashKeys.SessionId] is the join key to the session log. */
object CrashKeys {
    const val SessionId = "session_id"
    const val Feed = "feed"
    const val Screen = "screen"
    const val LastAppError = "last_app_error"
    const val HasApiKey = "has_api_key"
}
