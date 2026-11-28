package jr.brian.jarnfe.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnText
import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.themes.JarnImportedThemes
import jr.brian.jarnfe.themes.ThemeVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AUTOSAVE_DELAY_MS = 800L

/** Frames to wait so the tools' own first focus lands before History takes it. */
private const val HISTORY_FOCUS_FRAMES = 2

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

/**
 * The last saves, newest first, each with a picture of the screen being edited. A opens Compare,
 * where Restore makes that version the draft and saves it as a new version.
 */
@Composable
internal fun ThemeEditorHistory() {
    val editor = JarnThemeEditor.state ?: return
    val scope = rememberCoroutineScope()
    var comparing by remember { mutableStateOf<ThemeVersion?>(null) }
    val historyFocus = remember { FocusRequester() }
    HistoryFocusEffect(historyFocus)
    JarnText(stringResource(R.string.jarnfe_editor_history), JarnTheme.type.heading, JarnTheme.colors.textHigh, maxLines = 1)
    InspectActionRow(
        label = stringResource(R.string.jarnfe_editor_save),
        supporting = stringResource(R.string.jarnfe_editor_history_save_hint),
        modifier = Modifier.focusRequester(historyFocus),
    ) {
        scope.launch { JarnThemeEditor.saveVersion() }
    }
    JarnThemeEditor.versions.forEach { version ->
        HistoryRow(version, editor.view) { comparing = version }
    }
    ThemeEditorCompare(
        now = editor.spec,
        view = editor.view,
        version = comparing,
        onRestore = { version ->
            JarnThemeEditor.edit { copy(spec = version.spec) }
            scope.launch { JarnThemeEditor.saveVersion(version.spec) }
        },
        onDismiss = { comparing = null },
    )
}

/** After Start > History, waits for the tab to settle, then focuses History so it scrolls into view. */
@Composable
private fun HistoryFocusEffect(target: FocusRequester) {
    val requests = JarnThemeEditor.historyRequests
    LaunchedEffect(requests) {
        if (requests == 0) return@LaunchedEffect
        repeat(HISTORY_FOCUS_FRAMES) { withFrameNanos { } }
        runCatching { target.requestFocus() }
    }
}

@Composable
private fun HistoryRow(version: ThemeVersion, view: ThemeView, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        ThemeVersionThumbnail(version.spec, view, Modifier.width(HistoryThumbnailWidth))
        InspectActionRow(versionLabel(version), stringResource(R.string.jarnfe_editor_compare_hint), onClick = onClick)
    }
}

private val HistoryThumbnailWidth = 96.dp
