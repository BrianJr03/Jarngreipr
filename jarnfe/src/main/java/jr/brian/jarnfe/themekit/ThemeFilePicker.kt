package jr.brian.jarnfe.themekit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import jr.brian.frontend.look.themes.JARN_THEME_FILE_TYPES
import jr.brian.frontend.look.themes.ThemeImportProblem
import jr.brian.frontend.look.themes.ThemeImportResult
import jr.brian.jarnfe.R

/**
 * Opens the system document picker for a theme file and hands back the picked document. Inside
 * Jarn only: there is deliberately no "Open with" filter, so the hidden build stays hidden.
 */
@Composable
fun rememberThemeFilePicker(onPicked: (Uri) -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(latest)
    }
    return { launcher.launch(JARN_THEME_FILE_TYPES) }
}

/** The message for a failed import, or null when the theme was added. */
@StringRes
fun themeImportMessage(result: ThemeImportResult): Int? = when (result) {
    is ThemeImportResult.Installed -> null
    is ThemeImportResult.TooNew -> R.string.jarnfe_theme_import_too_new
    is ThemeImportResult.Rejected -> when (result.problem) {
        ThemeImportProblem.Unreadable -> R.string.jarnfe_theme_import_unreadable
        ThemeImportProblem.NotATheme -> R.string.jarnfe_theme_import_not_a_theme
        ThemeImportProblem.TooLarge, ThemeImportProblem.TooManyEntries -> R.string.jarnfe_theme_import_too_large
        ThemeImportProblem.UnsafeEntry, ThemeImportProblem.CompressionBomb -> R.string.jarnfe_theme_import_unsafe
        ThemeImportProblem.Damaged -> R.string.jarnfe_theme_import_damaged
    }
}
