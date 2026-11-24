package jr.brian.jarnfe.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.motion.JarnPageTransition
import jr.brian.core.ui.motion.rememberJarnPageEnter
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import kotlinx.coroutines.delay

private const val PREVIEW_PAGE_HOLD_MS = 1600L
private val PreviewPageHeight = 72.dp

/** Flips between two pages forward and back, replaying [transition] as soon as it changes. */
@Composable
internal fun PreviewPageTransition(transition: JarnPageTransition) {
    var page by remember { mutableIntStateOf(0) }
    LaunchedEffect(transition) {
        while (true) {
            page = 1 - page
            delay(PREVIEW_PAGE_HOLD_MS)
        }
    }
    Column {
        JarnText(
            text = stringResource(R.string.jarnfe_settings_preview_page),
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.textMid,
            maxLines = 1,
        )
        Box(Modifier.fillMaxWidth().height(PreviewPageHeight).clipToBounds()) {
            PreviewPage(page, rememberJarnPageEnter(page, transition))
        }
    }
}

@Composable
private fun PreviewPage(page: Int, motion: Modifier) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(motion)
            .clip(JarnTheme.radius.mdShape)
            .background(JarnTheme.colors.raised),
        contentAlignment = Alignment.Center,
    ) {
        JarnText(
            text = stringResource(R.string.jarnfe_canvas_page_number, page + 1),
            style = JarnTheme.type.labelStrong,
            color = JarnTheme.colors.textHigh,
            maxLines = 1,
        )
    }
}
