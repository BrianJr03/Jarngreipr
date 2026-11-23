package jr.brian.jarnfe.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnLookCard
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.LocalJarnScreenActive
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.JarnLookPreview
import jr.brian.jarnfe.look.jarnLookStoreOrNull

private val TwoColumnMaxWidth = 480.dp
private const val COMPACT_COLUMNS = 2
private const val WIDE_COLUMNS = 4

@Composable
internal fun JarnLookSection() {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() } ?: return
    val savedId by store.lookIdState.collectAsState()
    SettingsSectionTitle(R.string.jarnfe_look_title)
    JarnLookPicker(chosen = JarnLooks.fromId(savedId), onChoose = { store.setLookId(it.id) })
    ReducedMotionRow(store)
}

@Composable
private fun JarnLookPicker(chosen: JarnLook, onChoose: (JarnLook) -> Unit) {
    var focusedId by remember { mutableStateOf<String?>(null) }
    LiveLookPreview(focusedId)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < TwoColumnMaxWidth) COMPACT_COLUMNS else WIDE_COLUMNS
        Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md)) {
            JarnLooks.All.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
                ) {
                    row.forEach { look ->
                        JarnLookCard(
                            look = look,
                            chosen = look == chosen,
                            onClick = { onChoose(look) },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { state ->
                                    focusedId = nextFocusedLook(focusedId, look.id, state.isFocused)
                                },
                        )
                    }
                }
            }
        }
    }
}

/** Both screens follow the focused card, and fall back to the saved look once it loses focus. */
@Composable
private fun LiveLookPreview(focusedId: String?) {
    val previewId = focusedId.takeIf { LocalJarnScreenActive.current }
    DisposableEffect(previewId) {
        JarnLookPreview.lookId = previewId
        onDispose { JarnLookPreview.lookId = null }
    }
}

internal fun nextFocusedLook(current: String?, lookId: String, isFocused: Boolean): String? = when {
    isFocused -> lookId
    current == lookId -> null
    else -> current
}

@Composable
private fun ReducedMotionRow(store: JarnLookStore) {
    val reducedMotion by store.reducedMotionState.collectAsState()
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_look_reduced_motion),
        checked = reducedMotion,
        onCheckedChange = store::setReducedMotion,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_look_reduced_motion_hint),
    )
}
