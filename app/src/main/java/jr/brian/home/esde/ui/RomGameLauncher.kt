package jr.brian.home.esde.ui

import jr.brian.home.esde.data.*
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
import jr.brian.home.esde.util.EmulatorRegistry
import jr.brian.home.esde.util.EsdeCommandLauncher
import jr.brian.home.esde.util.buildAetherDocUri
import jr.brian.home.esde.util.buildSafDocumentUri
import jr.brian.home.esde.util.gameKey
import jr.brian.home.esde.util.resolveRomPath
import jr.brian.home.esde.util.sdCardVolumeId
import android.content.pm.PackageManager
import android.provider.DocumentsContract
import jr.brian.home.ui.util.PRIMARY_DISPLAY_ID
import jr.brian.home.util.launchApp
import java.io.File

sealed interface LaunchFailure {
    data class EmulatorNotInstalled(val packageName: String) : LaunchFailure
    data object NoEmulatorConfigured : LaunchFailure
    data class NoHandler(val packageName: String, val hint: String?) : LaunchFailure
    data class UnreadableRom(val packageName: String, val uri: Uri) : LaunchFailure
    data class Unknown(val cause: Throwable) : LaunchFailure
}

fun LaunchFailure.message(): String = when (this) {
    is LaunchFailure.EmulatorNotInstalled -> "Not installed: $packageName"
    LaunchFailure.NoEmulatorConfigured -> "No emulator set for this game — pick one from its details"
    is LaunchFailure.NoHandler -> hint ?: "$packageName wouldn't accept this ROM"
    is LaunchFailure.UnreadableRom ->
        "$packageName didn't accept this ROM — check that both this app and the emulator have access to the ROMs folder"
    is LaunchFailure.Unknown -> "Failed to launch: ${cause.message}"
}

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
                Log.d(TAG, "Requesting SAF tree for system=${game.systemName}")
                pendingGameLaunch = game to context
                onLaunchSafPicker(safPickerHintFor(romPath))
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
                Log.w(TAG, "FileProvider failed, using file URI", e)
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
            Log.e(TAG, "ROM path not resolved | system=${game.systemName} path=${game.path}")
            Toast.makeText(context, "ROM path not found", Toast.LENGTH_SHORT).show()
            return
        }

        val pkg = esdePrefs.getGameEmulator(gameKey(game))
            ?: game.emulatorPackage
            ?: EmulatorRegistry
                .candidatesForExtension(context, File(romPath).extension)
                .firstOrNull()
                ?.packageName
            ?: run { fail(context, LaunchFailure.NoEmulatorConfigured); return }

        val corePath =
            if (pkg.startsWith("com.retroarch")) esdePrefs.getGameCore(gameKey(game)) else null

        launchWithPackage(
            game = game,
            context = context,
            pkg = pkg,
            romPath = romPath,
            corePath = corePath,
            useSavedCommand = true,
        )
    }

    fun launchGameWithEmulator(game: GameInfo, emulatorPackage: String) {
        esdePrefs.setGameEmulator(gameKey(game), emulatorPackage)
        val romPath = resolveRomPath(game, esdePrefs.state.value.romsPaths) ?: run {
            Log.e(
                TAG,
                "launchGameWithEmulator: ROM path could not be resolved | systemName=${game.systemName} path=${game.path}"
            )
            Toast.makeText(activity, "ROM path not found", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d(TAG, "launchGameWithEmulator | pkg=$emulatorPackage | rom=$romPath")
        launchWithPackage(
            game = game,
            context = activity,
            pkg = emulatorPackage,
            romPath = romPath,
            corePath = null,
            useSavedCommand = false,
        )
    }

    /**
     * The shared launch sequence. Both entry points differ only in whether the
     * emulator package came from prefs or from the picker, and whether a saved
     * ES-DE command should be consulted.
     */
    private fun launchWithPackage(
        game: GameInfo,
        context: Context,
        pkg: String,
        romPath: String,
        corePath: String?,
        useSavedCommand: Boolean,
    ) {
        if (!EmulatorRegistry.isInstalled(context, pkg)) {
            fail(context, LaunchFailure.EmulatorNotInstalled(pkg))
            return
        }

        val needsExternalStorageUri =
            EmulatorRegistry.resolve(pkg)?.needsExternalStorageUri == true

        val effectiveContentUri: Uri = if (needsExternalStorageUri) {
            val aetherSafUri = buildAetherDocUri(
                game.systemName, romPath, esdePrefs::getSafTreeUri, context
            )
            if (aetherSafUri == null) {
                pendingGameLaunch = game to context
                onLaunchSafPicker(safPickerHintFor(romPath))
                return
            }
            aetherSafUri
        } else {
            resolveContentUri(game, romPath, context) ?: return
        }

        // Multi-file ROMs (.cue+.bin, .m3u+chunks) need the emulator to read siblings of the
        // entry-point file. A per-file URI grant only covers the file we point it at, so forward
        // our persistable tree grant too — the emulator can then resolve references inside the
        // same ROM directory.
        if (needsExternalStorageUri) {
            esdePrefs.getSafTreeUri(game.systemName)?.let { treeUriStr ->
                try {
                    context.grantUriPermission(
                        pkg,
                        treeUriStr.toUri(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
            }
        }

        // Issue the per-file read grant here so any downstream diagnostic can
        // observe the outcome. The intent builders call grantUriPermission
        // again for the same (pkg, uri) — that is idempotent.
        if (effectiveContentUri.scheme == "content") {
            try {
                context.grantUriPermission(
                    pkg,
                    effectiveContentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (e: Exception) {
                Log.w(TAG, "grantUriPermission($pkg, $effectiveContentUri) failed", e)
            }
        }

        warnIfNotObviouslyReadable(context, pkg, effectiveContentUri)

        if (useSavedCommand) {
            val savedCommand = esdePrefs.getGameLaunchCommand(gameKey(game))
            if (savedCommand != null) {
                launchWithSavedCommand(context, savedCommand, romPath, effectiveContentUri)
                return
            }
        }

        Log.d(TAG, "launchGame | pkg=$pkg rom=$romPath uri=$effectiveContentUri")

        val candidates = listOf(
            EsdeCommandLauncher.buildRomIntentFromPackage(
                pkg, romPath, effectiveContentUri, context, corePath, withComponent = true
            ),
            EsdeCommandLauncher.buildRomIntentFromPackage(
                pkg, romPath, effectiveContentUri, context, corePath, withComponent = false
            ),
        )
        onSignalGameLaunch()
        val failure = startFirstThatOpens(candidates, pkg)
        if (failure != null) {
            fail(context, failure)
            return
        }
        activity.finish()
    }

    private fun launchWithSavedCommand(
        context: Context,
        savedCommand: String,
        romPath: String,
        contentUri: Uri,
    ) {
        val findRulesFile = File(
            activity.filesDir.parent ?: "",
            "ES-DE/custom_systems/es_find_rules.xml"
        ).let { f ->
            if (f.exists()) f
            else File("/storage/emulated/0/ES-DE/custom_systems/es_find_rules.xml")
        }
        val customRules = EsdeCommandLauncher.parseCustomRules(findRulesFile)
        val intent = EsdeCommandLauncher.buildIntent(
            savedCommand, romPath, context, customRules, contentUri
        ) ?: run {
            Toast.makeText(context, "Failed to build launch intent", Toast.LENGTH_SHORT).show()
            return
        }
        val options = ActivityOptions.makeBasic().apply { launchDisplayId = PRIMARY_DISPLAY_ID }
        Log.d(TAG, "launchGame (command) | cmd=$savedCommand rom=$romPath")
        onSignalGameLaunch()
        try {
            activity.startActivity(intent, options.toBundle())
            activity.finish()
        } catch (e: Exception) {
            Log.e(TAG, "Saved-command launch failed", e)
            fail(context, LaunchFailure.Unknown(e))
        }
    }

    /**
     * Starts the first candidate that opens.
     *
     * Two attempts, in preference order: with the spec's explicit activity, then
     * without it. A stored activity name is a snapshot and a build that updated
     * since may have renamed it — asking the package what it handles now costs
     * one intent and fixes it.
     *
     * There is deliberately no fall back to getLaunchIntentForPackage. Opening
     * the emulator on its own file browser looks like a successful launch that
     * did nothing, which is why this feature reads as broken rather than as
     * failing.
     */
    private fun startFirstThatOpens(
        candidates: List<Intent>,
        packageName: String,
    ): LaunchFailure? {
        val options = ActivityOptions.makeBasic()
            .apply { launchDisplayId = PRIMARY_DISPLAY_ID }
            .toBundle()
        var failure: LaunchFailure? = null
        for (intent in candidates) {
            failure = startOrNull(intent, options) ?: return null
        }
        // Then anywhere the system will have it. Pinning to a display is a
        // request, not a right, and a game on the near panel beats no game.
        for (intent in candidates) {
            if (startOrNull(intent, null) == null) {
                Log.i(TAG, "$packageName refused the target display; opened on the default one")
                return null
            }
        }
        return failure
    }

    private fun startOrNull(intent: Intent, options: android.os.Bundle?): LaunchFailure? = try {
        activity.startActivity(intent, options)
        null
    } catch (e: android.content.ActivityNotFoundException) {
        Log.w(TAG, "Nothing handles ${intent.component ?: intent.`package`}", e)
        LaunchFailure.NoHandler(
            intent.`package` ?: "unknown",
            EsdeCommandLauncher.undocumentedHint(intent.`package` ?: ""),
        )
    } catch (e: SecurityException) {
        // The receiver refused, typically because our URI grant did not carry
        // and it also lacked its own way to open the file. Callers see the
        // "check that both apps have access" message.
        Log.w(TAG, "Not permitted to start ${intent.component}", e)
        val pkg = intent.`package` ?: intent.component?.packageName
        val uri = intent.data ?: intent.clipData?.getItemAt(0)?.uri
        if (pkg != null && uri != null) LaunchFailure.UnreadableRom(pkg, uri)
        else LaunchFailure.Unknown(e)
    } catch (e: IllegalStateException) {
        // Some ROMs report a refused display placement this way rather than as a
        // SecurityException. Uncaught it escapes the whole launch path.
        Log.w(TAG, "Refused to start ${intent.component}", e)
        LaunchFailure.Unknown(e)
    }

    /**
     * A DocumentsUI hint pointing at the ROM's own directory, so the picker
     * opens where the user needs to grant. Returns null when the path is not
     * on external-storage-provider-addressable storage.
     */
    private fun safPickerHintFor(romPath: String): Uri? {
        val romDir = File(romPath).parent ?: return null
        val (volumeId, rel) = when {
            romDir == "/storage/emulated/0" -> "primary" to ""
            romDir.startsWith("/storage/emulated/0/") ->
                "primary" to romDir.removePrefix("/storage/emulated/0/")
            romDir.startsWith("/storage/") -> {
                val after = romDir.removePrefix("/storage/")
                val slash = after.indexOf('/')
                if (slash < 0) after to ""
                else after.substring(0, slash) to after.substring(slash + 1)
            }
            else -> return null
        }
        return "content://com.android.externalstorage.documents/document/${Uri.encode("$volumeId:$rel")}".toUri()
    }

    /**
     * Advisory diagnostic — never blocks the launch. Two things can go wrong
     * that we might be able to inspect from here:
     *
     * - The document URI doesn't resolve in our ContentResolver. That can mean
     *   the file is gone or our persisted grant is stale.
     * - checkUriPermission returns anything other than GRANTED for the target
     *   package's uid. That only means no per-URI grant has been recorded for
     *   this exact URI — the emulator may still open the file via its own
     *   persisted tree grant on a different tree, or via broad storage access
     *   on an older target SDK.
     *
     * Either signal is worth writing to logcat so a real failure downstream
     * has context, but neither is grounds for refusing to start the activity.
     */
    private fun warnIfNotObviouslyReadable(context: Context, pkg: String, uri: Uri) {
        if (uri.scheme != "content") return
        val resolves = try {
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null, null, null
            )?.use { it.moveToFirst() } == true
        } catch (e: Exception) {
            Log.w(TAG, "Query for $uri failed — launching anyway", e)
            return
        }
        if (!resolves) {
            Log.w(TAG, "URI $uri did not resolve in our resolver — launching anyway; " +
                "the emulator may still read it via its own tree grant")
            return
        }
        val uid = try {
            context.packageManager.getPackageUid(pkg, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Cannot resolve uid for $pkg — launching anyway", e)
            return
        }
        val granted = context.checkUriPermission(
            uri, -1, uid, Intent.FLAG_GRANT_READ_URI_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Log.w(TAG, "No per-URI grant recorded for $pkg on $uri — launching anyway; " +
                "the emulator may still read via its own tree grant or all-files access")
        }
    }

    private fun fail(context: Context, failure: LaunchFailure) {
        Log.e(TAG, "Launch failed: $failure")
        Toast.makeText(context, failure.message(), Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "RomGameLauncher"
    }
}
