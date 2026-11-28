package jr.brian.jarnfe.editor

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.jarnfe.R
import jr.brian.jarnfe.themes.JarnImportedThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AUTOSAVE_DELAY_MS = 800L

/** Writes the draft a moment after each change, so leaving or a crash never loses work. */
@Composable
internal fun ThemeEditorAutosave(editor: ThemeEditorState) {
    LaunchedEffect(editor.themeId) { JarnThemeEditor.loadVersions() }
    LaunchedEffect(editor.spec) {
        delay(AUTOSAVE_DELAY_MS)
        val store = JarnThemeEditor.store ?: return@LaunchedEffect
        withContext(Dispatchers.IO) { store.saveDraft(editor.themeId, editor.spec) }
    }
}

internal suspend fun JarnThemeEditor.loadVersions() {
    val id = state?.themeId ?: return
    val store = store ?: return
    versions = withContext(Dispatchers.IO) { store.history(id) }
}

/** Saves the draft as the theme and as a new version, then lists it with the other themes. */
internal suspend fun JarnThemeEditor.saveVersion(spec: JarnThemeSpec? = state?.spec) {
    val id = state?.themeId ?: return
    val store = store ?: return
    val saved = spec ?: return
    withContext(Dispatchers.IO) { store.save(id, saved) }
    JarnImportedThemes.refresh(store.root)
    loadVersions()
}

/** The last saves, newest first. Restoring one makes it the draft and saves it as a new version. */
@Composable
internal fun ThemeEditorHistory() {
    val scope = rememberCoroutineScope()
    InspectActionRow(stringResource(R.string.jarnfe_editor_save), stringResource(R.string.jarnfe_editor_save_hint)) {
        scope.launch { JarnThemeEditor.saveVersion() }
    }
    JarnThemeEditor.versions.forEach { version ->
        val ago = DateUtils.getRelativeTimeSpanString(version.savedAt).toString()
        InspectActionRow(
            label = stringResource(R.string.jarnfe_editor_version, ago),
            supporting = stringResource(R.string.jarnfe_editor_restore_hint),
        ) {
            JarnThemeEditor.edit { copy(spec = version.spec) }
            scope.launch { JarnThemeEditor.saveVersion(version.spec) }
        }
    }
}
