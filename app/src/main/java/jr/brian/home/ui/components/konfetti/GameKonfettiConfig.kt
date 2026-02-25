package jr.brian.home.ui.components.konfetti

import kotlinx.serialization.Serializable

@Serializable
enum class KonfettiTrigger(val displayName: String) {
    GAME_SELECT("Game Select"),
    GAME_START("Game Start");
}

@Serializable
data class GameKonfettiConfig(
    val enabled: Boolean = true,
    val trigger: KonfettiTrigger = KonfettiTrigger.GAME_START,
    val usePreset: Boolean = true,
    val selectedPreset: String = KonfettiPreset.EXPLODE.name,
    val useCharShape: Boolean = false,
    val speed: Float = 10f,
    val maxSpeed: Float = 30f,
    val damping: Float = 0.9f,
    val angle: Int = 270,
    val spread: Int = 120,
    val timeToLive: Long = 3000L,
    val emitterDurationMs: Long = 2000L,
    val emitterAmountPerSecond: Int = 30,
    // Letter Burst settings
    val letterBurstChar: String = "",
    val letterBurstExplodePreset: String = KonfettiPreset.EXPLODE.name,
    val letterBurstFormationMs: Int = 1200,
    val letterBurstHoldMs: Long = 400L,
    val letterBurstParticleCount: Int = 100
) {
    /** Returns true when the selected preset is LETTER_BURST. */
    val isLetterBurst: Boolean
        get() = usePreset && selectedPreset == KonfettiPreset.LETTER_BURST.name
}
