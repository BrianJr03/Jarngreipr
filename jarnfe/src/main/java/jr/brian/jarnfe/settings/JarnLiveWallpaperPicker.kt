package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.LocalJarnScreenActive
import jr.brian.frontend.look.JARN_LIVE_WALLPAPER_OFF
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.wallpaper.JarnLiveWallpaper
import jr.brian.jarnfe.wallpaper.JarnLiveWallpaperPreview
import jr.brian.jarnfe.wallpaper.JarnLiveWallpapers

/** Opens in place, so the Stage preview stays visible on a single screen too. */
@Composable
internal fun JarnLiveWallpaperPicker(
    savedId: String?,
    onChoose: (String?) -> Unit,
    catalog: List<JarnLiveWallpaper> = JarnLiveWallpapers.All,
) {
    if (catalog.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    val rowFocus = remember { FocusRequester() }
    val chosen = JarnLiveWallpapers.fromId(savedId, catalog)
    JarnValueRow(
        label = stringResource(R.string.jarnfe_wallpaper_live),
        value = stringResource(chosen?.nameRes ?: R.string.jarnfe_wallpaper_live_off),
        onClick = { open = !open },
        modifier = Modifier.fillMaxWidth().focusRequester(rowFocus),
        supporting = stringResource(R.string.jarnfe_wallpaper_live_hint),
    )
    if (open) {
        val close = {
            rowFocus.requestFocus()
            open = false
        }
        LiveWallpaperOptions(
            chosenId = chosen?.id ?: JARN_LIVE_WALLPAPER_OFF,
            catalog = catalog,
            onPick = { id ->
                onChoose(id.takeUnless { it == JARN_LIVE_WALLPAPER_OFF })
                close()
            },
            onClose = close,
        )
    }
}

@Composable
private fun LiveWallpaperOptions(
    chosenId: String,
    catalog: List<JarnLiveWallpaper>,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
) {
    var focusedId by remember { mutableStateOf<String?>(null) }
    val chosenFocus = remember { FocusRequester() }
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!tabsFocus.hasFocus) chosenFocus.requestFocus()
    }
    LiveWallpaperFocusPreview(focusedId)
    JarnInputLayer(
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onClose)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().focusGroup(),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            liveWallpaperOptions(catalog).forEach { option ->
                JarnToggleRow(
                    label = stringResource(option.nameRes),
                    checked = option.id == chosenId,
                    onCheckedChange = { onPick(option.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (option.id == chosenId) Modifier.focusRequester(chosenFocus) else Modifier)
                        .onFocusChanged { state ->
                            focusedId = nextFocusedLook(focusedId, option.id, state.isFocused)
                        },
                )
            }
        }
    }
}

private data class LiveWallpaperOption(val id: String, @param:StringRes val nameRes: Int)

private fun liveWallpaperOptions(catalog: List<JarnLiveWallpaper>): List<LiveWallpaperOption> =
    listOf(LiveWallpaperOption(JARN_LIVE_WALLPAPER_OFF, R.string.jarnfe_wallpaper_live_off)) +
        catalog.map { LiveWallpaperOption(it.id, it.nameRes) }

@Composable
private fun LiveWallpaperFocusPreview(focusedId: String?) {
    val previewId = focusedId.takeIf { LocalJarnScreenActive.current }
    DisposableEffect(previewId) {
        JarnLiveWallpaperPreview.id = previewId
        onDispose { JarnLiveWallpaperPreview.id = null }
    }
}
