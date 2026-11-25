package jr.brian.home.esde.launch

import android.app.Activity
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.JinglesManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.getGameCore
import jr.brian.home.esde.data.getGameEmulator
import jr.brian.home.esde.data.getGameLaunchCommand
import jr.brian.home.esde.data.getSystemEmulator
import jr.brian.home.esde.data.setGameCore
import jr.brian.home.esde.data.setGameEmulator
import jr.brian.home.esde.data.setGameLaunchCommand
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.gameKey
import jr.brian.home.esde.util.persistSafTreeForSystem
import jr.brian.home.esde.util.resolveRomPath
import jr.brian.jarnfe.launch.JarnEmulatorChoice
import jr.brian.jarnfe.launch.JarnEmulatorChoiceKind
import jr.brian.jarnfe.launch.JarnGameFileStatus
import jr.brian.jarnfe.launch.JarnGameLauncher
import jr.brian.jarnfe.launch.JarnGameRef
import jr.brian.jarnfe.launch.JarnLaunchFailure
import jr.brian.jarnfe.launch.JarnLaunchRequest
import jr.brian.jarnfe.launch.JarnLaunchResult
import jr.brian.jarnfe.launch.JarnLaunchTarget
import java.io.File
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val TAG = "AppJarnGameLauncher"

/**
 * File.exists() can be false for a readable ROM under scoped storage, so absence only
 * counts as missing when this app has all-files access.
 */
internal fun isRomDefinitelyMissing(canSeeAllFiles: Boolean, exists: Boolean): Boolean =
    canSeeAllFiles && !exists

/** Present only when the file was really seen, so a scoped-storage blind spot stays Unknown. */
internal fun romFileStatus(missing: Boolean, canSeeAllFiles: Boolean): JarnGameFileStatus = when {
    missing -> JarnGameFileStatus.Missing
    canSeeAllFiles -> JarnGameFileStatus.Present
    else -> JarnGameFileStatus.Unknown
}

/** [gameDisplayId] is where the engine sent the game, so Jarn knows which of its windows it covers. */
internal fun RomLaunchOutcome.toJarnResult(companionChoices: List<String>, gameDisplayId: Int? = null): JarnLaunchResult =
    when (this) {
        is RomLaunchOutcome.Launched -> JarnLaunchResult.Launched(packageName, companionChoices, gameDisplayId)
        is RomLaunchOutcome.AppNotInstalled ->
            JarnLaunchResult.Failed(JarnLaunchFailure.AppNotInstalled, packageName)
        RomLaunchOutcome.RomPathNotFound -> JarnLaunchResult.MissingRom
        is RomLaunchOutcome.NeedsFileAccess ->
            JarnLaunchResult.NeedsFileAccess(game.systemName, pickerHint?.toString())
        RomLaunchOutcome.IntentBuildFailed -> JarnLaunchResult.Failed(JarnLaunchFailure.Unknown)
        is RomLaunchOutcome.Failed -> failure.toJarnResult()
    }

private fun LaunchFailure.toJarnResult(): JarnLaunchResult = when (this) {
    LaunchFailure.NoEmulatorConfigured ->
        JarnLaunchResult.NeedsEmulatorChoice(JarnEmulatorChoiceKind.Emulator)
    is LaunchFailure.EmulatorNotInstalled ->
        JarnLaunchResult.Failed(JarnLaunchFailure.EmulatorNotInstalled, packageName)
    is LaunchFailure.NoHandler ->
        JarnLaunchResult.Failed(JarnLaunchFailure.EmulatorRejected, packageName)
    is LaunchFailure.UnreadableRom ->
        JarnLaunchResult.Failed(JarnLaunchFailure.RomNotReadable, packageName)
    is LaunchFailure.Unknown -> JarnLaunchResult.Failed(JarnLaunchFailure.Unknown)
}

@Singleton
class AppJarnGameLauncher @Inject constructor(
    private val engine: RomLaunchEngine,
    private val esdePrefs: ESDEPreferencesManager,
    private val appDisplayPreferenceManager: AppDisplayPreferenceManager,
    private val romSearchStateHolder: RomSearchStateHolder,
    private val jinglesManager: Provider<JinglesManager>,
) : JarnGameLauncher {

    override fun launch(host: Activity, request: JarnLaunchRequest): JarnLaunchResult {
        val game = resolveGame(request.game)
        request.emulatorChoice?.let { saveChoice(game, it) }
        gateResult(game)?.let { return it }
        if (isMissing(game) || isMissingFromFolder(game, request.game)) {
            return JarnLaunchResult.MissingRom
        }

        val companionChoices = mutableListOf<String>()
        LastGameDisplay.id = null
        val outcome = engine.launchGame(
            host = launchHost(host, companionChoices),
            game = game,
            context = host,
            displayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                esdePrefs.getGameEmulator(gameKey(game)) ?: game.emulatorPackage ?: game.path
            ),
            options = RomLaunchOptions(
                displayOverride = (request.target as? JarnLaunchTarget.Display)?.displayId,
                strictAppStart = true,
                fallbackSafTreeUri = request.game.sourceTreeUri,
            ),
        )
        return outcome.toJarnResult(companionChoices, LastGameDisplay.id)
    }

    override fun grantFileAccess(host: Activity, game: JarnGameRef, treeUri: String): Boolean =
        runCatching {
            persistSafTreeForSystem(
                host,
                esdePrefs,
                game.systemName,
                treeUri.toUri(),
                registerParentRoot = false,
            )
        }.onFailure { Log.w(TAG, "Could not save the folder grant", it) }.isSuccess

    override fun fileStatus(game: JarnGameRef): JarnGameFileStatus {
        val resolved = resolveGame(game)
        if (isAndroidSystem(resolved.systemName)) return JarnGameFileStatus.Unknown
        return romFileStatus(
            missing = isMissing(resolved) || isMissingFromFolder(resolved, game),
            canSeeAllFiles = runCatching { Environment.isExternalStorageManager() }.getOrDefault(false),
        )
    }

    private fun launchHost(host: Activity, companionChoices: MutableList<String>) =
        object : RomLaunchHost {
            override val activity: Activity = host

            override fun onCompanionChoice(packages: List<String>) {
                companionChoices += packages
            }

            override fun onGameLaunching() {
                jinglesManager.get().onGameLaunched()
                romSearchStateHolder.gameLaunchSignal.tryEmit(Unit)
            }

            override fun onLaunchStarted() = Unit
        }

    private fun resolveGame(ref: JarnGameRef): GameInfo =
        romSearchStateHolder.allGames.value.firstOrNull {
            it.systemName == ref.systemName && it.path == ref.path
        }?.let { it.copy(romAbsolutePath = it.romAbsolutePath ?: ref.absolutePath) } ?: GameInfo(
            path = ref.path,
            name = ref.title,
            systemName = ref.systemName,
            romAbsolutePath = ref.absolutePath,
            emulatorPackage = ref.emulatorPackage,
            launchCommand = ref.launchCommand,
            artworkPath = ref.artworkPath,
        )

    private fun saveChoice(game: GameInfo, choice: JarnEmulatorChoice) {
        val key = gameKey(game)
        esdePrefs.setGameEmulator(key, choice.packageName)
        choice.command?.let { esdePrefs.setGameLaunchCommand(key, it) }
        choice.corePath?.let { esdePrefs.setGameCore(key, it) }
    }

    private fun gateResult(game: GameInfo): JarnLaunchResult? {
        val key = gameKey(game)
        val savedEmulator = esdePrefs.getGameEmulator(key)
        val gate = romLaunchGate(
            isAndroid = isAndroidSystem(game.systemName),
            isRetroArch = (savedEmulator ?: esdePrefs.getSystemEmulator(game.systemName) ?: game.emulatorPackage)
                ?.startsWith("com.retroarch") == true,
            hasCore = esdePrefs.getGameCore(key) != null,
            hasEmulator = esdePrefs.getGameLaunchCommand(key) != null ||
                savedEmulator != null ||
                esdePrefs.getSystemEmulator(game.systemName) != null ||
                game.launchCommand != null ||
                game.emulatorPackage != null,
        )
        return when (gate) {
            RomLaunchGate.Launch -> null
            RomLaunchGate.PickCore ->
                JarnLaunchResult.NeedsEmulatorChoice(JarnEmulatorChoiceKind.RetroArchCore)
            RomLaunchGate.PickEmulator ->
                JarnLaunchResult.NeedsEmulatorChoice(JarnEmulatorChoiceKind.Emulator)
        }
    }

    /** A folder-picked game that could not be found there has no real path to hand an emulator. */
    private fun isMissingFromFolder(game: GameInfo, ref: JarnGameRef): Boolean {
        if (ref.sourceTreeUri == null || game.romAbsolutePath != null) return false
        val guessed = resolveRomPath(game, esdePrefs.state.value.romsPaths) ?: return true
        return !File(guessed).exists()
    }

    private fun isMissing(game: GameInfo): Boolean {
        if (isAndroidSystem(game.systemName)) return false
        val romPath = resolveRomPath(game, esdePrefs.state.value.romsPaths) ?: return true
        return isRomDefinitelyMissing(
            canSeeAllFiles = runCatching { Environment.isExternalStorageManager() }
                .getOrDefault(false),
            exists = File(romPath).exists(),
        )
    }
}
