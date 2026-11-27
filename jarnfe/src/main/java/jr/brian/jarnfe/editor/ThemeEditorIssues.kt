package jr.brian.jarnfe.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

/** The load meter and the issue count, under every tab. */
@Composable
internal fun ThemeEditorFooter(editor: ThemeEditorState) {
    val issues = editor.issues(JarnThemeEditor.layoutIssues)
    val load = editor.viewSpec?.load ?: ThemeLoad.Light
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        JarnText(
            text = stringResource(R.string.jarnfe_editor_load, stringResource(load.labelRes)),
            style = JarnTheme.type.caption,
            color = if (load == ThemeLoad.Heavy) JarnTheme.colors.danger else JarnTheme.colors.textMid,
            maxLines = 1,
        )
        JarnText(
            text = if (issues.isEmpty()) {
                stringResource(R.string.jarnfe_editor_no_issues)
            } else {
                pluralStringResource(R.plurals.jarnfe_editor_issue_count, issues.size, issues.size)
            },
            style = JarnTheme.type.caption,
            color = if (issues.isEmpty()) JarnTheme.colors.textMid else JarnTheme.colors.danger,
            maxLines = 1,
        )
    }
}

/** One line per issue; A jumps to the piece in Inspect, or to Colors. */
@Composable
internal fun ThemeEditorIssues(editor: ThemeEditorState) {
    editor.issues(JarnThemeEditor.layoutIssues).forEach { issue ->
        val target = issue.target
        InspectActionRow(
            label = target?.let { editor.targetLabel(it) } ?: stringResource(R.string.jarnfe_editor_colors_target),
            supporting = stringResource(issue.messageRes),
        ) {
            JarnThemeEditor.update {
                if (target == null) copy(tab = ThemeEditorTab.Colors) else copy(selected = target, tab = ThemeEditorTab.Inspect)
            }
        }
    }
}
