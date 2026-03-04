package jr.brian.home.ui.theme.managers

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.core.net.toUri
import jr.brian.home.R
import jr.brian.home.ui.theme.ColorTheme
import jr.brian.home.util.PingThemeUtil
import jr.brian.ping.PingPermissions.hasPingPermissions
import jr.brian.ping.PingService
import jr.brian.pingnearby.PingNearbyClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val PREFS_NAME = "gaming_launcher_prefs"
private const val KEY_THEME = "selected_theme"
private const val KEY_CUSTOM_THEMES = "custom_themes"
private const val KEY_PING_AUTO_START = "ping_auto_start"
private const val KEY_PING_DISPLAY_NAME = "ping_display_name"
private const val CUSTOM_THEME_SEPARATOR = "|||"
private const val CUSTOM_THEME_FIELD_SEPARATOR = ":::"
private const val ESDE_PREFS_NAME = "esde_prefs"
private const val KEY_ESDE_SINGLE_SYSTEM_IMAGE = "single_system_image_path"
private const val KEY_WALLPAPER_NEARBY_AUTO_START = "wallpaper_nearby_auto_start"
private const val LAUNCHER_PREFS_NAME = "launcher_prefs"
private const val KEY_LAUNCHER_WALLPAPER_TYPE = "wallpaper_type"
private const val KEY_LAUNCHER_WALLPAPER_URI = "selected_wallpaper"

class ThemeManager(private val context: Context) {
    private val customThemes = mutableStateOf(loadCustomThemes())

    var currentTheme by mutableStateOf(loadTheme())
        private set

    val allThemes: List<ColorTheme>
        get() = ColorTheme.presetThemes + customThemes.value

    var isPingAutoStart by mutableStateOf(loadPingAutoStart())
        private set

    var pingDisplayName by mutableStateOf(loadPingDisplayName())
        private set

    var singleSystemImagePath by mutableStateOf(loadSingleSystemImagePath())
        private set

    var isWallpaperNearbyAutoStart by mutableStateOf(loadWallpaperNearbyAutoStart())
        private set

    var isWallpaperNearbyRunning by mutableStateOf(false)
        private set

    private val _nearbyDiscoveredEndpoints = MutableStateFlow<Map<String, String>>(emptyMap())
    val nearbyDiscoveredEndpoints: StateFlow<Map<String, String>> = _nearbyDiscoveredEndpoints.asStateFlow()

    private val _connectedEndpoints = MutableStateFlow<Set<String>>(emptySet())
    val connectedEndpoints: StateFlow<Set<String>> = _connectedEndpoints.asStateFlow()

    private val _receivedWallpaper = MutableSharedFlow<Bitmap>(extraBufferCapacity = 8)
    val receivedWallpaper: SharedFlow<Bitmap> = _receivedWallpaper.asSharedFlow()

    private var wallpaperNearbyClient: PingNearbyClient? = null
    private var nearbyScope: CoroutineScope? = null

    private fun loadTheme(): ColorTheme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeId = prefs.getString(KEY_THEME, ColorTheme.PINK_VIOLET.id) ?: ColorTheme.PINK_VIOLET.id

        if (themeId.startsWith(ColorTheme.CUSTOM_THEME_PREFIX)) {
            val customThemesList = loadCustomThemes()
            return customThemesList.find { it.id == themeId } ?: ColorTheme.PINK_VIOLET
        }

        return ColorTheme.fromId(themeId)
    }

    private fun loadCustomThemes(): List<ColorTheme> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customThemesString = prefs.getString(KEY_CUSTOM_THEMES, "") ?: ""

        if (customThemesString.isEmpty()) return emptyList()

        return customThemesString.split(CUSTOM_THEME_SEPARATOR).mapNotNull { themeData ->
            try {
                val parts = themeData.split(CUSTOM_THEME_FIELD_SEPARATOR)
                if (parts.size >= 5) {
                    ColorTheme.fromCustomData(
                        id = parts[0],
                        name = parts[1],
                        primaryColorHex = parts[2],
                        secondaryColorHex = parts[3],
                        isSolid = parts[4].toBoolean()
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun loadPingAutoStart(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PING_AUTO_START, false)
    }

    fun updatePingAutoStart(enabled: Boolean) {
        isPingAutoStart = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_PING_AUTO_START, enabled) }
    }

    private fun loadPingDisplayName(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PING_DISPLAY_NAME, "") ?: ""
    }

    fun updatePingDisplayName(name: String) {
        pingDisplayName = name
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_PING_DISPLAY_NAME, name) }
    }

    private fun loadSingleSystemImagePath(): String? {
        return context.getSharedPreferences(ESDE_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ESDE_SINGLE_SYSTEM_IMAGE, null)
    }

    fun updateSingleSystemImagePath(path: String?) {
        singleSystemImagePath = path
        val prefs = context.getSharedPreferences(ESDE_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            if (path != null) putString(KEY_ESDE_SINGLE_SYSTEM_IMAGE, path)
            else remove(KEY_ESDE_SINGLE_SYSTEM_IMAGE)
        }
    }

    private fun loadWallpaperNearbyAutoStart(): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WALLPAPER_NEARBY_AUTO_START, false)
    }

    fun updateWallpaperNearbyAutoStart(enabled: Boolean) {
        isWallpaperNearbyAutoStart = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_WALLPAPER_NEARBY_AUTO_START, enabled) }
    }

    fun isEsdeModeActive(): Boolean {
        val type = context.getSharedPreferences(LAUNCHER_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAUNCHER_WALLPAPER_TYPE, "NONE") ?: "NONE"
        return type == "ESDE"
    }

    fun getCurrentWallpaperUri(): Uri? {
        return if (isEsdeModeActive()) {
            val path = singleSystemImagePath ?: return null
            runCatching {
                val f = File(path)
                if (f.exists()) f.toUri() else path.toUri()
            }.getOrElse { path.toUri() }
        } else {
            val uri = context.getSharedPreferences(LAUNCHER_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAUNCHER_WALLPAPER_URI, null) ?: return null
            uri.toUri()
        }
    }

    fun startWallpaperNearby() {
        nearbyScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        wallpaperNearbyClient = PingNearbyClient(
            context = context,
            serviceId = context.packageName
        ).apply {
            onEndpointFound = { id, name ->
                val ownName = pingDisplayName.ifBlank { Build.MODEL }
                if (name != ownName) {
                    _nearbyDiscoveredEndpoints.update { it + (id to name) }
                }
            }
            onConnected = { id, name ->
                _connectedEndpoints.update { it + id }
                _nearbyDiscoveredEndpoints.update { current ->
                    if (id !in current) current + (id to name) else current
                }
            }
            onDisconnected = { id ->
                _nearbyDiscoveredEndpoints.update { it - id }
                _connectedEndpoints.update { it - id }
            }
            onImageReceived = { _, bitmap ->
                _receivedWallpaper.tryEmit(bitmap)
            }
            onFileReceived = { _, file ->
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) _receivedWallpaper.tryEmit(bitmap)
            }
        }
        wallpaperNearbyClient?.start(pingDisplayName.ifBlank { Build.MODEL })
        isWallpaperNearbyRunning = true
    }

    fun stopWallpaperNearby() {
        wallpaperNearbyClient?.stop()
        wallpaperNearbyClient = null
        nearbyScope?.cancel()
        nearbyScope = null
        isWallpaperNearbyRunning = false
        _nearbyDiscoveredEndpoints.value = emptyMap()
        _connectedEndpoints.value = emptySet()
    }

    fun sendWallpaperTo(endpointId: String) {
        nearbyScope?.launch {
            decodeWallpaperBitmap()?.let { bitmap ->
                wallpaperNearbyClient?.sendImage(endpointId, bitmap)
            }
        }
    }

    private fun decodeWallpaperBitmap(): Bitmap? {
        val uri = getCurrentWallpaperUri() ?: return null
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: BitmapFactory.decodeFile(uri.path)
        } catch (_: Exception) {
            try { BitmapFactory.decodeFile(uri.path) } catch (_: Exception) { null }
        }
    }

    private fun saveCustomThemes() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customThemesString = customThemes.value.joinToString(CUSTOM_THEME_SEPARATOR) { theme ->
            val primaryHex = String.format("#%08X", theme.primaryColor.toArgb())
            val secondaryHex = String.format("#%08X", theme.secondaryColor.toArgb())
            "${theme.id}$CUSTOM_THEME_FIELD_SEPARATOR${theme.customName}$CUSTOM_THEME_FIELD_SEPARATOR$primaryHex$CUSTOM_THEME_FIELD_SEPARATOR$secondaryHex$CUSTOM_THEME_FIELD_SEPARATOR${theme.isSolid}"
        }
        prefs.edit { putString(KEY_CUSTOM_THEMES, customThemesString) }
    }

    fun setTheme(theme: ColorTheme) {
        currentTheme = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_THEME, theme.id) }
        if (isPingAutoStart && context.hasPingPermissions()) shareCurrentTheme()
    }

    fun addCustomTheme(theme: ColorTheme) {
        if (theme.isCustom) {
            if (customThemes.value.any { it.id == theme.id }) return
            customThemes.value += theme
            saveCustomThemes()
        }
    }

    fun deleteCustomTheme(theme: ColorTheme) {
        if (theme.isCustom) {
            customThemes.value = customThemes.value.filter { it.id != theme.id }
            saveCustomThemes()

            if (currentTheme.id == theme.id) {
                setTheme(ColorTheme.PINK_VIOLET)
            }
        }
    }

    fun shareCurrentTheme() {
        PingService.notificationTitle = context.getString(R.string.ping_notification_title)
        val profile = PingThemeUtil.buildProfile(
            currentTheme,
            pingDisplayName.ifBlank { Build.MODEL }
        )
        val intent = PingService.buildIntent(context, profile)
        context.startForegroundService(intent)
    }

    fun stopSharing() {
        context.stopService(Intent(context, PingService::class.java))
    }
}

val LocalThemeManager =
    compositionLocalOf<ThemeManager> {
        error("ThemeManager not provided")
    }
