package com.simon.fehcatalog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.simon.fehcatalog.FehApplication
import com.simon.fehcatalog.data.AppContainer
import com.simon.fehcatalog.ui.catalog.CatalogScreen
import com.simon.fehcatalog.ui.catalog.CatalogViewModel
import com.simon.fehcatalog.ui.detail.HeroDetailScreen
import com.simon.fehcatalog.ui.detail.HeroDetailViewModel

@Composable
fun FehCatalogApp() {
    val container = rememberFehContainer()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.CATALOG
    ) {
        composable(Routes.CATALOG) {
            val viewModel: CatalogViewModel = viewModel(
                factory = CatalogViewModel.factory(container.heroRepository)
            )
            CatalogScreen(
                viewModel = viewModel,
                onHeroClick = { heroId -> navController.navigate(Routes.detail(heroId)) }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument(Routes.HERO_ID_ARG) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val heroId = backStackEntry.arguments?.getLong(Routes.HERO_ID_ARG) ?: 0L
            val viewModel: HeroDetailViewModel = viewModel(
                factory = HeroDetailViewModel.factory(container.heroRepository, heroId)
            )
            HeroDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun rememberFehContainer(): AppContainer {
    val context = LocalContext.current
    return remember(context) {
        (context.applicationContext as FehApplication).container
    }
}

private object Routes {
    const val HERO_ID_ARG = "heroId"
    const val CATALOG = "catalog"
    const val DETAIL = "detail/{$HERO_ID_ARG}"

    fun detail(heroId: Long): String = "detail/$heroId"
}
