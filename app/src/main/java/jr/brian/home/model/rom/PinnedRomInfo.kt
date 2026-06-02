package jr.brian.home.model.rom

import jr.brian.home.esde.model.GameInfo
import kotlinx.serialization.Serializable

@Serializable
data class PinnedRomInfo(
    val key: String,
    val name: String,
    val systemName: String,
    val path: String,
    val artworkPath: String? = null,
    val marqueeImagePath: String? = null,
    val screenshotPath: String? = null,
    val fanartPath: String? = null,
    val titlescreenPath: String? = null,
    val miximagePath: String? = null,
    val physicalMediaPath: String? = null,
    val emulatorPackage: String? = null,
    val launchCommand: String? = null,
    val displayMediaType: String? = null
)

fun GameInfo.toPinnedRomInfo() = PinnedRomInfo(
    key = "${systemName}/${path}",
    name = name,
    systemName = systemName,
    path = path,
    artworkPath = artworkPath,
    marqueeImagePath = marqueeImagePath,
    screenshotPath = screenshotPath,
    fanartPath = fanartPath,
    titlescreenPath = titlescreenPath,
    miximagePath = miximagePath,
    physicalMediaPath = physicalMediaPath,
    emulatorPackage = emulatorPackage,
    launchCommand = launchCommand
)

fun PinnedRomInfo.toGameInfo() = GameInfo(
    path = path,
    name = name,
    systemName = systemName,
    emulatorPackage = emulatorPackage,
    launchCommand = launchCommand,
    artworkPath = artworkPath,
    marqueeImagePath = marqueeImagePath,
    screenshotPath = screenshotPath,
    fanartPath = fanartPath,
    titlescreenPath = titlescreenPath,
    miximagePath = miximagePath,
    physicalMediaPath = physicalMediaPath
)

fun PinnedRomInfo.resolveDisplayPath(): String? = when (displayMediaType) {
    "Covers" -> artworkPath
    "Marquee" -> marqueeImagePath
    "Screenshots" -> screenshotPath
    "Fanart" -> fanartPath
    "TitleScreens" -> titlescreenPath
    "MixImages" -> miximagePath
    "PhysicalMedia" -> physicalMediaPath
    else -> artworkPath ?: marqueeImagePath ?: screenshotPath
        ?: fanartPath ?: titlescreenPath ?: miximagePath ?: physicalMediaPath
}
