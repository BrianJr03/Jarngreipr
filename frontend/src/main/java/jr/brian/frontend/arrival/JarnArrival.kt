package jr.brian.frontend.arrival

import android.content.Context
import androidx.core.content.edit

/** Set by Classic right after it opens Jarn from the preview or Settings, read once by Jarn's first run. */
object JarnArrival {
    private const val PREFS = "jarnfe_arrival_prefs"
    private const val KEY_FROM_CLASSIC = "from_classic"

    fun markFromClassic(context: Context) {
        context.prefs().edit { putBoolean(KEY_FROM_CLASSIC, true) }
    }

    /** True once after a switch from Classic, then cleared. */
    fun takeFromClassic(context: Context): Boolean {
        val prefs = context.prefs()
        val fromClassic = prefs.getBoolean(KEY_FROM_CLASSIC, false)
        if (fromClassic) prefs.edit { remove(KEY_FROM_CLASSIC) }
        return fromClassic
    }

    private fun Context.prefs() = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
