package jr.brian.jarnfe.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import jr.brian.core.model.theme.ThemeSource
import jr.brian.jarnfe.library.LibraryBuiltInTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Themes on this device that aren't in code: ones the player made and ones they imported. They
 * follow the Look only when the theme says so, so an imported theme keeps its own colors.
 */
internal object JarnUserThemes {
    var entries by mutableStateOf<List<JarnThemeEntry>>(emptyList())
        private set

    suspend fun reload(store: JarnUserThemeStore) {
        entries = withContext(Dispatchers.IO) { store.list() }.map(::entryFor)
    }

    fun entryFor(theme: StoredTheme): JarnThemeEntry = JarnThemeEntry(
        id = theme.id,
        kind = if (theme.spec.meta.source == ThemeSource.Jarn) JarnThemeKind.User else JarnThemeKind.Imported,
        theme = LibraryBuiltInTheme(theme.spec),
        ownColors = !theme.spec.meta.followsLook,
        themeDir = theme.folder,
    )
}

/** Reads the themes folder once per composition, so a saved or restored user theme can show. */
@Composable
internal fun JarnUserThemesLoader() {
    val context = LocalContext.current
    LaunchedEffect(context) { JarnUserThemes.reload(context.jarnUserThemeStore()) }
}
