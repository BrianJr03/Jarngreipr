package jr.brian.home.data.config

import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.model.rom.PinnedRomInfo
import kotlinx.serialization.Serializable

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
