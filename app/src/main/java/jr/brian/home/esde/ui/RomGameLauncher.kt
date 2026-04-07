package jr.brian.home.esde.ui

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.EsdeCommandLauncher
import jr.brian.home.esde.util.buildAetherDocUri
import jr.brian.home.esde.util.buildSafDocumentUri
import jr.brian.home.esde.util.gameKey
import jr.brian.home.esde.util.resolveRomPath
import jr.brian.home.esde.util.sdCardVolumeId
import jr.brian.home.util.launchApp
import java.io.File

class RomGameLauncher(
    private val activity: ComponentActivity,
    private val esdePrefs: ESDEPreferencesManager,
    private val onSignalGameLaunch: () -> Unit,
    private val onLaunchSafPicker: (Uri?) -> Unit
) {
    var pendingGameLaunch: Pair<GameInfo, Context>? = null

    fun resolveAndroidAppByLabel(label: String): Intent? {
        val pm = activity.packageManager
        return pm.getInstalledApplications(0).firstOrNull { appInfo ->
            pm.getApplicationLabel(appInfo).toString().equals(label, ignoreCase = true)
        }?.let { appInfo ->
            pm.getLaunchIntentForPackage(appInfo.packageName)
        }
    }

    fun resolveContentUri(game: GameInfo, romPath: String, context: Context): Uri? {
        val volumeId = sdCardVolumeId(romPath)
        return if (volumeId != null) {
            val safUri =
                buildSafDocumentUri(romPath, volumeId, game.systemName, esdePrefs::getSafTreeUri)
            if (safUri == null) {
                Log.d("RomSearchResults", "Requesting SAF tree for system=${game.systemName}")
                pendingGameLaunch = game to context
                val romDir = File(romPath).parent ?: "/storage/$volumeId"
                val relDir = romDir.removePrefix("/storage/$volumeId/")
                val hint =
                    "content://com.android.externalstorage.documents/document/${Uri.encode("$volumeId:$relDir")}".toUri()
                onLaunchSafPicker(hint)
                null
            } else safUri
        } else {
            try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    File(romPath)
                )
            } catch (e: Exception) {
                Log.w("RomSearchResults", "FileProvider failed, using file URI", e)
                Uri.fromFile(File(romPath))
            }
        }
    }

    fun launchGame(
        game: GameInfo,
        context: Context,
        displayPreference: AppDisplayPreferenceManager.DisplayPreference
    ) {
        if (game.systemName.equals("androidgames", ignoreCase = true) ||
            game.systemName.equals("androidapps", ignoreCase = true)
        ) {
            val key = game.path.trimEnd('/').removeSuffix(".app")
            val intent = activity.packageManager.getLaunchIntentForPackage(key)
                ?: resolveAndroidAppByLabel(key)
            if (intent != null) {
                onSignalGameLaunch()
                launchApp(
                    context = context,
                    packageName = key,
                    displayPreference = displayPreference
                )
            } else {
                Toast.makeText(context, "App not installed: $key", Toast.LENGTH_SHORT).show()
            }
            activity.finish()
            return
        }

        val romPath = resolveRomPath(game, esdePrefs.state.value.romsPaths) ?: run {
            Log.e(
                "RomSearchResults",
                "ROM path not resolved | system=${game.systemName} path=${game.path}"
            )
            Toast.makeText(context, "ROM path not found", Toast.LENGTH_SHORT).show()
            return
        }

        val pkg = esdePrefs.getGameEmulator(gameKey(game))
            ?: game.emulatorPackage
            ?: run {
                val knownFallbacks = listOf(
                    "org.ppsspp.ppsspp",
                    "org.ppsspp.ppssppgold",
                    "xyz.aethersx2.android"
                )
                val fallbackPkg = knownFallbacks.firstOrNull { p ->
                    activity.packageManager.getLaunchIntentForPackage(p) != null
                }
                if (fallbackPkg != null) {
                    val intent = activity.packageManager.getLaunchIntentForPackage(fallbackPkg)!!
                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    val options = ActivityOptions.makeBasic().apply { launchDisplayId = 0 }
                    onSignalGameLaunch()
                    activity.startActivity(intent, options.toBundle())
                    activity.finish()
                } else {
                    Toast.makeText(
                        context,
                        "No emulator configured for this game",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }

        if (pkg == "org.ppsspp.ppsspp" || pkg == "org.ppsspp.ppssppgold" || pkg == "xyz.aethersx2.android") {
            val intent = activity.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val options = ActivityOptions.makeBasic().apply { launchDisplayId = 0 }
                onSignalGameLaunch()
                activity.startActivity(intent, options.toBundle())
                activity.finish()
            } else {
                Toast.makeText(context, "Emulator not installed: $pkg", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val savedCommand = esdePrefs.getGameLaunchCommand(gameKey(game))
        if (savedCommand != null) {
            val findRulesFile =
                File(
                    activity.filesDir.parent ?: "",
                    "ES-DE/custom_systems/es_find_rules.xml"
                ).let { f ->
                    if (f.exists()) f else File("/storage/emulated/0/ES-DE/custom_systems/es_find_rules.xml")
                }
            val customRules = EsdeCommandLauncher.parseCustomRules(findRulesFile)
            val intent =
                EsdeCommandLauncher.buildIntent(savedCommand, romPath, context, customRules)
                    ?: run {
                        Toast.makeText(context, "Failed to build launch intent", Toast.LENGTH_SHORT)
                            .show()
                        return
                    }
            val options = ActivityOptions.makeBasic().apply { launchDisplayId = 0 }
            Log.d("RomSearchResults", "launchGame (command) | cmd=$savedCommand rom=$romPath")
            onSignalGameLaunch()
            activity.startActivity(intent, options.toBundle())
            activity.finish()
            return
        }

        val corePath =
            if (pkg.startsWith("com.retroarch")) esdePrefs.getGameCore(gameKey(game)) else null
        val effectiveContentUri: Uri = resolveContentUri(game, romPath, context) ?: return

        Log.d("RomSearchResults", "launchGame | pkg=$pkg rom=$romPath uri=$effectiveContentUri")

        try {
            val romIntent = EsdeCommandLauncher.buildRomIntentFromPackage(
                pkg, romPath, effectiveContentUri, context, corePath
            )
            val options = ActivityOptions.makeBasic().apply { launchDisplayId = 0 }
            val canHandleRom = activity.packageManager.resolveActivity(romIntent, 0) != null
            val intent = if (canHandleRom) {
                romIntent
            } else {
                activity.packageManager.getLaunchIntentForPackage(pkg)
                    ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    ?: run {
                        Toast.makeText(context, "App not installed: $pkg", Toast.LENGTH_SHORT)
                            .show()
                        return
                    }
            }
            onSignalGameLaunch()
            activity.startActivity(intent, options.toBundle())
            activity.finish()
        } catch (e: Exception) {
            Log.e("RomSearchResults", "Failed to launch $pkg", e)
            Toast.makeText(context, "Failed to launch: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchGameWithEmulator(game: GameInfo, emulatorPackage: String) {
        esdePrefs.setGameEmulator(gameKey(game), emulatorPackage)
        val romPath = resolveRomPath(game, esdePrefs.state.value.romsPaths) ?: run {
            Log.e(
                "RomSearchResults",
                "launchGameWithEmulator: ROM path could not be resolved | systemName=${game.systemName} path=${game.path}"
            )
            Toast.makeText(activity, "ROM path not found", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("RomSearchResults", "launchGameWithEmulator | pkg=$emulatorPackage | rom=$romPath")
        val contentUri: Uri
        if (emulatorPackage == "xyz.aethersx2.android") {
            val aetherSafUri = buildAetherDocUri(game.systemName, romPath, esdePrefs::getSafTreeUri)
            if (aetherSafUri == null) {
                pendingGameLaunch = game to activity
                val romDir = File(romPath).parent ?: "/storage/emulated/0"
                val rel = romDir.removePrefix("/storage/emulated/0/")
                val hint =
                    "content://com.android.externalstorage.documents/document/${Uri.encode("primary:$rel")}".toUri()
                onLaunchSafPicker(hint)
                return
            }
            contentUri = aetherSafUri
        } else {
            contentUri = resolveContentUri(game, romPath, activity) ?: return
        }
        try {
            val romIntent = EsdeCommandLauncher.buildRomIntentFromPackage(
                packageName = emulatorPackage,
                romAbsPath = romPath,
                contentUri = contentUri,
                context = activity
            )
            Log.d(
                "RomSearchResults",
                "  → intent data=${romIntent.data} extras=${romIntent.extras}"
            )
            val options = ActivityOptions.makeBasic().apply { launchDisplayId = 0 }
            val canHandleRom = activity.packageManager.resolveActivity(romIntent, 0) != null
            val intent = if (canHandleRom) {
                romIntent
            } else {
                activity.packageManager.getLaunchIntentForPackage(emulatorPackage)
                    ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    ?: run {
                        Toast.makeText(
                            activity,
                            "App not installed: $emulatorPackage",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
            }
            onSignalGameLaunch()
            activity.startActivity(intent, options.toBundle())
            activity.finish()
        } catch (e: Exception) {
            Log.e("RomSearchResults", "Emulator picker launch failed for $emulatorPackage", e)
            Toast.makeText(activity, "Failed to launch: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
