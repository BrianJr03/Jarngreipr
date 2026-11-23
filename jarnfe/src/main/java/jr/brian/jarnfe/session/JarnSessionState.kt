package jr.brian.jarnfe.session

import jr.brian.jarnfe.input.JarnScreen

enum class StageRoute { Library, GameList, Settings }

enum class LibrarySection { Systems, Favorites, Recent }

enum class DeckTab { Now, Browse, Canvas }

/**
 * Shared state of both screens. [osFocusedScreen] mirrors which activity Android
 * currently treats as top resumed, which can differ from [activeScreen] for the
 * moment between a switch request and the system moving input focus.
 *
 * [awaitingDeckLaunch] keeps a freshly launched Deck from claiming the active
 * screen, because Android always gives focus to the activity started last.
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
) {
    val stageRoute: StageRoute get() = stageStack.last()

    /** With one display only Stage is composed, so the Deck panel takes input as a Stage layer. */
    val inputScreen: JarnScreen
        get() = if (deckDisplayAvailable) activeScreen else JarnScreen.Stage

    val canGoBack: Boolean
        get() = deckPanelOpen || stageStack.size > 1 || activeScreen == JarnScreen.Deck

    fun isLit(screen: JarnScreen): Boolean =
        activeScreen == screen || (screen == JarnScreen.Stage && !deckDisplayAvailable)

    fun needsFocusHandoff(screen: JarnScreen): Boolean =
        deckDisplayAvailable &&
            !awaitingDeckLaunch &&
            activeScreen == screen &&
            osFocusedScreen != null &&
            osFocusedScreen != screen

    fun openRoute(route: StageRoute): JarnSessionState {
        val existing = stageStack.indexOf(route)
        val stack = if (existing >= 0) stageStack.take(existing + 1) else stageStack + route
        return copy(stageStack = stack)
    }

    fun back(): JarnSessionState = when {
        deckPanelOpen -> copy(deckPanelOpen = false, activeScreen = JarnScreen.Stage)
        stageRoute == StageRoute.Settings -> popRoute()
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
        deckDisplayAvailable -> copy(activeScreen = activeScreen.other())
        else -> withDeckPanel(!deckPanelOpen)
    }

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
            return copy(
                deckDisplayAvailable = true,
                deckPanelOpen = false,
                activeScreen = if (deckPanelOpen) JarnScreen.Stage else activeScreen,
            )
        }
        val keepPanel = !deckDisplayAvailable && deckPanelOpen
        return copy(
            deckDisplayAvailable = false,
            deckPanelOpen = keepPanel,
            activeScreen = if (keepPanel) JarnScreen.Deck else JarnScreen.Stage,
            osFocusedScreen = osFocusedScreen.takeUnless { it == JarnScreen.Deck },
            awaitingDeckLaunch = false,
        )
    }

    fun shouldLaunchDeck(isStarted: Boolean, deckHostCreated: Boolean): Boolean =
        isStarted && deckDisplayAvailable && !deckHostCreated && !awaitingDeckLaunch

    fun deckLaunchStarted(): JarnSessionState = copy(awaitingDeckLaunch = true)

    fun deckLaunchFailed(): JarnSessionState = copy(awaitingDeckLaunch = false)

    fun onTopResumed(screen: JarnScreen, isTop: Boolean): JarnSessionState = when {
        !isTop -> copy(osFocusedScreen = osFocusedScreen.takeUnless { it == screen })
        awaitingDeckLaunch -> copy(
            osFocusedScreen = screen,
            awaitingDeckLaunch = screen != JarnScreen.Deck,
        )
        !deckDisplayAvailable -> copy(osFocusedScreen = screen)
        else -> copy(osFocusedScreen = screen, activeScreen = screen)
    }

    fun encode(): String = listOf(
        activeScreen.name,
        stageStack.joinToString(ROUTE_SEPARATOR) { it.name },
        librarySection.name,
        deckTab.name,
    ).joinToString(FIELD_SEPARATOR)

    private fun popRoute() = copy(stageStack = stageStack.dropLast(1))

    companion object {
        private const val FIELD_SEPARATOR = "|"
        private const val ROUTE_SEPARATOR = ","
        private const val FIELD_COUNT = 4

        fun decode(encoded: String?): JarnSessionState? {
            val fields = encoded?.split(FIELD_SEPARATOR)?.takeIf { it.size == FIELD_COUNT } ?: return null
            val routes = fields[1].split(ROUTE_SEPARATOR).map { enumOrNull<StageRoute>(it) ?: return null }
            if (routes.firstOrNull() != StageRoute.Library || routes.distinct().size != routes.size) return null
            return JarnSessionState(
                activeScreen = enumOrNull<JarnScreen>(fields[0]) ?: return null,
                stageStack = routes,
                librarySection = enumOrNull<LibrarySection>(fields[2]) ?: return null,
                deckTab = enumOrNull<DeckTab>(fields[3]) ?: return null,
            )
        }

        private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
            enumValues<T>().firstOrNull { it.name == name }
    }
}

private fun <T> List<T>.stepped(current: T, delta: Int): T =
    this[(indexOf(current) + delta).coerceIn(indices)]

private fun JarnScreen.other(): JarnScreen =
    if (this == JarnScreen.Stage) JarnScreen.Deck else JarnScreen.Stage
