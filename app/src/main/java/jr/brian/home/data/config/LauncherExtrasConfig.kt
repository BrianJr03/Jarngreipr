package jr.brian.home.data.config

import kotlinx.serialization.Serializable

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
