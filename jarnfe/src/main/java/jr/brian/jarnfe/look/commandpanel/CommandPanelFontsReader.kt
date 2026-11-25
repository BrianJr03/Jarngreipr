package jr.brian.jarnfe.look.commandpanel

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import jr.brian.core.ui.theme.JarnType
import jr.brian.jarnfe.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

private val NoCommandPanelFontMatch: StateFlow<CommandPanelFontMatch?> = MutableStateFlow(null)

internal object CommandPanelFontsReader {
    private var controller: CommandPanelFontsController? = null
    private var fontCache: CommandPanelFontCache? = null

    /** One cache per process, so the reader and the offer share families in memory. */
    fun fontCache(context: Context): CommandPanelFontCache =
        fontCache ?: CommandPanelFontCache(context.applicationContext.noBackupFilesDir).also { fontCache = it }

    fun controller(context: Context): CommandPanelFontsController {
        val appContext = context.applicationContext
        val current = controller ?: CommandPanelFontsController(
            port = AndroidCommandPanelFontsPort(appContext),
            rows = CommandPanelFontsRowCache(appContext.noBackupFilesDir),
            fonts = fontCache(appContext),
            lane = Dispatchers.IO.limitedParallelism(1),
        ).also { controller = it }
        return current
    }
}

/**
 * What Command Panel's fonts offer right now: [availability] (with the look's name) and a [sample] type to show them,
 * or null when there's nothing to show. For the fonts row and the font prompt.
 */
@Immutable
internal data class CommandPanelFontOffer(
    val availability: FontMatchAvailability = FontMatchAvailability.Checking,
    val sample: JarnType? = null,
)

/**
 * The type built from Command Panel's fonts, or null for Jarn's own. While [enabled] is false this touches neither
 * Command Panel nor the copies, except that [forgetWhenOff] deletes the copies once.
 */
@Composable
internal fun rememberCommandPanelFontType(enabled: Boolean, forgetWhenOff: Boolean): JarnType? {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember(enabled, context) {
        if (enabled) CommandPanelFontsReader.controller(context).also { it.seedFromCache() } else null
    }
    LaunchedEffect(forgetWhenOff, context) {
        if (forgetWhenOff) CommandPanelFontsReader.controller(context).forget()
    }
    DisposableEffect(controller, owner) {
        val binding = controller?.let(::CommandPanelLifecycleBinding)
        binding?.let(owner.lifecycle::addObserver)
        onDispose {
            binding?.let(owner.lifecycle::removeObserver)
            binding?.release()
        }
    }
    val match = key(controller) { (controller?.match ?: NoCommandPanelFontMatch).collectAsState() }
    return match.value?.type
}

/**
 * Read once per resume while on screen. Never observes or copies fonts: an imported font only shows in [sample] once
 * the reader has a verified copy, otherwise that role keeps Jarn's font.
 */
@Composable
internal fun rememberCommandPanelFontOffer(): State<CommandPanelFontOffer> {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val offer = remember { mutableStateOf(CommandPanelFontOffer()) }
    LaunchedEffect(context, owner) {
        val appContext = context.applicationContext
        val port = AndroidCommandPanelFontsPort(appContext)
        val cache = CommandPanelFontsReader.fontCache(appContext)
        owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            offer.value = withContext(Dispatchers.IO) {
                val read = port.read()
                commandPanelFontOffer(read, appContext.hasCommandPanelProvider(), cache) { id ->
                    commandPanelLookNameRes(id)?.let(appContext::getString)
                }
            }
        }
    }
    return offer
}

internal fun commandPanelFontOffer(
    read: CommandPanelFontsRead,
    providerExists: Boolean,
    cache: CommandPanelFontCache,
    lookName: (String) -> String?,
): CommandPanelFontOffer {
    val availability = fontMatchAvailability(read, providerExists, lookName)
    val sample = (read as? CommandPanelFontsRead.Fresh)?.fonts
        ?.takeIf { availability is FontMatchAvailability.Available }
        ?.let { fonts -> buildCommandPanelFontMatch(fonts) { cache.family(it.sha256, open = null) }.type }
    return CommandPanelFontOffer(availability, sample)
}

/** Command Panel's name for a built-in `look_id`. A saved look (`custom:<id>`) or an unknown id has none. */
@StringRes
internal fun commandPanelLookNameRes(lookId: String): Int? = when (lookId) {
    "AUTO" -> R.string.jarnfe_cp_look_auto
    "AURORA_GLASS" -> R.string.jarnfe_cp_look_aurora_glass
    "AURORA_BLUR" -> R.string.jarnfe_cp_look_aurora_blur
    "CARTRIDGE" -> R.string.jarnfe_cp_look_cartridge
    "TACTICAL_HUD" -> R.string.jarnfe_cp_look_tactical_hud
    "SOFT_CONSOLE" -> R.string.jarnfe_cp_look_soft_console
    "SOFT_CONSOLE_OLED" -> R.string.jarnfe_cp_look_soft_console_oled
    "SEGMENT_LCD" -> R.string.jarnfe_cp_look_segment_lcd
    "BLOOM" -> R.string.jarnfe_cp_look_bloom
    "NEON_ARCADE" -> R.string.jarnfe_cp_look_neon_arcade
    "TERMINAL" -> R.string.jarnfe_cp_look_terminal
    "COCKPIT" -> R.string.jarnfe_cp_look_cockpit
    "STICKER_BOOK" -> R.string.jarnfe_cp_look_sticker_book
    "INDUSTRIAL" -> R.string.jarnfe_cp_look_industrial
    "JELLY" -> R.string.jarnfe_cp_look_jelly
    "COMIC_PANEL" -> R.string.jarnfe_cp_look_comic_panel
    "Y2K_CHROME" -> R.string.jarnfe_cp_look_y2k_chrome
    "E_INK" -> R.string.jarnfe_cp_look_e_ink
    else -> null
}
