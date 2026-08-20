package com.ahmaddody.newsreader.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ahmaddody.newsreader.presentation.detail.ArticleDetailRoute
import com.ahmaddody.newsreader.presentation.detail.ArticleDetailViewModel
import com.ahmaddody.newsreader.presentation.list.ArticleListRoute
import com.ahmaddody.newsreader.presentation.list.ArticleListViewModel
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
internal data object ArticleListDestination

@Serializable
internal data class ArticleDetailDestination(val articleId: String)

@Composable
fun NewsNavHost(
    listViewModel: ArticleListViewModel = koinViewModel(),
    detailViewModelProvider: @Composable (String) -> ArticleDetailViewModel = { articleId ->
        koinViewModel(parameters = { parametersOf(articleId) })
    },
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ArticleListDestination,
    ) {
        composable<ArticleListDestination> {
            ArticleListRoute(
                viewModel = listViewModel,
                onArticleClick = { articleId ->
                    navController.navigate(ArticleDetailDestination(articleId))
                },
            )
        }

        composable<ArticleDetailDestination> { backStackEntry ->
            val destination = backStackEntry.toRoute<ArticleDetailDestination>()
            ArticleDetailRoute(
                viewModel = detailViewModelProvider(destination.articleId),
                onBack = navController::popBackStack,
            )
        }
    }
}

