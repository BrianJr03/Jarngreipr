package jr.brian.home.esde.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import jr.brian.home.esde.model.WallpaperState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide holder for the live ES-DE [WallpaperState].
 *
 * `ESDEViewModel` is `@HiltViewModel` and therefore activity-scoped: MainActivity
 * and FrontEndActivity each get their own instance with their own state, so wiring
 * the shared ES-DE event listener into one activity's viewmodel left the other
 * activity's state permanently empty. Any composition outside the writing
 * activity — e.g. `FrontendTile` under FrontEndActivity — would then read the
 * uninitialized default and never update.
 *
 * Holding the state at `@Singleton` scope makes it survive any single activity's
 * lifecycle and lets both activities provide the same value into
 * [jr.brian.home.esde.util.LocalEsdeWallpaperState].
 */
@Singleton
class WallpaperStateHolder @Inject constructor() {
    val state: MutableState<WallpaperState> = mutableStateOf(WallpaperState())
}
