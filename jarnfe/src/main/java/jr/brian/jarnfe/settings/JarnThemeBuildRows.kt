package jr.brian.jarnfe.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnSheet
import jr.brian.jarnfe.R
import jr.brian.jarnfe.editor.copyBaseName
import jr.brian.jarnfe.editor.editableInPlace
import jr.brian.jarnfe.editor.nextCopyName
import jr.brian.jarnfe.editor.openThemeEditor
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.themes.JarnThemeEntry
import jr.brian.jarnfe.themes.JarnThemeRegistry
import jr.brian.jarnfe.themes.jarnUserThemeStore
import kotlinx.coroutines.launch

/** Look > Layout > Build a layout: a copy of the layout in use, opened in the editor. */
@Composable
internal fun JarnBuildLayoutRow(current: JarnThemeEntry) {
    val launch = rememberEditorLauncher()
    val copyName = rememberCopyName(current)
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_editor_build),
        onClick = { launch(current, copyName) },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_editor_build_hint),
    )
}

/** Select on a focused layout card opens its options. */
@Composable
internal fun JarnThemeCardOptionsInput(focused: JarnThemeEntry?, onOptions: (JarnThemeEntry) -> Unit) {
    JarnInputLayer(
        enabled = focused != null,
        bindings = { focused?.let { entry -> on(JarnAction.Context, R.string.jarnfe_editor_card_options) { onOptions(entry) } } },
    )
}

/** Edit (themes on this device) and Duplicate and edit (any layout). */
@Composable
internal fun JarnThemeCardOptions(entry: JarnThemeEntry?, onDismiss: () -> Unit) {
    val launch = rememberEditorLauncher()
    JarnSheet(visible = entry != null, onDismiss = onDismiss, title = entry?.displayName()) {
        val shown = entry ?: return@JarnSheet
        val copyName = rememberCopyName(shown)
        if (shown.editableInPlace) {
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_game_edit),
                onClick = {
                    onDismiss()
                    launch(shown, null)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_editor_duplicate_edit),
            onClick = {
                onDismiss()
                launch(shown, copyName)
            },
            modifier = Modifier.fillMaxWidth(),
            supporting = stringResource(R.string.jarnfe_editor_duplicate_edit_hint).takeUnless { shown.editableInPlace },
        )
        JarnThemeShareRow(shown.id, onDone = onDismiss)
    }
}

/** "Grid copy", or "Grid copy 2" and on when that name is taken; a copy of a copy numbers on. */
@Composable
private fun rememberCopyName(entry: JarnThemeEntry): String {
    val resources = LocalContext.current.resources
    val name = copyBaseName(
        name = entry.displayName(),
        plain = { resources.getString(R.string.jarnfe_editor_copy_name, it) },
        numbered = { base, n -> resources.getString(R.string.jarnfe_editor_copy_name_numbered, base, n) },
    )
    val first = stringResource(R.string.jarnfe_editor_copy_name, name)
    val taken = JarnThemeRegistry.installed.mapTo(HashSet()) { it.theme.spec.meta.name }
    return remember(first, taken) {
        nextCopyName(first, { n -> resources.getString(R.string.jarnfe_editor_copy_name_numbered, name, n) }, taken)
    }
}

/** Opens the editor on a layout, off the main thread. A null copy name edits it in place. */
@Composable
private fun rememberEditorLauncher(): (JarnThemeEntry, String?) -> Unit {
    val context = LocalContext.current
    val store = remember(context) { context.jarnUserThemeStore() }
    val scope = rememberCoroutineScope()
    return { entry, copyName -> scope.launch { openThemeEditor(store, entry, copyName) } }
}
