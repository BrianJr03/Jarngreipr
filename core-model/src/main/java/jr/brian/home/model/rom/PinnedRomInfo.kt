package jr.brian.home.model.rom

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
