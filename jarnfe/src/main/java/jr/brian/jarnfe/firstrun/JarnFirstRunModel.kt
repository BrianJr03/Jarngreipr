package jr.brian.jarnfe.firstrun

import jr.brian.core.model.ArtSource
import jr.brian.frontend.library.art.ArtSourceStatus

internal enum class JarnFirstRunGesture { Back, Context, SwitchScreen, Tabs, OtherTabs }

/** [needsTwoScreens] steps teach a button that has nothing to act on with a single display. */
internal enum class JarnFirstRunStep(
    val gesture: JarnFirstRunGesture? = null,
    val needsTwoScreens: Boolean = false,
) {
    Welcome,
    Library,
    Media,
    Look,
    PressA,
    PressB(JarnFirstRunGesture.Back),
    PressX(JarnFirstRunGesture.Context),
    PressSelect(JarnFirstRunGesture.SwitchScreen, needsTwoScreens = true),
    PressTabs(JarnFirstRunGesture.Tabs),
    PressOtherTabs(JarnFirstRunGesture.OtherTabs, needsTwoScreens = true),
    Done;

    companion object {
        fun fromName(name: String?): JarnFirstRunStep =
            entries.firstOrNull { it.name == name } ?: Welcome

        fun available(twoScreens: Boolean): List<JarnFirstRunStep> =
            entries.filter { twoScreens || !it.needsTwoScreens }
    }
}

internal const val JARN_FIRST_RUN_PRACTICE_TABS = 3

internal data class JarnFirstRunState(
    val step: JarnFirstRunStep = JarnFirstRunStep.Welcome,
    val confirmingSkip: Boolean = false,
    val practiced: Boolean = false,
    val practiceTab: Int = 0,
) {
    fun position(twoScreens: Boolean): Int =
        JarnFirstRunStep.available(twoScreens).indexOf(settled(twoScreens).step)

    fun total(twoScreens: Boolean): Int = JarnFirstRunStep.available(twoScreens).size

    /** A display can leave mid-tour, so a step that needs it gives way to the next one that fits. */
    fun settled(twoScreens: Boolean): JarnFirstRunState {
        if (twoScreens || !step.needsTwoScreens) return this
        val next = JarnFirstRunStep.entries.drop(step.ordinal + 1).first { !it.needsTwoScreens }
        return JarnFirstRunState(next)
    }

    /** Null once the last step is passed. */
    fun next(twoScreens: Boolean): JarnFirstRunState? {
        if (confirmingSkip) return copy(confirmingSkip = false)
        val steps = JarnFirstRunStep.available(twoScreens)
        val following = steps.getOrNull(steps.indexOf(settled(twoScreens).step) + 1) ?: return null
        return JarnFirstRunState(following)
    }

    /** The first step has nowhere to go back to, so it asks before leaving the tour. */
    fun back(twoScreens: Boolean): JarnFirstRunState {
        if (confirmingSkip) return copy(confirmingSkip = false)
        val steps = JarnFirstRunStep.available(twoScreens)
        val index = steps.indexOf(settled(twoScreens).step)
        return if (index <= 0) copy(confirmingSkip = true) else JarnFirstRunState(steps[index - 1])
    }

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
