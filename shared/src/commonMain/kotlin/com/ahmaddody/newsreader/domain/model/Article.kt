package com.ahmaddody.newsreader.domain.model

data class Article(
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val publishedAt: String,
    val sourceName: String?,
    val articleUrl: String?,
)

