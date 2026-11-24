package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import kotlinx.coroutines.launch

/**
 * The bottom of the area to reveal when focus lands at [focusedTop], or null when that is not the
 * group's first row. The first row starts right after the heading and one row gap.
 */
internal fun settingsHeadingReveal(focusedTop: Float, focusedBottom: Float, headingBottom: Float, gap: Float): Float? =
    focusedBottom.takeIf { focusedTop <= headingBottom + gap + HEADING_SLOP_PX }

private const val HEADING_SLOP_PX = 1f

private class SettingsGroupFocus {
    var group: LayoutCoordinates? = null
    var headingBottom = 0f
    var last: LayoutCoordinates? = null
}

/**
 * A titled block of rows. Groups sit further apart than the rows inside them, and focusing the
 * first row also brings the heading into view, so scrolling up never leaves it above the screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsGroup(@StringRes titleRes: Int, content: @Composable () -> Unit) {
    val requester = remember { BringIntoViewRequester() }
    val focus = remember { SettingsGroupFocus() }
    val scope = rememberCoroutineScope()
    val gap = JarnTheme.space.sm
    val gapPx = with(LocalDensity.current) { gap.toPx() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester)
            .onGloballyPositioned { focus.group = it }
            .onFocusedBoundsChanged { focused ->
                val isNew = focused != null && focused !== focus.last
                focus.last = focused
                val group = focus.group
                if (!isNew || focused == null || group == null) return@onFocusedBoundsChanged
                if (!group.isAttached || !focused.isAttached) return@onFocusedBoundsChanged
                val bounds = group.localBoundingBoxOf(focused)
                val bottom = settingsHeadingReveal(bounds.top, bounds.bottom, focus.headingBottom, gapPx)
                    ?: return@onFocusedBoundsChanged
                scope.launch { requester.bringIntoView(Rect(0f, 0f, group.size.width.toFloat(), bottom)) }
            },
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Box(Modifier.onGloballyPositioned { focus.headingBottom = it.size.height.toFloat() }) {
            SettingsSectionTitle(titleRes)
        }
        content()
    }
}

/** One option of a pick-one list, so every such list in Settings reads the same way. */
@Composable
internal fun SettingsChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    JarnValueRow(
        label = label,
        value = stringResource(if (selected) R.string.jarnfe_art_in_use else R.string.jarnfe_art_use),
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        supporting = supporting,
    )
}
