package jr.brian.home.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped mirror of the transient Compose state the launcher
 * content tracks (page index, dock Y, overlay-visibility flags).
 * These used to live as `remember`-locals inside
 * [jr.brian.home.MainActivity]'s `setContent`, which worked under
 * the old window-reparent swap model (the activity never recreated
 * so the locals survived) but regresses under the pinned-slot swap:
 * when the launcher content moves between [MainActivity] and
 * [jr.brian.home.esde.ui.FrontEndActivity], the previous host's
 * composition disposes and its remembers die. Hoisting here keeps
 * one source of truth, visible to whichever activity is rendering
 * the launcher at any moment.
 *
 * The fields are intentionally fine-grained — individual
 * [MutableStateFlow]s rather than one giant data-class — so a change
 * to the page index doesn't invalidate observers reading
 * `dockTopY` and vice versa, matching what the previous
 * `by remember { mutableIntStateOf(…) }` locals did.
 */
@Singleton
class LauncherUiStateHolder @Inject constructor() {

    /**
     * One-shot trigger fired by a wallpaper interaction to open the
     * marquee press shortcut inside the launcher content. The
     * launcher resets this to `false` after consuming it.
     */
    private val _triggerMarqueePressShortcut = MutableStateFlow(false)
    val triggerMarqueePressShortcut: StateFlow<Boolean> =
        _triggerMarqueePressShortcut.asStateFlow()

    fun triggerMarquee() {
        _triggerMarqueePressShortcut.value = true
    }

    fun markMarqueeHandled() {
        _triggerMarqueePressShortcut.value = false
    }

    /**
     * Whether a launcher overlay (app drawer, settings sheet, search
     * sheet, etc.) is open. Used to hide the marquee while the user
     * is in a sheet so it doesn't distract from the open content.
     */
    private val _isAnyOverlayVisible = MutableStateFlow(false)
    val isAnyOverlayVisible: StateFlow<Boolean> = _isAnyOverlayVisible.asStateFlow()

    fun setAnyOverlayVisible(value: Boolean) {
        _isAnyOverlayVisible.value = value
    }

    /** Current launcher page index (home / canvas / app drawer tabs). */
    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    fun setCurrentPageIndex(value: Int) {
        _currentPageIndex.value = value
    }

    /**
     * Pager-level horizontal scroll progress (0f at the start of the
     * current page, 1f at the next). Used by the wallpaper for a
     * subtle parallax effect that lives behind the pager.
     */
    private val _pagerScrollProgress = MutableStateFlow(0f)
    val pagerScrollProgress: StateFlow<Float> = _pagerScrollProgress.asStateFlow()

    fun setPagerScrollProgress(value: Float) {
        _pagerScrollProgress.value = value
    }

    /**
     * Toggled by the ES-DE screensaver event listener (and by the
     * wallpaper double-tap when screensaver is active) to collapse
     * every launcher chrome layer. Separate from
     * [hideLauncherUIForGameBrowsing] because the two can overlap and
     * each has its own trigger lifecycle.
     */
    private val _hideLauncherUIForScreensaver = MutableStateFlow(false)
    val hideLauncherUIForScreensaver: StateFlow<Boolean> =
        _hideLauncherUIForScreensaver.asStateFlow()

    fun setHideLauncherUIForScreensaver(value: Boolean) {
        _hideLauncherUIForScreensaver.value = value
    }

    /**
     * Toggled by the ES-DE game-browsing event listener so the user
     * sees a cleaner view when they're navigating ES-DE on the other
     * screen. Independent of the screensaver flag.
     */
    private val _hideLauncherUIForGameBrowsing = MutableStateFlow(false)
    val hideLauncherUIForGameBrowsing: StateFlow<Boolean> =
        _hideLauncherUIForGameBrowsing.asStateFlow()

    fun setHideLauncherUIForGameBrowsing(value: Boolean) {
        _hideLauncherUIForGameBrowsing.value = value
    }

    /**
     * Y position (in pixels) of the dock's top edge inside the
     * launcher's root box, measured by the dock itself and reported
     * so the wallpaper can shape its gradient around it. Nullable
     * so the wallpaper can distinguish "not measured yet" from
     * "dock at 0".
     */
    private val _dockTopY = MutableStateFlow<Float?>(null)
    val dockTopY: StateFlow<Float?> = _dockTopY.asStateFlow()

    fun setDockTopY(value: Float?) {
        _dockTopY.value = value
    }
}
