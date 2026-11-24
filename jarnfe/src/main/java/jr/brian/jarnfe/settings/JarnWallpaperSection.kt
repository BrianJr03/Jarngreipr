package jr.brian.jarnfe.settings

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.wallpaper.JarnLiveWallpapers
import jr.brian.jarnfe.wallpaper.JarnWallpaperSource
import jr.brian.jarnfe.wallpaper.jarnDisabledRowBacking
import jr.brian.jarnfe.wallpaper.jarnWallpaperSourceOrNull
import kotlinx.coroutines.launch

private val WallpaperMimeTypes = arrayOf("image/*")

@Composable
internal fun JarnWallpaperSection(initialFocus: FocusRequester? = null) {
    val context = LocalContext.current
    val source = remember(context) { context.jarnWallpaperSourceOrNull() } ?: return
    val store = remember(context) { context.jarnLookStoreOrNull() }
    val liveId = store?.liveWallpaperIdState?.collectAsState()?.value
    val hasWallpaper = source.currentUri != null
    val showsLive = JarnLiveWallpapers.fromId(liveId) != null
    val openPicker = rememberWallpaperPicker(source, onApplied = { store?.setLiveWallpaperId(null) })
    JarnWallpaperPausedNote(hasMovingWallpaper = hasWallpaper || showsLive)
    JarnValueRow(
        label = stringResource(R.string.jarnfe_wallpaper_choose),
        value = stringResource(if (hasWallpaper) R.string.jarnfe_wallpaper_custom else R.string.jarnfe_wallpaper_theme),
        onClick = openPicker,
        modifier = Modifier.fillMaxWidth().then(initialFocus?.let { Modifier.focusRequester(it) } ?: Modifier),
        supporting = stringResource(R.string.jarnfe_wallpaper_choose_hint),
    )
    store?.let { JarnLiveWallpaperPicker(savedId = liveId, onChoose = it::setLiveWallpaperId) }
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_wallpaper_use_theme),
        onClick = {
            source.useThemeBackground()
            store?.setLiveWallpaperId(null)
        },
        modifier = Modifier.fillMaxWidth().jarnDisabledRowBacking(hasWallpaper || showsLive),
        supporting = stringResource(R.string.jarnfe_wallpaper_use_theme_hint),
        enabled = hasWallpaper || showsLive,
        focusableWhenDisabled = true,
    )
}

@Composable
private fun rememberWallpaperPicker(source: JarnWallpaperSource, onApplied: () -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                if (source.applyPicked(uri)) onApplied() else JarnNotices.show(R.string.jarnfe_wallpaper_failed)
            }
        }
    }
    return {
        try {
            launcher.launch(WallpaperMimeTypes)
        } catch (_: ActivityNotFoundException) {
            JarnNotices.show(R.string.jarnfe_wallpaper_no_picker)
        }
    }
}
