package jr.brian.jarnfe.session

import jr.brian.jarnfe.input.JarnScreen

enum class StageRoute { Library, GameList, Settings, Search, Systems }

/** Saved by name, so adding a section never changes what an older session restores. */
enum class LibrarySection { Systems, AllGames, Favorites, Recent }

enum class DeckTab { Now, Apps, Canvas }

/** [Data] holds backup, restore and updates. */
enum class SettingsSection { Library, Art, Look, Display, Apps, General, Data }

val VisibleSettingsSections: List<SettingsSection> = SettingsSection.entries

/**
 * Shared state of both screens. [osFocusedScreen] mirrors which activity Android
 * currently treats as top resumed, which can differ from [activeScreen] for the
 * moment between a switch request and the system moving input focus.
 *
 * [awaitingDeckLaunch] keeps a freshly launched Deck from claiming the active
 * screen, because Android always gives focus to the activity started last.
 *
 * [activeScreen] and [osFocusedScreen] are roles. [swapRequested] is the saved swap, which only
 * takes effect while a Deck window exists, so Stage can never be left without a window.
 *
 * [tapHold] is set while a pinned Controller after tap choice keeps control off the touched window.
 */
data class JarnSessionState(
    val activeScreen: JarnScreen = JarnScreen.Stage,
    val stageStack: List<StageRoute> = listOf(StageRoute.Library),
    val librarySection: LibrarySection = LibrarySection.Systems,
    val deckTab: DeckTab = DeckTab.Now,
    val deckDisplayAvailable: Boolean = false,
    val deckPanelOpen: Boolean = false,
    val osFocusedScreen: JarnScreen? = null,
    val awaitingDeckLaunch: Boolean = false,
    val swapRequested: Boolean = false,
    val deckHostReady: Boolean = false,
    val settingsSection: SettingsSection = SettingsSection.Library,
    val settingsReturnScreen: JarnScreen? = null,
    val tapHold: JarnTapHold? = null,
    val deckWindowLost: Boolean = false,
) {
    val stageRoute: StageRoute get() = stageStack.last()

    /**
     * The display is still there but its window closed. Until it returns the session runs as
     * single screen, so input, panels and hints never target a window that does not exist.
     */
    fun onDeckWindowLost(): JarnSessionState {
        val hostless = withDeckHost(false)
        if (!deckDisplayAvailable && !deckWindowLost) return hostless
        return hostless.withDeckDisplay(false).copy(deckWindowLost = true)
    }

    fun onDeckWindowBack(): JarnSessionState =
        if (deckWindowLost) copy(deckWindowLost = false).withDeckDisplay(true) else this

    fun onDeckWindowAbandoned(): JarnSessionState = copy(deckWindowLost = false)

    val rolesSwapped: Boolean
        get() = swapRequested && deckDisplayAvailable && deckHostReady

    /** The role shown by the window whose unswapped role is [host]. */
    fun roleOn(host: JarnScreen): JarnScreen = if (rolesSwapped) host.other() else host

    fun withSwapRequested(requested: Boolean): JarnSessionState =
        keepingOsFocus(copy(swapRequested = requested))

    fun withDeckHost(ready: Boolean): JarnSessionState =
        keepingOsFocus(copy(deckHostReady = ready, tapHold = tapHold.takeIf { ready }))
            .movingSettingsControls(this)

    /** Settings controls need a real Deck window. Without one they stay on Stage. */
    val settingsOnDeck: Boolean
        get() = deckDisplayAvailable && deckHostReady

    private fun movingSettingsControls(previous: JarnSessionState): JarnSessionState =
        if (stageRoute == StageRoute.Settings && settingsOnDeck != previous.settingsOnDeck) {
            copy(activeScreen = if (settingsOnDeck) JarnScreen.Deck else JarnScreen.Stage)
        } else {
            this
        }

    /** The focused window did not change, but the role it shows may have. */
    private fun keepingOsFocus(next: JarnSessionState): JarnSessionState =
        if (next.rolesSwapped != rolesSwapped) {
            next.copy(osFocusedScreen = next.osFocusedScreen?.other(), tapHold = null)
        } else {
            next
        }

    /** With one display only Stage is composed, so the Deck panel takes input as a Stage layer. */
    val inputScreen: JarnScreen
        get() = if (deckDisplayAvailable) activeScreen else JarnScreen.Stage

    /** The role the controller is not on. */
    val oppositeScreen: JarnScreen
        get() = activeScreen.other()

    val canGoBack: Boolean
        get() = deckPanelOpen || stageStack.size > 1 || activeScreen == JarnScreen.Deck

    fun isLit(screen: JarnScreen): Boolean =
        activeScreen == screen || (screen == JarnScreen.Stage && !deckDisplayAvailable)

    fun needsFocusHandoff(screen: JarnScreen): Boolean =
        deckDisplayAvailable &&
            !awaitingDeckLaunch &&
            activeScreen == screen &&
            osFocusedScreen != null &&
            osFocusedScreen != screen &&
            tapHold?.kept != screen

    fun openRoute(route: StageRoute): JarnSessionState {
        val existing = stageStack.indexOf(route)
        val stack = if (existing >= 0) stageStack.take(existing + 1) else stageStack + route
        return copy(stageStack = stack)
    }

    /** Controls open on the Deck, so the Stage is left free to show the live preview. */
    fun openSettings(): JarnSessionState {
        val returnScreen = if (stageRoute == StageRoute.Settings) settingsReturnScreen else activeScreen
        return openRoute(StageRoute.Settings).copy(
            activeScreen = if (settingsOnDeck) JarnScreen.Deck else JarnScreen.Stage,
            deckPanelOpen = false,
            settingsReturnScreen = returnScreen,
        )
    }

    fun stepSettingsSection(delta: Int): JarnSessionState =
        copy(settingsSection = VisibleSettingsSections.stepped(settingsSection, delta))

    /** System management always runs on Stage, so a Deck entry hands control back to it. */
    fun openSystems(): JarnSessionState =
        openRoute(StageRoute.Systems).copy(activeScreen = JarnScreen.Stage, deckPanelOpen = false)

    fun back(): JarnSessionState = when {
        deckPanelOpen -> copy(deckPanelOpen = false, activeScreen = JarnScreen.Stage)
        stageRoute == StageRoute.Settings -> popRoute().copy(
            activeScreen = settingsReturnScreen?.takeIf { deckDisplayAvailable } ?: activeScreen,
            settingsReturnScreen = null,
        )
        activeScreen == JarnScreen.Deck -> copy(activeScreen = JarnScreen.Stage)
        stageStack.size > 1 -> popRoute()
        else -> this
    }

    fun activate(screen: JarnScreen): JarnSessionState =
        if (deckDisplayAvailable) copy(activeScreen = screen) else this

    fun stepLibrarySection(delta: Int): JarnSessionState =
        copy(librarySection = LibrarySection.entries.stepped(librarySection, delta))

    fun stepDeckTab(delta: Int): JarnSessionState =
        copy(deckTab = DeckTab.entries.stepped(deckTab, delta))

    fun switchActiveScreen(): JarnSessionState = when {
        deckDisplayAvailable -> copy(activeScreen = activeScreen.other(), tapHold = null)
        else -> withDeckPanel(!deckPanelOpen)
    }

    /** Moves control to the Deck's Now tab, as a panel when there is no second display. */
    fun openDeckNow(): JarnSessionState =
        copy(
            deckTab = DeckTab.Now,
            activeScreen = JarnScreen.Deck,
            deckPanelOpen = !deckDisplayAvailable,
        )

    /** Results stay on Stage while control moves to the keyboard on the Deck. */
    fun openSearch(): JarnSessionState =
        openRoute(StageRoute.Search).copy(
            activeScreen = JarnScreen.Deck,
            deckPanelOpen = !deckDisplayAvailable,
        )

    fun withDeckPanel(open: Boolean): JarnSessionState {
        if (deckDisplayAvailable) return this
        return copy(
            deckPanelOpen = open,
            activeScreen = if (open) JarnScreen.Deck else JarnScreen.Stage,
        )
    }

    fun withDeckDisplay(available: Boolean): JarnSessionState {
        if (available) {
            if (deckDisplayAvailable) return this
            return keepingOsFocus(
                copy(
                    deckDisplayAvailable = true,
                    deckPanelOpen = false,
                    activeScreen = if (deckPanelOpen) JarnScreen.Stage else activeScreen,
                ),
            ).movingSettingsControls(this)
        }
        val keepPanel = !deckDisplayAvailable && deckPanelOpen
        return copy(
            deckDisplayAvailable = false,
            deckPanelOpen = keepPanel,
            activeScreen = if (keepPanel) JarnScreen.Deck else JarnScreen.Stage,
            osFocusedScreen = osFocusedScreen?.let(::roleOn).takeUnless { it == JarnScreen.Deck },
            awaitingDeckLaunch = false,
            tapHold = null,
            deckWindowLost = false,
        )
    }

    fun shouldLaunchDeck(isStarted: Boolean, deckHostCreated: Boolean): Boolean =
        isStarted && deckDisplayAvailable && !deckHostCreated && !awaitingDeckLaunch

    fun deckLaunchStarted(): JarnSessionState = copy(awaitingDeckLaunch = true)

    fun deckLaunchFailed(): JarnSessionState = copy(awaitingDeckLaunch = false)

    /** [host] is the window reporting, which shows the opposite role while swapped. */
    fun onTopResumed(host: JarnScreen, isTop: Boolean): JarnSessionState {
        val role = roleOn(host)
        return when {
            !isTop -> copy(osFocusedScreen = osFocusedScreen.takeUnless { it == role })
            awaitingDeckLaunch -> copy(
                osFocusedScreen = role,
                awaitingDeckLaunch = host != JarnScreen.Deck,
            )
            !deckDisplayAvailable -> copy(osFocusedScreen = role)
            tapHold?.kept == activeScreen && role != activeScreen -> copy(osFocusedScreen = role)
            else -> copy(
                osFocusedScreen = role,
                activeScreen = role,
                tapHold = tapHold?.takeIf { it.kept == role && it.modalReturn != null },
            )
        }
    }

    fun encode(): String = listOf(
        activeScreen.name,
        stageStack.joinToString(ROUTE_SEPARATOR) { it.name },
        librarySection.name,
        deckTab.name,
    ).joinToString(FIELD_SEPARATOR)

    /** Coming back to Settings from a route above it returns control to its Deck controls. */
    private fun popRoute(): JarnSessionState {
        val popped = copy(stageStack = stageStack.dropLast(1))
        return if (popped.stageRoute == StageRoute.Settings && popped.settingsOnDeck) {
            popped.copy(activeScreen = JarnScreen.Deck)
        } else {
            popped
        }
    }

    companion object {
        private const val FIELD_SEPARATOR = "|"
        private const val ROUTE_SEPARATOR = ","
        private const val FIELD_COUNT = 4
        private const val LEGACY_BROWSE_TAB = "Browse"

        fun decode(encoded: String?): JarnSessionState? {
            val fields = encoded?.split(FIELD_SEPARATOR)?.takeIf { it.size == FIELD_COUNT } ?: return null
            val routes = fields[1].split(ROUTE_SEPARATOR).map { enumOrNull<StageRoute>(it) ?: return null }
            if (routes.firstOrNull() != StageRoute.Library || routes.distinct().size != routes.size) return null
            return JarnSessionState(
                activeScreen = enumOrNull<JarnScreen>(fields[0]) ?: return null,
                stageStack = routes,
                librarySection = enumOrNull<LibrarySection>(fields[2]) ?: return null,
                deckTab = enumOrNull<DeckTab>(fields[3].migratedDeckTab()) ?: return null,
            )
        }

        /** Apps replaced Browse, so a session saved before the rename still restores. */
        private fun String.migratedDeckTab(): String = if (this == LEGACY_BROWSE_TAB) DeckTab.Apps.name else this

        private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
            enumValues<T>().firstOrNull { it.name == name }
    }
}

private fun <T> List<T>.stepped(current: T, delta: Int): T =
    this[(indexOf(current) + delta).coerceIn(indices)]

private fun JarnScreen.other(): JarnScreen =
    if (this == JarnScreen.Stage) JarnScreen.Deck else JarnScreen.Stage
