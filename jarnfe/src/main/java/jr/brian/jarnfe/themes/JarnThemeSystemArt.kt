package jr.brian.jarnfe.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import jr.brian.jarnfe.themekit.ThemeSystemArtSource

/** Per-system art from the theme the systems view draws, the preview included while one shows. */
@Composable
internal fun rememberThemeSystemArt(): ThemeSystemArtSource {
    val entry = rememberJarnActiveTheme().systems
    return remember(entry) { ThemeSystemArtSource(entry.theme.spec.systemArt, entry.themeDir) }
}
