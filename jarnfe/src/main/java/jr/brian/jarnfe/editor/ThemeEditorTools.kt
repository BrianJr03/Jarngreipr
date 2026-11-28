package jr.brian.jarnfe.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.ui.components.JarnChoiceRow
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

/** The tools: a tab strip over the chosen tab, which scrolls on its own and takes focus when [focused]. */
@Composable
internal fun ThemeEditorTools(editor: ThemeEditorState, modifier: Modifier = Modifier, focused: Boolean = true) {
    val body = remember { FocusRequester() }
    LaunchedEffect(focused, editor.tab) { if (focused) runCatching { body.requestFocus() } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md)) {
        JarnTabs(
            labels = ThemeEditorTab.entries.map { stringResource(it.labelRes) },
            selectedIndex = editor.tab.ordinal,
            onSelect = { index -> JarnThemeEditor.update { copy(tab = ThemeEditorTab.entries[index]) } },
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.focusRequester(body).focusGroup().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            ) { ThemeEditorTabBody(editor) }
        }
        ThemeEditorFooter(editor)
    }
}

@Composable
private fun ThemeEditorTabBody(editor: ThemeEditorState) {
    when (editor.tab) {
        ThemeEditorTab.Layers -> ThemeEditorLayersTab(editor)
        ThemeEditorTab.Inspect -> ThemeEditorInspectTab(editor)
        ThemeEditorTab.Colors -> ThemeEditorColorsTab(editor)
        ThemeEditorTab.Preview -> ThemeEditorPreviewTab(editor)
    }
}

@Composable
private fun ThemeEditorLayersTab(editor: ThemeEditorState) {
    ThemeEditorIssues(editor)
    editor.targets.forEach { target ->
        JarnChoiceRow(
            label = editor.targetLabel(target),
            chosen = target == editor.selected,
            onClick = { JarnThemeEditor.update { copy(selected = target) } },
            modifier = Modifier.fillMaxWidth(),
            supporting = stringResource(R.string.jarnfe_editor_navigator_hint).takeIf { target == ThemeEditorTarget.Navigator },
        )
    }
    ThemeEditorNewLayer.entries.forEach { kind ->
        InspectActionRow(
            label = stringResource(R.string.jarnfe_editor_add, stringResource(kind.labelRes)),
            supporting = stringResource(R.string.jarnfe_editor_add_hint),
        ) { JarnThemeEditor.edit { withNewLayer(kind) } }
    }
}

@get:StringRes
private val ThemeEditorNewLayer.labelRes: Int
    get() = when (this) {
        ThemeEditorNewLayer.Picture -> R.string.jarnfe_editor_layer_picture
        ThemeEditorNewLayer.Text -> R.string.jarnfe_editor_layer_text
        ThemeEditorNewLayer.Rating -> R.string.jarnfe_editor_layer_rating
        ThemeEditorNewLayer.Date -> R.string.jarnfe_editor_layer_date
        ThemeEditorNewLayer.Panel -> R.string.jarnfe_editor_layer_panel
        ThemeEditorNewLayer.Video -> R.string.jarnfe_editor_layer_video
    }

@Composable
private fun ThemeEditorPreviewTab(editor: ThemeEditorState) {
    JarnValueRow(
        label = stringResource(R.string.jarnfe_editor_view),
        value = stringResource(editor.view.labelRes),
        onClick = { JarnThemeEditor.update { withView(ThemeView.entries[(view.ordinal + 1) % ThemeView.entries.size]) } },
        modifier = Modifier.fillMaxWidth(),
    )
    ThemeEditorHistory()
}

/** "List" for the navigator, else the layer's kind and its id, so two pictures can be told apart. */
@Composable
internal fun ThemeEditorState.targetLabel(target: ThemeEditorTarget): String = when (target) {
    ThemeEditorTarget.Navigator -> stringResource(R.string.jarnfe_editor_navigator)
    is ThemeEditorTarget.Layer -> {
        val kind = layer(target.id)?.kindRes?.let { stringResource(it) }.orEmpty()
        stringResource(R.string.jarnfe_editor_layer_named, kind, target.id)
    }
}

@get:StringRes
private val ThemeEditorTab.labelRes: Int
    get() = when (this) {
        ThemeEditorTab.Layers -> R.string.jarnfe_editor_tab_layers
        ThemeEditorTab.Inspect -> R.string.jarnfe_editor_tab_inspect
        ThemeEditorTab.Colors -> R.string.jarnfe_look_colors_title
        ThemeEditorTab.Preview -> R.string.jarnfe_editor_tab_preview
    }

@get:StringRes
internal val ThemeView.labelRes: Int
    get() = when (this) {
        ThemeView.Systems -> R.string.jarnfe_section_systems
        ThemeView.Games -> R.string.jarnfe_theme_games_override
    }

@get:StringRes
internal val ThemeLayer.kindRes: Int
    get() = when (this) {
        is ThemeLayer.Picture -> R.string.jarnfe_editor_layer_picture
        is ThemeLayer.Text -> R.string.jarnfe_editor_layer_text
        is ThemeLayer.Rating -> R.string.jarnfe_editor_layer_rating
        is ThemeLayer.Date -> R.string.jarnfe_editor_layer_date
        is ThemeLayer.Panel -> R.string.jarnfe_editor_layer_panel
        is ThemeLayer.Video -> R.string.jarnfe_editor_layer_video
    }
