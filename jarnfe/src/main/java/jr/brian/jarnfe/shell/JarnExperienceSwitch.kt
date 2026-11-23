package jr.brian.jarnfe.shell

import android.app.Activity
import android.content.Context

/** Leaves Jarn FE for Classic. False means Classic did not open and the choice is unchanged. */
interface JarnExperienceSwitch {
    fun switchToClassic(host: Activity): Boolean
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnExperienceSwitchProvider {
    val jarnExperienceSwitch: JarnExperienceSwitch
}

fun Context.jarnExperienceSwitchOrNull(): JarnExperienceSwitch? =
    (applicationContext as? JarnExperienceSwitchProvider)?.jarnExperienceSwitch

/** Both windows close only after Classic has opened, so a failure leaves Jarn FE usable. */
internal fun leaveForClassic(host: Activity, switch: JarnExperienceSwitch): Boolean {
    val opened = switch.switchToClassic(host)
    if (opened) JarnScreenActivity.finishAllWindows()
    return opened
}
