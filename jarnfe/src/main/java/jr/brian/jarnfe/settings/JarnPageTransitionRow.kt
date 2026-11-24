package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.motion.JarnPageTransition
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.R

/** Cycles the gentle presets. With Reduce motion on, every one of them plays as a short fade. */
@Composable
internal fun PageTransitionRow(store: JarnLookStore, transition: JarnPageTransition) {
    val hint = if (JarnTheme.motion.reduced) {
        R.string.jarnfe_settings_page_transition_reduced_hint
    } else {
        R.string.jarnfe_settings_page_transition_hint
    }
    JarnValueRow(
        label = stringResource(R.string.jarnfe_settings_page_transition),
        value = stringResource(transition.labelRes),
        onClick = { store.updateDisplay { it.copy(pageTransition = it.pageTransition.next()) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(hint),
    )
}

@get:StringRes
internal val JarnPageTransition.labelRes: Int
    get() = when (this) {
        JarnPageTransition.JarnSlide -> R.string.jarnfe_page_transition_jarn_slide
        JarnPageTransition.Fade -> R.string.jarnfe_page_transition_fade
        JarnPageTransition.Slide -> R.string.jarnfe_page_transition_slide
        JarnPageTransition.Depth -> R.string.jarnfe_page_transition_depth
        JarnPageTransition.Stack -> R.string.jarnfe_page_transition_stack
        JarnPageTransition.GlideUp -> R.string.jarnfe_page_transition_glide_up
        JarnPageTransition.Carousel -> R.string.jarnfe_browse_view_carousel
    }

private fun JarnPageTransition.next(): JarnPageTransition =
    JarnPageTransition.entries[(ordinal + 1) % JarnPageTransition.entries.size]
