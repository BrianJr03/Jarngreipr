package jr.brian.jarnfe.launch

import android.app.Activity
import android.content.Context

data class JarnGameRef(
    val systemName: String,
    val path: String,
    val title: String,
    val absolutePath: String? = null,
    val emulatorPackage: String? = null,
    val launchCommand: String? = null,
    val artworkPath: String? = null,
    val sourceTreeUri: String? = null,
)

sealed interface JarnLaunchTarget {
    /** The display Classic would use for this game. */
    data object ClassicDefault : JarnLaunchTarget

    data class Display(val displayId: Int) : JarnLaunchTarget
}

/** A picker answer. It is saved to the game exactly as Classic's picker saves it. */
data class JarnEmulatorChoice(
    val packageName: String,
    val command: String? = null,
    val corePath: String? = null,
)

data class JarnLaunchRequest(
    val game: JarnGameRef,
    val target: JarnLaunchTarget = JarnLaunchTarget.ClassicDefault,
    val emulatorChoice: JarnEmulatorChoice? = null,
)

enum class JarnEmulatorChoiceKind { Emulator, RetroArchCore }

enum class JarnLaunchFailure {
    EmulatorNotInstalled,
    AppNotInstalled,
    EmulatorRejected,
    RomNotReadable,
    Unknown,
}

sealed interface JarnLaunchResult {
    /** [companionChoices] is non-empty when the caller must ask which companion to open. */
    data class Launched(
        val packageName: String,
        val companionChoices: List<String> = emptyList(),
    ) : JarnLaunchResult

    data class NeedsEmulatorChoice(val kind: JarnEmulatorChoiceKind) : JarnLaunchResult

    /** Open a tree picker at [pickerHint], then call [JarnGameLauncher.grantFileAccess]. */
    data class NeedsFileAccess(val systemName: String, val pickerHint: String?) : JarnLaunchResult

    data object MissingRom : JarnLaunchResult

    data class Failed(
        val reason: JarnLaunchFailure,
        val packageName: String? = null,
    ) : JarnLaunchResult
}

/** [Unknown] when the file can't be checked, for example without all-files access. */
enum class JarnGameFileStatus { Present, Missing, Unknown }

/** Launches games without showing UI. Every outcome the caller must handle is a result. */
interface JarnGameLauncher {
    fun launch(host: Activity, request: JarnLaunchRequest): JarnLaunchResult

    /** Checks the game's file without launching it. Unknown is never treated as missing. */
    fun fileStatus(game: JarnGameRef): JarnGameFileStatus = JarnGameFileStatus.Unknown

    /** Returns false when the grant could not be saved. Retry [launch] after true. */
    fun grantFileAccess(host: Activity, game: JarnGameRef, treeUri: String): Boolean
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnGameLauncherProvider {
    val jarnGameLauncher: JarnGameLauncher
}

fun Context.jarnGameLauncherOrNull(): JarnGameLauncher? =
    (applicationContext as? JarnGameLauncherProvider)?.jarnGameLauncher
