package jr.brian.home.util

import android.os.Build

/**
 * Guard for AYN Thor-only features (currently: hardware Home button interception).
 *
 * The interception path only works on devices whose firmware surfaces
 * `KEYCODE_HOME` as a plain key event before `PhoneWindowManager` consumes it.
 * The AYN Thor is the one such device this launcher targets — on stock AOSP the
 * feature silently does nothing, so we hide the setting entirely to avoid
 * confusing users on other hardware.
 *
 * Identifiers observed on a real Thor:
 *   ro.product.manufacturer = "AYN"
 *   ro.product.model        = "AYN Thor"
 *
 * We gate on BOTH so a future non-Thor AYN device doesn't accidentally light up
 * the feature. Comparisons are case-insensitive because vendors are inconsistent
 * about capitalization of their own props.
 */
object ThorDetection {

    const val THOR_MANUFACTURER = "AYN"
    const val THOR_MODEL_KEYWORD = "Thor"

    /** Convenience wrapper that reads the current [Build] props. */
    fun isThor(): Boolean = isThor(Build.MANUFACTURER, Build.MODEL)

    /** Pure form for unit tests. */
    fun isThor(manufacturer: String?, model: String?): Boolean {
        val mfg = manufacturer?.trim().orEmpty()
        val mdl = model?.trim().orEmpty()
        return mfg.equals(THOR_MANUFACTURER, ignoreCase = true) &&
            mdl.contains(THOR_MODEL_KEYWORD, ignoreCase = true)
    }
}
