package jr.brian.home.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import jr.brian.home.ui.screens.ReceivedThemesScreen
import jr.brian.home.util.Routes

fun NavGraphBuilder.receivedThemesScreen(navController: NavController) {
    composable(Routes.RECEIVED_THEMES) {
        ReceivedThemesScreen(onNavigateBack = { navController.popBackStack() })
    }
}