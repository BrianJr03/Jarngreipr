package jr.brian.home.data.config

import jr.brian.home.esde.model.SystemCustomization
import kotlinx.serialization.Serializable

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
    /** v64: emulator, core and command choices. Null on pre-v64 exports, and import keeps them. */
    val gameEmulatorMap: Map<String, String>? = null,
    val systemEmulatorMap: Map<String, String>? = null,
    val gameCommandMap: Map<String, String>? = null,
    val gameCoreMap: Map<String, String>? = null,
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
