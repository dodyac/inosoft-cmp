package com.ahmaddody.newsreader.domain.model

sealed interface RefreshResult {
    data class Success(val updatedCount: Int) : RefreshResult

    data class Failure(
        val error: AppError,
        val hasUsableCache: Boolean,
    ) : RefreshResult
}

