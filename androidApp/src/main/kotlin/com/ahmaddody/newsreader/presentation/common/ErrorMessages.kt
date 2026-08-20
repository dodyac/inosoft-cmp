package com.ahmaddody.newsreader.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahmaddody.newsreader.R
import com.ahmaddody.newsreader.domain.model.AppError

@Composable
fun errorMessage(error: AppError): String = stringResource(
    when (error) {
        AppError.MissingApiKey -> R.string.error_missing_api_key
        AppError.NetworkUnavailable -> R.string.error_network
        AppError.Timeout -> R.string.error_timeout
        AppError.Unauthorized -> R.string.error_unauthorized
        AppError.RateLimited -> R.string.error_rate_limited
        AppError.RequestRejected -> R.string.error_request_rejected
        AppError.ServerUnavailable -> R.string.error_server
        AppError.MalformedResponse -> R.string.error_malformed_response
        AppError.DatabaseUnavailable -> R.string.error_database
        AppError.ArticleNotFound -> R.string.error_article_not_found
        AppError.Unknown -> R.string.error_unknown
    },
)

