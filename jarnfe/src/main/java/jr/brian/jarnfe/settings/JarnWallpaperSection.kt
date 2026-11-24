package jr.brian.jarnfe.settings

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.wallpaper.JarnLiveWallpapers
import jr.brian.jarnfe.wallpaper.JarnWallpaperScreen
import jr.brian.jarnfe.wallpaper.JarnWallpaperSource
import jr.brian.jarnfe.wallpaper.jarnDisabledRowBacking
import jr.brian.jarnfe.wallpaper.jarnScreenWallpaperUri
import jr.brian.jarnfe.wallpaper.jarnWallpaperSourceOrNull
import kotlinx.coroutines.launch

private val WallpaperMimeTypes = arrayOf("image/*")

@Composable
internal fun JarnWallpaperSection(initialFocus: FocusRequester? = null) {
    val context = LocalContext.current
    val source = remember(context) { context.jarnWallpaperSourceOrNull() } ?: return
    val store = remember(context) { context.jarnLookStoreOrNull() }
    val liveId = store?.liveWallpaperIdState?.collectAsState()?.value
    val sameOnBoth = store?.sameWallpaperOnBothState?.collectAsState()?.value ?: true
    val bottomUri = source.usableOrNull(store?.bottomWallpaperUriState?.collectAsState()?.value)
    val hasTwoScreens = JarnSession.state.deckDisplayAvailable && store != null
    val perScreen = hasTwoScreens && !sameOnBoth
    val showsLive = JarnLiveWallpapers.fromId(liveId) != null
    val uriOf = { screen: JarnWallpaperScreen -> jarnScreenWallpaperUri(screen, !perScreen, source.currentUri, bottomUri) }
    val hasAnyWallpaper = uriOf(JarnWallpaperScreen.Top) != null || uriOf(JarnWallpaperScreen.Bottom) != null
    JarnWallpaperPausedNote(hasMovingWallpaper = hasAnyWallpaper || showsLive)
    if (hasTwoScreens) SameOnBothScreensRow(source, sameOnBoth, initialFocus)
    if (perScreen) {
        JarnWallpaperScreen.entries.forEach { screen ->
            WallpaperScreenRows(source, store, screen, uriOf(screen) != null, showsLive)
        }
    } else {
        WallpaperChooseRow(
            source = source,
            store = store,
            screen = JarnWallpaperScreen.Top,
            hasWallpaper = uriOf(JarnWallpaperScreen.Top) != null,
            modifier = if (hasTwoScreens) Modifier else initialFocus?.let { Modifier.focusRequester(it) } ?: Modifier,
        )
    }
    store?.let { JarnLiveWallpaperPicker(savedId = liveId, onChoose = it::setLiveWallpaperId) }
    if (!perScreen) {
        WallpaperThemeRow(source, store, JarnWallpaperScreen.Top, hasAnyWallpaper || showsLive)
    }
}

@Composable
private fun SameOnBothScreensRow(source: JarnWallpaperSource, sameOnBoth: Boolean, initialFocus: FocusRequester?) {
    val scope = rememberCoroutineScope()
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_wallpaper_same_on_both),
        checked = sameOnBoth,
        onCheckedChange = { same -> scope.launch { source.setSameOnBothScreens(same) } },
        modifier = Modifier.fillMaxWidth().then(initialFocus?.let { Modifier.focusRequester(it) } ?: Modifier),
        supporting = stringResource(R.string.jarnfe_wallpaper_same_on_both_hint),
    )
}

/** One screen's own picker and theme rows, under that screen's name. */
@Composable
private fun WallpaperScreenRows(
    source: JarnWallpaperSource,
    store: JarnLookStore?,
    screen: JarnWallpaperScreen,
    hasWallpaper: Boolean,
    showsLive: Boolean,
) {
    SettingsSectionTitle(screen.titleRes)
    WallpaperChooseRow(source, store, screen, hasWallpaper)
    WallpaperThemeRow(source, store, screen, hasWallpaper || showsLive)
}

@Composable
private fun WallpaperChooseRow(
    source: JarnWallpaperSource,
    store: JarnLookStore?,
    screen: JarnWallpaperScreen,
    hasWallpaper: Boolean,
    modifier: Modifier = Modifier,
) {
    val openPicker = rememberWallpaperPicker(source, screen, onApplied = { store?.setLiveWallpaperId(null) })
    JarnValueRow(
        label = stringResource(R.string.jarnfe_wallpaper_choose),
        value = stringResource(if (hasWallpaper) R.string.jarnfe_wallpaper_custom else R.string.jarnfe_wallpaper_theme),
        onClick = openPicker,
        modifier = Modifier.fillMaxWidth().then(modifier),
        supporting = stringResource(R.string.jarnfe_wallpaper_choose_hint),
    )
}

/** Clears [screen]'s image or GIF, and the shared live wallpaper with it. */
@Composable
private fun WallpaperThemeRow(
    source: JarnWallpaperSource,
    store: JarnLookStore?,
    screen: JarnWallpaperScreen,
    enabled: Boolean,
) {
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_wallpaper_use_theme),
        onClick = {
            source.useThemeBackground(screen)
            store?.setLiveWallpaperId(null)
        },
        modifier = Modifier.fillMaxWidth().jarnDisabledRowBacking(enabled),
        supporting = stringResource(R.string.jarnfe_wallpaper_use_theme_hint),
        enabled = enabled,
        focusableWhenDisabled = true,
    )
}

@get:StringRes
private val JarnWallpaperScreen.titleRes: Int
    get() = when (this) {
        JarnWallpaperScreen.Top -> R.string.jarnfe_wallpaper_top_screen
        JarnWallpaperScreen.Bottom -> R.string.jarnfe_wallpaper_bottom_screen
    }

/** Picking an image or GIF turns off the live wallpaper, which both screens share. */
@Composable
private fun rememberWallpaperPicker(
    source: JarnWallpaperSource,
    screen: JarnWallpaperScreen,
    onApplied: () -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                if (source.applyPicked(uri, screen)) onApplied() else JarnNotices.show(R.string.jarnfe_wallpaper_failed)
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
