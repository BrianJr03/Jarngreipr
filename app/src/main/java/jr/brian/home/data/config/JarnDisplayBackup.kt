package jr.brian.home.data.config

import jr.brian.core.ui.motion.JarnPageTransition
import jr.brian.core.ui.theme.JarnSwitchCue
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnTapControl

/** Jarn FE display choices to and from a backup. A null field keeps the current value. */
internal fun JarnDisplayPrefs.toConfig() = JarnDisplayConfig(
    systemTileScale = systemTileScale,
    gameTileScale = gameTileScale,
    hintsVisible = hintsVisible,
    buttonHaptics = buttonHaptics,
    tapControl = tapControl.id,
    appNamesVisible = appNamesVisible,
    undoCountdownVisible = undoCountdownVisible,
    switchCue = switchCue.id,
    systemView = systemView.id,
    gameView = gameView.id,
    pageTransition = pageTransition.id,
    systemNamesVisible = systemNamesVisible,
    randomVisible = randomVisible,
    lastPlayedVisible = lastPlayedVisible,
    systemLogoVisible = systemLogoVisible,
    gameCountVisible = gameCountVisible,
    lastPlayedTextVisible = lastPlayedTextVisible,
    gameRowVisible = gameRowVisible,
)

internal fun JarnDisplayConfig.applyTo(current: JarnDisplayPrefs) = current.copy(
    systemTileScale = systemTileScale ?: current.systemTileScale,
    gameTileScale = gameTileScale ?: current.gameTileScale,
    hintsVisible = hintsVisible ?: current.hintsVisible,
    buttonHaptics = buttonHaptics ?: current.buttonHaptics,
    tapControl = JarnTapControl.fromId(tapControl) ?: current.tapControl,
    appNamesVisible = appNamesVisible ?: current.appNamesVisible,
    undoCountdownVisible = undoCountdownVisible ?: current.undoCountdownVisible,
    switchCue = switchCue?.let(JarnSwitchCue::fromId) ?: current.switchCue,
    systemView = systemView?.let(JarnBrowseView::fromId) ?: current.systemView,
    gameView = gameView?.let(JarnBrowseView::fromId) ?: current.gameView,
    pageTransition = pageTransition?.let(JarnPageTransition::fromId) ?: current.pageTransition,
    systemNamesVisible = systemNamesVisible ?: current.systemNamesVisible,
    randomVisible = randomVisible ?: current.randomVisible,
    lastPlayedVisible = lastPlayedVisible ?: current.lastPlayedVisible,
    systemLogoVisible = systemLogoVisible ?: current.systemLogoVisible,
    gameCountVisible = gameCountVisible ?: current.gameCountVisible,
    lastPlayedTextVisible = lastPlayedTextVisible ?: current.lastPlayedTextVisible,
    gameRowVisible = gameRowVisible ?: current.gameRowVisible,
)
