package jr.brian.home.esde.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.core.model.ScreenHost
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.RecentlyPlayedManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.launch.LaunchFailure
import jr.brian.home.esde.launch.RomLaunchEngine
import jr.brian.home.esde.launch.RomLaunchHost
import jr.brian.home.esde.launch.RomLaunchOutcome
import jr.brian.home.esde.model.GameInfo

fun LaunchFailure.message(): String = when (this) {
    is LaunchFailure.EmulatorNotInstalled -> "Not installed: $packageName"
    LaunchFailure.NoEmulatorConfigured -> "No emulator set for this game — pick one from its details"
    is LaunchFailure.NoHandler -> hint ?: "$packageName wouldn't accept this ROM"
    is LaunchFailure.UnreadableRom ->
        "$packageName didn't accept this ROM — check that both this app and the emulator have access to the ROMs folder"
    is LaunchFailure.Unknown -> "Failed to launch: ${cause.message}"
}

/** Classic's presentation of [RomLaunchEngine]: toasts, the SAF picker, and host finishing. */
class RomGameLauncher(
    private val activity: ComponentActivity,
    esdePrefs: ESDEPreferencesManager,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    recentlyPlayedManager: RecentlyPlayedManager,
    private val romSearchStateHolder: RomSearchStateHolder,
    private val onSignalGameLaunch: () -> Unit,
    private val onLaunchSafPicker: (Uri?) -> Unit
) {
    private val engine = RomLaunchEngine(
        esdePrefs,
        appDisplayPreferenceManager,
        recentlyPlayedManager,
    )

    private val host = object : RomLaunchHost {
        override val activity: Activity get() = this@RomGameLauncher.activity

        override fun onCompanionChoice(packages: List<String>) {
            romSearchStateHolder.showCompanionLaunchPickerSignal.tryEmit(packages)
        }

        override fun onGameLaunching() = onSignalGameLaunch()

        override fun onLaunchStarted() = finishIfOnPrimary()
    }

    var pendingGameLaunch: Pair<GameInfo, Context>? = null

    /**
     * Pinned-slot game-launch rule: only the host that owns the
     * primary display finishes so the game can land there; the other
     * host stays visible. On FRONTEND_ON_BOTTOM that keeps the
     * frontend interactive on the external display while the game
     * runs on primary — the dual-screen value prop.
     */
    private fun finishIfOnPrimary() {
        (activity as? ScreenHost)?.finishIfOn(PRIMARY_DISPLAY_ID)
            ?: activity.finish()
    }

    fun launchGame(
        game: GameInfo,
        context: Context,
        displayPreference: AppDisplayPreferenceManager.DisplayPreference
    ) {
        present(engine.launchGame(host, game, context, displayPreference), context)
    }

    fun launchGameWithEmulator(game: GameInfo, emulatorPackage: String) {
        present(engine.launchGameWithEmulator(host, game, emulatorPackage), activity)
    }

    private fun present(outcome: RomLaunchOutcome, context: Context) {
        when (outcome) {
            is RomLaunchOutcome.Launched -> Unit
            is RomLaunchOutcome.AppNotInstalled -> {
                Toast.makeText(
                    context,
                    "App not installed: ${outcome.packageName}",
                    Toast.LENGTH_SHORT
                ).show()
                finishIfOnPrimary()
            }
            RomLaunchOutcome.RomPathNotFound ->
                Toast.makeText(context, "ROM path not found", Toast.LENGTH_SHORT).show()
            is RomLaunchOutcome.NeedsFileAccess -> {
                pendingGameLaunch = outcome.game to context
                onLaunchSafPicker(outcome.pickerHint)
            }
            RomLaunchOutcome.IntentBuildFailed ->
                Toast.makeText(context, "Failed to build launch intent", Toast.LENGTH_SHORT).show()
            is RomLaunchOutcome.Failed -> fail(context, outcome.failure)
        }
    }

    private fun fail(context: Context, failure: LaunchFailure) {
        Log.e(TAG, "Launch failed: $failure")
        Toast.makeText(context, failure.message(), Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val TAG = "RomGameLauncher"
    }
}
