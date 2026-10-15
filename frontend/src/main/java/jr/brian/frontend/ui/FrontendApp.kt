package jr.brian.frontend.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import jr.brian.frontend.ui.games.GameDetailScreen
import jr.brian.frontend.ui.games.GamesScreen
import jr.brian.frontend.ui.home.HomeScreen
import jr.brian.frontend.ui.search.SearchScreen
import jr.brian.frontend.ui.settings.SettingsScreen
import jr.brian.frontend.ui.setup.SetupScreen
import jr.brian.frontend.ui.theme.FrontendTheme

/**
 * Root composable for the new frontend. Owns the single NavHost
 * every screen hangs off (plan §3.2 — no boolean overlay flags, one
 * back stack).
 *
 * Later Phase 3 commits register Search / Settings / Setup routes
 * as new `composable { }` entries without changing this function's
 * shape.
 */
@Composable
fun FrontendApp() {
    FrontendTheme {
        val nav = rememberNavController()
        NavHost(
            navController = nav,
            startDestination = FrontendDestinations.Home,
        ) {
            composable(FrontendDestinations.Home) {
                HomeScreen(
                    onSystemSelected = { nav.navigate(FrontendDestinations.games(it.id)) },
                    onGameSelected = { nav.navigate(FrontendDestinations.detail(it.id)) },
                    onSearch = { nav.navigate(FrontendDestinations.Search) },
                    onSettings = { nav.navigate(FrontendDestinations.Settings) },
                    onSetup = { nav.navigate(FrontendDestinations.Setup) },
                )
            }
            composable(
                route = FrontendDestinations.GamesRoute,
                arguments = listOf(navArgument("systemId") { type = NavType.StringType }),
            ) { entry ->
                val systemId = entry.arguments?.getString("systemId").orEmpty()
                GamesScreen(
                    systemId = systemId,
                    onGameSelected = { nav.navigate(FrontendDestinations.detail(it.id)) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                route = FrontendDestinations.DetailRoute,
                arguments = listOf(navArgument("gameId") { type = NavType.LongType }),
            ) { entry ->
                val gameId = entry.arguments?.getLong("gameId") ?: 0L
                GameDetailScreen(gameId = gameId, onBack = { nav.popBackStack() })
            }
            composable(FrontendDestinations.Search) {
                SearchScreen(
                    onGameSelected = { nav.navigate(FrontendDestinations.detail(it.id)) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(FrontendDestinations.Settings) {
                SettingsScreen(onBack = { nav.popBackStack() })
            }
            composable(FrontendDestinations.Setup) {
                SetupScreen(
                    onDone = {
                        nav.popBackStack()
                        nav.navigate(FrontendDestinations.Home) {
                            popUpTo(FrontendDestinations.Home) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
