package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.home.ui.components.konfetti.GameKonfettiConfig
import jr.brian.home.ui.components.konfetti.KonfettiPreset
import jr.brian.home.ui.components.konfetti.KonfettiPresets
import kotlinx.serialization.json.Json
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import java.util.concurrent.TimeUnit

class GameKonfettiManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    var config by mutableStateOf(loadConfig())
        private set

    fun updateConfig(newConfig: GameKonfettiConfig) {
        config = newConfig
        prefs.edit().apply {
            putString(KEY_CONFIG, json.encodeToString(GameKonfettiConfig.serializer(), newConfig))
            apply()
        }
    }

    fun buildParties(
        colors: List<Int>,
        charShape: Shape.DrawableShape? = null
    ): List<Party> {
        val cfg = config
        if (cfg.usePreset) {
            val preset = KonfettiPreset.fromName(cfg.selectedPreset)
            // LETTER_BURST is handled via LetterFormationBurst composable, not Party list
            if (preset == KonfettiPreset.LETTER_BURST) return emptyList()
            return KonfettiPresets.getPartiesFromColors(preset, colors, charShape)
        }
        return buildCustomParties(cfg, colors, charShape)
    }

    /**
     * Resolves the character to use for Letter Burst mode.
     * Falls back to the configured letterBurstChar, then the game filename's first char,
     * then 'A'.
     */
    fun resolveLetterBurstChar(gameFilename: String? = null): Char {
        val cfg = config
        // If user set a specific char in editor, use it (uppercase letters/digits only)
        if (cfg.letterBurstChar.isNotEmpty()) {
            val ch = cfg.letterBurstChar.first().uppercaseChar()
            if (ch.isLetterOrDigit()) return ch
        }
        // Otherwise use the game's first alphanumeric character (strip path and extension)
        if (gameFilename != null) {
            val nameOnly = gameFilename
                .substringAfterLast("/")
                .substringBeforeLast(".")
            val firstChar = nameOnly.firstOrNull { it.isLetterOrDigit() }
            if (firstChar != null) return firstChar.uppercaseChar()
        }
        return 'A'
    }

    /**
     * Returns the burst preset to use after the letter formation.
     */
    fun letterBurstExplodePreset(): KonfettiPreset {
        return KonfettiPreset.fromName(config.letterBurstExplodePreset)
    }

    private fun buildCustomParties(
        cfg: GameKonfettiConfig,
        colors: List<Int>,
        charShape: Shape.DrawableShape?
    ): List<Party> {
        val shapes = if (charShape != null) listOf(charShape) else listOf(
            Shape.Circle,
            Shape.Square
        )
        val party = Party(
            speed = cfg.speed,
            maxSpeed = cfg.maxSpeed,
            damping = cfg.damping,
            angle = cfg.angle,
            spread = cfg.spread,
            timeToLive = cfg.timeToLive,
            colors = colors,
            shapes = shapes,
            emitter = Emitter(
                duration = cfg.emitterDurationMs,
                TimeUnit.MILLISECONDS
            ).perSecond(cfg.emitterAmountPerSecond),
            position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0))
        )

        return listOf(
            party,
            party.copy(position = Position.Relative(0.5, 1.0)),
            party.copy(
                angle = (cfg.angle + 180) % 360,
                position = Position.Relative(1.0, 0.5)
            )
        )
    }

    private fun loadConfig(): GameKonfettiConfig {
        val raw = prefs.getString(KEY_CONFIG, null) ?: return GameKonfettiConfig()
        return try {
            json.decodeFromString(GameKonfettiConfig.serializer(), raw)
        } catch (_: Exception) {
            GameKonfettiConfig()
        }
    }

    companion object {
        private const val PREFS_NAME = "game_konfetti_prefs"
        private const val KEY_CONFIG = "game_konfetti_config"
    }
}
