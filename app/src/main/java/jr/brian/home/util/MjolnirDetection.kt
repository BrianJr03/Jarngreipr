package jr.brian.home.util

import android.content.Context
import android.content.pm.PackageManager

/**
 * Detection for the third-party launcher whose accessibility service also
 * intercepts the hardware Home button.
 *
 * Two apps consuming the same key event have undefined ordering — the result is
 * double launches or dropped presses. The [jr.brian.home.ui.components.settings.HomeInterceptionSettingItem]
 * confirmation dialog uses this to sharpen its copy when the conflict is
 * concretely present rather than merely hypothetical.
 *
 * Depends on the app's existing `QUERY_ALL_PACKAGES` permission — a `<queries>`
 * entry is not required.
 */
object MjolnirDetection {

    const val MJOLNIR_PACKAGE = "xyz.blacksheep.mjolnir"

    fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(MJOLNIR_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
