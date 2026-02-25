package jr.brian.home.esde.model

import jr.brian.home.ui.components.konfetti.KonfettiTrigger

data class GameKonfettiEvent(
    val gameFilename: String,
    val trigger: KonfettiTrigger
)
