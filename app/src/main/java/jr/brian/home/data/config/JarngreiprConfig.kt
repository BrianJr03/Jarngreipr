package jr.brian.home.data.config

import jr.brian.frontend.library.art.ArtChoice
import jr.brian.frontend.library.art.GameArtChoices
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.data.RecentlyPlayedEntry
import jr.brian.home.esde.model.SystemCustomization
import jr.brian.home.esde.model.SystemFolderMapping
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.ui.components.konfetti.GameKonfettiConfig
import jr.brian.frontend.library.play.BackedUpPlayStat
import jr.brian.frontend.library.setup.BackedUpFolder
import kotlinx.serialization.Serializable

@Serializable
data class JarngreiprConfig(
    val version: Int = CONFIG_VERSION,
    val exportedAt: String = "",
    val ui: UiConfig = UiConfig(),
    val app: AppConfig = AppConfig(),
    val page: PageConfig = PageConfig(),
    val feature: FeatureConfig = FeatureConfig(),
    val system: SystemConfig = SystemConfig()
) {
    companion object {
        // v2: added page.canvasLayouts for Unified Canvas pages.
        // v3: CanvasLayout split into shared content + per-orientation
        //     arrangements; v2 blobs decode with empty arrangements and are
        //     auto-repaired by CanvasLayoutManager.repair on apply.
        // v5: added CanvasItem.RssMusicItem variant. Older blobs decode
        //     unchanged (no rss_music entries); new blobs with rss_music are
        //     unreadable by pre-v5 builds, so the version bump signals that.
        // v6: added RomSearchConfig.systemCustomizations + systemOrder for per-system
        //     frontend tile customization (background, name overlay toggle, color, order).
        // v7: added RomSearchConfig.frontendHintsVisible for the bottom hint-row toggle.
        // v8: added RomSearchConfig.frontendFloatIntensity for the floating-animation slider.
        // v9: added RomSearchConfig.canvasContinuousSpinRoms for per-rom continuous-spin toggle.
        // v10: added RomSearchConfig.gameMediaMap + systemMediaMap for per-game and per-system
        //      card media type overrides.
        // v11: added RomSearchConfig.frontendFocusHapticEnabled for the frontend focus-tick
        //      haptic toggle (applies to both system and game cards).
        // v12: added SystemConfig.hiddenSystems for the Frontend "Filter Systems" sheet.
        // v13: added CanvasLayout.menuButtonVisible for per-page toggle of the inline
        //      menu tile. Older blobs decode with the default (true) via
        //      ignoreUnknownKeys, matching the pre-toggle behavior.
        // v14: added UiConfig.promptForDisplayOnLaunch — set of package names that
        //      surface a display chooser on tap. Older blobs decode with the empty
        //      default (no prompting), matching pre-toggle behavior.
        // v15: added RomSearchConfig.frontendFocusBackgroundSystems and
        //      frontendFocusBackgroundGames per-route toggles for the focus-background
        //      feature; SystemCustomization.focusBackgroundUri for a per-system full-screen
        //      backdrop distinct from the tile-level backgroundUri. Older blobs decode
        //      with defaults (both routes on, no focus URI) via ignoreUnknownKeys.
        // v16: added RomSearchConfig.frontendTransition + frontendTransitionMs for the
        //      Systems <-> Games route transition preset and its duration. Older blobs
        //      decode with the default preset ("Fade") and default 280ms via
        //      ignoreUnknownKeys.
        // v17: added CanvasItem.EsdeArtItem.backgroundColorArgb + backgroundCornerRadiusDp
        //      for the optional background-box customization on ES-DE Display tiles.
        //      Older blobs decode with both fields null (no background) via
        //      ignoreUnknownKeys, preserving the pre-toggle transparent rendering.
        // v18: split RomSearchConfig.frontendFocusBackgroundDim into
        //      frontendFocusBackgroundDimSystems / frontendFocusBackgroundDimGames so
        //      the two routes can be dimmed independently. The old field is kept
        //      (deprecated) and still written on export so v17-and-earlier builds can
        //      still read a config exported now; import falls back to it for pre-split
        //      configs.
        // v19: added RomSearchConfig.frontendSystemRowAlignment /
        //      frontendGameRowAlignment (row-mode vertical anchor: Top/Center/Bottom)
        //      and frontendSystemTileScale / frontendGameTileScale (tile-size
        //      multiplier 1.0..1.3 controlling row width and grid columns). Older
        //      blobs decode with defaults (Center, 1.0f) via ignoreUnknownKeys.
        // v20: added RomSearchConfig.gamelistDecorationEnabled — toggle for
        //      whether ES-DE gamelist.xml decorates the filesystem scan. Older
        //      blobs decode with the default (true) via ignoreUnknownKeys,
        //      preserving pre-toggle behaviour.
        // v21: added FeatureConfig.homeButton for the "Home Button Interception"
        //      toggle. Older blobs decode with the default (off) via
        //      ignoreUnknownKeys — matching the pre-toggle behaviour of a device
        //      that has never had interception enabled.
        // v22: added SystemConfig.systemFolderMappings for the Frontend "Add
        //      Systems" screen — user-declared "this folder is system X"
        //      entries used to map arbitrary folders (including SD-card ones)
        //      to systems. Older blobs decode with the empty default via
        //      ignoreUnknownKeys, matching pre-feature behaviour.
        // v23: added HomeButtonConfig.homeTarget / mainScreen for the display
        //      target selector under Home Button Interception. Older blobs
        //      decode with both fields null via ignoreUnknownKeys — a null
        //      target means "use the runtime migration default"
        //      (BOTH-when-frontend-enabled, else BOTTOM), matching the
        //      pre-selector routing behaviour.
        // v24: added PowerSettingsConfig.pagerSwipeSensitivity for the tab-
        //      swipe sensitivity slider (0..100). Older blobs decode with
        //      the default (0 = Compose default snap threshold) via
        //      ignoreUnknownKeys, matching pre-slider behaviour.
        // v25: added RomSearchConfig.scraper (ScraperConfig) — ScreenScraper
        //      + SteamGridDB credentials, per-backend enable toggles, media-
        //      type selections, and an optional concurrency override. Older
        //      blobs decode with ScraperConfig() defaults (both backends off,
        //      empty credentials) via ignoreUnknownKeys.
        // v26: added RomSearchConfig.userMetadata (UserMetadataConfig) — a
        //      master toggle for the Edit Info sheet's manual overrides.
        //      The override blobs themselves live on disk at
        //      `<scrapedRoot>/<system>/.jarngreipr_user/` and are not inlined
        //      in config. Older blobs decode with UserMetadataConfig() default
        //      (enabled = true) via ignoreUnknownKeys.
        // v27: HomeButtonConfig.homeTarget is now ignored on import and no
        //      longer written on export — Home press routing is driven by
        //      gestures (single/double/long press) instead of a stored target.
        //      The field is retained in the schema with its null default so
        //      older blobs still decode via ignoreUnknownKeys; the value is
        //      simply never consulted.
        // v28: added HomeButtonConfig.focusLockAutoManageEnabled — the
        //      "Auto-manage Focus Lock" toggle gating whether we write to
        //      AYN's screen_focus_lock on swaps, app launches, game
        //      launches, and boot. Older blobs decode with the default
        //      (true) via ignoreUnknownKeys, matching the on-by-default
        //      behaviour new installs ship with.
        // v29: split RomSearchConfig.gameLayout into
        //      primaryGameLayout / secondaryGameLayout so each display
        //      can show games in Row or Grid independently (matches the
        //      systems split from v26-ish). Legacy gameLayout retained
        //      in the schema and still written on export so pre-split
        //      builds restoring a current config get a sensible single-
        //      layout fallback. Older blobs decode with both new fields
        //      inheriting the legacy value on import.
        // v30: dropped HomeButtonConfig.focusLockAutoManageEnabled
        //      (added in v28). AYN's SettingsProvider gates
        //      `screen_focus_lock` behind a MOVED_TO_SECURE-style
        //      whitelist that third-party apps can't bypass, so the
        //      feature is removed. Older blobs that still carry the
        //      field decode via `ignoreUnknownKeys`; the value is
        //      ignored on import.
        // v31: re-added HomeButtonConfig.focusLockAutoManageEnabled.
        //      Writes now route through Shizuku's shell service, which
        //      runs at the privileged UID AYN's SettingsProvider does
        //      allow. Users opt in by installing Shizuku and granting
        //      the app permission from the Extras section; without
        //      Shizuku the toggle is inert and the write silently
        //      no-ops. Default is true to match the pre-v30 behaviour.
        // v32: replaced the boolean focusLockAutoManageEnabled with
        //      focusLockMode (string: Off/Top/Bottom/Follow). The
        //      legacy boolean is retained in the schema (true -> Follow,
        //      false -> Off on import) so pre-v32 blobs upgrade
        //      without data loss and so pre-v32 app versions still get
        //      a sensible value if they restore a current config.
        //      Default is Follow.
        // v33: added CanvasItem.LastPlayedItem variant and
        //      FeatureConfig.recentlyPlayed, the launch-history list the
        //      Last Played widget binds to. Older blobs decode with the
        //      empty default via ignoreUnknownKeys.
        // v34: added FeatureConfig.screenAssignment — persisted dual-
        //      screen layout name (FRONTEND_ON_TOP / FRONTEND_ON_BOTTOM).
        //      Nullable so pre-v34 blobs decode without a value; import
        //      leaves the device's current assignment alone in that
        //      case rather than reverting to the default, so restoring
        //      an old backup never silently flips a user's layout.
        // v35: added SystemConfig.rcNotificationsEnabled — the "Get
        //      notified about RC builds" toggle gating whether update
        //      checks include GitHub pre-releases. Older blobs decode
        //      with the default (false) via ignoreUnknownKeys, matching
        //      stable-channel behaviour.
        // v36: a ROM can now have up to two companion apps. The schema
        //      adds gameCompanionsMap (Map<String, List<String>>). The
        //      legacy single-value gameCompanionMap is kept (deprecated)
        //      and still written on export so pre-v36 builds restoring
        //      a current config get the first companion. Import prefers
        //      the new field; falls back to converting the legacy single
        //      value to a one-element list.
        // v37: the back button shortcut can hold a second shortcut; when
        //      both are set the back gesture opens a picker. Adds
        //      PowerSettingsConfig.secondaryBackButtonShortcut and
        //      secondaryBackButtonShortcutAppPackage. Older blobs decode
        //      with NONE / null via ignoreUnknownKeys, keeping the
        //      single-shortcut behaviour.
        // v38: added FeatureConfig.experience, the Classic / JarnFE selector.
        //      Nullable so pre-v38 blobs decode without a value; import
        //      leaves the device's current experience alone in that case.
        //      Separate from romSearch.frontendEnabled.
        // v39: added FeatureConfig.artSource, Jarn FE's preferred art source.
        //      Nullable so pre-v39 blobs decode without a value; import
        //      leaves the device's current source alone in that case.
        //      Also added FeatureConfig.systemArtSource, with the same rule.
        // v40: added FeatureConfig.libraryFolders, the Jarn FE game folders.
        //      Null on older blobs, and import then leaves folders alone.
        //      Restored folders never carry a grant and never replace a row.
        // v41: added FeatureConfig.jarnLook and jarnReducedMotion, the Jarn FE
        //      appearance. Null on older blobs, and import then keeps both.
        // v42: added FeatureConfig.jarnArtChoices, the Jarn FE art per screen.
        //      Null on older blobs, and import then keeps the current choices.
        // v43: added FeatureConfig.jarnPinnedApps, the Jarn FE pinned apps in order.
        //      Null on older blobs, and import then keeps the current pins.
        // v44: added FeatureConfig.jarnDisplay, the Jarn FE tile sizes, hints and
        //      button haptics. Null on older blobs or fields, and import keeps those.
        // v45: added FeatureConfig.jarnLiveWallpaper, the Jarn FE live wallpaper id.
        //      Null on older blobs, and import keeps the current choice. "off" clears it.
        // v46: added JarnDisplayConfig.tapControl, the screen that keeps the controller
        //      after a tap. Null or unknown values leave the current choice alone.
        // v47: added JarnDisplayConfig.appNamesVisible and undoCountdownVisible, the Jarn FE
        //      show app names and undo timer choices. Null on older blobs, and import keeps them.
        // v48: added FeatureConfig.jarnPlayStats, play counts and time per game. Null on
        //      older blobs, and import keeps current totals. Import only ever raises them.
        // v49: added FeatureConfig.jarnFirstRunDone, whether the Jarn FE tour was finished.
        //      Null on older blobs, and import keeps the current value. Import only sets it.
        // v50: added FeatureConfig.jarnMatchCommandPanel, whether Jarn FE follows Command
        //      Panel's colors. Null on older blobs, and import keeps the current value.
        //      The palette itself is never backed up.
        // v51: added JarnDisplayConfig.switchCue, how Jarn FE shows a screen switch (pulse, dim
        //      or tab_mark). Null on older blobs, and import keeps the current value. An
        //      unknown id falls back to pulse.
        // v52: added UiConfig.appearance (theme, custom themes, OLED, icon shape, tab animation),
        //      FeatureConfig.rssFeeds, JinglesConfig.enabled and repos, and
        //      PageConfig.widgetProviders. All null on older blobs, and import keeps current values.
        //      Feeds, repos and custom themes are only ever added. Restore also clears photo tiles
        //      whose file is missing and widget tiles whose widget isn't the one backed up.
        // v53: added JarnDisplayConfig.systemView and gameView, the Jarn FE Grid or Carousel
        //      choices (grid or carousel). Null on older blobs, and import keeps the current
        //      value. An unknown id falls back to grid.
        // v54: added FeatureConfig.jarnWallpaperScreens, Jarn FE "Same on both screens" and the
        //      bottom screen's own image or GIF. Null on older blobs, and import keeps both.
        // v55: added JarnDisplayConfig.pageTransition, how a Jarn FE Canvas page arrives. Null on
        //      older blobs, and import keeps the current value. An unknown id falls back to jarn_slide.
        // v56: added JarnDisplayConfig.systemNamesVisible, Jarn FE "Show system names" for the
        //      line under system cards. Null on older blobs, and import keeps the current value.
        // v57: added FeatureConfig.jarnPreview (Meet Jarn showings, last version, tried). Null on older
        //      blobs; import only raises the count and only turns tried on.
        // v58: added JarnDisplayConfig.randomVisible and lastPlayedVisible, Jarn FE "Show Random" and
        //      "Show Last Played" on Now. Null on older blobs, and import keeps the current value.
        // v59: added JarnDisplayConfig.systemArtVisible, gameCountVisible and recentShelfVisible,
        //      the optional parts of Jarn FE Now's system view. Null on older blobs keeps them.
        const val CONFIG_VERSION = 59
    }
}

@Serializable
data class UiConfig(
    val gridSettings: GridSettingsConfig = GridSettingsConfig(),
    val appDisplayPreferences: Map<String, String> = emptyMap(),
    val promptForDisplayOnLaunch: Set<String> = emptySet(),
    val powerSettings: PowerSettingsConfig = PowerSettingsConfig(),
    val selectedIconPackage: String? = null,
    val wallpaper: WallpaperConfig = WallpaperConfig(),
    val frontendWallpaper: WallpaperConfig = WallpaperConfig(),
    val searchLayout: SearchLayoutConfig = SearchLayoutConfig(),
    val customAppNames: Map<String, String> = emptyMap(),
    /** v52: theme and icon look. Null on pre-v52 exports, and import keeps the current look. */
    val appearance: AppearanceConfig? = null
)

@Serializable
data class GridSettingsConfig(
    val columnCount: Int = 4,
    val rowCount: Int = 6,
    val unlimitedMode: Boolean = true,
    val notificationShadeEnabled: Boolean = false,
    val tabTransitionAnimationName: String = "",
    val iconSnapEnabled: Boolean = true,
    val snapMode: String = "ICON",
    val bottomFlingAppDrawerEnabled: Boolean = true,
    val shadeBackgroundColorArgb: Long = 0xFF111111L,
    val shadeCornerRadiusDp: Int = 20,
    val shadeBackgroundAlpha: Float = 1f,
    val shadeAccentColorArgb: Long = 0L
)

@Serializable
data class PowerSettingsConfig(
    val powerButtonVisible: Boolean = false,
    val quickDeleteVisible: Boolean = false,
    val headerVisible: Boolean = true,
    val wakeMethod: String = "DOUBLE_TAP",
    val backButtonShortcutEnabled: Boolean = false,
    val backButtonShortcut: String = "NONE",
    val backButtonShortcutAppPackage: String? = null,
    val secondaryBackButtonShortcut: String = "NONE",
    val secondaryBackButtonShortcutAppPackage: String? = null,
    val poweredOffBrightness: Int = 40,
    val appDrawerFilterByPage: Boolean = false,
    val pagerSwipeSensitivity: Int = 0
)

@Serializable
data class WallpaperConfig(
    val type: String = "NONE",
    val activeUri: String? = null,
    val savedImageUri: String? = null,
    val savedGifUri: String? = null,
    val savedVideoUri: String? = null
)

@Serializable
data class SearchLayoutConfig(
    val isHorizontalLayout: Boolean = false
)

@Serializable
data class AppConfig(
    val visibility: AppVisibilityConfig = AppVisibilityConfig(),
    val positions: Map<String, PagePositionConfig> = emptyMap(),
    val folders: Map<String, List<FolderItemConfig>> = emptyMap(),
    val pinnedRoms: Map<String, List<PinnedRomInfo>> = emptyMap()
)

@Serializable
data class AppVisibilityConfig(
    val hiddenAppsByPage: Map<String, List<String>> = emptyMap(),
    val newAppsVisibleByDefault: Boolean = true,
    val showAppNames: Boolean = false,
    val showHomeScreenAppNames: Boolean = true,
    val appLabelFontSize: Int = 12,
    val showFolderNames: Boolean = true,
    val showSettingsBackButton: Boolean = true
)

@Serializable
data class PagePositionConfig(
    val freeMode: Boolean = false,
    val dragLocked: Boolean = true,
    val scrollDisabled: Boolean = false,
    val bottomFlingDisabled: Boolean = false,
    val items: Map<String, AppPositionItemConfig> = emptyMap()
)

@Serializable
data class AppPositionItemConfig(
    val x: Float,
    val y: Float,
    val iconSize: Float = 64f
)

@Serializable
data class FolderItemConfig(
    val id: String,
    val name: String,
    val apps: List<String>,
    val x: Float,
    val y: Float,
    val iconSize: Float = 64f,
    val backgroundColorArgb: Int? = null,
    val backgroundImagePath: String? = null
)

@Serializable
data class PageConfig(
    val pageCount: Int = 1,
    val pageTypes: List<String> = listOf("APPS_TAB"),
    val homeTabIndex: Int = 0,
    val widgetPageApps: Map<String, WidgetPageAppsConfig> = emptyMap(),
    /** Per-page Unified Canvas layouts, keyed by pageIndex.toString(). */
    val canvasLayouts: Map<String, CanvasLayout> = emptyMap(),
    /** v52: widget id to provider, so restore can tell a live widget from a reused id. */
    val widgetProviders: Map<String, String>? = null
)

@Serializable
data class WidgetPageAppsConfig(
    val visibleApps: List<String> = emptyList(),
    val appsFirst: Boolean = false
)

@Serializable
data class FeatureConfig(
    val dock: DockConfig = DockConfig(),
    val controlPad: ControlPadConfig = ControlPadConfig(),
    val appDrawerFab: AppDrawerFabConfig = AppDrawerFabConfig(),
    val gameKonfetti: GameKonfettiConfig = GameKonfettiConfig(),
    val floatyMode: FloatyModeConfig = FloatyModeConfig(),
    val jingles: JinglesConfig = JinglesConfig(),
    val bgMusic: BgMusicConfig = BgMusicConfig(),
    val romSearch: RomSearchConfig = RomSearchConfig(),
    val homeButton: HomeButtonConfig = HomeButtonConfig(),
    val recentlyPlayed: List<RecentlyPlayedEntry> = emptyList(),
    /**
     * v34: persisted [jr.brian.core.model.ScreenAssignment] name
     * (`FRONTEND_ON_TOP` / `FRONTEND_ON_BOTTOM`). Null on pre-v34
     * exports — import leaves the device's current assignment alone
     * in that case rather than reverting to the default, so restoring
     * an old backup never silently flips a user's dual-screen layout.
     */
    val screenAssignment: String? = null,
    /**
     * v38: persisted [jr.brian.core.model.Experience] name. Null on pre-v38
     * exports. Import leaves the device's current experience alone in that
     * case, so restoring an old backup never silently switches a user out
     * of Jarn FE.
     */
    val experience: String? = null,
    /**
     * v39: persisted [jr.brian.core.model.ArtSource] name. Null on pre-v39
     * exports. Import leaves the device's current source alone in that case.
     */
    val artSource: String? = null,
    /**
     * v39: persisted [jr.brian.core.model.SystemArtSource] name. Null on
     * pre-v39 exports. Import leaves the device's current source alone then.
     */
    val systemArtSource: String? = null,
    /**
     * v40: game folders, without their access grants. Null on pre-v40
     * exports. Import only adds missing folders, so games and favorites stay.
     */
    val libraryFolders: List<LibraryFolderConfig>? = null,
    /** v41: Jarn FE look id. Null on pre-v41 exports, and import keeps the current look. */
    val jarnLook: String? = null,
    /** v41: Jarn FE reduced motion. Null on pre-v41 exports, and import keeps the current value. */
    val jarnReducedMotion: Boolean? = null,
    /** v42: Jarn FE art per screen. Null on pre-v42 exports, and import keeps current choices. */
    val jarnArtChoices: JarnArtChoicesConfig? = null,
    /** v43: Jarn FE pinned apps in order. Null on pre-v43 exports, and import keeps current pins. */
    val jarnPinnedApps: List<String>? = null,
    /** v44: Jarn FE display choices. Null on pre-v44 exports, and import keeps the current ones. */
    val jarnDisplay: JarnDisplayConfig? = null,
    /** v45: Jarn FE live wallpaper id or "off". Null on pre-v45 exports, and import keeps the current one. */
    val jarnLiveWallpaper: String? = null,
    /** v48: play totals per game. Null on pre-v48 exports, and import keeps current totals. */
    val jarnPlayStats: List<JarnPlayStatConfig>? = null,
    /** v49: Jarn FE tour finished. Null on pre-v49 exports. Import never turns it back off. */
    val jarnFirstRunDone: Boolean? = null,
    /** v50: Jarn FE follows Command Panel's colors. Null on pre-v50 exports, and import keeps the current value. */
    val jarnMatchCommandPanel: Boolean? = null,
    /** v52: RSS feeds in order. Null on pre-v52 exports. Import only adds missing feeds. */
    val rssFeeds: List<RssFeedConfig>? = null,
    /** v54: Jarn FE wallpaper per screen. Null on pre-v54 exports, and import keeps the current choice. */
    val jarnWallpaperScreens: JarnWallpaperScreensConfig? = null,
    /** v57: Meet Jarn preview state. Null on pre-v57 exports, and import keeps the current state. */
    val jarnPreview: JarnPreviewConfig? = null
)

@Serializable
data class JarnPlayStatConfig(
    val systemId: String = "",
    val relativePath: String = "",
    val playCount: Int = 0,
    val playTimeMinutes: Int = 0,
)

internal fun BackedUpPlayStat.toConfig() =
    JarnPlayStatConfig(systemId, relativePath, playCount, playTimeMinutes)

internal fun JarnPlayStatConfig.toBackedUpPlayStat() =
    BackedUpPlayStat(systemId, relativePath, playCount, playTimeMinutes)

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
    val systemArtVisible: Boolean? = null,
    val gameCountVisible: Boolean? = null,
    val recentShelfVisible: Boolean? = null,
)

@Serializable
data class JarnArtChoicesConfig(
    val topBySystem: Map<String, String> = emptyMap(),
    val bottomBySystem: Map<String, String> = emptyMap(),
    val topByGame: Map<String, String> = emptyMap(),
    val bottomByGame: Map<String, String> = emptyMap()
)

internal fun GameArtChoices.toConfig() = JarnArtChoicesConfig(
    topBySystem = topBySystem.mapValues { it.value.name },
    bottomBySystem = bottomBySystem.mapValues { it.value.name },
    topByGame = topByGame.mapValues { it.value.name },
    bottomByGame = bottomByGame.mapValues { it.value.name }
)

internal fun JarnArtChoicesConfig.toChoices() = GameArtChoices(
    topBySystem = topBySystem.knownArtChoices(),
    bottomBySystem = bottomBySystem.knownArtChoices(),
    topByGame = topByGame.knownArtChoices(),
    bottomByGame = bottomByGame.knownArtChoices()
)

private fun Map<String, String>.knownArtChoices(): Map<String, ArtChoice> =
    entries.mapNotNull { (key, name) -> ArtChoice.fromNameOrNull(name)?.let { key to it } }.toMap()

@Serializable
data class LibraryFolderConfig(
    val treeUri: String = "",
    val displayPath: String = "",
    val volumeId: String? = null
)

internal fun BackedUpFolder.toConfig() = LibraryFolderConfig(treeUri, displayPath, volumeId)

internal fun LibraryFolderConfig.toBackedUpFolder() = BackedUpFolder(treeUri, displayPath, volumeId)

@Serializable
data class HomeButtonConfig(
    val interceptionEnabled: Boolean = false,
    /**
     * Legacy field from the v23 target-selector era. Routing is now driven
     * by gestures (single / double / long press) instead of a persisted
     * target, so this value is retained in the schema purely so older blobs
     * continue to decode via `ignoreUnknownKeys`. It is never read on
     * import nor written on export.
     */
    val homeTarget: String? = null,
    /** Persisted [jr.brian.home.model.MainScreen] name; defaults to `BOTTOM`. */
    val mainScreen: String = "BOTTOM",
    /**
     * Legacy boolean from v31. Retained so pre-v32 blobs still decode
     * and so pre-v32 app versions reading a current export get a
     * sensible value (true -> Follow, false -> Off). The v32-aware
     * import prefers [focusLockMode] when non-null.
     */
    val focusLockAutoManageEnabled: Boolean = true,
    /**
     * v32: name of a [jr.brian.home.data.FocusLockMode] entry
     * (Off / Top / Bottom / Follow). Nullable so pre-v32 blobs
     * decode without a value and fall back to the legacy boolean
     * on import. Default on export is Follow.
     */
    val focusLockMode: String? = null
)

@Serializable
data class DockConfig(
    val apps: List<String> = emptyList(),
    val colorArgb: Int = -16777216,
    val size: String = "MEDIUM",
    val visible: Boolean = true,
    val visiblePages: List<Int> = emptyList(),
    val maxApps: Int = 5
)

@Serializable
data class ControlPadConfig(
    val items: List<ControlPadItemConfig> = emptyList(),
    val cameraSensitivity: Float = 0.05f,
    val joystickMode: String = "RIGHT_ONLY"
)

@Serializable
data class ControlPadItemConfig(
    val label: String,
    val mappedButton: String? = null
)

@Serializable
data class AppDrawerFabConfig(
    val colorArgb: Int = -65536,
    val enabled: Boolean = true,
    val visiblePages: List<Int> = emptyList(),
    val explicitPages: Boolean = false,
    val position: String = "LEFT"
)

@Serializable
data class FloatyModeConfig(
    val isUnlocked: Boolean = false,
    val isActive: Boolean = false,
    val enabledTabs: List<Int> = emptyList(),
    val sectionTapKonfettiEnabled: Boolean = false,
    val poweredOffFloatyEffectEnabled: Boolean = false,
    val appsModalFloatyEffectEnabled: Boolean = false,
    val appDrawerFloatyAppCount: Int = 0,
    val appDrawerBubblePopEnabled: Boolean = false
)

@Serializable
data class JinglesConfig(
    val isMuted: Boolean = false,
    val volume: Float = 1.0f,
    val regexPriority: Boolean = false,
    val isNormalizationEnabled: Boolean = false,
    /** v52: jingles on or off. Null on pre-v52 exports, and import keeps the current value. */
    val enabled: Boolean? = null,
    /** v52: GitHub jingle repos. Null on pre-v52 exports. Import only adds missing repos. */
    val repos: List<String>? = null
)

@Serializable
data class BgMusicConfig(
    val mode: String = "SINGLE_FILE",
    val folderUri: String? = null,
    val fileUri: String? = null,
    val volume: Float = 0.5f,
    val isMuted: Boolean = false
)

@Serializable
data class RomSearchConfig(
    // Retained for import compatibility with older exports. The feature it
    // gated (SELECT-toggled on-screen keyboard hints) was removed; value is
    // read but no longer applied.
    val hintsKbVisible: Boolean = true,
    val frontendEnabled: Boolean = false,
    val companionLaunchWatchdogEnabled: Boolean = false,
    val secondaryMediaEnabled: Boolean = true,
    val jarngreiprMediaEnabled: Boolean = true,
    val systemLayout: String = "Grid",
    val primarySystemLayout: String = "Row",
    val secondarySystemLayout: String = "Grid",
    val gameLayout: String = "Grid",
    val primaryGameLayout: String = "Grid",
    val secondaryGameLayout: String = "Grid",
    val systemCustomizations: Map<String, SystemCustomization> = emptyMap(),
    val systemOrder: List<String> = emptyList(),
    val frontendHintsVisible: Boolean = true,
    val frontendFloatIntensity: Float = 1f,
    val canvasContinuousSpinRoms: Set<String> = emptySet(),
    val gameMediaMap: Map<String, String> = emptyMap(),
    val systemMediaMap: Map<String, String> = emptyMap(),
    @Deprecated("Replaced by gameCompanionsMap in v36 (ROMs can now have up to two companions). Still written on export so pre-v36 builds get the first companion back, and still read on import as a fallback for pre-v36 configs.")
    val gameCompanionMap: Map<String, String> = emptyMap(),
    val gameCompanionsMap: Map<String, List<String>> = emptyMap(),
    val frontendFocusHapticEnabled: Boolean = true,
    val frontendFocusBackgroundEnabled: Boolean = false,
    val frontendFocusBackgroundSystems: Boolean = true,
    val frontendFocusBackgroundGames: Boolean = true,
    @Deprecated("Split into frontendFocusBackgroundDimSystems / frontendFocusBackgroundDimGames; retained for import of pre-split configs")
    val frontendFocusBackgroundDim: Float = 0.55f,
    val frontendFocusBackgroundDimSystems: Float = 0.55f,
    val frontendFocusBackgroundDimGames: Float = 0.55f,
    val frontendTransition: String = "Fade",
    val frontendTransitionMs: Int = 280,
    val frontendSystemRowAlignment: String = "Center",
    val frontendGameRowAlignment: String = "Center",
    val frontendSystemTileScale: Float = 1.0f,
    val frontendGameTileScale: Float = 1.0f,
    val gamelistDecorationEnabled: Boolean = true,
    val scraper: ScraperConfig = ScraperConfig(),
    val userMetadata: UserMetadataConfig = UserMetadataConfig()
)

@Serializable
data class UserMetadataConfig(
    val enabled: Boolean = true
)

@Serializable
data class ScraperConfig(
    val screenScraperEnabled: Boolean = false,
    val screenScraperDevId: String = "",
    val screenScraperDevPassword: String = "",
    val screenScraperUserId: String = "",
    val screenScraperUserPassword: String = "",
    val screenScraperMediaTypes: Set<String> = emptySet(),
    val steamGridDbEnabled: Boolean = false,
    val steamGridDbApiKey: String = "",
    val steamGridDbMediaTypes: Set<String> = emptySet(),
    val concurrencyOverride: Int = 0
)

@Serializable
data class SystemConfig(
    val badgesVisible: Boolean = true,
    val shadeTabPage: Int = 0,
    val hiddenSystems: List<String> = emptyList(),
    val systemFolderMappings: List<SystemFolderMapping> = emptyList(),
    val rcNotificationsEnabled: Boolean = false
)
