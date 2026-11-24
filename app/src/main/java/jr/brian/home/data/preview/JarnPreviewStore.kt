package jr.brian.home.data.preview

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.Experience
import jr.brian.frontend.look.JarnLookStore
import jr.brian.home.data.ExperienceStore
import jr.brian.home.data.JarnFeOffer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** Showings and tries of the Meet Jarn preview. [tried] also counts a JarnFE save or a finished Jarn tour. */
data class JarnPreviewState(
    val showings: Int = 0,
    val lastShownVersion: String? = null,
    val tried: Boolean = false,
    val installKind: JarnInstallKind = JarnInstallKind.FreshInstall,
)

const val JARN_PREVIEW_MAX_SHOWINGS = 3

/** The preview's own rules, without the build check or the saved experience. */
internal fun JarnPreviewState.allowsShowing(versionName: String): Boolean =
    !tried && showings < JARN_PREVIEW_MAX_SHOWINGS && lastShownVersion != versionName

/** What only this store writes, before the other stores' signals are folded in. */
internal data class JarnPreviewSaved(
    val showings: Int = 0,
    val lastShownVersion: String? = null,
    val tried: Boolean = false,
)

/** Restore rules, like the tour-done flag: tried only turns on and the count only goes up. */
internal fun JarnPreviewSaved.mergedWith(
    showings: Int?,
    lastShownVersion: String?,
    tried: Boolean?,
): JarnPreviewSaved {
    val restoredCount = showings?.coerceAtLeast(0) ?: 0
    val takeVersion = lastShownVersion != null && (this.lastShownVersion == null || restoredCount > this.showings)
    return JarnPreviewSaved(
        showings = maxOf(this.showings, restoredCount),
        lastShownVersion = if (takeVersion) lastShownVersion else this.lastShownVersion,
        tried = this.tried || tried == true,
    )
}

@Singleton
class JarnPreviewStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val experienceStore: ExperienceStore,
    private val jarnLookStore: JarnLookStore,
) : JarnPreviewGate {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Taken when the store is first created, so it is read before What's New marks this version seen. */
    private val installKind = context.detectInstallKind()

    private val saved = MutableStateFlow(readSaved())

    val state: Flow<JarnPreviewState> = combine(
        saved,
        experienceStore.everJarnFeState,
        jarnLookStore.firstRunDoneState,
    ) { stored, everJarnFe, firstRunDone -> stored.toState(everJarnFe || firstRunDone) }

    val current: JarnPreviewState
        get() = saved.value.toState(experienceStore.everJarnFe || jarnLookStore.firstRunDone)

    override fun shouldShow(versionName: String): Boolean =
        experienceStore.experience != Experience.JarnFE &&
            current.allowsShowing(versionName) &&
            JarnFeOffer.isOffered(context)

    override fun recordShown(versionName: String) {
        val stored = saved.value
        if (stored.lastShownVersion == versionName) return
        write(stored.copy(showings = stored.showings + 1, lastShownVersion = versionName))
    }

    override fun markTried() {
        if (!saved.value.tried) write(saved.value.copy(tried = true))
    }

    /** Backups record the effective [JarnPreviewState.tried], so a restore elsewhere retires it too. */
    internal fun restore(showings: Int?, lastShownVersion: String?, tried: Boolean?) {
        val next = saved.value.mergedWith(showings, lastShownVersion, tried)
        if (next != saved.value) write(next)
    }

    private fun JarnPreviewSaved.toState(triedElsewhere: Boolean) = JarnPreviewState(
        showings = showings,
        lastShownVersion = lastShownVersion,
        tried = tried || triedElsewhere,
        installKind = installKind,
    )

    private fun write(next: JarnPreviewSaved) {
        saved.value = next
        prefs.edit {
            putInt(KEY_SHOWINGS, next.showings)
            putString(KEY_LAST_SHOWN_VERSION, next.lastShownVersion)
            putBoolean(KEY_TRIED, next.tried)
        }
    }

    private fun readSaved() = JarnPreviewSaved(
        showings = prefs.getInt(KEY_SHOWINGS, 0),
        lastShownVersion = prefs.getString(KEY_LAST_SHOWN_VERSION, null),
        tried = prefs.getBoolean(KEY_TRIED, false),
    )

    private companion object {
        const val PREFS_NAME = "jarn_preview_prefs"
        const val KEY_SHOWINGS = "showings"
        const val KEY_LAST_SHOWN_VERSION = "last_shown_version"
        const val KEY_TRIED = "tried"
    }
}
