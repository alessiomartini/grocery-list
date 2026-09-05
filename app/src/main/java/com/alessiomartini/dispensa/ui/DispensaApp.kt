package com.alessiomartini.dispensa.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alessiomartini.dispensa.DispensaApplication
import com.alessiomartini.dispensa.R
import com.alessiomartini.dispensa.data.ItemStatus
import com.alessiomartini.dispensa.ui.expiry.ExpiryScreen
import com.alessiomartini.dispensa.ui.expiry.ExpiryViewModel
import com.alessiomartini.dispensa.ui.list.ListScreen
import com.alessiomartini.dispensa.ui.list.ListViewModel
import com.alessiomartini.dispensa.ui.recipes.RecipesScreen
import com.alessiomartini.dispensa.ui.recipes.RecipesViewModel
import com.alessiomartini.dispensa.ui.settings.SettingsScreen
import com.alessiomartini.dispensa.ui.settings.SettingsViewModel
import com.alessiomartini.dispensa.ui.settings.UpdateViewModel
import kotlinx.coroutines.launch

private object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

/** Index into [Pages] / the bottom nav, in swipe order. */
private object Pages {
    const val TO_BUY = 0
    const val PANTRY = 1
    const val EXPIRY = 2
    const val RECIPES = 3
    const val COUNT = 4
}

private data class BottomNavItem(val page: Int, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Pages.TO_BUY, R.string.section_to_buy, Icons.Filled.ShoppingCart),
    BottomNavItem(Pages.PANTRY, R.string.section_in_pantry, Icons.Filled.Inventory2),
    BottomNavItem(Pages.EXPIRY, R.string.nav_expiry, Icons.Filled.CalendarMonth),
    BottomNavItem(Pages.RECIPES, R.string.nav_recipes, Icons.Filled.Restaurant)
)

@Composable
fun DispensaApp(app: DispensaApplication) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainPager(app, onSettingsClick = { navController.navigate(Routes.SETTINGS) })
        }
        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(
                factory = LambdaViewModelFactory { SettingsViewModel(app.settingsRepository) }
            )
            val updateViewModel: UpdateViewModel = viewModel(
                factory = LambdaViewModelFactory { UpdateViewModel(app.updateRepository) }
            )
            SettingsScreen(viewModel, updateViewModel, onBack = { navController.popBackStack() })
        }
    }
}

/** The 4 main tabs, swipeable left/right and kept in sync with the bottom nav. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainPager(app: DispensaApplication, onSettingsClick: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { Pages.COUNT })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { navItem ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == navItem.page,
                        onClick = { scope.launch { pagerState.animateScrollToPage(navItem.page) } },
                        icon = { Icon(navItem.icon, contentDescription = null) },
                        label = { Text(stringResource(navItem.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())
        ) { page ->
            when (page) {
                Pages.TO_BUY -> {
                    val viewModel: ListViewModel = viewModel(
                        factory = LambdaViewModelFactory { ListViewModel(app.itemRepository) }
                    )
                    ListScreen(viewModel, status = ItemStatus.TO_BUY, onSettingsClick = onSettingsClick)
                }
                Pages.PANTRY -> {
                    val viewModel: ListViewModel = viewModel(
                        factory = LambdaViewModelFactory { ListViewModel(app.itemRepository) }
                    )
                    ListScreen(viewModel, status = ItemStatus.IN_PANTRY, onSettingsClick = onSettingsClick)
                }
                Pages.EXPIRY -> {
                    val viewModel: ExpiryViewModel = viewModel(
                        factory = LambdaViewModelFactory { ExpiryViewModel(app.itemRepository) }
                    )
                    ExpiryScreen(viewModel, onSettingsClick = onSettingsClick)
                }
                Pages.RECIPES -> {
                    val viewModel: RecipesViewModel = viewModel(
                        factory = LambdaViewModelFactory {
                            RecipesViewModel(app.itemRepository, app.recipeSuggestionRepository)
                        }
                    )
                    RecipesScreen(viewModel, onSettingsClick = onSettingsClick)
                }
            }
        }
    }
}
