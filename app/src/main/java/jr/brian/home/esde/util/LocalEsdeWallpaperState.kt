package jr.brian.home.esde.util

import androidx.compose.runtime.compositionLocalOf
import jr.brian.home.esde.model.WallpaperState

/**
 * Live CompositionLocal carrying the currently-displayed ES-DE
 * [WallpaperState] — i.e. the same state the wallpaper renders, including
 * `logoPath` and `currentImagePath`, updated by ES-DE events via
 * `ESDEViewModel.updateForSystem` / `updateForGame`.
 *
 * Consumers (e.g. [jr.brian.home.canvas.ui.FrontendTile]) just read
 * `LocalEsdeWallpaperState.current` — Compose recomposes the readers when
 * the hosting activity re-provides a new state value, so tiles react
 * automatically to system / game / screensaver events with no additional
 * listener wiring.
 *
 * Uses non-static [compositionLocalOf] because the value changes frequently
 * and readers must recompose on each new value (a static local would skip
 * reads on value change).
 *
 * The default lambda throws so a missing provider fails at first read
 * instead of silently returning a stale empty state — the previous default
 * (`WallpaperState()`) hid a real bug where FrontEndActivity's composition
 * subtree never got a provider and every consumer under it rendered
 * placeholder art forever. Every provider site must supply the shared
 * [jr.brian.home.esde.data.WallpaperStateHolder]'s value.
 */
val LocalEsdeWallpaperState = compositionLocalOf<WallpaperState> {
    error("LocalEsdeWallpaperState not provided — wrap the composition in a CompositionLocalProvider that supplies WallpaperStateHolder.state.value")
}
