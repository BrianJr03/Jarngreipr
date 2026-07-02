package jr.brian.home.data.config

import jr.brian.home.esde.data.*
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.media3.common.util.UnstableApi
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.data.AppManagers
import jr.brian.home.data.BgMusicManager
import jr.brian.home.data.DockSize
import jr.brian.home.data.FabPosition
import jr.brian.home.data.FolderManager
import jr.brian.home.data.JoystickMode
import jr.brian.home.data.ManagerContainer
import jr.brian.home.data.PageManagers
import jr.brian.home.data.SnapMode
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.model.BackButtonShortcut
import jr.brian.home.model.PageType
import jr.brian.home.model.PhysicalButton
import jr.brian.home.model.WakeMethod
import jr.brian.home.model.ControlPadItem
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.ui.theme.managers.WallpaperType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class ImportExportManager @Inject constructor(private val managers: ManagerContainer) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun buildConfig(): JarngreiprConfig = withContext(Dispatchers.Main) {
        JarngreiprConfig(
            exportedAt = Instant.now().toString(),
            ui = buildUiConfig(),
            app = buildAppConfig(),
            page = buildPageConfig(),
            feature = buildFeatureConfig(),
            system = buildSystemConfig()
        )
    }

    private suspend fun buildUiConfig(): UiConfig {
        val ui = managers.ui
        val ps = ui.powerSettingsManager
        return UiConfig(
            gridSettings = GridSettingsConfig(
                columnCount = ui.gridSettingsManager.columnCount,
                rowCount = ui.gridSettingsManager.rowCount,
                unlimitedMode = ui.gridSettingsManager.unlimitedMode,
                notificationShadeEnabled = ui.gridSettingsManager.notificationShadeEnabled,
                tabTransitionAnimationName = ui.gridSettingsManager.tabTransitionAnimationName,
                iconSnapEnabled = ui.gridSettingsManager.iconSnapEnabled,
                snapMode = ui.gridSettingsManager.snapMode.name,
                bottomFlingAppDrawerEnabled = ui.gridSettingsManager.bottomFlingAppDrawerEnabled,
                shadeBackgroundColorArgb = ui.gridSettingsManager.shadeBackgroundColorArgb,
                shadeCornerRadiusDp = ui.gridSettingsManager.shadeCornerRadiusDp,
                shadeBackgroundAlpha = ui.gridSettingsManager.shadeBackgroundAlpha,
                shadeAccentColorArgb = ui.gridSettingsManager.shadeAccentColorArgb
            ),
            appDisplayPreferences = ui.appDisplayPreferenceManager.getAllPreferences(),
            powerSettings = PowerSettingsConfig(
                powerButtonVisible = ps.powerButtonVisible.value,
                quickDeleteVisible = ps.quickDeleteVisible.value,
                headerVisible = ps.headerVisible.value,
                wakeMethod = ps.wakeMethod.value.name,
                backButtonShortcutEnabled = ps.backButtonShortcutEnabled.value,
                backButtonShortcut = ps.backButtonShortcut.value.name,
                backButtonShortcutAppPackage = ps.backButtonShortcutAppPackage.value,
                poweredOffBrightness = ps.poweredOffBrightness.value,
                appDrawerFilterByPage = ps.appDrawerFilterByPage.value
            ),
            selectedIconPackage = ui.iconPackManager.selectedIconPack.first(),
            wallpaper = WallpaperConfig(
                type = ui.wallpaperManager.currentWallpaper.type.name,
                activeUri = ui.wallpaperManager.currentWallpaper.uri,
                savedImageUri = ui.wallpaperManager.savedImageUri,
                savedGifUri = ui.wallpaperManager.savedGifUri,
                savedVideoUri = ui.wallpaperManager.savedVideoUri
            ),
            searchLayout = SearchLayoutConfig(
                isHorizontalLayout = ui.searchLayoutManager.isHorizontalLayout
            ),
            customAppNames = ui.customAppNameManager.customNames.value
        )
    }

    private suspend fun buildAppConfig(): AppConfig {
        val app = managers.app
        val maxPages = PageCountRange.MAX

        val hiddenByPage = app.appVisibilityManager.hiddenAppsByPage.value
            .filter { it.value.isNotEmpty() }
            .mapKeys { it.key.toString() }
            .mapValues { it.value.toList() }

        val positionsByPage = buildPositionsByPage(app, maxPages)

        val foldersByKey = buildFoldersByKey(app, maxPages)

        return AppConfig(
            visibility = AppVisibilityConfig(
                hiddenAppsByPage = hiddenByPage,
                newAppsVisibleByDefault = app.appVisibilityManager.newAppsVisibleByDefault.value,
                showAppNames = app.appVisibilityManager.showAppNames,
                showHomeScreenAppNames = app.appVisibilityManager.showHomeScreenAppNames,
                appLabelFontSize = app.appVisibilityManager.appLabelFontSize,
                showFolderNames = app.appVisibilityManager.showFolderNames,
                showSettingsBackButton = app.appVisibilityManager.showSettingsBackButton
            ),
            positions = positionsByPage,
            folders = foldersByKey
        )
    }

    private fun buildPositionsByPage(
        app: AppManagers,
        maxPages: Int
    ): Map<String, PagePositionConfig> {
        val result = mutableMapOf<String, PagePositionConfig>()
        for (page in 0 until maxPages) {
            val positions = app.appPositionManager.getPositions(page)
            val freeMode = app.appPositionManager.isFreeModeByPage.value[page] ?: false
            val dragLocked = app.appPositionManager.isDragLockedByPage.value[page] ?: true
            val scrollDisabled = app.appPositionManager.isScrollDisabledByPage.value[page] ?: false
            val bottomFlingDisabled =
                app.appPositionManager.isBottomFlingDisabledByPage.value[page] ?: false
            val hasNonDefaultState =
                positions.isNotEmpty() || freeMode || !dragLocked || scrollDisabled || bottomFlingDisabled
            if (hasNonDefaultState) {
                result[page.toString()] = PagePositionConfig(
                    freeMode = freeMode,
                    dragLocked = dragLocked,
                    scrollDisabled = scrollDisabled,
                    bottomFlingDisabled = bottomFlingDisabled,
                    items = positions.mapValues { (_, pos) ->
                        AppPositionItemConfig(pos.x, pos.y, pos.iconSize)
                    }
                )
            }
        }
        return result
    }

    private suspend fun buildFoldersByKey(
        app: AppManagers,
        maxPages: Int
    ): Map<String, List<FolderItemConfig>> {
        val result = mutableMapOf<String, List<FolderItemConfig>>()
        val tabTypes = listOf(
            FolderManager.TAB_TYPE_APPS,
            FolderManager.TAB_TYPE_WIDGETS,
            CanvasTabType.VALUE
        )
        for (page in 0 until maxPages) {
            for (tabType in tabTypes) {
                val folders = app.folderManager.getFolders(page, tabType).first()
                if (folders.isNotEmpty()) {
                    result["${tabType}_$page"] = folders.map { folder ->
                        FolderItemConfig(
                            id = folder.id,
                            name = folder.name,
                            apps = folder.appPackageNames,
                            x = folder.position.x,
                            y = folder.position.y,
                            iconSize = folder.position.iconSize,
                            backgroundColorArgb = folder.backgroundColorArgb,
                            backgroundImagePath = folder.backgroundImagePath
                        )
                    }
                }
            }
        }
        return result
    }

    private suspend fun buildPageConfig(): PageConfig {
        val page = managers.page
        val pageTypes = page.pageTypeManager.pageTypes.value
        val widgetPageApps = buildWidgetPageApps(page, pageTypes.size)
        val canvasLayouts = page.canvasLayoutManager.layoutsByPage.value
            .filterValues { it.items.isNotEmpty() || it != CanvasLayout() }
            .mapKeys { it.key.toString() }
        return PageConfig(
            pageCount = pageTypes.size,
            pageTypes = pageTypes.map { it.name },
            homeTabIndex = page.homeTabManager.homeTabIndex.value,
            widgetPageApps = widgetPageApps,
            canvasLayouts = canvasLayouts
        )
    }

    private suspend fun buildWidgetPageApps(
        page: PageManagers,
        pageCount: Int
    ): Map<String, WidgetPageAppsConfig> {
        val result = mutableMapOf<String, WidgetPageAppsConfig>()
        for (p in 0 until pageCount) {
            val visibleApps = page.widgetPageAppManager.getVisibleApps(p).first()
            val appsFirst = page.widgetPageAppManager.getAppsFirstOrder(p).first()
            if (visibleApps.isNotEmpty() || appsFirst) {
                result[p.toString()] = WidgetPageAppsConfig(visibleApps.toList(), appsFirst)
            }
        }
        return result
    }

    private fun buildFeatureConfig(): FeatureConfig {
        val f = managers.feature
        return FeatureConfig(
            dock = DockConfig(
                apps = f.dockManager.dockApps.value,
                colorArgb = f.dockManager.dockColor.value.value.toInt(),
                size = f.dockManager.dockSize.value.name,
                visible = f.dockManager.isDockVisible.value,
                visiblePages = f.dockManager.dockVisiblePages.value.toList(),
                maxApps = f.dockManager.maxDockApps.value
            ),
            controlPad = ControlPadConfig(
                items = f.controlPadManager.controlPadItems.value.map { item ->
                    ControlPadItemConfig(
                        label = item.label,
                        mappedButton = item.mappedButton?.name
                    )
                },
                cameraSensitivity = f.controlPadManager.cameraSensitivity.value,
                joystickMode = f.controlPadManager.joystickMode.value.name
            ),
            appDrawerFab = AppDrawerFabConfig(
                colorArgb = f.appDrawerFabManager.fabColor.value.value.toInt(),
                enabled = f.appDrawerFabManager.isFabEnabled.value,
                visiblePages = f.appDrawerFabManager.fabVisiblePages.value.toList(),
                explicitPages = f.appDrawerFabManager.fabExplicitPages.value,
                position = f.appDrawerFabManager.fabPosition.value.name
            ),
            gameKonfetti = f.gameKonfettiManager.config,
            floatyMode = FloatyModeConfig(
                isUnlocked = f.floatyModeManager.isUnlocked,
                isActive = f.floatyModeManager.isFloatyModeActive,
                enabledTabs = f.floatyModeManager.enabledTabs.toList(),
                sectionTapKonfettiEnabled = f.floatyModeManager.isSectionTapKonfettiEnabled,
                poweredOffFloatyEffectEnabled = f.floatyModeManager.isPoweredOffFloatyEffectEnabled,
                appsModalFloatyEffectEnabled = f.floatyModeManager.isAppsModalFloatyEffectEnabled,
                appDrawerFloatyAppCount = f.floatyModeManager.appDrawerFloatyAppCount,
                appDrawerBubblePopEnabled = f.floatyModeManager.isAppDrawerBubblePopEnabled
            ),
            jingles = JinglesConfig(
                isMuted = f.jinglesManager.isMuted.value,
                volume = f.jinglesManager.volume.value,
                regexPriority = f.jinglesManager.regexPriority.value,
                isNormalizationEnabled = f.jinglesManager.isNormalizationEnabled.value
            ),
            bgMusic = BgMusicConfig(
                mode = f.bgMusicManager.mode.name,
                folderUri = f.bgMusicManager.folderUri,
                fileUri = f.bgMusicManager.singleFileUri,
                volume = f.bgMusicManager.vol
            ),
            romSearch = RomSearchConfig(
                hintsKbVisible = f.esdePreferencesManager.state.value.romSearchHintsKbVisible,
                frontendEnabled = f.esdePreferencesManager.state.value.frontendEnabled,
                secondaryMediaEnabled = f.esdePreferencesManager.state.value.secondaryMediaEnabled,
                systemLayout = f.esdePreferencesManager.state.value.systemLayout.name,
                gameLayout = f.esdePreferencesManager.state.value.gameLayout.name,
                systemCustomizations = f.esdePreferencesManager.state.value.systemCustomizations,
                systemOrder = f.esdePreferencesManager.state.value.systemOrder,
                frontendHintsVisible = f.esdePreferencesManager.state.value.frontendHintsVisible,
                frontendFloatIntensity = f.esdePreferencesManager.state.value.frontendFloatIntensity,
                canvasContinuousSpinRoms = f.esdePreferencesManager.state.value.canvasContinuousSpinRoms,
                gameMediaMap = f.esdePreferencesManager.state.value.romSearchGameMediaMap,
                systemMediaMap = f.esdePreferencesManager.state.value.systemMediaMap
            )
        )
    }

    private fun buildSystemConfig() = SystemConfig(
        badgesVisible = managers.system.notificationManager.badgesVisible,
        shadeTabPage = managers.system.notificationManager.shadeTabPage
    )

    // ── Import ──────────────────────────────────────────────────────────────

    suspend fun applyConfig(config: JarngreiprConfig) = withContext(Dispatchers.Main) {
        applyUiConfig(config.ui)
        applyAppConfig(config.app)
        applyPageConfig(config.page)
        applyFeatureConfig(config.feature)
        applySystemConfig(config.system)
    }

    private suspend fun applyUiConfig(config: UiConfig) {
        val ui = managers.ui

        ui.gridSettingsManager.updateColumnCount(config.gridSettings.columnCount)
        ui.gridSettingsManager.updateRowCount(config.gridSettings.rowCount)
        ui.gridSettingsManager.setUnlimitedMode(config.gridSettings.unlimitedMode)
        ui.gridSettingsManager.setNotificationShadeEnabled(config.gridSettings.notificationShadeEnabled)
        ui.gridSettingsManager.setTabTransitionAnimationName(config.gridSettings.tabTransitionAnimationName)
        ui.gridSettingsManager.setIconSnapEnabled(config.gridSettings.iconSnapEnabled)
        runCatching {
            val mode = SnapMode.valueOf(config.gridSettings.snapMode)
            if (mode != SnapMode.OFF) {
                ui.gridSettingsManager.setSnapMode(mode)
            }
        }
        ui.gridSettingsManager.setBottomFlingAppDrawerEnabled(config.gridSettings.bottomFlingAppDrawerEnabled)
        ui.gridSettingsManager.setShadeBackgroundColorArgb(config.gridSettings.shadeBackgroundColorArgb)
        ui.gridSettingsManager.setShadeCornerRadiusDp(config.gridSettings.shadeCornerRadiusDp)
        ui.gridSettingsManager.setShadeBackgroundAlpha(config.gridSettings.shadeBackgroundAlpha)
        ui.gridSettingsManager.setShadeAccentColorArgb(config.gridSettings.shadeAccentColorArgb)

        ui.appDisplayPreferenceManager.restoreAllPreferences(config.appDisplayPreferences)

        val ps = config.powerSettings
        ui.powerSettingsManager.setPowerButtonVisibility(ps.powerButtonVisible)
        ui.powerSettingsManager.setQuickDeleteVisibility(ps.quickDeleteVisible)
        ui.powerSettingsManager.setHeaderVisibility(ps.headerVisible)
        runCatching { ui.powerSettingsManager.setWakeMethod(WakeMethod.valueOf(ps.wakeMethod)) }
        ui.powerSettingsManager.setBackButtonShortcutEnabled(ps.backButtonShortcutEnabled)
        runCatching { ui.powerSettingsManager.setBackButtonShortcut(BackButtonShortcut.valueOf(ps.backButtonShortcut)) }
        ui.powerSettingsManager.setBackButtonShortcutAppPackage(ps.backButtonShortcutAppPackage)
        ui.powerSettingsManager.setPoweredOffBrightness(ps.poweredOffBrightness)
        ui.powerSettingsManager.setAppDrawerFilterByPage(ps.appDrawerFilterByPage)

        ui.iconPackManager.setSelectedIconPack(config.selectedIconPackage)

        val wc = config.wallpaper
        ui.wallpaperManager.updateSavedImageUri(wc.savedImageUri)
        ui.wallpaperManager.updateSavedGifUri(wc.savedGifUri)
        ui.wallpaperManager.updateSavedVideoUri(wc.savedVideoUri)
        runCatching {
            ui.wallpaperManager.setWallpaper(wc.activeUri, WallpaperType.valueOf(wc.type))
        }

        ui.searchLayoutManager.setHorizontalLayout(config.searchLayout.isHorizontalLayout)

        ui.customAppNameManager.customNames.value.keys.toList().forEach { pkg ->
            ui.customAppNameManager.removeCustomName(pkg)
        }
        config.customAppNames.forEach { (pkg, name) ->
            ui.customAppNameManager.setCustomName(pkg, name)
        }
    }

    private suspend fun applyAppConfig(config: AppConfig) {
        val app = managers.app
        val vis = config.visibility

        app.appVisibilityManager.setNewAppsVisibleByDefault(vis.newAppsVisibleByDefault)
        app.appVisibilityManager.updateShowAppNames(vis.showAppNames)
        app.appVisibilityManager.updateShowHomeScreenAppNames(vis.showHomeScreenAppNames)
        app.appVisibilityManager.updateAppLabelFontSize(vis.appLabelFontSize)
        app.appVisibilityManager.updateShowFolderNames(vis.showFolderNames)
        app.appVisibilityManager.updateShowSettingsBackButton(vis.showSettingsBackButton)
        for (page in 0 until PageCountRange.MAX) {
            app.appVisibilityManager.setHiddenApps(page, emptySet())
        }
        vis.hiddenAppsByPage.forEach { (pageStr, apps) ->
            pageStr.toIntOrNull()?.let { page ->
                app.appVisibilityManager.setHiddenApps(page, apps.toSet())
            }
        }

        for (page in 0 until PageCountRange.MAX) {
            app.appPositionManager.clearAllPositions(page)
        }
        config.positions.forEach { (pageStr, pageConfig) ->
            val page = pageStr.toIntOrNull() ?: return@forEach
            app.appPositionManager.setFreeMode(page, pageConfig.freeMode)
            app.appPositionManager.setDragLock(page, pageConfig.dragLocked)
            app.appPositionManager.setScrollDisabled(page, pageConfig.scrollDisabled)
            app.appPositionManager.setBottomFlingDisabled(page, pageConfig.bottomFlingDisabled)
            pageConfig.items.forEach { (pkg, item) ->
                app.appPositionManager.savePosition(
                    pageIndex = page,
                    position = AppPosition(
                        packageName = pkg,
                        x = item.x,
                        y = item.y,
                        iconSize = item.iconSize
                    )
                )
            }
        }

        val tabTypes = listOf(
            FolderManager.TAB_TYPE_APPS,
            FolderManager.TAB_TYPE_WIDGETS,
            CanvasTabType.VALUE
        )
        for (page in 0 until PageCountRange.MAX) {
            for (tabType in tabTypes) {
                app.folderManager.setAllFolders(page, tabType, emptyList())
            }
        }
        config.folders.forEach { (key, folderConfigs) ->
            val lastUnderscore = key.lastIndexOf('_')
            if (lastUnderscore == -1) return@forEach
            val tabType = key.substring(0, lastUnderscore)
            val page = key.substring(lastUnderscore + 1).toIntOrNull() ?: return@forEach
            val folders = folderConfigs.map { fc ->
                Folder(
                    id = fc.id,
                    name = fc.name,
                    appPackageNames = fc.apps,
                    position = AppPosition(fc.id, fc.x, fc.y, fc.iconSize),
                    backgroundColorArgb = fc.backgroundColorArgb,
                    backgroundImagePath = fc.backgroundImagePath
                )
            }
            app.folderManager.setAllFolders(page, tabType, folders)
        }
    }

    private suspend fun applyPageConfig(config: PageConfig) {
        val page = managers.page

        val targetTypes = config.pageTypes
            .mapNotNull { runCatching { PageType.valueOf(it) }.getOrNull() }
            .ifEmpty { listOf(PageType.APPS_TAB) }

        page.pageTypeManager.reorderPages(targetTypes)
        page.pageCountManager.setPageCount(targetTypes.size)
        page.homeTabManager.setHomeTabIndex(config.homeTabIndex)

        for (p in 0 until PageCountRange.MAX) {
            page.widgetPageAppManager.clearPageData(p)
        }
        config.widgetPageApps.forEach { (pageStr, wpc) ->
            val p = pageStr.toIntOrNull() ?: return@forEach
            wpc.visibleApps.forEach { pkg ->
                page.widgetPageAppManager.addVisibleApp(
                    pageIndex = p,
                    packageName = pkg
                )
            }
            if (wpc.appsFirst) page.widgetPageAppManager.toggleSectionOrder(p)
        }

        page.canvasLayoutManager.clearAll()
        config.canvasLayouts.forEach { (pageStr, layout) ->
            pageStr.toIntOrNull()?.let { idx ->
                page.canvasLayoutManager.replaceLayout(idx, layout)
            }
        }
    }

    private fun applyFeatureConfig(config: FeatureConfig) {
        val f = managers.feature

        val dock = config.dock
        f.dockManager.setDockApps(dock.apps)
        f.dockManager.setDockColor(Color(dock.colorArgb))
        runCatching { f.dockManager.setDockSize(DockSize.valueOf(dock.size)) }
        f.dockManager.setDockVisibility(dock.visible)
        f.dockManager.setDockVisiblePages(dock.visiblePages.toSet())
        f.dockManager.setMaxDockApps(dock.maxApps)

        config.controlPad.items.forEachIndexed { index, itemConfig ->
            val button = itemConfig.mappedButton?.let {
                runCatching { PhysicalButton.valueOf(it) }.getOrNull()
            }
            f.controlPadManager.setControlPadItem(index, ControlPadItem(itemConfig.label, button))
        }
        f.controlPadManager.setCameraSensitivity(config.controlPad.cameraSensitivity)
        runCatching {
            f.controlPadManager.setJoystickMode(
                JoystickMode.valueOf(config.controlPad.joystickMode)
            )
        }

        val fab = config.appDrawerFab
        f.appDrawerFabManager.setFabColor(Color(fab.colorArgb))
        f.appDrawerFabManager.setFabEnabled(fab.enabled)
        f.appDrawerFabManager.setFabVisiblePages(fab.visiblePages.toSet())
        f.appDrawerFabManager.setFabExplicitPages(fab.explicitPages)
        runCatching { f.appDrawerFabManager.setFabPosition(FabPosition.valueOf(fab.position)) }

        f.gameKonfettiManager.updateConfig(config.gameKonfetti)

        val floaty = config.floatyMode
        if (floaty.isUnlocked) f.floatyModeManager.unlock()
        f.floatyModeManager.setFloatyMode(floaty.isActive)
        f.floatyModeManager.restoreEnabledTabs(floaty.enabledTabs.toSet())
        f.floatyModeManager.updateSectionTapKonfettiEffectEnabled(floaty.sectionTapKonfettiEnabled)
        f.floatyModeManager.updatePoweredOffFloatyEffectEnabled(floaty.poweredOffFloatyEffectEnabled)
        f.floatyModeManager.updateAppsModalFloatyEffectEnabled(floaty.appsModalFloatyEffectEnabled)
        f.floatyModeManager.updateAppDrawerFloatyAppCount(floaty.appDrawerFloatyAppCount)
        f.floatyModeManager.updateAppDrawerBubblePopEnabled(floaty.appDrawerBubblePopEnabled)

        val jingles = config.jingles
        f.jinglesManager.setMuted(jingles.isMuted)
        f.jinglesManager.setVolume(jingles.volume)
        f.jinglesManager.setRegexPriority(jingles.regexPriority)
        f.jinglesManager.setNormalizationEnabled(jingles.isNormalizationEnabled)

        val bgMusic = config.bgMusic
        f.bgMusicManager.restoreSettings(
            folderUri = bgMusic.folderUri,
            fileUri = bgMusic.fileUri,
            mode = runCatching { BgMusicManager.Mode.valueOf(bgMusic.mode) }.getOrElse { BgMusicManager.Mode.SINGLE_FILE },
            volume = bgMusic.volume
        )

        f.esdePreferencesManager.setRomSearchHintsKbVisible(config.romSearch.hintsKbVisible)
        f.esdePreferencesManager.setFrontendEnabled(config.romSearch.frontendEnabled)
        f.esdePreferencesManager.setSecondaryMediaEnabled(config.romSearch.secondaryMediaEnabled)
        runCatching { FrontendLayout.valueOf(config.romSearch.systemLayout) }.getOrNull()
            ?.let { f.esdePreferencesManager.setSystemLayout(it) }
        runCatching { FrontendLayout.valueOf(config.romSearch.gameLayout) }.getOrNull()
            ?.let { f.esdePreferencesManager.setGameLayout(it) }
        f.esdePreferencesManager.setAllSystemCustomizations(config.romSearch.systemCustomizations)
        f.esdePreferencesManager.setSystemOrder(config.romSearch.systemOrder)
        f.esdePreferencesManager.setFrontendHintsVisible(config.romSearch.frontendHintsVisible)
        f.esdePreferencesManager.setFrontendFloatIntensity(config.romSearch.frontendFloatIntensity)
        f.esdePreferencesManager.setAllCanvasContinuousSpin(config.romSearch.canvasContinuousSpinRoms)
        f.esdePreferencesManager.setAllGameMediaMap(config.romSearch.gameMediaMap)
        f.esdePreferencesManager.setAllSystemMediaMap(config.romSearch.systemMediaMap)
    }

    private fun applySystemConfig(config: SystemConfig) {
        val sys = managers.system
        if (sys.notificationManager.badgesVisible != config.badgesVisible) {
            sys.notificationManager.toggleBadgesVisible()
        }
        sys.notificationManager.saveShadeTabPage(config.shadeTabPage)
    }

    fun encodeToJson(config: JarngreiprConfig): String =
        json.encodeToString(config)

    fun decodeFromJson(jsonString: String): JarngreiprConfig =
        json.decodeFromString(jsonString)

    suspend fun exportToStream(outputStream: OutputStream) {
        val config = buildConfig()
        val jsonString = encodeToJson(config)
        withContext(Dispatchers.IO) {
            outputStream.writer().use { it.write(jsonString) }
        }
    }

    suspend fun importFromStream(inputStream: InputStream): Result<Unit> = runCatching {
        val jsonString = withContext(Dispatchers.IO) {
            inputStream.bufferedReader().readText()
        }
        val config = decodeFromJson(jsonString)
        applyConfig(config)
    }

    private object PageCountRange {
        const val MAX = 6
    }
}
