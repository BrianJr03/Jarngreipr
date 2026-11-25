package jr.brian.jarnfe.launch

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Immutable

data class JarnEmulatorOption(
    val packageName: String,
    val label: String,
    val command: String? = null,
    val expectedExtension: String? = null,
)

data class JarnCoreOption(val label: String, val path: String)

data class JarnAppOption(val packageName: String, val label: String)

@Immutable
data class JarnGameSetup(
    val emulators: List<JarnEmulatorOption> = emptyList(),
    val currentEmulator: String? = null,
    val currentEmulatorLabel: String? = null,
    val usesRetroArch: Boolean = false,
    val cores: List<JarnCoreOption> = emptyList(),
    val currentCore: String? = null,
    val companions: List<JarnAppOption> = emptyList(),
    val isFavorite: Boolean = false,
)

/** Per-game choices, saved in the same storage Classic reads. */
interface JarnGameOptions {
    /** Fills in the real file location for a game added through a folder picker. */
    suspend fun locate(game: JarnGameRef, libraryKey: String): JarnGameRef
    suspend fun load(game: JarnGameRef, libraryKey: String): JarnGameSetup
    suspend fun installedApps(): List<JarnAppOption>
    fun appOptions(packages: List<String>): List<JarnAppOption>
    fun chooseEmulator(game: JarnGameRef, option: JarnEmulatorOption)
    fun chooseCore(game: JarnGameRef, corePath: String)
    suspend fun retroArchCores(): List<JarnCoreOption>

    /** How many games have their own choice that [applyToSystem] would replace. */
    suspend fun systemChoiceConflicts(systemId: String, choice: JarnEmulatorChoice): Int

    /** Makes [choice] the system default and replaces every game's own emulator choice. */
    suspend fun applyToSystem(systemId: String, choice: JarnEmulatorChoice)
    fun addCompanion(game: JarnGameRef, packageName: String)
    fun clearCompanions(game: JarnGameRef)
    fun openCompanion(host: Activity, packageName: String)

    /** Returns false when the game is no longer in the library. */
    suspend fun setFavorite(libraryKey: String, favorite: Boolean): Boolean

    /** Hides the game in Jarn and Classic. Returns false when the game is no longer in the library. */
    suspend fun setHidden(libraryKey: String, hidden: Boolean): Boolean
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnGameOptionsProvider {
    val jarnGameOptions: JarnGameOptions
}

fun Context.jarnGameOptionsOrNull(): JarnGameOptions? =
    (applicationContext as? JarnGameOptionsProvider)?.jarnGameOptions
