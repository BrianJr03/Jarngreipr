package jr.brian.jarnfe.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibrarySystem

/** Which screen the canvas shows, what it pretends about the library, Try it, and the saved versions. */
@Composable
internal fun ThemeEditorPreviewTab(editor: ThemeEditorState) {
    JarnValueRow(
        label = stringResource(R.string.jarnfe_editor_view),
        value = stringResource(editor.view.labelRes),
        onClick = { JarnThemeEditor.update { withView(ThemeView.entries[(view.ordinal + 1) % ThemeView.entries.size]) } },
        modifier = Modifier.fillMaxWidth(),
    )
    JarnValueRow(
        label = stringResource(R.string.jarnfe_editor_preview_shape),
        value = stringResource(if (editor.narrow) R.string.jarnfe_editor_preview_shape_narrow else R.string.jarnfe_editor_preview_shape_wide),
        onClick = { JarnThemeEditor.update { withNarrowCanvas(!narrow) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_editor_preview_shape_hint),
    )
    PreviewPickRows()
    PreviewCaseRows()
    InspectActionRow(stringResource(R.string.jarnfe_editor_try), stringResource(R.string.jarnfe_editor_try_hint)) {
        ThemeEditorTryIt.start()
    }
    ThemeEditorHistory()
}

/** The system and game the canvas centers on, stepped with A, or both picked at random. */
@Composable
private fun PreviewPickRows() {
    val settings = ThemeEditorPreview.settings
    val library = JarnLibrary.state
    val none = stringResource(R.string.jarnfe_editor_preview_pick_none)
    val system = library.systems.items.firstOrNull { it.id == settings.systemId } ?: library.focusedSystem
    val game = library.allGames.items.firstOrNull { it.id == settings.gameId } ?: library.focusedAllGame
    JarnValueRow(
        label = stringResource(R.string.jarnfe_editor_preview_system),
        value = system?.title ?: none,
        onClick = { ThemeEditorPreview.update { copy(systemId = ThemeEditorPreview.next(library.systems.items, system?.id, JarnLibrarySystem::id)) } },
        modifier = Modifier.fillMaxWidth(),
    )
    JarnValueRow(
        label = stringResource(R.string.jarnfe_editor_preview_game),
        value = game?.title ?: none,
        onClick = { ThemeEditorPreview.update { copy(gameId = ThemeEditorPreview.next(library.allGames.items, game?.id, JarnLibraryGame::id)) } },
        modifier = Modifier.fillMaxWidth(),
    )
    InspectActionRow(stringResource(R.string.jarnfe_editor_preview_shuffle), stringResource(R.string.jarnfe_editor_preview_shuffle_hint)) {
        ThemeEditorPreview.shuffle(JarnLibrary.state)
    }
}

/** The hard data case to check against, and reduced motion. */
@Composable
private fun PreviewCaseRows() {
    val settings = ThemeEditorPreview.settings
    val cases = ThemeEditorPreviewCase.entries
    JarnValueRow(
        label = stringResource(R.string.jarnfe_editor_preview_case),
        value = stringResource(settings.case.labelRes),
        onClick = { ThemeEditorPreview.update { copy(case = cases[(case.ordinal + 1) % cases.size]) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_editor_preview_case_hint),
    )
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_editor_preview_reduced_motion),
        checked = settings.reducedMotion,
        onCheckedChange = { on -> ThemeEditorPreview.update { copy(reducedMotion = on) } },
        modifier = Modifier.fillMaxWidth(),
    )
}
