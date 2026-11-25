package jr.brian.jarnfe.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.theme.JarnEasingStandard
import jr.brian.core.ui.theme.JarnMotion
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnPlayingDim
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.launch.JarnCompanionSheetHost
import jr.brian.jarnfe.launch.JarnGameActions
import jr.brian.jarnfe.launch.offeredCompanionController
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.rememberGameWideArt
import jr.brian.jarnfe.library.rememberNowGameArt
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.shell.jarnScreenDim
import jr.brian.jarnfe.shell.rememberDeviceStatus
import jr.brian.jarnfe.shell.rememberJarnScreenDim
import jr.brian.jarnfe.wallpaper.JarnWallpaperBackdrop

internal const val NOW_PLAYING_TAG = "jarn_now_playing"
internal const val NOW_PLAYING_WAKE_TAG = "jarn_now_playing_wake"
private const val DIM_FADE_MILLIS = 1_200
private const val DRIFT_GLIDE_MILLIS = 2_000
private const val COVER_LIFT = 1.02f

/** The player's Now Playing choices, or the defaults before the store is ready. */
@Composable
internal fun rememberNowPlayingPrefs(): JarnDisplayPrefs {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() } ?: return JarnDisplayPrefs()
    val display by store.displayState.collectAsState()
    return display
}

/** The role whose screen is black right now, if any. The companion offer always shows. */
internal fun nowPlayingBlackoutRole(state: JarnSessionState, prefs: JarnDisplayPrefs): JarnScreen? = nowPlayingBlackoutRole(
    session = JarnNowPlaying.session,
    screenOff = prefs.playingScreenOff,
    deckDisplayAvailable = state.deckDisplayAvailable,
    rolesSwapped = state.rolesSwapped,
    osFocusedScreen = state.osFocusedScreen,
    offerShowing = offeredCompanionController() != null,
)

/** The role showing Now Playing right now, if any. */
internal fun nowPlayingRole(state: JarnSessionState, prefs: JarnDisplayPrefs): JarnScreen? = nowPlayingRole(
    session = JarnNowPlaying.session,
    enabled = prefs.playingScreenVisible,
    deckDisplayAvailable = state.deckDisplayAvailable,
    rolesSwapped = state.rolesSwapped,
    osFocusedScreen = state.osFocusedScreen,
)

/**
 * What a role's window shows: Now Playing when this role is the free screen beside a game,
 * otherwise [content]. The companion sheet sits above either on its role.
 */
@Composable
internal fun NowPlayingSlot(role: JarnScreen, state: JarnSessionState, content: @Composable () -> Unit) {
    val prefs = rememberNowPlayingPrefs()
    val session = JarnNowPlaying.session
    val blackout = rememberNowPlayingBlackout(session?.launchedAtUptimeMs, prefs.playingDim)
    when {
        nowPlayingBlackoutRole(state, prefs) == role && blackout.isBlack(prefs.playingDim) ->
            NowPlayingBlackout(onWake = { blackout.wake() })
        session != null && nowPlayingRole(state, prefs) == role -> NowPlayingPage(session, prefs.playingDim)
        else -> content()
    }
    val sheetRole = companionSheetRole(JarnNowPlaying.session, state.deckDisplayAvailable, state.rolesSwapped)
    if (sheetRole == role) JarnCompanionSheetHost(state.deckDisplayAvailable, state.osFocusedScreen)
}

/** The header's way back to a page the player left, on the role that page lives on. */
@Composable
internal fun NowPlayingHeaderPill(role: JarnScreen, modifier: Modifier = Modifier) {
    val prefs = rememberNowPlayingPrefs()
    val session = JarnNowPlaying.session ?: return
    val state = JarnSession.state
    val pillRole = nowPlayingPillRole(session, prefs.playingScreenVisible, state.deckDisplayAvailable, state.rolesSwapped)
    if (pillRole == role) NowPlayingPill(session.game.title, modifier)
}

/** Dims slowly and wakes on the screen token. Reduced motion uses the panel's fade for both. */
private fun nowPlayingDimSpec(target: Float, motion: JarnMotion): AnimationSpec<Float> = when {
    motion.reduced -> motion.panel.spec()
    target > 0f -> tween(DIM_FADE_MILLIS, easing = JarnEasingStandard)
    else -> motion.screen.spec()
}

/** The Deck's page while a game runs on the top screen. The companion sheet is drawn above it. */
@Composable
internal fun NowPlayingPage(session: NowPlayingSession, dim: JarnPlayingDim) {
    val dimState = rememberNowPlayingDim(dim)
    val dimAmount = rememberJarnScreenDim(if (dimState.isDimmed) 1f else 0f, ::nowPlayingDimSpec)
    val now = rememberMinuteClock(session.startedAtWallMs)
    val tone = when {
        !dimState.isDimmed -> NowPlayingTone.Awake
        nowPlayingLongDim(dimState.dimmedSinceMs, now) -> NowPlayingTone.LongDimmed
        else -> NowPlayingTone.Dimmed
    }
    val game = rememberNowPlayingLibraryGame(session.game)
    val fanart = rememberGameWideArt(game, canLoad = true)
    Box(
        Modifier
            .fillMaxSize()
            .testTag(NOW_PLAYING_TAG)
            .onPreviewKeyEvent { if (!dimState.isDimmed) dimState.wake(); false }
            .pointerInput(dimState) {
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    if (!dimState.isDimmed) dimState.wake()
                }
            },
    ) {
        Box(Modifier.matchParentSize().background(Color.Black).jarnScreenDim(dimAmount, NOW_PLAYING_DIM_ALPHA)) {
            JarnWallpaperBackdrop(showWallpaper = true)
            NowPlayingBackdrop(fanart)
        }
        NowPlayingContent(session, game, dimAmount.value, tone, now, dimState)
        if (dimState.isDimmed) NowPlayingWakeLayer(dimState)
    }
}

@Composable
private fun NowPlayingContent(
    session: NowPlayingSession,
    game: JarnLibraryGame,
    dim: Float,
    tone: NowPlayingTone,
    nowMs: Long,
    dimState: NowPlayingDimState,
) {
    val drift = rememberNowPlayingDrift(dimState, nowMs)
    val lift = rememberWakeLift(dimState.isDimmed)
    val art = rememberNowGameArt(game, canLoad = true)
    val margin = JarnTheme.space.deckMargin
    Box(
        Modifier
            .fillMaxSize()
            .padding(margin)
            .offset { drift },
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NowPlayingCover(game, art, dim, Modifier.nowPlayingCoverLift(lift))
            NowPlayingDetails(session, game, dim, tone, nowMs, Modifier.weight(1f).fillMaxHeight())
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            NowPlayingStatus(rememberDeviceStatus(), tone)
        }
    }
}

@Composable
private fun NowPlayingDetails(
    session: NowPlayingSession,
    game: JarnLibraryGame,
    dim: Float,
    tone: NowPlayingTone,
    nowMs: Long,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(JarnTheme.space.lg)) {
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.nowPlayingFade(dim),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            NowPlayingEyebrow()
            NowPlayingTitle(game.title, nowPlayingSystemTitle(game.systemId))
        }
        NowPlayingSessionTime(nowPlayingSessionText(nowMs - session.startedAtWallMs), tone)
        Box(Modifier.nowPlayingFade(dim)) { NowPlayingStats(game) }
        Spacer(Modifier.weight(1f))
        Box(Modifier.nowPlayingFade(dim)) { NowPlayingActions(session, awake = tone == NowPlayingTone.Awake) }
    }
}

/** Show Deck, and Open a companion when the launch had a choice. B leaves like Show Deck. */
@Composable
private fun NowPlayingActions(session: NowPlayingSession, awake: Boolean) {
    val first = remember { FocusRequester() }
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        enabled = awake,
        bindings = {
            on(JarnAction.Back, R.string.jarnfe_hint_back) { JarnNowPlaying.dismiss() }
            on(JarnAction.SwitchScreen) {}
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md)) {
            JarnButton(
                label = stringResource(R.string.jarnfe_now_playing_show_deck),
                onClick = JarnNowPlaying::dismiss,
                modifier = Modifier.focusRequester(first),
                variant = JarnButtonVariant.Secondary,
            )
            if (session.companions.isNotEmpty()) {
                JarnButton(
                    label = stringResource(R.string.jarnfe_now_playing_companion),
                    onClick = { JarnGameActions.controller?.offerCompanions(session.companions) },
                    variant = JarnButtonVariant.Secondary,
                )
            }
        }
    }
    LaunchedEffect(awake) { if (awake) runCatching { first.requestFocus() } }
}

/**
 * Over the dimmed page, so the waking tap or button never also presses what is under it. Swap is
 * left to the app, as everywhere.
 */
@Composable
private fun NowPlayingWakeLayer(dimState: NowPlayingDimState) {
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        bindings = {
            JarnAction.entries.filter { it != JarnAction.Swap }.forEach { action ->
                on(action, repeats = false) { dimState.wake() }
            }
        },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .testTag(NOW_PLAYING_WAKE_TAG)
                .pointerInput(dimState) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        dimState.wake()
                    }
                },
        )
    }
}

/** While dimmed, the readout walks a little each minute. Reduced motion jumps instead of gliding. */
@Composable
private fun rememberNowPlayingDrift(dimState: NowPlayingDimState, nowMs: Long): IntOffset {
    val since = dimState.dimmedSinceMs
    val step = if (since == null) null else (nowMs - since) / NOW_PLAYING_DRIFT_EVERY_MS
    val (x, y) = step?.let(::nowPlayingDrift) ?: (0 to 0)
    val density = LocalDensity.current
    val target = with(density) { IntOffset(x.dp.roundToPx(), y.dp.roundToPx()) }
    val reduced = JarnTheme.motion.reduced
    val offset by animateIntOffsetAsState(
        targetValue = target,
        animationSpec = if (reduced) snap() else tween(DRIFT_GLIDE_MILLIS, easing = LinearEasing),
        label = "nowPlayingDrift",
    )
    return offset
}

/** The cover rises a touch and settles when the page wakes. None with reduced motion. */
@Composable
private fun rememberWakeLift(isDimmed: Boolean): Float {
    val motion = JarnTheme.motion
    val lift = remember { Animatable(1f) }
    val hasDimmed = remember { BooleanArray(1) }
    LaunchedEffect(isDimmed) {
        if (isDimmed) hasDimmed[0] = true
        if (isDimmed || !hasDimmed[0] || motion.reduced) return@LaunchedEffect
        lift.animateTo(COVER_LIFT, motion.screen.spec())
        lift.animateTo(1f, motion.screen.spec())
    }
    return lift.value
}
