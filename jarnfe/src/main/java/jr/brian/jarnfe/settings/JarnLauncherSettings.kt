package jr.brian.jarnfe.settings

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow

const val JARN_APP_LABEL_DEFAULT_SIZE = 12
const val JARN_APP_LABEL_MIN_SIZE = 8
const val JARN_APP_LABEL_MAX_SIZE = 24

@Immutable
data class JarnLauncherPrefs(
    val appLabelSize: Int = JARN_APP_LABEL_DEFAULT_SIZE,
    val dockVisible: Boolean = true,
    val dockApps: List<String> = emptyList(),
    val maxDockApps: Int = 5,
) {
    val dockCount: Int get() = dockApps.count { it.isNotEmpty() }
}

enum class JarnSupportLink { Coffee, Kofi, Discord }

/** Classic's own stores, so a change in either experience shows in both and stays in backups. */
interface JarnLauncherSettings {
    val prefs: Flow<JarnLauncherPrefs>

    fun readPrefs(): JarnLauncherPrefs

    fun setAppLabelSize(size: Int)

    fun setDockVisible(visible: Boolean)

    /** False when the dock has no free slot. */
    fun setDockApp(packageName: String, inDock: Boolean): Boolean

    suspend fun whatsNew(): String

    fun openSupportLink(host: Activity, link: JarnSupportLink): Boolean
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnLauncherSettingsProvider {
    val jarnLauncherSettings: JarnLauncherSettings
}

internal fun Context.jarnLauncherSettingsOrNull(): JarnLauncherSettings? =
    (applicationContext as? JarnLauncherSettingsProvider)?.jarnLauncherSettings

@Composable
internal fun rememberJarnLauncherPrefs(): JarnLauncherPrefs {
    val context = LocalContext.current
    val settings = remember(context) { context.jarnLauncherSettingsOrNull() } ?: return JarnLauncherPrefs()
    val initial = remember(settings) { settings.readPrefs() }
    return settings.prefs.collectAsState(initial).value
}

/** Classic's size is in sp around a default of 12, so that default keeps Jarn's label size. */
internal fun jarnAppLabelScale(size: Int): Float =
    size.coerceIn(JARN_APP_LABEL_MIN_SIZE, JARN_APP_LABEL_MAX_SIZE).toFloat() / JARN_APP_LABEL_DEFAULT_SIZE

/** The first open dock slot, or null when every slot up to [max] holds an app. */
fun jarnNextDockSlot(apps: List<String>, max: Int): Int? {
    val open = apps.take(max).indexOfFirst { it.isEmpty() }
    return when {
        open >= 0 -> open
        apps.size < max -> apps.size
        else -> null
    }
}

private val MarkdownHeading = Regex("^#{1,6}\\s+")
private val MarkdownBullet = Regex("^[-*]\\s+")

/** Patch notes are Markdown, and the reader shows plain text. */
internal fun jarnPlainPatchNotes(notes: String): String = notes.lineSequence()
    .map { line ->
        line.trim()
            .replace("**", "")
            .replace(MarkdownHeading, "")
            .replace(MarkdownBullet, "• ")
    }
    .joinToString("\n")
    .trim()
