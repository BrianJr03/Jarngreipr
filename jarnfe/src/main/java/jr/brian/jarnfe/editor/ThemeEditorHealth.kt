package jr.brian.jarnfe.editor

import androidx.annotation.StringRes
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ViewSpec
import jr.brian.jarnfe.R
import jr.brian.jarnfe.themekit.ThemeLayoutIssue

private const val PICTURE_COST = 1f
private const val VIDEO_COST = 3f
private const val OTHER_COST = 0.25f
private const val OK_FROM = 4f
private const val HEAVY_FROM = 8f

/** How much a view asks of the device, by its pictures and videos. */
internal enum class ThemeLoad(@param:StringRes val labelRes: Int) {
    Light(R.string.jarnfe_editor_load_light),
    Ok(R.string.jarnfe_editor_load_ok),
    Heavy(R.string.jarnfe_editor_load_heavy),
}

internal val ViewSpec.load: ThemeLoad
    get() {
        val cost = layers.sumOf { layer ->
            when (layer) {
                is ThemeLayer.Picture -> PICTURE_COST
                is ThemeLayer.Video -> VIDEO_COST
                else -> OTHER_COST
            }.toDouble()
        }.toFloat() + PICTURE_COST
        return when {
            cost >= HEAVY_FROM -> ThemeLoad.Heavy
            cost >= OK_FROM -> ThemeLoad.Ok
            else -> ThemeLoad.Light
        }
    }

/** A plain line about one piece, which jumps to it. Null [target] is the theme's colors. */
internal data class ThemeEditorIssue(val target: ThemeEditorTarget?, @param:StringRes val messageRes: Int)

/** What the layout engine changed or flagged, plus unreadable colors, in player words. */
internal fun ThemeEditorState.issues(layout: List<ThemeLayoutIssue>): List<ThemeEditorIssue> {
    val pieces = layout.map { issue ->
        val target = issue.layerId?.let(ThemeEditorTarget::Layer) ?: ThemeEditorTarget.Navigator
        ThemeEditorIssue(target, issue.messageRes)
    }
    val colors = if (!spec.meta.followsLook && !spec.tokens.colors.isReadable) {
        listOf(ThemeEditorIssue(null, R.string.jarnfe_editor_hard_to_read))
    } else {
        emptyList()
    }
    return colors + pieces
}

@get:StringRes
private val ThemeLayoutIssue.messageRes: Int
    get() = when (this) {
        ThemeLayoutIssue.PrimaryInvalid -> R.string.jarnfe_editor_issue_list_invalid
        ThemeLayoutIssue.PrimaryTooSmall -> R.string.jarnfe_editor_issue_list_small
        ThemeLayoutIssue.PrimaryClamped -> R.string.jarnfe_editor_issue_list_clamped
        is ThemeLayoutIssue.InvalidRect -> R.string.jarnfe_editor_issue_no_size
        is ThemeLayoutIssue.OffScreen -> R.string.jarnfe_editor_issue_off_screen
        is ThemeLayoutIssue.ExtraVideo -> R.string.jarnfe_editor_issue_two_videos
        is ThemeLayoutIssue.TextMoved -> R.string.jarnfe_editor_issue_text_moved
        is ThemeLayoutIssue.TextInSafeZone -> R.string.jarnfe_editor_issue_text_covered
        is ThemeLayoutIssue.InSafeZone -> R.string.jarnfe_editor_issue_under_bars
        is ThemeLayoutIssue.CoveredPrimary -> R.string.jarnfe_editor_issue_covers_list
    }
