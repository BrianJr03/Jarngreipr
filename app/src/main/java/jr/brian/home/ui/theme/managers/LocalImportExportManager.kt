package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.config.ImportExportManager

val LocalImportExportManager = staticCompositionLocalOf<ImportExportManager> {
    error("No ImportExportManager provided")
}
