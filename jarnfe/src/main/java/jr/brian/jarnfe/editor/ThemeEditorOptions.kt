package jr.brian.jarnfe.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.jarnfe.R

/** Start on the canvas: redo and snapping, plus whatever later steps add. */
@Composable
internal fun ThemeEditorOptions(editor: ThemeEditorState, visible: Boolean, onDismiss: () -> Unit) {
    JarnSheet(visible = visible, onDismiss = onDismiss, title = stringResource(R.string.jarnfe_editor_options)) {
        if (JarnThemeEditor.canRedo) {
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_editor_redo),
                onClick = JarnThemeEditor::redo,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        JarnToggleRow(
            label = stringResource(R.string.jarnfe_editor_snap),
            checked = editor.snap,
            onCheckedChange = { on -> JarnThemeEditor.update { copy(snap = on) } },
            modifier = Modifier.fillMaxWidth(),
            supporting = stringResource(R.string.jarnfe_editor_snap_hint),
        )
    }
}
