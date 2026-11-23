package jr.brian.frontend.apps

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

private const val PIN_SEPARATOR = ","

internal fun decodeAppPins(stored: String?): List<String> =
    stored.orEmpty().split(PIN_SEPARATOR).cleanedPins()

private fun List<String>.cleanedPins(): List<String> = filter { it.isNotBlank() }.distinct()

/** Jarn FE's pinned apps in pin order. An uninstalled app keeps its pin in case it comes back. */
@Singleton
class JarnAppPinStore @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _pinned = MutableStateFlow(decodeAppPins(prefs.getString(KEY_PINS, null)))
    val pinnedState: StateFlow<List<String>> = _pinned.asStateFlow()
    val pinned: List<String> get() = _pinned.value

    fun setPinned(packageName: String, pinned: Boolean) = change { current ->
        when {
            !pinned -> current - packageName
            packageName in current -> current
            else -> current + packageName
        }
    }

    fun restore(packages: List<String>) = change { packages.cleanedPins() }

    private fun change(transform: (List<String>) -> List<String>) {
        _pinned.update(transform)
        prefs.edit { putString(KEY_PINS, _pinned.value.joinToString(PIN_SEPARATOR)) }
    }

    private companion object {
        const val PREFS_NAME = "jarnfe_app_pin_prefs"
        const val KEY_PINS = "pinned_packages"
    }
}
