package com.ahmaddody.newsreader.presentation.di

import com.ahmaddody.newsreader.presentation.detail.ArticleDetailViewModel
import com.ahmaddody.newsreader.presentation.list.ArticleListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { ArticleListViewModel(observeArticles = get(), refreshArticles = get()) }
    viewModel { parameters ->
        ArticleDetailViewModel(
            articleId = parameters.get(),
            observeArticle = get(),
        )
    }
}
