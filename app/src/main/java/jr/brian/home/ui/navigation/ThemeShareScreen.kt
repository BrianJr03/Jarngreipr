package jr.brian.home.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import jr.brian.home.ui.screens.ThemeShareScreen
import jr.brian.home.util.Routes

fun NavGraphBuilder.themeShareScreen(navController: NavController) {
    composable(Routes.THEME_SHARE) {
        ThemeShareScreen(onNavigateBack = { navController.popBackStack() })
    }
}