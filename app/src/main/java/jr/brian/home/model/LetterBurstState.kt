package jr.brian.home.model

import jr.brian.home.ui.components.konfetti.KonfettiPreset

data class LetterBurstState(
    val char: Char,
    val colors: List<Int>,
    val burstPreset: KonfettiPreset,
    val formationMs: Int,
    val holdMs: Long,
    val particleCount: Int
)