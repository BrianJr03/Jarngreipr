package jr.brian.home.ui.theme.managers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.core.net.toUri
import jr.brian.home.R
import jr.brian.home.model.TransferProgress
import jr.brian.home.service.ThemeSharingTile
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

    init {
        syncBootReceiverState()
    }

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
    val nearbyDiscoveredEndpoints: StateFlow<Map<String, String>> =
        _nearbyDiscoveredEndpoints.asStateFlow()

    private val _connectedEndpoints = MutableStateFlow<Set<String>>(emptySet())
    val connectedEndpoints: StateFlow<Set<String>> = _connectedEndpoints.asStateFlow()

    private val _receivedWallpaperFile =
        MutableSharedFlow<Triple<WallpaperType, Uri, String>>(extraBufferCapacity = 8)
    val receivedWallpaperFile: SharedFlow<Triple<WallpaperType, Uri, String>> =
        _receivedWallpaperFile.asSharedFlow()

    private val _transferProgress = MutableStateFlow<Map<String, TransferProgress>>(emptyMap())
    val transferProgress: StateFlow<Map<String, TransferProgress>> = _transferProgress.asStateFlow()

    private val _failedTransferEndpoints = MutableStateFlow<Set<String>>(emptySet())
    val failedTransferEndpoints: StateFlow<Set<String>> = _failedTransferEndpoints.asStateFlow()

    private var wallpaperNearbyClient: PingNearbyClient? = null
    private var nearbyScope: CoroutineScope? = null

    private fun loadTheme(): ColorTheme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeId =
            prefs.getString(KEY_THEME, ColorTheme.PINK_VIOLET.id) ?: ColorTheme.PINK_VIOLET.id

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
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun loadPingAutoStart(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PING_AUTO_START, false)
    }

    private fun syncBootReceiverState() {
        val state = if (loadPingAutoStart()) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        runCatching {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, "jr.brian.ping.BootReceiver"),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    fun updatePingAutoStart(enabled: Boolean) {
        isPingAutoStart = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_PING_AUTO_START, enabled) }
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, "jr.brian.ping.BootReceiver"),
            state,
            PackageManager.DONT_KILL_APP
        )
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
                _transferProgress.update { it - id }
            }
            onTransferUpdate = { endpointId, progress ->
                _transferProgress.update { it + (endpointId to TransferProgress(fraction = progress)) }
            }
            onTransferFailed = { endpointId ->
                _transferProgress.update { it - endpointId }
                _failedTransferEndpoints.update { it + endpointId }
            }
            onImageReceived = { endpointId, bitmap ->
                _transferProgress.update { it - endpointId }
                handleReceivedMedia(bitmap, _nearbyDiscoveredEndpoints.value[endpointId] ?: endpointId)
            }
            onFileReceived = { endpointId, pfd ->
                nearbyScope?.launch {
                    val nearbyDir = File(context.filesDir, "nearby_wallpapers").also { it.mkdirs() }
                    val tempFile = File(nearbyDir, "wp_tmp_${System.currentTimeMillis()}")
                    try {
                        pfd.use { descriptor ->
                            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                                tempFile.outputStream().buffered(65536).use { output ->
                                    input.copyTo(output, bufferSize = 65536)
                                    output.flush()
                                }
                            }
                        }

                        val header = ByteArray(GIF_MARKER.size)
                        tempFile.inputStream().use { it.read(header) }

                        val destFile: File
                        val type: WallpaperType

                        if (header.contentEquals(GIF_MARKER)) {
                            type = WallpaperType.GIF
                            destFile = File(nearbyDir, "wp_${System.currentTimeMillis()}.gif")
                            tempFile.inputStream().use { input ->
                                input.skip(GIF_MARKER.size.toLong())
                                destFile.outputStream().buffered(65536).use { output ->
                                    input.copyTo(output, bufferSize = 65536)
                                    output.flush()
                                }
                            }
                            tempFile.delete()
                        } else {
                            type = if (isGif(header)) WallpaperType.GIF else WallpaperType.VIDEO
                            val ext = if (type == WallpaperType.GIF) "gif" else "mp4"
                            destFile = File(nearbyDir, "wp_${System.currentTimeMillis()}.$ext")
                            tempFile.renameTo(destFile)
                        }

                        val senderName = _nearbyDiscoveredEndpoints.value[endpointId] ?: endpointId
                        _receivedWallpaperFile.tryEmit(Triple(type, destFile.toUri(), senderName))
                        _transferProgress.update { it + (endpointId to TransferProgress(fraction = 1f)) }
                    } catch (_: Exception) {
                        tempFile.delete()
                        _transferProgress.update { it + (endpointId to TransferProgress(fraction = 1f)) }
                        Toast.makeText(context, "Failed to receive wallpaper", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
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
        _transferProgress.value = emptyMap()
    }

    fun getCurrentWallpaperType(): WallpaperType {
        val typeString = context.getSharedPreferences(LAUNCHER_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAUNCHER_WALLPAPER_TYPE, "NONE") ?: "NONE"
        return try {
            WallpaperType.valueOf(typeString)
        } catch (_: Exception) {
            WallpaperType.NONE
        }
    }

    fun sendWallpaperTo(endpointId: String) {
        nearbyScope?.launch {
            _failedTransferEndpoints.update { it - endpointId }
            val uri = getCurrentWallpaperUri() ?: return@launch
            when (getCurrentWallpaperType()) {
                WallpaperType.GIF -> {
                    // Prepend marker so the receiver's library routes this through
                    // onFileReceived instead of decoding it as a static Bitmap
                    val tempFile = File(context.cacheDir, "send_${System.currentTimeMillis()}.tmp")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            output.write(GIF_MARKER)
                            input.copyTo(output)
                        }
                    }
                    wallpaperNearbyClient?.sendFile(endpointId, tempFile.toUri())
                }

                WallpaperType.VIDEO -> {
                    wallpaperNearbyClient?.sendFile(endpointId, uri)
                }

                else -> {
                    decodeWallpaperBitmap()?.let { bitmap ->
                        wallpaperNearbyClient?.sendImage(endpointId, bitmap)
                    }
                }
            }
        }
    }

    private fun handleReceivedMedia(bitmap: Bitmap, senderName: String) {
        nearbyScope?.launch {
            val nearbyDir = File(context.filesDir, "nearby_wallpapers").also { it.mkdirs() }
            val destFile = File(nearbyDir, "wp_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(destFile)
                .use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            _receivedWallpaperFile.tryEmit(Triple(WallpaperType.IMAGE, destFile.toUri(), senderName))
        }
    }

    // GIF magic bytes: GIF8
    private fun isGif(bytes: ByteArray) = bytes.size >= 3 &&
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte()

    companion object {
        // 4-byte prefix prepended to GIFs before sending via sendFile.
        // Makes BitmapFactory fail to decode the stream → library routes to onFileReceived
        // instead of decoding the first frame and firing onImageReceived.
        private val GIF_MARKER =
            byteArrayOf(0xFE.toByte(), 0xFE.toByte(), 0xFE.toByte(), 0xFE.toByte())
    }

    private fun decodeWallpaperBitmap(): Bitmap? {
        val uri = getCurrentWallpaperUri() ?: return null
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: BitmapFactory.decodeFile(uri.path)
        } catch (_: Exception) {
            try {
                BitmapFactory.decodeFile(uri.path)
            } catch (_: Exception) {
                null
            }
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
        ThemeSharingTile.syncState(context, true)
    }

    fun stopSharing() {
        context.stopService(Intent(context, PingService::class.java))
        ThemeSharingTile.syncState(context, false)
    }
}

val LocalThemeManager =
    compositionLocalOf<ThemeManager> {
        error("ThemeManager not provided")
    }
