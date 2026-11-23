package jr.brian.jarnfe.apps

import android.app.Activity
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import jr.brian.jarnfe.canvas.JarnAppDisplay
import kotlinx.coroutines.flow.Flow

@Immutable
data class JarnAppOptions(
    val display: JarnAppDisplay,
    val askOnLaunch: Boolean,
    val customName: String?,
    val hasSecondDisplay: Boolean,
    val pinned: Boolean = false,
    val hidden: Boolean = false,
)

@Immutable
data class JarnIconPack(
    val packageName: String,
    val name: String,
    val icon: Drawable? = null,
)

/** Per-app choices, on the same stores Classic reads. */
interface JarnAppOptionsData {
    val selectedIconPack: Flow<String?>

    fun appOptions(packageName: String): JarnAppOptions

    fun setPinned(packageName: String, pinned: Boolean)

    fun setHidden(packageName: String, hidden: Boolean)

    fun setAppDisplay(packageName: String, display: JarnAppDisplay)

    fun setAskDisplayOnLaunch(packageName: String, ask: Boolean)

    /** A null [name] goes back to the app's own name. */
    fun setAppName(packageName: String, name: String?)

    /** Returns false when the image could not be used. */
    suspend fun setAppIcon(packageName: String, uri: String): Boolean

    /** Returns false when the pack's icon could not be used. */
    suspend fun setAppIconFromPack(packageName: String, iconPack: String, iconName: String): Boolean

    suspend fun clearAppIcon(packageName: String)

    suspend fun iconPacks(): List<JarnIconPack>

    /** A null [packageName] goes back to each app's own icon. */
    suspend fun setIconPack(packageName: String?)

    suspend fun iconPackIconNames(iconPack: String): List<String>

    suspend fun iconPackIcon(iconPack: String, iconName: String): Drawable?

    fun launchApp(host: Activity, packageName: String)

    fun launchAppOn(host: Activity, packageName: String, display: JarnAppDisplay)

    fun openAppInfo(host: Activity, packageName: String)
}

internal fun JarnAppOptions.shouldAskDisplay(): Boolean = askOnLaunch && hasSecondDisplay

/** Matches ignoring case and the separators pack authors put in icon names. */
internal fun jarnFilterIconNames(names: List<String>, query: String): List<String> {
    val needle = query.iconSearchKey()
    if (needle.isEmpty()) return names
    return names.filter { needle in it.iconSearchKey() }
}

private fun String.iconSearchKey(): String = lowercase().filterNot { it == '_' || it == '-' || it.isWhitespace() }
