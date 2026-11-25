package jr.brian.home.esde.launch

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.util.Log
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.util.launchApp

private const val TAG = "CompanionLaunch"

/** The display the last game was sent to, so a companion picked after the launch opens beside it. */
internal object LastGameDisplay {
    @Volatile var id: Int? = null
}

/**
 * The display a companion opens on: the first one the game isn't on. A game with no known display
 * (or one that has gone) counts as on the primary display, where games open by default. Null when
 * there is only one display, so the companion launches the way any app does.
 */
internal fun companionDisplayId(gameDisplayId: Int?, displayIds: List<Int>): Int? {
    if (displayIds.distinct().size < 2) return null
    val game = gameDisplayId?.takeIf { it in displayIds } ?: PRIMARY_DISPLAY_ID
    return displayIds.firstOrNull { it != game }
}

/**
 * Opens a game's companion app. With two screens it always goes to the one the game isn't on, so it
 * never covers the game, whatever the app's own display choice says: that choice is for opening
 * the app from the app list. With one screen it opens the way the app list would open it.
 *
 * The controller stays where it is. A companion opened before the game is followed by the game's
 * launch, which moves control to the game's screen, and one picked afterwards opens on the screen
 * the picker was on.
 */
fun launchCompanion(
    context: Context,
    packageName: String,
    preference: DisplayPreference,
    gameDisplayId: Int? = LastGameDisplay.id,
) {
    val displayIds = runCatching {
        (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)?.displays?.map { it.displayId }
    }.getOrNull().orEmpty()
    val target = companionDisplayId(gameDisplayId, displayIds)
        ?: return launchApp(context, packageName, preference)
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ?: return
    runCatching {
        context.startActivity(intent, ActivityOptions.makeBasic().setLaunchDisplayId(target).toBundle())
    }.onFailure { Log.w(TAG, "Could not open $packageName on display $target", it) }
}
