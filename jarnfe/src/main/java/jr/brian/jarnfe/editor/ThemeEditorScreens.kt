package jr.brian.jarnfe.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.session.JarnSessionState

private const val SHEET_WEIGHT = 1f
private const val CANVAS_WEIGHT = 2f

/**
 * The editor's Stage: the canvas, plus the tools as a right-third sheet when there is no Deck
 * window. With no session (the editor was restored without one) it steps back out.
 */
@Composable
internal fun ThemeEditorStage(session: JarnSessionState) {
    val editor = JarnThemeEditor.state
    if (editor == null) {
        LaunchedEffect(Unit) { JarnThemeEditor.leave() }
        return
    }
    val toolsOnDeck = session.editorToolsOnDeck
    var optionsOpen by remember { mutableStateOf(false) }
    var layerOptionsOpen by remember { mutableStateOf(false) }
    ThemeEditorCanvasInput(
        editor = editor,
        toolsOnDeck = toolsOnDeck,
        paused = optionsOpen || layerOptionsOpen,
        onOptions = { optionsOpen = true },
        onLayerOptions = { layerOptionsOpen = true },
    )
    Row(Modifier.fillMaxSize()) {
        ThemeEditorCanvas(editor, Modifier.weight(CANVAS_WEIGHT).fillMaxHeight())
        if (!toolsOnDeck && editor.held == null) ThemeEditorSheet(editor, Modifier.weight(SHEET_WEIGHT).fillMaxHeight())
    }
    ThemeEditorOptions(editor, visible = optionsOpen, onDismiss = { optionsOpen = false })
    ThemeEditorLayerSheet(editor, visible = layerOptionsOpen, onDismiss = { layerOptionsOpen = false })
}

/** The tools on the Deck, which the editor owns while it is open. */
@Composable
internal fun ThemeEditorDeck() {
    val editor = JarnThemeEditor.state ?: return
    ThemeEditorToolsInput(editor, onCanvas = null)
    ThemeEditorTools(editor, Modifier.fillMaxSize().padding(JarnTheme.space.deckMargin))
}

@Composable
private fun ThemeEditorSheet(editor: ThemeEditorState, modifier: Modifier) {
    ThemeEditorToolsInput(editor, onCanvas = { JarnThemeEditor.update { copy(sheetFocused = false) } })
    Box(modifier.background(JarnTheme.colors.surface).padding(JarnTheme.space.lg)) {
        ThemeEditorTools(editor, Modifier.fillMaxSize(), focused = editor.sheetFocused)
    }
}

@Composable
internal fun ThemeEditorTitle() {
    val name = JarnThemeEditor.state?.spec?.meta?.name.orEmpty()
    JarnText(
        text = stringResource(R.string.jarnfe_editor_title, name),
        style = JarnTheme.type.label,
        color = JarnTheme.colors.textMid,
        modifier = Modifier.padding(vertical = JarnTheme.space.xl),
        maxLines = 1,
    )
}
