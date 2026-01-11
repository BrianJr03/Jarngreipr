package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.FolderManager

val LocalFolderManager = staticCompositionLocalOf<FolderManager> {
    error("No FolderManager provided")
}
