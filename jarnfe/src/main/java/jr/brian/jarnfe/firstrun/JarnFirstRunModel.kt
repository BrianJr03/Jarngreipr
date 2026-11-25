package jr.brian.jarnfe.firstrun

import jr.brian.core.model.ArtSource
import jr.brian.frontend.library.art.ArtSourceStatus
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.JarnShoulderPair
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.shell.roleAt

internal enum class JarnFirstRunGesture { Back, Context, SwitchScreen, Tabs, OtherTabs }

/**
 * The tab lesson whose practice tabs belong on [role]'s screen: L1/R1 on the top screen and L2/R2
 * on the bottom one. With a single screen the guide stands in for the Deck.
 */
internal fun JarnSessionState.practiceTabsStepOn(role: JarnScreen): JarnFirstRunStep? = when {
    !settingsOnDeck -> JarnFirstRunStep.PressTabs.takeIf { role == JarnScreen.Deck }
    roleAt(JarnShoulderPair.Top) == role -> JarnFirstRunStep.PressTabs
    else -> JarnFirstRunStep.PressOtherTabs
}

/**
 * [needsTwoScreens] steps teach a button that has nothing to act on with a single display.
 * [needsEsde] steps only show when ES-DE was found and the library has games.
 */
internal enum class JarnFirstRunStep(
    val gesture: JarnFirstRunGesture? = null,
    val needsTwoScreens: Boolean = false,
    val needsEsde: Boolean = false,
) {
    Welcome,
    Library,
    Esde(needsEsde = true),
    Media,
    Look,
    SwitchCue(needsTwoScreens = true),
    PressA,
    PressB(JarnFirstRunGesture.Back),
    /** Saved by name, so it keeps its old name: options moved from X to Select in #393. */
    PressX(JarnFirstRunGesture.Context),

    /** Saved by name, so it keeps its old name: switching screens moved from Select to X in #393. */
    PressSelect(JarnFirstRunGesture.SwitchScreen, needsTwoScreens = true),
    PressTabs(JarnFirstRunGesture.Tabs),
    PressOtherTabs(JarnFirstRunGesture.OtherTabs, needsTwoScreens = true),
    Done,

    /** The short tour's first step, for people coming from Classic. Never part of the full tour. */
    Ready;

    companion object {
        fun fromName(name: String?): JarnFirstRunStep =
            entries.firstOrNull { it.name == name } ?: Welcome

        fun available(
            twoScreens: Boolean,
            path: JarnFirstRunPath = JarnFirstRunPath.Full,
            offersEsde: Boolean = false,
        ): List<JarnFirstRunStep> =
            path.steps().filter { (twoScreens || !it.needsTwoScreens) && (offersEsde || !it.needsEsde) }

        private val Lessons = listOf(SwitchCue, PressA, PressB, PressX, PressSelect, PressTabs, PressOtherTabs)

        private fun JarnFirstRunPath.steps(): List<JarnFirstRunStep> = when (this) {
            JarnFirstRunPath.Full -> entries - Ready
            JarnFirstRunPath.Arrival -> listOf(Ready, Look, Done)
            JarnFirstRunPath.ArrivalLessons -> listOf(Ready, Look) + Lessons + Done
        }
    }
}

/**
 * [Arrival] is the short tour for someone coming from Classic: Ready, Look and Done.
 * [ArrivalLessons] is the same tour after "Show me the buttons", with the button lessons before Done.
 */
internal enum class JarnFirstRunPath { Full, Arrival, ArrivalLessons }

internal const val JARN_FIRST_RUN_PRACTICE_TABS = 3

/** A saved Ready step can only belong to the short tour, so a restart keeps it short. */
internal fun JarnFirstRunStep.restoredPath(): JarnFirstRunPath =
    if (this == JarnFirstRunStep.Ready) JarnFirstRunPath.Arrival else JarnFirstRunPath.Full

internal data class JarnFirstRunState(
    val step: JarnFirstRunStep = JarnFirstRunStep.Welcome,
    val confirmingSkip: Boolean = false,
    val practiced: Boolean = false,
    val practiceTab: Int = 0,
    val path: JarnFirstRunPath = step.restoredPath(),
    val offersEsde: Boolean = false,
) {
    private fun steps(twoScreens: Boolean) = JarnFirstRunStep.available(twoScreens, path, offersEsde)

    private fun at(step: JarnFirstRunStep) = JarnFirstRunState(step, path = path, offersEsde = offersEsde)

    /** ES-DE was found with games here, or no longer is: the ES-DE step comes or goes. */
    fun withEsdeOffer(offer: Boolean, twoScreens: Boolean): JarnFirstRunState =
        if (offer == offersEsde) this else copy(offersEsde = offer).settled(twoScreens)

    fun position(twoScreens: Boolean): Int = steps(twoScreens).indexOf(settled(twoScreens).step)

    fun total(twoScreens: Boolean): Int = steps(twoScreens).size

    /** A display can leave mid-tour, so a step that needs it gives way to the next one that fits. */
    fun settled(twoScreens: Boolean): JarnFirstRunState {
        val steps = steps(twoScreens)
        if (step in steps) return this
        return at(steps.firstOrNull { it.ordinal > step.ordinal } ?: JarnFirstRunStep.Done)
    }

    /** Null once the last step is passed. */
    fun next(twoScreens: Boolean): JarnFirstRunState? {
        if (confirmingSkip) return copy(confirmingSkip = false)
        val steps = steps(twoScreens)
        val following = steps.getOrNull(steps.indexOf(settled(twoScreens).step) + 1) ?: return null
        return at(following)
    }

    /** The first step has nowhere to go back to, so it asks before leaving the tour. */
    fun back(twoScreens: Boolean): JarnFirstRunState {
        if (confirmingSkip) return copy(confirmingSkip = false)
        val steps = steps(twoScreens)
        val index = steps.indexOf(settled(twoScreens).step)
        return if (index <= 0) copy(confirmingSkip = true) else at(steps[index - 1])
    }

    /** "Show me the buttons" on the short tour's Done: the lessons, then Done again. */
    fun withLessons(twoScreens: Boolean): JarnFirstRunState {
        if (path != JarnFirstRunPath.Arrival || step != JarnFirstRunStep.Done) return this
        val lessons = JarnFirstRunStep.available(twoScreens, JarnFirstRunPath.ArrivalLessons)
        val first = lessons[lessons.indexOf(JarnFirstRunStep.Look) + 1]
        return JarnFirstRunState(first, path = JarnFirstRunPath.ArrivalLessons, offersEsde = offersEsde)
    }

    /** Done on the short tour offers the lessons as well as finishing. */
    val offersLessons: Boolean
        get() = path == JarnFirstRunPath.Arrival && step == JarnFirstRunStep.Done && !confirmingSkip

    fun practice(gesture: JarnFirstRunGesture, tabDelta: Int = 0): JarnFirstRunState {
        if (confirmingSkip || step.gesture != gesture) return this
        val tab = (practiceTab + tabDelta).coerceIn(0, JARN_FIRST_RUN_PRACTICE_TABS - 1)
        return copy(practiced = true, practiceTab = tab)
    }
}

internal sealed interface JarnMediaSummary {
    data object Checking : JarnMediaSummary

    /** [wonOverOther] is true when ES-DE was picked while RetroHrai media was also there. */
    data class Detected(val source: ArtSource, val wonOverOther: Boolean) : JarnMediaSummary

    data class InUse(val source: ArtSource) : JarnMediaSummary

    /** The source in use has no files, but [source] does. */
    data class Elsewhere(val source: ArtSource) : JarnMediaSummary

    data object Missing : JarnMediaSummary
}

internal fun jarnMediaSummary(
    detected: ArtSource?,
    selected: ArtSource,
    statuses: List<ArtSourceStatus>?,
): JarnMediaSummary {
    if (detected != null) {
        val otherHasMedia = statuses.orEmpty().any { it.source == ArtSource.RetroHrai && it.isAvailable }
        return JarnMediaSummary.Detected(detected, detected == ArtSource.EsDe && otherHasMedia)
    }
    if (statuses == null) return JarnMediaSummary.Checking
    if (statuses.any { it.source == selected && it.isAvailable }) return JarnMediaSummary.InUse(selected)
    val other = statuses.firstOrNull { it.isAvailable } ?: return JarnMediaSummary.Missing
    return JarnMediaSummary.Elsewhere(other.source)
}
