package jr.brian.home.data

import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import dagger.Lazy
import jr.brian.core.model.Experience
import jr.brian.core.model.JarnHomeAction
import jr.brian.core.model.JarnHomeLoopGuard
import jr.brian.core.model.JarnHomePress
import jr.brian.core.model.JarnHomeRepeat
import jr.brian.core.model.JarnHomeWindow
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.core.model.resolveJarnHomeAction
import jr.brian.core.model.resolveLauncherExperience
import jr.brian.home.R
import jr.brian.home.ui.util.launchJarnFe
import jr.brian.home.ui.util.resolveBottomDisplayId
import jr.brian.home.ui.util.showJarnBesideGame
import jr.brian.jarnfe.shell.JarnHomeEntry
import javax.inject.Inject
import javax.inject.Singleton

/** Sends a Home press to Jarn FE when it is the saved experience. False means use Classic. */
@Singleton
class ExperienceHomeRouter @Inject constructor(
    private val store: Lazy<ExperienceStore>,
) {
    private val loopGuard = JarnHomeLoopGuard()
    private val repeatPress = JarnHomeRepeat()

    fun openJarnIfChosen(
        context: Context,
        press: JarnHomePress,
        forceClassic: Boolean = false,
        entryDisplayId: Int? = null,
    ): Boolean = openChosenJarn(
        jarnAvailable = JarnFeOffer.isOffered(context),
        store = store::get,
        forceClassic = forceClassic,
        isLoopFree = { isLoopFree(entryDisplayId) },
        open = { openForPress(context, press) },
    )

    private fun isLoopFree(entryDisplayId: Int?): Boolean =
        entryDisplayId == null ||
            entryDisplayId == PRIMARY_DISPLAY_ID ||
            loopGuard.allowsJarn(SystemClock.elapsedRealtime(), JarnHomeEntry.isDeckUp)

    /** Every action that keeps a game reports success, so Classic never opens over that game. */
    private fun openForPress(context: Context, press: JarnHomePress): Boolean {
        val bottomDisplayId = resolveBottomDisplayId(context)
        val action = resolveJarnHomeAction(
            press = press,
            cover = JarnHomeEntry.gameCover,
            hasBottomDisplay = bottomDisplayId != null,
            isRepeatPress = { repeatPress.isRepeat(SystemClock.elapsedRealtime()) },
        )
        when (action) {
            JarnHomeAction.OpenJarn -> return launchJarnFe(context)
            JarnHomeAction.ShowDeck -> bottomDisplayId?.let {
                showJarnBesideGame(context, JarnHomeWindow.Deck, it)
            }
            JarnHomeAction.ShowStage ->
                showJarnBesideGame(context, JarnHomeWindow.Stage, PRIMARY_DISPLAY_ID)
            JarnHomeAction.AskAgain ->
                Toast.makeText(context, R.string.jarn_home_press_again, Toast.LENGTH_LONG).show()
        }
        return true
    }
}

/**
 * Builds without Jarn FE return before reading the store, so their Classic path is untouched. A
 * Home press never saves a choice: only a saved Jarn opens it.
 */
internal fun openChosenJarn(
    jarnAvailable: Boolean,
    store: () -> ExperienceStore,
    forceClassic: Boolean,
    isLoopFree: () -> Boolean,
    open: () -> Boolean,
): Boolean {
    if (!jarnAvailable) return false
    val experienceStore = store()
    val choice = resolveLauncherExperience(
        jarnAvailable = true,
        stored = experienceStore.storedExperience,
        forceClassic = forceClassic,
    )
    return choice == Experience.JarnFE && isLoopFree() && open()
}
