package jr.brian.frontend.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jr.brian.frontend.ui.home.HomeScreen
import jr.brian.frontend.ui.theme.FrontendTheme

/**
 * Root composable for the new frontend. Owns the single NavHost
 * every screen hangs off (plan §3.2 — no boolean overlay flags, one
 * back stack).
 *
 * Phase 3a wires only the Home destination; later Phase 3 commits
 * register Games / Detail / Search / Settings routes without
 * changing this file beyond new `composable { }` entries.
 */
@Composable
fun FrontendApp() {
    FrontendTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = FrontendDestinations.Home,
        ) {
            composable(FrontendDestinations.Home) {
                HomeScreen(
                    onSystemSelected = { /* Phase 3d wires Games route */ },
                    onGameSelected = { /* Phase 3d wires Detail route */ },
                    onSearch = { /* Phase 3e */ },
                    onSettings = { /* Phase 3f */ },
                )
            }
        }
    }
}
