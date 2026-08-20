package com.ahmaddody.newsreader.domain.model

sealed interface AppError {
    data object MissingApiKey : AppError
    data object NetworkUnavailable : AppError
    data object Timeout : AppError
    data object Unauthorized : AppError
    data object RateLimited : AppError
    data object RequestRejected : AppError
    data object ServerUnavailable : AppError
    data object MalformedResponse : AppError
    data object DatabaseUnavailable : AppError
    data object ArticleNotFound : AppError
    data object Unknown : AppError
}

