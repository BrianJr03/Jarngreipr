package jr.brian.frontend.library.art

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.ArtSource
import jr.brian.core.model.SystemArtSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Persisted Jarn FE [ArtSource]. Missing or unknown values read as the default. */
@Singleton
class ArtSourceStore @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<ArtSource> = _state.asStateFlow()

    val source: ArtSource get() = _state.value

    private val _detectedState = MutableStateFlow(
        prefs.getString(KEY_DETECTED_SOURCE, null)?.let(ArtSource::fromNameOrDefault),
    )

    /** Device-local and left out of backups, because a restored source is a choice. */
    val detectedState: StateFlow<ArtSource?> = _detectedState.asStateFlow()

    /** Null until a source is saved, so an untouched default never passes as a choice. */
    val savedSource: ArtSource? get() = source.takeIf { prefs.contains(KEY_SOURCE) }

    @Synchronized
    fun setSource(source: ArtSource) {
        _state.value = source
        _detectedState.value = null
        prefs.edit {
            putString(KEY_SOURCE, source.name)
            remove(KEY_DETECTED_SOURCE)
        }
    }

    /** Saves [source] only if nothing is saved yet, so a choice made meanwhile wins. */
    @Synchronized
    fun saveDetected(source: ArtSource): Boolean {
        if (prefs.contains(KEY_SOURCE)) return false
        _state.value = source
        _detectedState.value = source
        prefs.edit {
            putString(KEY_SOURCE, source.name)
            putString(KEY_DETECTED_SOURCE, source.name)
        }
        return true
    }

    private fun load(): ArtSource =
        ArtSource.fromNameOrDefault(prefs.getString(KEY_SOURCE, null))

    private val _systemState = MutableStateFlow(
        SystemArtSource.fromNameOrDefault(prefs.getString(KEY_SYSTEM_SOURCE, null)),
    )
    val systemState: StateFlow<SystemArtSource> = _systemState.asStateFlow()

    val systemSource: SystemArtSource get() = _systemState.value

    fun setSystemSource(source: SystemArtSource) {
        _systemState.value = source
        prefs.edit { putString(KEY_SYSTEM_SOURCE, source.name) }
    }

    private companion object {
        const val PREFS_NAME = "jarnfe_art_prefs"
        const val KEY_SOURCE = "art_source"
        const val KEY_DETECTED_SOURCE = "detected_art_source"
        const val KEY_SYSTEM_SOURCE = "system_art_source"
    }
}
