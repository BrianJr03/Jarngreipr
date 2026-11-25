package jr.brian.jarnfe.systems

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.jarnfe.launch.JarnAppOption
import jr.brian.jarnfe.launch.JarnEmulatorOption
import kotlinx.coroutines.flow.Flow

@Immutable
data class JarnSystemAppearance(
    val backgroundUri: String? = null,
    val focusBackgroundUri: String? = null,
    val showName: Boolean = true,
    val solidColorArgb: Long? = null,
)

@Immutable
data class JarnManagedSystem(
    val id: String,
    val title: String,
    val gameCount: Int,
    val isHidden: Boolean,
    val appearance: JarnSystemAppearance = JarnSystemAppearance(),
    val emulatorLabel: String? = null,
    val folderPath: String? = null,
    val folderIsAdded: Boolean = false,
    val hiddenGameCount: Int = 0,
)

/** A game the user hid. [key] is what [JarnSystemManager.showGames] takes. */
@Immutable
data class JarnHiddenGame(val key: String, val title: String)

@Immutable
data class JarnAddedFolder(val systemName: String, val treeUri: String, val displayPath: String)

@Immutable
data class JarnSystemChoice(val id: String, val title: String, val matchesFolder: Boolean)

sealed interface JarnFolderCheck {
    data class Found(val gameCount: Int) : JarnFolderCheck
    data object NoGames : JarnFolderCheck
    data object AccessLost : JarnFolderCheck
}

/** System settings, saved in the same storage Classic reads. Nothing here scans the library. */
interface JarnSystemManager {
    val systems: Flow<List<JarnManagedSystem>>
    val addedFolders: Flow<List<JarnAddedFolder>>

    fun setHidden(systemId: String, hidden: Boolean)
    fun setAllHidden(systemIds: List<String>, hidden: Boolean)
    fun setAppearance(systemId: String, appearance: JarnSystemAppearance)
    fun resetAppearance(systemId: String)

    /** Games hidden in Jarn or Classic, by title. */
    fun hiddenGames(systemId: String): Flow<List<JarnHiddenGame>>
    fun showGames(keys: Collection<String>)

    /** Null when the system has no game to match emulators against. */
    suspend fun emulators(systemId: String): List<JarnEmulatorOption>?
    suspend fun installedApps(): List<JarnAppOption>
    fun setEmulator(systemId: String, packageName: String)
    fun clearEmulator(systemId: String)

    suspend fun checkFolder(systemId: String, treeUri: Uri): JarnFolderCheck
    suspend fun setFolder(systemId: String, treeUri: Uri): Boolean
    fun clearFolder(systemId: String)

    suspend fun systemChoices(treeUri: Uri): List<JarnSystemChoice>
    suspend fun addFolder(systemId: String, treeUri: Uri): Boolean
    fun removeFolder(folder: JarnAddedFolder)
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnSystemManagerProvider {
    val jarnSystemManager: JarnSystemManager
}

fun Context.jarnSystemManagerOrNull(): JarnSystemManager? =
    (applicationContext as? JarnSystemManagerProvider)?.jarnSystemManager

/** Classic may have saved a system under its id or any of its folder names. */
fun systemStorageKeys(id: String, folderAliases: List<String>): List<String> =
    (listOf(id) + folderAliases).filter { it.isNotBlank() }.distinct()

internal object JarnSystems {
    var manager by mutableStateOf<JarnSystemManager?>(null)
    private var target: String? = null

    fun openAt(systemId: String?) {
        target = systemId
    }

    fun takeTarget(): String? = target.also { target = null }
}
