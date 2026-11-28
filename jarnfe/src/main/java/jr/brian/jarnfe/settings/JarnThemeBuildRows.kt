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
import jr.brian.jarnfe.editor.editableInPlace
import jr.brian.jarnfe.editor.openThemeEditor
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.themes.JarnThemeEntry
import jr.brian.jarnfe.themes.jarnUserThemeStore
import kotlinx.coroutines.launch

/** Look > Layout > Build a layout: a copy of the layout in use, opened in the editor. */
@Composable
internal fun JarnBuildLayoutRow(current: JarnThemeEntry) {
    val launch = rememberEditorLauncher()
    val copyName = stringResource(R.string.jarnfe_editor_copy_name, current.displayName())
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
        val copyName = stringResource(R.string.jarnfe_editor_copy_name, shown.displayName())
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

/** Opens the editor on a layout, off the main thread. A null copy name edits it in place. */
@Composable
private fun rememberEditorLauncher(): (JarnThemeEntry, String?) -> Unit {
    val context = LocalContext.current
    val store = remember(context) { context.jarnUserThemeStore() }
    val scope = rememberCoroutineScope()
    return { entry, copyName -> scope.launch { openThemeEditor(store, entry, copyName) } }
}
