package jr.brian.jarnfe.themes

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import jr.brian.core.model.theme.JarnThemeDecodeResult
import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.frontend.look.esde.EsdeThemeImport
import jr.brian.frontend.look.themes.EsdeThemeLocator
import jr.brian.frontend.look.themes.EsdeThemeUpdates
import jr.brian.frontend.look.themes.JarnThemeImporter
import jr.brian.jarnfe.library.LibraryBuiltInTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val THEMES_DIR = "themes"
private const val THEME_FILE = "theme.json"
private const val MAX_THEME_JSON_BYTES = 1024 * 1024

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnThemeImportProvider {
    val jarnEsdeThemeLocator: EsdeThemeLocator
    val jarnEsdeThemeUpdates: EsdeThemeUpdates
    val jarnThemeFileImporter: JarnThemeImporter
}

internal fun Context.jarnThemeImportOrNull(): JarnThemeImportProvider? =
    applicationContext as? JarnThemeImportProvider

internal fun Context.jarnThemesRoot(): File = File(filesDir, THEMES_DIR)

internal fun Context.jarnEsdeThemeImport(): EsdeThemeImport = EsdeThemeImport(jarnThemesRoot())

/**
 * Themes the player added (ES-DE imports and theme files), read from `filesDir/themes`. Each brings
 * its own colors, so it never follows the Look. A folder whose theme no longer reads is skipped.
 */
internal object JarnImportedThemes {
    var entries by mutableStateOf<List<JarnThemeEntry>>(emptyList())
        private set

    private var loaded = false

    suspend fun refresh(themesRoot: File) {
        val read = withContext(Dispatchers.IO) { readImportedThemes(themesRoot) }
        withContext(Dispatchers.Main.immediate) {
            loaded = true
            entries = read
        }
    }

    suspend fun loadOnce(themesRoot: File) {
        if (!loaded) refresh(themesRoot)
    }
}

internal fun readImportedThemes(themesRoot: File): List<JarnThemeEntry> =
    themesRoot.listFiles().orEmpty()
        .filter { it.isDirectory && !it.name.startsWith(".") }
        .sortedBy { it.name }
        .mapNotNull { dir ->
            val file = File(dir, THEME_FILE).takeIf { it.isFile && it.length() <= MAX_THEME_JSON_BYTES }
            val decoded = file?.let { runCatching { JarnThemeFormat.decode(it.readText()) }.getOrNull() }
            (decoded as? JarnThemeDecodeResult.Decoded)?.spec?.let { spec ->
                JarnThemeEntry(
                    id = dir.name,
                    kind = JarnThemeKind.Imported,
                    theme = LibraryBuiltInTheme(spec),
                    ownColors = true,
                    themeDir = dir,
                )
            }
        }

/** Reads the imported themes the first time any screen asks, so a saved import is found on launch. */
@Composable
internal fun LoadImportedThemes() {
    val context = LocalContext.current
    LaunchedEffect(context) { JarnImportedThemes.loadOnce(context.jarnThemesRoot()) }
}
