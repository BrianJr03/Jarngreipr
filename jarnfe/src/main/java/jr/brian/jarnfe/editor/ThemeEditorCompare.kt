package jr.brian.jarnfe.editor

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.themes.ThemeVersion

/** "Saved 5 minutes ago", for a history row or the compare sheet. */
@Composable
internal fun versionLabel(version: ThemeVersion): String =
    stringResource(R.string.jarnfe_editor_version, DateUtils.getRelativeTimeSpanString(version.savedAt).toString())

/**
 * Compare: the draft and a saved version side by side on the screen being edited, what changed
 * between them, and Restore. B or Close leaves the draft as it is.
 */
@Composable
internal fun ThemeEditorCompare(
    now: JarnThemeSpec,
    view: ThemeView,
    version: ThemeVersion?,
    onRestore: (ThemeVersion) -> Unit,
    onDismiss: () -> Unit,
) {
    JarnSheet(visible = version != null, onDismiss = onDismiss, title = stringResource(R.string.jarnfe_editor_compare)) {
        val shown = version ?: return@JarnSheet
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md)) {
            LabeledThumbnail(stringResource(R.string.jarnfe_editor_compare_now), now, view, Modifier.weight(1f))
            LabeledThumbnail(versionLabel(shown), shown.spec, view, Modifier.weight(1f))
        }
        DiffLines(themeVersionDiff(then = shown.spec, now = now))
        InspectActionRow(stringResource(R.string.jarnfe_editor_restore), stringResource(R.string.jarnfe_editor_restore_hint)) {
            onRestore(shown)
            onDismiss()
        }
        InspectActionRow(stringResource(R.string.jarnfe_editor_compare_close), onClick = onDismiss)
    }
}

@Composable
private fun LabeledThumbnail(label: String, spec: JarnThemeSpec, view: ThemeView, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
        ThemeVersionThumbnail(spec, view, Modifier.fillMaxWidth())
        JarnText(label, JarnTheme.type.caption, JarnTheme.colors.textMid, maxLines = 1)
    }
}

/** One plain line per kind of change, or "No changes". */
@Composable
private fun DiffLines(diff: ThemeVersionDiff) {
    val lines = buildList {
        if (diff.added > 0) add(pluralStringResource(R.plurals.jarnfe_editor_diff_added, diff.added, diff.added))
        if (diff.removed > 0) add(pluralStringResource(R.plurals.jarnfe_editor_diff_removed, diff.removed, diff.removed))
        if (diff.changed > 0) add(pluralStringResource(R.plurals.jarnfe_editor_diff_changed, diff.changed, diff.changed))
        if (diff.listChanged) add(stringResource(R.string.jarnfe_editor_diff_list))
        if (diff.colorsChanged) add(stringResource(R.string.jarnfe_editor_diff_colors))
        if (diff.same) add(stringResource(R.string.jarnfe_editor_diff_same))
    }
    lines.forEach { JarnText(it, JarnTheme.type.body, JarnTheme.colors.textHigh) }
}
