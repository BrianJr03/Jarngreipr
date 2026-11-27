package jr.brian.home.data.config

import androidx.annotation.VisibleForTesting
import jr.brian.home.esde.data.*
import jr.brian.home.esde.model.DEFAULT_SCREENSCRAPER_MEDIA_TYPES
import jr.brian.home.esde.model.DEFAULT_STEAMGRIDDB_MEDIA_TYPES
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.model.FrontendRowAlignment
import jr.brian.home.esde.model.FrontendTransition

/** The ES-DE frontend slice of a backup, kept out of [ImportExportManager] so it doesn't grow. */
internal fun ESDEPrefsState.toRomSearchConfig(): RomSearchConfig = RomSearchConfig(
    frontendEnabled = frontendEnabled,
    companionLaunchWatchdogEnabled = companionLaunchWatchdogEnabled,
    secondaryMediaEnabled = secondaryMediaEnabled,
    jarngreiprMediaEnabled = jarngreiprMediaEnabled,
    // Legacy field — kept populated with the primary-display value
    // so an older app version restoring this config still gets a
    // sensible single-layout setting.
    systemLayout = primarySystemLayout.name,
    primarySystemLayout = primarySystemLayout.name,
    secondarySystemLayout = secondarySystemLayout.name,
    // Legacy field — kept populated with the primary-display game
    // layout so an older app version restoring this config still
    // gets a sensible single-layout setting.
    gameLayout = primaryGameLayout.name,
    primaryGameLayout = primaryGameLayout.name,
    secondaryGameLayout = secondaryGameLayout.name,
    systemCustomizations = systemCustomizations,
    systemOrder = systemOrder,
    frontendHintsVisible = frontendHintsVisible,
    frontendFloatIntensity = frontendFloatIntensity,
    canvasContinuousSpinRoms = canvasContinuousSpinRoms,
    gameMediaMap = romSearchGameMediaMap,
    systemMediaMap = systemMediaMap,
    // Legacy field intentionally retained in the export payload so a
    // pre-v36 build still restores the first companion per ROM. Seeded
    // from the new list by taking the first entry.
    gameCompanionMap = gameCompanionsMap
        .mapNotNull { (k, v) -> v.firstOrNull()?.let { k to it } }
        .toMap(),
    gameCompanionsMap = gameCompanionsMap,
    frontendFocusHapticEnabled = frontendFocusHapticEnabled,
    frontendFocusBackgroundEnabled = frontendFocusBackgroundEnabled,
    frontendFocusBackgroundSystems = frontendFocusBackgroundSystems,
    frontendFocusBackgroundGames = frontendFocusBackgroundGames,
    // Legacy field is intentionally kept in the export payload so a config
    // exported by this build can still be read by pre-split builds; seed it
    // from the systems value as a reasonable single-value approximation.
    frontendFocusBackgroundDim = frontendFocusBackgroundDimSystems,
    frontendFocusBackgroundDimSystems = frontendFocusBackgroundDimSystems,
    frontendFocusBackgroundDimGames = frontendFocusBackgroundDimGames,
    frontendTransition = frontendTransition.name,
    frontendTransitionMs = frontendTransitionMs,
    frontendSystemRowAlignment = frontendSystemRowAlignment.name,
    frontendGameRowAlignment = frontendGameRowAlignment.name,
    frontendSystemTileScale = frontendSystemTileScale,
    frontendGameTileScale = frontendGameTileScale,
    gamelistDecorationEnabled = gamelistDecorationEnabled,
    gameEmulatorMap = gameEmulatorMap,
    systemEmulatorMap = systemEmulatorMap,
    gameCommandMap = gameCommandMap,
    gameCoreMap = gameCoreMap,
    scraper = buildScraperConfigForExport(this)
)

@Suppress("DEPRECATION")
internal fun ESDEPreferencesManager.applyRomSearchConfig(config: RomSearchConfig) {
    // config.hintsKbVisible intentionally ignored — the feature it
    // gated was removed. The field stays on RomSearchConfig so older exports
    // still decode without error.
    setFrontendEnabled(config.frontendEnabled)
    setCompanionLaunchWatchdogEnabled(config.companionLaunchWatchdogEnabled)
    setSecondaryMediaEnabled(config.secondaryMediaEnabled)
    setJarngreiprMediaEnabled(config.jarngreiprMediaEnabled)
    // New per-display fields fall back to the legacy single value when
    // restoring a config written by an older app version.
    val legacySystemLayout = runCatching {
        FrontendLayout.valueOf(config.systemLayout)
    }.getOrNull()
    (runCatching { FrontendLayout.valueOf(config.primarySystemLayout) }.getOrNull()
        ?: legacySystemLayout)
        ?.let { setPrimarySystemLayout(it) }
    (runCatching { FrontendLayout.valueOf(config.secondarySystemLayout) }.getOrNull()
        ?: legacySystemLayout)
        ?.let { setSecondarySystemLayout(it) }
    val legacyGameLayout = runCatching {
        FrontendLayout.valueOf(config.gameLayout)
    }.getOrNull()
    (runCatching { FrontendLayout.valueOf(config.primaryGameLayout) }.getOrNull()
        ?: legacyGameLayout)
        ?.let { setPrimaryGameLayout(it) }
    (runCatching { FrontendLayout.valueOf(config.secondaryGameLayout) }.getOrNull()
        ?: legacyGameLayout)
        ?.let { setSecondaryGameLayout(it) }
    setAllSystemCustomizations(config.systemCustomizations)
    setSystemOrder(config.systemOrder)
    setFrontendHintsVisible(config.frontendHintsVisible)
    setFrontendFloatIntensity(config.frontendFloatIntensity)
    setAllCanvasContinuousSpin(config.canvasContinuousSpinRoms)
    setAllGameMediaMap(config.gameMediaMap)
    setAllSystemMediaMap(config.systemMediaMap)
    // Prefer the v36 list field; fall back to converting the legacy single-
    // value map so pre-v36 configs still restore companions.
    @Suppress("DEPRECATION")
    val companions = config.gameCompanionsMap.takeIf { it.isNotEmpty() }
        ?: config.gameCompanionMap.mapValues { listOf(it.value) }
    setAllGameCompanionsMap(companions)
    setFrontendFocusHapticEnabled(config.frontendFocusHapticEnabled)
    setFrontendFocusBackgroundEnabled(config.frontendFocusBackgroundEnabled)
    setFrontendFocusBackgroundSystems(config.frontendFocusBackgroundSystems)
    setFrontendFocusBackgroundGames(config.frontendFocusBackgroundGames)
    // Pre-split configs carry only the deprecated combined field; new configs
    // carry the per-scope fields. When the per-scope field is at its default and
    // the legacy field differs, prefer the legacy — that's the case where a v17
    // (or older) config was exported before the split existed.
    @Suppress("DEPRECATION")
    val legacyDim = config.frontendFocusBackgroundDim
    val systemsDim = config.frontendFocusBackgroundDimSystems.let { new ->
        if (new == 0.55f && legacyDim != 0.55f) legacyDim else new
    }
    val gamesDim = config.frontendFocusBackgroundDimGames.let { new ->
        if (new == 0.55f && legacyDim != 0.55f) legacyDim else new
    }
    setFrontendFocusBackgroundDimSystems(systemsDim)
    setFrontendFocusBackgroundDimGames(gamesDim)
    setFrontendTransition(
        FrontendTransition.fromStoredName(config.frontendTransition)
    )
    setFrontendTransitionMs(config.frontendTransitionMs)
    setFrontendSystemRowAlignment(
        resolveRowAlignment(config.frontendSystemRowAlignment)
    )
    setFrontendGameRowAlignment(
        resolveRowAlignment(config.frontendGameRowAlignment)
    )
    setFrontendSystemTileScale(config.frontendSystemTileScale)
    setFrontendGameTileScale(config.frontendGameTileScale)
    setGamelistDecorationEnabled(config.gamelistDecorationEnabled)
    config.run {
        restoreLaunchChoices(gameEmulatorMap, systemEmulatorMap, gameCommandMap, gameCoreMap)
    }

    applyScraperConfigImport(config.scraper, this)
}

private fun resolveRowAlignment(stored: String): FrontendRowAlignment =
    FrontendRowAlignment.entries.firstOrNull { it.name == stored }
        ?: FrontendRowAlignment.Center

/**
 * Builds the [ScraperConfig] slice of an export. Secrets (dev ID / password,
 * user ID / password, SteamGridDB key) are intentionally emitted as empty
 * strings so a user who shares their config.json never leaks credentials to
 * the recipient. The empty strings keep the on-disk schema stable so a
 * pre-v26 build can still decode the config.
 */
@VisibleForTesting
internal fun buildScraperConfigForExport(prefs: ESDEPrefsState): ScraperConfig = ScraperConfig(
    screenScraperEnabled = prefs.screenScraperEnabled,
    screenScraperDevId = "",
    screenScraperDevPassword = "",
    screenScraperUserId = "",
    screenScraperUserPassword = "",
    screenScraperMediaTypes = prefs.screenScraperMediaTypes,
    steamGridDbEnabled = prefs.steamGridDbEnabled,
    steamGridDbApiKey = "",
    steamGridDbMediaTypes = prefs.steamGridDbMediaTypes,
    concurrencyOverride = prefs.scraperConcurrencyOverride,
)

/**
 * Applies a [ScraperConfig] from an import to [prefs]. Credential fields are
 * only written when non-empty: a config produced by [buildScraperConfigForExport]
 * carries empty strings, meaning "the exporter chose not to share this";
 * clobbering the user's stored secrets in that case would log them out of
 * ScreenScraper on every import. Older configs that still carry credentials
 * import normally.
 */
@VisibleForTesting
internal fun applyScraperConfigImport(
    scraper: ScraperConfig,
    prefs: ESDEPreferencesManager,
) {
    prefs.setScreenScraperEnabled(scraper.screenScraperEnabled)
    if (scraper.screenScraperDevId.isNotEmpty()) {
        prefs.setScreenScraperDevId(scraper.screenScraperDevId)
    }
    if (scraper.screenScraperDevPassword.isNotEmpty()) {
        prefs.setScreenScraperDevPassword(scraper.screenScraperDevPassword)
    }
    if (scraper.screenScraperUserId.isNotEmpty()) {
        prefs.setScreenScraperUserId(scraper.screenScraperUserId)
    }
    if (scraper.screenScraperUserPassword.isNotEmpty()) {
        prefs.setScreenScraperUserPassword(scraper.screenScraperUserPassword)
    }
    prefs.setScreenScraperMediaTypes(
        scraper.screenScraperMediaTypes.ifEmpty { DEFAULT_SCREENSCRAPER_MEDIA_TYPES }
    )
    prefs.setSteamGridDbEnabled(scraper.steamGridDbEnabled)
    if (scraper.steamGridDbApiKey.isNotEmpty()) {
        prefs.setSteamGridDbApiKey(scraper.steamGridDbApiKey)
    }
    prefs.setSteamGridDbMediaTypes(
        scraper.steamGridDbMediaTypes.ifEmpty { DEFAULT_STEAMGRIDDB_MEDIA_TYPES }
    )
    prefs.setScraperConcurrencyOverride(scraper.concurrencyOverride)
}
