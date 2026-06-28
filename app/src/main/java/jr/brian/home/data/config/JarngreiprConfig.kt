package jr.brian.home.data.config

import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.ui.components.konfetti.GameKonfettiConfig
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
        const val CONFIG_VERSION = 2
    }
}

@Serializable
data class UiConfig(
    val gridSettings: GridSettingsConfig = GridSettingsConfig(),
    val appDisplayPreferences: Map<String, String> = emptyMap(),
    val powerSettings: PowerSettingsConfig = PowerSettingsConfig(),
    val selectedIconPackage: String? = null,
    val wallpaper: WallpaperConfig = WallpaperConfig(),
    val searchLayout: SearchLayoutConfig = SearchLayoutConfig(),
    val customAppNames: Map<String, String> = emptyMap()
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
    val poweredOffBrightness: Int = 40,
    val appDrawerFilterByPage: Boolean = false
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
    val canvasLayouts: Map<String, CanvasLayout> = emptyMap()
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
    val romSearch: RomSearchConfig = RomSearchConfig()
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
    val isNormalizationEnabled: Boolean = false
)

@Serializable
data class BgMusicConfig(
    val mode: String = "SINGLE_FILE",
    val folderUri: String? = null,
    val fileUri: String? = null,
    val volume: Float = 0.5f
)

@Serializable
data class RomSearchConfig(
    val hintsKbVisible: Boolean = true
)

@Serializable
data class SystemConfig(
    val badgesVisible: Boolean = true,
    val shadeTabPage: Int = 0
)
