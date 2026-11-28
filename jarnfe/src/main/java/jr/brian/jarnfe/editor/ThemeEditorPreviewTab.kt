package jr.brian.jarnfe.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.jarnfe.R

/** Which screen the canvas shows, Try it, and the saved versions. */
@Composable
internal fun ThemeEditorPreviewTab(editor: ThemeEditorState) {
    JarnValueRow(
        label = stringResource(R.string.jarnfe_editor_view),
        value = stringResource(editor.view.labelRes),
        onClick = { JarnThemeEditor.update { withView(ThemeView.entries[(view.ordinal + 1) % ThemeView.entries.size]) } },
        modifier = Modifier.fillMaxWidth(),
    )
    InspectActionRow(stringResource(R.string.jarnfe_editor_try), stringResource(R.string.jarnfe_editor_try_hint)) {
        ThemeEditorTryIt.start()
    }
    ThemeEditorHistory()
}
