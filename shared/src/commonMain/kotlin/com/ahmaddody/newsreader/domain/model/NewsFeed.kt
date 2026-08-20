package com.ahmaddody.newsreader.domain.model

/**
 * The two NewsAPI collections this app can read.
 *
 * `/v2/top-headlines?country=id` is the endpoint named in the brief, but free NewsAPI plans have
 * sparse regional source mapping and frequently return an empty list for Indonesia. [Everything]
 * queries `/v2/everything` by keyword instead, so the app always has usable content.
 * Each feed is cached separately, so switching tabs never discards the other feed's offline copy.
 */
enum class NewsFeed {
    TopHeadlines,
    Everything,
    ;

    companion object {
        val Default = TopHeadlines
    }
}
