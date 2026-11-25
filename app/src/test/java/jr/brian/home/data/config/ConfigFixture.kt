package jr.brian.home.data.config

/** A v68 backup with every area filled in, so each config class takes part in the fixture. */
internal fun configFixture() = JarngreiprConfig(
    exportedAt = "2026-11-25T12:00:00Z",
    feature = FeatureConfig(
        jarnDisplay = JarnDisplayConfig(hintsVisible = false, playingScreenVisible = true, playingDim = "30s"),
        jarnPlayStats = listOf(JarnPlayStatConfig("gc", "Melee.rvz", playCount = 3, playTimeMinutes = 42)),
        libraryFolders = listOf(LibraryFolderConfig("content://tree/roms", "/ROMs", "primary")),
        jarnArtChoices = JarnArtChoicesConfig(topBySystem = mapOf("gc" to "cover")),
        jarnPinnedApps = listOf("com.example.app"),
        jarnSystemSorts = mapOf("gc" to "name"),
        romSearch = RomSearchConfig(frontendEnabled = true),
    ),
    system = SystemConfig(hiddenGames = listOf("gc/Hidden.rvz")),
)
