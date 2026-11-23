package jr.brian.frontend.library.art

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Persisted Jarn FE art choices. Classic never reads these. */
@Singleton
class GameArtChoiceStore @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(decodeArtChoices(prefs.getString(KEY_CHOICES, null)))
    val state: StateFlow<GameArtChoices> = _state.asStateFlow()

    val choices: GameArtChoices get() = _state.value

    fun setForGame(screen: ArtScreen, gameKey: String, systemId: String, choice: ArtChoice) =
        change { it.withGame(screen, gameKey, systemId, choice) }

    fun setForSystem(screen: ArtScreen, systemId: String, choice: ArtChoice) =
        change { it.withSystem(screen, systemId, choice) }

    fun restore(choices: GameArtChoices) = change { choices }

    private fun change(transform: (GameArtChoices) -> GameArtChoices) {
        _state.update(transform)
        prefs.edit { putString(KEY_CHOICES, encodeArtChoices(_state.value)) }
    }

    private companion object {
        const val PREFS_NAME = "jarnfe_art_choice_prefs"
        const val KEY_CHOICES = "choices"
    }
}

private const val FIELD_TOP_SYSTEMS = "topBySystem"
private const val FIELD_BOTTOM_SYSTEMS = "bottomBySystem"
private const val FIELD_TOP_GAMES = "topByGame"
private const val FIELD_BOTTOM_GAMES = "bottomByGame"

internal fun encodeArtChoices(choices: GameArtChoices): String = JSONObject()
    .put(FIELD_TOP_SYSTEMS, choices.topBySystem.toJson())
    .put(FIELD_BOTTOM_SYSTEMS, choices.bottomBySystem.toJson())
    .put(FIELD_TOP_GAMES, choices.topByGame.toJson())
    .put(FIELD_BOTTOM_GAMES, choices.bottomByGame.toJson())
    .toString()

/** Damaged data reads as no choices, and an unknown kind is skipped. */
internal fun decodeArtChoices(raw: String?): GameArtChoices {
    if (raw.isNullOrBlank()) return GameArtChoices()
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return GameArtChoices()
    return GameArtChoices(
        topBySystem = root.choiceMap(FIELD_TOP_SYSTEMS),
        bottomBySystem = root.choiceMap(FIELD_BOTTOM_SYSTEMS),
        topByGame = root.choiceMap(FIELD_TOP_GAMES),
        bottomByGame = root.choiceMap(FIELD_BOTTOM_GAMES),
    )
}

private fun Map<String, ArtChoice>.toJson() = JSONObject().also { json ->
    forEach { (key, choice) -> json.put(key, choice.name) }
}

private fun JSONObject.choiceMap(field: String): Map<String, ArtChoice> {
    val entries = optJSONObject(field) ?: return emptyMap()
    return entries.keys().asSequence().mapNotNull { key ->
        ArtChoice.fromNameOrNull(entries.optString(key))?.let { key to it }
    }.toMap()
}
