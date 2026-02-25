package jr.brian.home.ui.components.konfetti

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Rotation
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit

/**
 * Konfetti animation presets adapted from the DreaBDay project.
 * Each preset returns a list of [Party] configurations that define the confetti behavior.
 */
enum class KonfettiPreset(val displayName: String) {
    FESTIVE("Festive"),
    EXPLODE("Explode"),
    PARADE("Parade"),
    RAIN("Rain");

    companion object {
        fun fromName(name: String): KonfettiPreset {
            return entries.find { it.name == name } ?: PARADE
        }
    }
}

object KonfettiPresets {

    /**
     * Resolves theme colors at composition time and returns the parties for the given preset.
     */
    @Composable
    fun getParties(preset: KonfettiPreset): List<Party> {
        val themeColors = listOf(
            ThemePrimaryColor.toArgb(),
            ThemeSecondaryColor.toArgb(),
            ThemeAccentColor.toArgb()
        )
        return when (preset) {
            KonfettiPreset.FESTIVE -> festive(themeColors)
            KonfettiPreset.EXPLODE -> explode(themeColors)
            KonfettiPreset.PARADE -> parade(themeColors)
            KonfettiPreset.RAIN -> rain(themeColors)
        }
    }

    private fun baseRain(colors: List<Int>) = Party(
        speed = 0f,
        maxSpeed = 15f,
        damping = 0.9f,
        angle = Angle.BOTTOM,
        spread = Spread.ROUND,
        colors = colors,
        emitter = Emitter(duration = 5, TimeUnit.SECONDS).perSecond(100),
        position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0))
    )

    private fun festive(colors: List<Int>): List<Party> {
        val emitter = Emitter(
            duration = 100,
            TimeUnit.MILLISECONDS
        ).max(100)
        val party = Party(
            speed = 30f,
            maxSpeed = 50f,
            damping = 0.9f,
            angle = Angle.TOP,
            spread = 45,
            size = listOf(Size.LARGE, Size.LARGE, Size.LARGE),
            timeToLive = 3000L,
            rotation = Rotation(),
            colors = colors,
            emitter = emitter,
            position = Position.Relative(0.5, 1.0)
        )
        return listOf(
            baseRain(colors).copy(emitter = emitter),
            party,
            party.copy(
                speed = 55f,
                maxSpeed = 65f,
                spread = 10,
                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(10),
            ),
            party.copy(
                speed = 50f,
                maxSpeed = 60f,
                spread = 120,
                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(40),
            ),
            party.copy(
                speed = 65f,
                maxSpeed = 80f,
                spread = 10,
                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(10),
            )
        )
    }

    private fun explode(colors: List<Int>): List<Party> {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = colors,
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(0.2, 0.3)
        )
        return listOf(
            party,
            party.copy(position = Position.Relative(0.8, 0.3))
        )
    }

    private fun parade(colors: List<Int>): List<Party> {
        val party = Party(
            speed = 10f,
            maxSpeed = 30f,
            damping = 0.9f,
            angle = Angle.RIGHT - 45,
            spread = Spread.WIDE,
            colors = colors,
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(30),
            position = Position.Relative(0.0, 0.0)
        )
        return listOf(
            party.copy(position = Position.Relative(0.0, 0.2)),
            party.copy(position = Position.Relative(0.0, 0.8)),
            party.copy(position = Position.Relative(0.2, 0.3)),
            party.copy(position = Position.Relative(0.2, 0.7)),
            party.copy(
                angle = party.angle - 90,
                position = Position.Relative(1.0, 0.2)
            ),
            party.copy(
                angle = party.angle - 90,
                position = Position.Relative(1.0, 0.8)
            ),
            party.copy(
                angle = party.angle - 90,
                position = Position.Relative(.8, 0.3)
            ),
            party.copy(
                angle = party.angle - 90,
                position = Position.Relative(.8, 0.7)
            ),
        )
    }

    private fun rain(colors: List<Int>): List<Party> {
        return listOf(baseRain(colors))
    }
}
