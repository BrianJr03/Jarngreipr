package jr.brian.home.data.config

import jr.brian.home.esde.data.*
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.media3.common.util.UnstableApi
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.data.AppManagers
import jr.brian.home.data.BgMusicManager
import jr.brian.home.data.DockSize
import jr.brian.home.data.ExperienceStore
import jr.brian.home.data.FabPosition
import jr.brian.home.data.JoystickMode
import jr.brian.home.data.ManagerContainer
import jr.brian.home.data.PageManagers
import jr.brian.home.data.ScreenAssignmentStore
import jr.brian.home.data.SnapMode
import jr.brian.core.model.Experience
import jr.brian.core.model.ScreenAssignment
import jr.brian.home.model.BackButtonShortcut
import jr.brian.core.model.MainScreen
import jr.brian.home.model.PageType
import jr.brian.home.model.PhysicalButton
import jr.brian.home.model.WakeMethod
import jr.brian.home.model.ControlPadItem
import jr.brian.home.model.app.AppPosition
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
class ImportExportManager @Inject constructor(
    private val managers: ManagerContainer,
    private val screenAssignmentStore: ScreenAssignmentStore,
    private val experienceStore: ExperienceStore,
    private val artSourceStore: jr.brian.frontend.library.art.ArtSourceStore,
    private val libraryFolderBackup: jr.brian.frontend.library.setup.LibraryFolderBackup,
    private val jarnLookStore: jr.brian.frontend.look.JarnLookStore,
    private val artChoiceStore: jr.brian.frontend.library.art.GameArtChoiceStore,
    private val appPinStore: jr.brian.frontend.apps.JarnAppPinStore,
    private val playStatsBackup: jr.brian.frontend.library.play.PlayStatsBackup,
    private val backupExtras: SettingsBackupExtras,
    private val jarnPreviewStore: jr.brian.home.data.preview.JarnPreviewStore,
) {
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
            feature = buildFeatureConfig().copy(
                libraryFolders = libraryFolderBackup.export()?.map { it.toConfig() },
                jarnPlayStats = playStatsBackup.export()?.map { it.toConfig() },
            ),
            system = buildSystemConfig()
        ).let { backupExtras.addTo(it) }
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
            promptForDisplayOnLaunch = ui.appDisplayPreferenceManager.getPromptForDisplayPackages(),
            powerSettings = PowerSettingsConfig(
                powerButtonVisible = ps.powerButtonVisible.value,
                quickDeleteVisible = ps.quickDeleteVisible.value,
                headerVisible = ps.headerVisible.value,
                wakeMethod = ps.wakeMethod.value.name,
                backButtonShortcutEnabled = ps.backButtonShortcutEnabled.value,
                backButtonShortcut = ps.backButtonShortcut.value.name,
                backButtonShortcutAppPackage = ps.backButtonShortcutAppPackage.value,
                secondaryBackButtonShortcut = ps.secondaryBackButtonShortcut.value.name,
                secondaryBackButtonShortcutAppPackage = ps.secondaryBackButtonShortcutAppPackage.value,
                poweredOffBrightness = ps.poweredOffBrightness.value,
                appDrawerFilterByPage = ps.appDrawerFilterByPage.value,
                pagerSwipeSensitivity = ps.pagerSwipeSensitivity.value
            ),
            selectedIconPackage = ui.iconPackManager.selectedIconPack.first(),
            wallpaper = WallpaperConfig(
                type = ui.wallpaperManager.currentWallpaper.type.name,
                activeUri = ui.wallpaperManager.currentWallpaper.uri,
                savedImageUri = ui.wallpaperManager.savedImageUri,
                savedGifUri = ui.wallpaperManager.savedGifUri,
                savedVideoUri = ui.wallpaperManager.savedVideoUri
            ),
            frontendWallpaper = WallpaperConfig(
                type = ui.frontendWallpaperManager.currentWallpaper.type.name,
                activeUri = ui.frontendWallpaperManager.currentWallpaper.uri,
                savedImageUri = ui.frontendWallpaperManager.savedImageUri,
                savedGifUri = ui.frontendWallpaperManager.savedGifUri,
                savedVideoUri = ui.frontendWallpaperManager.savedVideoUri
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

        val foldersByKey = buildFoldersByKey(app)

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

    private suspend fun buildFoldersByKey(app: AppManagers): Map<String, List<FolderItemConfig>> =
        buildFolderBackup { page, tabType ->
            app.folderManager.readFolders(page, tabType)?.map { it.toFolderItemConfig() }
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

    @Suppress("DEPRECATION")
    private fun buildFeatureConfig(): FeatureConfig {
        val f = managers.feature
        return FeatureConfig(
            dock = DockConfig(
                apps = f.dockManager.dockApps.value,
                colorArgb = f.dockManager.dockColor.value.toArgb(),
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
                colorArgb = f.appDrawerFabManager.fabColor.value.toArgb(),
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
                volume = f.bgMusicManager.vol,
                isMuted = f.bgMusicManager.muted
            ),
            homeButton = HomeButtonConfig(
                interceptionEnabled = f.homeButtonManager.interceptionEnabled.value,
                mainScreen = f.homeButtonManager.mainScreen.value.name,
                // Legacy boolean kept populated for pre-v32 app versions:
                // Off -> false, anything else -> true.
                focusLockAutoManageEnabled = f.focusLockPrefsManager.mode.value !=
                    jr.brian.home.data.FocusLockMode.Off,
                focusLockMode = f.focusLockPrefsManager.mode.value.name,
            ),
            recentlyPlayed = f.recentlyPlayedManager.entries.value,
            screenAssignment = screenAssignmentStore.assignment.name,
            experience = experienceStore.experience.name,
            artSource = artSourceStore.savedSource?.name,
            systemArtSource = artSourceStore.systemSource.name,
            jarnLook = jarnLookStore.lookId,
            jarnReducedMotion = jarnLookStore.reducedMotion,
            jarnArtChoices = artChoiceStore.choices.toConfig(),
            jarnPinnedApps = appPinStore.pinned,
            jarnDisplay = jarnLookStore.display.toConfig(),
            jarnLiveWallpaper = jarnLookStore.liveWallpaperBackup(),
            jarnFirstRunDone = jarnLookStore.firstRunDone,
            jarnMatchCommandPanel = jarnLookStore.matchCommandPanel,
            jarnMatchCommandPanelFonts = jarnLookStore.matchCommandPanelFonts,
            jarnFontsPromptShown = jarnLookStore.fontsPromptShown,
            jarnEsdeImportNoticeShown = jarnLookStore.esdeImportNoticeShown,
            jarnVideoHintShown = jarnLookStore.videoHintShown,
            jarnWallpaperScreens = jarnLookStore.wallpaperScreensBackup(),
            jarnPreview = jarnPreviewStore.previewBackup(),
            romSearch = f.esdePreferencesManager.state.value.toRomSearchConfig(),
        )
    }

    private fun buildSystemConfig() = SystemConfig(
        badgesVisible = managers.system.notificationManager.badgesVisible,
        shadeTabPage = managers.system.notificationManager.shadeTabPage,
        hiddenSystems = managers.feature.esdePreferencesManager.state.value.hiddenSystems.toList(),
        hiddenGames = managers.feature.esdePreferencesManager.state.value.hiddenGames.sorted(),
        systemFolderMappings = managers.feature.esdePreferencesManager.state.value.systemFolderMappings,
        rcNotificationsEnabled = managers.system.appUpdateManager.rcNotificationsEnabled
    )

    // ── Import ──────────────────────────────────────────────────────────────

    suspend fun applyConfig(backup: JarngreiprConfig) = withContext(Dispatchers.Main) {
        val config = backupExtras.prepareForRestore(backup)
        applyUiConfig(config.ui)
        applyAppConfig(config.app)
        applyPageConfig(config.page)
        applyFeatureConfig(config.feature)
        applySystemConfig(config.system)
        config.feature.libraryFolders?.let { folders ->
            libraryFolderBackup.restore(folders.map { it.toBackedUpFolder() })
        }
        config.feature.jarnPlayStats?.let { stats ->
            playStatsBackup.restore(stats.map { it.toBackedUpPlayStat() })
        }
        backupExtras.restore(config)
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
        ui.appDisplayPreferenceManager.restorePromptForDisplayPackages(config.promptForDisplayOnLaunch)

        val ps = config.powerSettings
        ui.powerSettingsManager.setPowerButtonVisibility(ps.powerButtonVisible)
        ui.powerSettingsManager.setQuickDeleteVisibility(ps.quickDeleteVisible)
        ui.powerSettingsManager.setHeaderVisibility(ps.headerVisible)
        runCatching { ui.powerSettingsManager.setWakeMethod(WakeMethod.valueOf(ps.wakeMethod)) }
        ui.powerSettingsManager.setBackButtonShortcutEnabled(ps.backButtonShortcutEnabled)
        runCatching { ui.powerSettingsManager.setBackButtonShortcut(BackButtonShortcut.valueOf(ps.backButtonShortcut)) }
        ui.powerSettingsManager.setBackButtonShortcutAppPackage(ps.backButtonShortcutAppPackage)
        ui.powerSettingsManager.setSecondaryBackButtonShortcut(
            runCatching { BackButtonShortcut.valueOf(ps.secondaryBackButtonShortcut) }
                .getOrDefault(BackButtonShortcut.NONE)
        )
        ui.powerSettingsManager.setSecondaryBackButtonShortcutAppPackage(ps.secondaryBackButtonShortcutAppPackage)
        ui.powerSettingsManager.setPoweredOffBrightness(ps.poweredOffBrightness)
        ui.powerSettingsManager.setAppDrawerFilterByPage(ps.appDrawerFilterByPage)
        ui.powerSettingsManager.setPagerSwipeSensitivity(ps.pagerSwipeSensitivity)

        ui.iconPackManager.setSelectedIconPack(config.selectedIconPackage)

        val wc = config.wallpaper
        ui.wallpaperManager.updateSavedImageUri(wc.savedImageUri)
        ui.wallpaperManager.updateSavedGifUri(wc.savedGifUri)
        ui.wallpaperManager.updateSavedVideoUri(wc.savedVideoUri)
        runCatching {
            ui.wallpaperManager.setWallpaper(wc.activeUri, WallpaperType.valueOf(wc.type))
        }

        val fwc = config.frontendWallpaper
        ui.frontendWallpaperManager.updateSavedImageUri(fwc.savedImageUri)
        ui.frontendWallpaperManager.updateSavedGifUri(fwc.savedGifUri)
        ui.frontendWallpaperManager.updateSavedVideoUri(fwc.savedVideoUri)
        runCatching {
            ui.frontendWallpaperManager.setWallpaper(fwc.activeUri, WallpaperType.valueOf(fwc.type))
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

        planFolderRestore(config.folders).forEach { step ->
            when (step) {
                is FolderRestoreStep.ClearIfReadable ->
                    app.folderManager.clearFoldersIfReadable(step.page, step.tabType)
                is FolderRestoreStep.Replace -> app.folderManager.setAllFolders(
                    step.page,
                    step.tabType,
                    step.folders.map { it.toFolder() }
                )
            }
        }
    }

    private suspend fun applyPageConfig(config: PageConfig) {
        val page = managers.page

        val targetTypes = config.pageTypes
            .mapNotNull { runCatching { PageType.valueOf(it) }.getOrNull() }
            .ifEmpty { listOf(PageType.UNIFIED_CANVAS) }

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

    @Suppress("DEPRECATION")
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
            volume = bgMusic.volume,
            muted = bgMusic.isMuted
        )

        f.esdePreferencesManager.applyRomSearchConfig(config.romSearch)

        f.recentlyPlayedManager.restore(config.recentlyPlayed)

        f.homeButtonManager.setInterceptionEnabled(config.homeButton.interceptionEnabled)
        f.homeButtonManager.setMainScreen(MainScreen.fromNameOrDefault(config.homeButton.mainScreen))
        val mode = config.homeButton.focusLockMode?.let {
            jr.brian.home.data.FocusLockMode.fromNameOrDefault(it)
        } ?: if (config.homeButton.focusLockAutoManageEnabled) {
            jr.brian.home.data.FocusLockMode.Follow
        } else {
            jr.brian.home.data.FocusLockMode.Off
        }
        f.focusLockPrefsManager.setMode(mode)

        // Null means the imported blob predates v34 — leave the device's
        // current assignment alone so a Backup → Restore from an older
        // app version never silently flips a user's layout.
        config.screenAssignment?.let {
            screenAssignmentStore.setAssignment(ScreenAssignment.fromNameOrDefault(it))
        }

        // Null means the imported blob predates v38, so keep the device's
        // current experience (same rule as screenAssignment above).
        config.experience?.let {
            experienceStore.setExperience(Experience.fromNameOrDefault(it))
        }

        // Null means the blob predates v39, so keep the current art source.
        config.artSource?.let {
            artSourceStore.setSource(jr.brian.core.model.ArtSource.fromNameOrDefault(it))
        }
        config.systemArtSource?.let {
            artSourceStore.setSystemSource(jr.brian.core.model.SystemArtSource.fromNameOrDefault(it))
        }

        // Null means the blob predates v41, so keep the current appearance.
        config.jarnLook?.let(jarnLookStore::setLookId)
        config.jarnReducedMotion?.let(jarnLookStore::setReducedMotion)
        config.jarnArtChoices?.let { artChoiceStore.restore(it.toChoices()) }
        config.jarnPinnedApps?.let(appPinStore::restore)
        config.jarnDisplay?.let { jarnLookStore.updateDisplay(it::applyTo) }
        jarnLookStore.restoreLiveWallpaper(config.jarnLiveWallpaper)
        if (config.jarnFirstRunDone == true) jarnLookStore.markFirstRunDone()
        config.jarnMatchCommandPanel?.let(jarnLookStore::setMatchCommandPanel)
        config.jarnMatchCommandPanelFonts?.let(jarnLookStore::setMatchCommandPanelFonts)
        if (config.jarnFontsPromptShown == true) jarnLookStore.markFontsPromptShown()
        if (config.jarnEsdeImportNoticeShown == true) jarnLookStore.markEsdeImportNoticeShown()
        if (config.jarnVideoHintShown == true) jarnLookStore.markVideoHintShown()
        jarnLookStore.restoreWallpaperScreens(config.jarnWallpaperScreens)
        jarnPreviewStore.restorePreview(config.jarnPreview)
    }

    private fun applySystemConfig(config: SystemConfig) {
        val sys = managers.system
        if (sys.notificationManager.badgesVisible != config.badgesVisible) {
            sys.notificationManager.toggleBadgesVisible()
        }
        sys.notificationManager.saveShadeTabPage(config.shadeTabPage)
        managers.feature.esdePreferencesManager.setHiddenSystems(config.hiddenSystems)
        managers.feature.esdePreferencesManager.restoreHiddenGames(config.hiddenGames)
        managers.feature.esdePreferencesManager.setAllSystemFolderMappings(config.systemFolderMappings)
        sys.appUpdateManager.setRcNotificationsEnabled(config.rcNotificationsEnabled)
    }

    fun encodeToJson(config: JarngreiprConfig): String =
        json.encodeToString(config)

    fun decodeFromJson(jsonString: String): JarngreiprConfig =
        json.decodeFromString(jsonString)

    /** Returns how many folder pages were left out because they couldn't be read. */
    suspend fun exportToStream(outputStream: OutputStream): Int {
        val config = buildConfig()
        val jsonString = encodeToJson(config)
        withContext(Dispatchers.IO) {
            outputStream.writer().use { it.write(jsonString) }
        }
        return omittedFolderPageCount(config.app.folders)
    }

    suspend fun importFromStream(inputStream: InputStream): Result<Unit> = runCatching {
        val jsonString = withContext(Dispatchers.IO) {
            inputStream.bufferedReader().readText()
        }
        val config = decodeFromJson(jsonString)
        applyConfig(config)
    }

    private object PageCountRange {
        const val MAX = FOLDER_BACKUP_PAGE_COUNT
    }
}
