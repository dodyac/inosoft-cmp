package com.ahmaddody.newsreader.data.local

import com.ahmaddody.newsreader.domain.model.Article
import com.ahmaddody.newsreader.domain.model.NewsFeed
import kotlinx.coroutines.flow.map

internal class RoomArticleLocalDataSource(
    private val dao: ArticleDao,
) : ArticleLocalDataSource {
    override fun observeArticles(feed: NewsFeed) = dao.observeAll(feed.name).map { entities ->
        entities.map(ArticleEntity::toDomain)
    }

    override fun observeArticle(articleId: String) = dao.observeById(articleId).map { entity ->
        entity?.toDomain()
    }

    override suspend fun hasArticles(feed: NewsFeed) = dao.hasArticles(feed.name)

    override suspend fun replaceAll(feed: NewsFeed, articles: List<Article>) {
        dao.replaceFeed(feed.name, articles.map { article -> article.toEntity(feed) })
    }
}
