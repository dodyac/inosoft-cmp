package com.ahmaddody.newsreader.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NewsApiResponse(
    val status: String = "",
    val totalResults: Int? = null,
    val articles: List<ArticleDto>? = null,
    val code: String? = null,
    val message: String? = null,
)

@Serializable
internal data class ArticleDto(
    val source: SourceDto? = null,
    val author: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    @SerialName("urlToImage") val imageUrl: String? = null,
    val publishedAt: String? = null,
    val content: String? = null,
)

@Serializable
internal data class SourceDto(
    val id: String? = null,
    val name: String? = null,
)
