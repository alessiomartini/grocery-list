package com.alessiomartini.dispensa.ui

import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alessiomartini.dispensa.DispensaApplication
import com.alessiomartini.dispensa.R
import com.alessiomartini.dispensa.ui.expiry.ExpiryScreen
import com.alessiomartini.dispensa.ui.expiry.ExpiryViewModel
import com.alessiomartini.dispensa.ui.list.ListScreen
import com.alessiomartini.dispensa.ui.list.ListViewModel
import com.alessiomartini.dispensa.ui.recipes.RecipesScreen
import com.alessiomartini.dispensa.ui.recipes.RecipesViewModel
import com.alessiomartini.dispensa.ui.settings.SettingsScreen
import com.alessiomartini.dispensa.ui.settings.SettingsViewModel
import com.alessiomartini.dispensa.ui.settings.UpdateViewModel
import com.alessiomartini.dispensa.data.ItemStatus

private object Routes {
    const val TO_BUY = "to_buy"
    const val PANTRY = "pantry"
    const val EXPIRY = "expiry"
    const val RECIPES = "recipes"
    const val SETTINGS = "settings"
}

private data class BottomNavItem(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.TO_BUY, R.string.section_to_buy, Icons.Filled.ShoppingCart),
    BottomNavItem(Routes.PANTRY, R.string.section_in_pantry, Icons.Filled.Inventory2),
    BottomNavItem(Routes.EXPIRY, R.string.nav_expiry, Icons.Filled.CalendarMonth),
    BottomNavItem(Routes.RECIPES, R.string.nav_recipes, Icons.Filled.Restaurant)
)

@Composable
fun DispensaApp(app: DispensaApplication) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != Routes.SETTINGS) {
                NavigationBar {
                    bottomNavItems.forEach { navItem ->
                        NavigationBarItem(
                            selected = currentRoute == navItem.route,
                            onClick = {
                                navController.navigate(navItem.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(navItem.icon, contentDescription = null) },
                            label = { Text(stringResource(navItem.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TO_BUY,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(Routes.TO_BUY) {
                val viewModel: ListViewModel = viewModel(
                    factory = LambdaViewModelFactory { ListViewModel(app.itemRepository) }
                )
                ListScreen(
                    viewModel,
                    status = ItemStatus.TO_BUY,
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.PANTRY) {
                val viewModel: ListViewModel = viewModel(
                    factory = LambdaViewModelFactory { ListViewModel(app.itemRepository) }
                )
                ListScreen(
                    viewModel,
                    status = ItemStatus.IN_PANTRY,
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.EXPIRY) {
                val viewModel: ExpiryViewModel = viewModel(
                    factory = LambdaViewModelFactory { ExpiryViewModel(app.itemRepository) }
                )
                ExpiryScreen(viewModel, onSettingsClick = { navController.navigate(Routes.SETTINGS) })
            }
            composable(Routes.RECIPES) {
                val viewModel: RecipesViewModel = viewModel(
                    factory = LambdaViewModelFactory {
                        RecipesViewModel(app.itemRepository, app.recipeSuggestionRepository)
                    }
                )
                RecipesScreen(viewModel, onSettingsClick = { navController.navigate(Routes.SETTINGS) })
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
}
