package jr.brian.jarnfe.library

import android.hardware.display.DisplayManager
import android.view.Display
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnVideoFit
import jr.brian.jarnfe.launch.GameActionsPage
import jr.brian.jarnfe.launch.JarnGameActions
import jr.brian.jarnfe.wallpaper.rememberJarnVideoPreviewCanPlay
import kotlinx.coroutines.delay

/** On top of the settled selection's own wait, so scrolling never starts a player. */
internal const val NOW_VIDEO_START_DELAY_MILLIS = 600L
internal const val NOW_VIDEO_TAG = "now_video"

/**
 * Whether Now may play the settled game's preview behind it, whether it plays muted, and whether
 * it fills the scene ([crop]) or fits inside it over blurred art.
 */
@Immutable
internal data class NowVideoPreview(val enabled: Boolean, val muted: Boolean = true, val crop: Boolean = false) {
    companion object {
        val Off = NowVideoPreview(enabled = false)
    }
}

internal fun JarnDisplayPrefs.nowVideoPreview(): NowVideoPreview = NowVideoPreview(
    enabled = videoPreviews,
    muted = !videoPreviewSound,
    crop = videoFit == JarnVideoFit.Crop,
)

/** The game page says here when its More sheet covers the scene, since the scene sits outside it. */
@Stable
internal class NowVideoHold {
    var sheetOpen by mutableStateOf(false)
}

internal val LocalNowVideoHold = staticCompositionLocalOf<NowVideoHold?> { null }

/** Files that failed to play this session, so a broken preview is not retried on every visit. */
internal object NowVideoFailures {
    private val paths = mutableStateSetOf<String>()

    operator fun contains(path: String): Boolean = path in paths

    fun add(path: String) {
        paths += path
    }

    fun clear() = paths.clear()
}

/**
 * The settled game's preview, over its held art and under the shade. The art stays until the
 * first frame draws, so a slow or broken file never shows black.
 */
@Composable
internal fun BoxScope.NowSceneVideo(
    selection: LibrarySelection,
    preview: NowVideoPreview,
    backdrop: NowVideoBackdrop? = null,
) {
    NowVideoSoundMode(preview)
    val game = (selection as? LibrarySelection.Game)?.takeUnless { it.resume }?.game
    if (game == null || !preview.enabled) return
    val videos = rememberJarnGameVideos() ?: return
    val canPlay = rememberNowVideoCanPlay()
    val path = if (canPlay) rememberNowGameArt(game).videoPath else null
    NowGameVideo(game.id, path, canPlay, videos, preview.muted, preview.crop, backdrop)
}

/**
 * One player at most, made [NOW_VIDEO_START_DELAY_MILLIS] after [path] rests. A closed gate or a
 * launch releases it at once; a game change or a covering sheet fades it out first.
 */
@Composable
internal fun BoxScope.NowGameVideo(
    gameId: String,
    path: String?,
    canPlay: Boolean,
    videos: JarnGameVideos,
    muted: Boolean,
    crop: Boolean = false,
    backdrop: NowVideoBackdrop? = null,
) {
    val heldByLaunch = rememberHeldByLaunch(gameId, canPlay)
    if (!canPlay || heldByLaunch) return
    val wanted = path?.takeUnless { it in NowVideoFailures || nowVideoCovered() }
    NowVideoSoundClaim(gameId, claims = wanted != null && !muted)
    val target = rememberArmedPath(wanted)
    val shown = rememberLeavingPath(target, JarnTheme.motion.focusOut.durationMillis.toLong())
    if (shown != null) {
        key(shown, crop) {
            NowVideoPlayer(videos, shown, visible = shown == target, muted, crop, backdrop)
        }
    }
}

/** Lets the game page tell the scene its More sheet is up. */
@Composable
internal fun NowVideoSheetReport(scroll: ScrollState) {
    val hold = LocalNowVideoHold.current ?: return
    val open by remember(scroll) { derivedStateOf { scroll.value > 0 } }
    DisposableEffect(hold, open) {
        hold.sheetOpen = open
        onDispose { hold.sheetOpen = false }
    }
}

@Composable
private fun nowVideoCovered(): Boolean = nowSceneCovered(LocalNowVideoHold.current)

/** True while the More sheet or a game action page sits over the scene. */
internal fun nowSceneCovered(hold: NowVideoHold?): Boolean =
    hold?.sheetOpen == true || JarnGameActions.page != GameActionsPage.Actions

/** Pressing Play holds this game's preview until the player comes back from the game or moves on. */
@Composable
private fun rememberHeldByLaunch(gameId: String, canPlay: Boolean): Boolean {
    val launches = JarnLibrary.launches
    var seen by remember(gameId) { mutableIntStateOf(launches) }
    if (!canPlay) SideEffect { seen = launches }
    return launches != seen
}

@Composable
private fun rememberArmedPath(path: String?): String? {
    val armed by produceState<String?>(null, path) {
        value = null
        if (path != null) {
            delay(NOW_VIDEO_START_DELAY_MILLIS)
            value = path
        }
    }
    return armed?.takeIf { it == path }
}

/** Keeps the last path for [fadeMillis] after [target] clears, so its player can fade out. */
@Composable
private fun rememberLeavingPath(target: String?, fadeMillis: Long): String? {
    var leaving by remember { mutableStateOf<String?>(null) }
    SideEffect { if (target != null) leaving = target }
    LaunchedEffect(target) {
        if (target != null) return@LaunchedEffect
        delay(fadeMillis)
        leaving = null
    }
    return target ?: leaving
}

@Composable
private fun BoxScope.NowVideoPlayer(
    videos: JarnGameVideos,
    path: String,
    visible: Boolean,
    muted: Boolean,
    crop: Boolean,
    backdrop: NowVideoBackdrop?,
) {
    val context = LocalContext.current
    val video = remember(videos, path, crop) { videos.create(context, path, crop = crop) }
    DisposableEffect(video) { onDispose { video.release() } }
    val state by video.state.collectAsState()
    NowVideoVisibilityReport(visible && state == JarnGameVideoState.Ready)
    NowVideoBackdropReport(backdrop, blurred = !crop && visible && state == JarnGameVideoState.Ready)
    val audible = rememberNowVideoAudible(muted, visible && state == JarnGameVideoState.Ready)
    LaunchedEffect(video, audible) { video.setMuted(!audible) }
    LaunchedEffect(state) {
        if (state == JarnGameVideoState.Failed) NowVideoFailures.add(path)
    }
    val alpha by rememberNowVideoAlpha(visible && state == JarnGameVideoState.Ready)
    AndroidView(
        factory = { video.view },
        modifier = Modifier
            .matchParentSize()
            .testTag(NOW_VIDEO_TAG)
            .graphicsLayer { this.alpha = alpha },
    )
}

@Composable
private fun rememberNowVideoAlpha(shown: Boolean): State<Float> {
    val motion = JarnTheme.motion
    return animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = if (shown) motion.videoReveal.spec() else motion.focusOut.spec(),
        label = "NowVideoAlpha",
    )
}

/**
 * The wallpaper gate covers a game being up, battery saver and Reduce motion. The window's own
 * lifecycle and display cover a stopped window and a dark screen, which the gate can miss.
 */
@Composable
private fun rememberNowVideoCanPlay(): Boolean {
    val canAnimate by rememberJarnVideoPreviewCanPlay()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return canAnimate && lifecycle.isAtLeast(Lifecycle.State.STARTED) && rememberDisplayLit()
}

@Composable
private fun rememberDisplayLit(): Boolean {
    val view = LocalView.current
    var lit by remember(view) { mutableStateOf(view.isDisplayLit()) }
    DisposableEffect(view) {
        val displays = view.context.getSystemService(DisplayManager::class.java)
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == view.display?.displayId) lit = view.isDisplayLit()
            }

            override fun onDisplayAdded(displayId: Int) = Unit

            override fun onDisplayRemoved(displayId: Int) = Unit
        }
        displays.registerDisplayListener(listener, null)
        lit = view.isDisplayLit()
        onDispose { displays.unregisterDisplayListener(listener) }
    }
    return lit
}

private fun View.isDisplayLit(): Boolean = display?.state == Display.STATE_ON
