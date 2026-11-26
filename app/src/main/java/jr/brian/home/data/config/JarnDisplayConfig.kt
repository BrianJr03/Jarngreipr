package jr.brian.home.data.config

import kotlinx.serialization.Serializable

/** Each field is nullable so a partial backup only changes what it carries. */
@Serializable
data class JarnDisplayConfig(
    val systemTileScale: Float? = null,
    val gameTileScale: Float? = null,
    val hintsVisible: Boolean? = null,
    val buttonHaptics: Boolean? = null,
    val tapControl: String? = null,
    val appNamesVisible: Boolean? = null,
    val undoCountdownVisible: Boolean? = null,
    val switchCue: String? = null,
    val systemView: String? = null,
    val gameView: String? = null,
    val pageTransition: String? = null,
    val systemNamesVisible: Boolean? = null,
    val randomVisible: Boolean? = null,
    val lastPlayedVisible: Boolean? = null,
    val systemLogoVisible: Boolean? = null,
    val gameCountVisible: Boolean? = null,
    val lastPlayedTextVisible: Boolean? = null,
    val gameRowVisible: Boolean? = null,
    val systemBackgroundVisible: Boolean? = null,
    val playingScreenVisible: Boolean? = null,
    val playingDim: String? = null,
    val playingScreenOff: Boolean? = null,
    val videoPreviews: Boolean? = null,
    val videoPreviewSound: Boolean? = null,
    val videoFit: String? = null,
    val tabFocus: String? = null,
    val pauseOnBatterySaver: Boolean? = null,
    val librarySectionOrder: List<String>? = null,
    val deckTabOrder: List<String>? = null,
    val stickMute: Boolean? = null,
    val coverGlow: String? = null,
)
