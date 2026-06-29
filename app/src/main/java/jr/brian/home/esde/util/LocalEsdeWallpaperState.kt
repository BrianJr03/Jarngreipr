package jr.brian.home.esde.util

import androidx.compose.runtime.compositionLocalOf
import jr.brian.home.esde.model.WallpaperState

/**
 * Live CompositionLocal carrying the currently-displayed ES-DE
 * [WallpaperState] — i.e. the same state the wallpaper renders, including
 * `logoPath` and `currentImagePath`, updated by ES-DE events via
 * `ESDEViewModel.updateForSystem` / `updateForGame`.
 *
 * Consumers (e.g. [jr.brian.home.canvas.ui.EsdeArtTile]) just read
 * `LocalEsdeWallpaperState.current` — Compose recomposes the readers when
 * MainActivity re-provides a new state value, so tiles react automatically
 * to system / game / screensaver events with no additional listener wiring.
 *
 * Uses non-static [compositionLocalOf] because the value changes frequently
 * and readers must recompose on each new value (a static local would skip
 * reads on value change).
 */
val LocalEsdeWallpaperState = compositionLocalOf { WallpaperState() }
