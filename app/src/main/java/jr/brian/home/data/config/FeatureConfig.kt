package jr.brian.home.data.config

import jr.brian.home.data.RecentlyPlayedEntry
import jr.brian.home.ui.components.konfetti.GameKonfettiConfig
import kotlinx.serialization.Serializable

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
    /** v62: Jarn FE uses Command Panel's fonts. Null on pre-v62 exports, and import keeps the current value. */
    val jarnMatchCommandPanelFonts: Boolean? = null,
    /** v63: Jarn FE fonts prompt answered. Null on pre-v63 exports. Import never turns it back off. */
    val jarnFontsPromptShown: Boolean? = null,
    /** v65: Jarn FE "ES-DE found" notice shown. Null on pre-v65 exports. Import never turns it back off. */
    val jarnEsdeImportNoticeShown: Boolean? = null,
    /** v67: Jarn FE game order by system id. Null on pre-v67 exports, and import keeps them. */
    val jarnSystemSorts: Map<String, String>? = null,
    /** v52: RSS feeds in order. Null on pre-v52 exports. Import only adds missing feeds. */
    val rssFeeds: List<RssFeedConfig>? = null,
    /** v54: Jarn FE wallpaper per screen. Null on pre-v54 exports, and import keeps the current choice. */
    val jarnWallpaperScreens: JarnWallpaperScreensConfig? = null,
    /** v57: Meet Jarn preview state. Null on pre-v57 exports, and import keeps the current state. */
    val jarnPreview: JarnPreviewConfig? = null
)
