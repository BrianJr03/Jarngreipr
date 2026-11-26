package jr.brian.jarnfe.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.look.rememberJarnDisplay

private const val SHADE_START = 0.38f
private const val SHADE_MIDDLE = 0.62f
private const val SHADE_MIDDLE_ALPHA = 0.34f
private const val SHADE_END_ALPHA = 0.72f

internal data class NowSceneGameArt(val gameId: String, val path: String?)

private data class NowWall(val systemId: String, val covers: List<LibraryWallCover>)

/** Null until the system's art is known, so the held image is not dropped while it loads. */
internal fun nowSystemPaths(system: JarnLibrarySystem, now: JarnSystemNow?): List<String>? {
    val paths = listOfNotNull(system.nowBackgroundPath, system.artPath, now?.sceneArtPath).filter(String::isNotBlank).distinct()
    return paths.takeIf { it.isNotEmpty() || now?.isLoaded == true }
}

/** Null until this game's wide art has resolved, even if it resolves to nothing. */
internal fun nowGamePaths(game: JarnLibraryGame, art: NowSceneGameArt?): List<String>? =
    art?.takeIf { it.gameId == game.id }?.let { resolved ->
        listOfNotNull(resolved.path).filter(String::isNotBlank)
    }

/** The one image behind Now. System and game views both sit on it, so art holds across them. */
@Composable
internal fun NowScene(
    selection: LibrarySelection,
    systemNow: JarnSystemNow?,
    preview: NowVideoPreview = NowVideoPreview.Off,
    content: @Composable BoxScope.() -> Unit,
) {
    val hold = remember { NowVideoHold() }
    val backdrop = remember { NowVideoBackdrop() }
    CompositionLocalProvider(LocalNowVideoHold provides hold) {
        Box(Modifier.fillMaxSize().nowVideoTapToFill()) {
            NowVideoFill {
                NowSceneArt(selection, systemNow, backdrop)
                NowSceneVideo(selection, preview, backdrop)
            }
            NowHiddenUiInput()
            Box(Modifier.matchParentSize().clipToBounds().nowHideableUi()) {
                NowBottomShade()
                content()
            }
        }
    }
}

@Composable
private fun BoxScope.NowSceneArt(selection: LibrarySelection, systemNow: JarnSystemNow?, backdrop: NowVideoBackdrop) {
    val showsBackground = rememberJarnDisplay().systemBackgroundVisible
    val target = when (selection) {
        LibrarySelection.None, is LibrarySelection.Collection -> emptyList()
        is LibrarySelection.System -> if (showsBackground) nowSystemPaths(selection.system, systemNow) else emptyList()
        is LibrarySelection.Game -> nowGamePaths(selection.game, rememberNowSceneArt(selection.game))
    }
    NowRevealWhen(target != null)
    val paths = rememberHeldPaths(target)
    val wall = rememberHeldWall(systemNow).takeIf { nowShowsWall(selection, showsBackground) }
    Box(Modifier.matchParentSize().nowVideoBackdrop(backdrop.blurred)) {
        LibraryHeldArt(paths) {
            wall?.let { LibraryCoverWall(it.systemId, it.covers, Modifier.matchParentSize()) }
        }
    }
}

/** The cover wall stands in for system art, so hiding the system background hides it on a system too. */
internal fun nowShowsWall(selection: LibrarySelection, showsSystemBackground: Boolean): Boolean = when (selection) {
    LibrarySelection.None, is LibrarySelection.Collection -> false
    is LibrarySelection.System -> showsSystemBackground
    is LibrarySelection.Game -> true
}

@Composable
private fun rememberHeldPaths(target: List<String>?): List<String> {
    var held by remember { mutableStateOf(emptyList<String>()) }
    val paths = target ?: held
    SideEffect { held = paths }
    return paths
}

@Composable
private fun rememberHeldWall(now: JarnSystemNow?): NowWall? {
    var held by remember { mutableStateOf<NowWall?>(null) }
    val loaded = now?.takeIf { it.isLoaded }
    val next = remember(loaded) {
        loaded?.let { data ->
            NowWall(data.systemId, data.shelf.map { LibraryWallCover(it.game.artPath, it.game.title) })
        }
    }
    SideEffect { if (next != null) held = next }
    return next ?: held
}

@Composable
private fun rememberNowSceneArt(game: JarnLibraryGame): NowSceneGameArt? {
    val source = JarnLibrary.source
    val canLoad = rememberJarnArtCanLoad()
    val art by produceState<NowSceneGameArt?>(null, source, game.id, canLoad) {
        if (canLoad) source.observeWideArt(game.id).collect { value = NowSceneGameArt(game.id, it) }
    }
    return art
}

@Composable
private fun BoxScope.NowBottomShade() {
    val base = JarnTheme.colors.bg
    val shade = remember(base) {
        Brush.verticalGradient(
            SHADE_START to Color.Transparent,
            SHADE_MIDDLE to base.copy(alpha = SHADE_MIDDLE_ALPHA),
            1f to base.copy(alpha = SHADE_END_ALPHA),
        )
    }
    Box(Modifier.matchParentSize().background(shade))
}
