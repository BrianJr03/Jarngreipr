package jr.brian.frontend.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Phase 3a theme shell. Uses Material defaults so the module compiles
 * and runs standalone. Later Phase 3 commits populate :core-ui with
 * the real `FrontendTokens` palette; this wrapper is the swap point
 * when that lands.
 *
 * Dark-first because the frontend lives on a game-launcher display and
 * most Thor users browse in low light. `isSystemInDarkTheme()` still
 * governs so a user who configures light mode gets it.
 */
@Composable
fun FrontendTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}

// Placeholder palette — intentional defaults until :core-ui ships the
// real tokens in a later Phase 3 commit. Values are the Material 3
// baseline tones hand-tuned very mildly to keep the frontend's dark
// background slightly cooler than the launcher's.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF002F6C),
    secondary = Color(0xFFB4C8E7),
    background = Color(0xFF0F1115),
    surface = Color(0xFF171A20),
    onSurface = Color(0xFFE2E6EE),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4B6BC9),
    secondary = Color(0xFF6481AF),
)
