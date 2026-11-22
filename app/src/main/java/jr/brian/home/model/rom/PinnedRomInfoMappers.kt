package jr.brian.home.model.rom

import jr.brian.home.esde.model.GameInfo

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
