package jr.brian.home.esde.launch

import android.app.Activity
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.data.RecentlyPlayedManager
import jr.brian.home.data.ScreenFocusLockWriter
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.getGameCompanions
import jr.brian.home.esde.data.getGameCore
import jr.brian.home.esde.data.getGameEmulator
import jr.brian.home.esde.data.getGameLaunchCommand
import jr.brian.home.esde.data.getSafTreeUri
import jr.brian.home.esde.data.getSystemEmulator
import jr.brian.home.esde.data.setGameEmulator
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.EmulatorRegistry
import jr.brian.home.esde.util.EsdeCommandLauncher
import jr.brian.home.esde.util.RomLaunchContract
import jr.brian.home.esde.util.buildAetherDocUri
import jr.brian.home.esde.util.buildSafDocumentUri
import jr.brian.home.esde.util.gameKey
import jr.brian.home.esde.util.resolveRomPath
import jr.brian.home.esde.util.sdCardVolumeId
import jr.brian.home.util.launchApp
import jr.brian.home.util.launchAppOnOppositeDisplay
import java.io.File
import javax.inject.Inject

sealed interface LaunchFailure {
    data class EmulatorNotInstalled(val packageName: String) : LaunchFailure
    data object NoEmulatorConfigured : LaunchFailure
    data class NoHandler(val packageName: String, val hint: String?) : LaunchFailure
    data class UnreadableRom(val packageName: String, val uri: Uri) : LaunchFailure
    data class Unknown(val cause: Throwable) : LaunchFailure
}

sealed interface RomLaunchOutcome {
    data class Launched(val packageName: String) : RomLaunchOutcome
    data class AppNotInstalled(val packageName: String) : RomLaunchOutcome
    data object RomPathNotFound : RomLaunchOutcome
    data class NeedsFileAccess(val game: GameInfo, val pickerHint: Uri?) : RomLaunchOutcome
    data object IntentBuildFailed : RomLaunchOutcome
    data class Failed(val failure: LaunchFailure) : RomLaunchOutcome
}

/** The caller-specific effects of a launch, invoked at fixed points in the sequence. */
interface RomLaunchHost {
    val activity: Activity
    fun onCompanionChoice(packages: List<String>)
    fun onGameLaunching()
    fun onLaunchStarted()
}

/**
 * @param displayOverride replaces the display Classic would pick.
 * @param strictAppStart starts Android games from the resolved intent and reports failure.
 *   Classic leaves this off to keep [launchApp]'s silent failure behavior.
 */
data class RomLaunchOptions(
    val displayOverride: Int? = null,
    val strictAppStart: Boolean = false,
)

enum class RomLaunchGate { Launch, PickCore, PickEmulator }

fun romLaunchGate(
    isAndroid: Boolean,
    isRetroArch: Boolean,
    hasCore: Boolean,
    hasEmulator: Boolean,
): RomLaunchGate = when {
    isAndroid -> RomLaunchGate.Launch
    isRetroArch && !hasCore -> RomLaunchGate.PickCore
    hasEmulator -> RomLaunchGate.Launch
    else -> RomLaunchGate.PickEmulator
}

fun isAndroidSystem(systemName: String): Boolean =
    systemName.equals("androidgames", ignoreCase = true) ||
        systemName.equals("androidapps", ignoreCase = true)

class RomLaunchEngine @Inject constructor(
    private val esdePrefs: ESDEPreferencesManager,
    private val appDisplayPreferenceManager: AppDisplayPreferenceManager,
    private val recentlyPlayedManager: RecentlyPlayedManager,
) {

    private fun maybeLaunchCompanion(host: RomLaunchHost, game: GameInfo, context: Context) {
        val installed = esdePrefs.getGameCompanions(gameKey(game))
            .filter {
                runCatching {
                    context.packageManager.getLaunchIntentForPackage(it) != null
                }.getOrDefault(false)
            }
        when (installed.size) {
            0 -> return
            1 -> launchCompanionPkg(installed[0], context)
            else -> host.onCompanionChoice(installed)
        }
    }

    private fun launchCompanionPkg(pkg: String, context: Context) {
        val pref = appDisplayPreferenceManager.getAppDisplayPreference(pkg)
        val hasExternalDisplay = runCatching {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            (dm?.displays?.size ?: 1) > 1
        }.getOrDefault(false)
        if (hasExternalDisplay) {
            launchAppOnOppositeDisplay(context, pkg, pref)
        } else {
            launchApp(context, pkg, pref)
        }
    }

    private fun resolveAndroidAppByLabel(activity: Activity, label: String): Intent? {
        val pm = activity.packageManager
        return pm.getInstalledApplications(0).firstOrNull { appInfo ->
            pm.getApplicationLabel(appInfo).toString().equals(label, ignoreCase = true)
        }?.let { appInfo ->
            pm.getLaunchIntentForPackage(appInfo.packageName)
        }
    }

    private fun resolveContentUri(
        game: GameInfo,
        romPath: String,
        context: Context,
    ): ContentUriResult {
        val volumeId = sdCardVolumeId(romPath)
        return if (volumeId != null) {
            val safUri =
                buildSafDocumentUri(romPath, volumeId, game.systemName, esdePrefs::getSafTreeUri)
            if (safUri == null) {
                Log.d(TAG, "Requesting SAF tree for system=${game.systemName}")
                ContentUriResult.NeedsAccess
            } else {
                ContentUriResult.Resolved(safUri)
            }
        } else {
            val uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    File(romPath)
                )
            } catch (e: Exception) {
                Log.w(TAG, "FileProvider failed, using file URI", e)
                Uri.fromFile(File(romPath))
            }
            ContentUriResult.Resolved(uri)
        }
    }

    fun launchGame(
        host: RomLaunchHost,
        game: GameInfo,
        context: Context,
        displayPreference: DisplayPreference,
        options: RomLaunchOptions = RomLaunchOptions(),
    ): RomLaunchOutcome {
        if (isAndroidSystem(game.systemName)) {
            return launchAndroidGame(host, game, context, displayPreference, options)
        }

        val romPath = resolveRomPath(game, esdePrefs.state.value.romsPaths) ?: run {
            Log.e(TAG, "ROM path not resolved | system=${game.systemName} path=${game.path}")
            return RomLaunchOutcome.RomPathNotFound
        }

        val pkg = esdePrefs.getGameEmulator(gameKey(game))
            ?: esdePrefs.getSystemEmulator(game.systemName)
            ?: game.emulatorPackage
            ?: EmulatorRegistry
                .candidatesForExtension(context, File(romPath).extension)
                .firstOrNull()
                ?.packageName
            ?: EsdeCommandLauncher.builtinSystemEmulator(context, game.systemName)
            ?: return RomLaunchOutcome.Failed(LaunchFailure.NoEmulatorConfigured)

        val corePath =
            if (pkg.startsWith("com.retroarch")) esdePrefs.getGameCore(gameKey(game)) else null

        return launchWithPackage(
            host = host,
            game = game,
            context = context,
            pkg = pkg,
            romPath = romPath,
            corePath = corePath,
            useSavedCommand = true,
            displayId = options.displayOverride ?: PRIMARY_DISPLAY_ID,
        )
    }

    fun launchGameWithEmulator(
        host: RomLaunchHost,
        game: GameInfo,
        emulatorPackage: String,
    ): RomLaunchOutcome {
        esdePrefs.setGameEmulator(gameKey(game), emulatorPackage)
        val romPath = resolveRomPath(game, esdePrefs.state.value.romsPaths) ?: run {
            Log.e(
                TAG,
                "launchGameWithEmulator: ROM path could not be resolved | systemName=${game.systemName} path=${game.path}"
            )
            return RomLaunchOutcome.RomPathNotFound
        }
        Log.d(TAG, "launchGameWithEmulator | pkg=$emulatorPackage | rom=$romPath")
        return launchWithPackage(
            host = host,
            game = game,
            context = host.activity,
            pkg = emulatorPackage,
            romPath = romPath,
            corePath = null,
            useSavedCommand = false,
            displayId = PRIMARY_DISPLAY_ID,
        )
    }

    private fun launchAndroidGame(
        host: RomLaunchHost,
        game: GameInfo,
        context: Context,
        displayPreference: DisplayPreference,
        options: RomLaunchOptions,
    ): RomLaunchOutcome {
        val key = game.path.trimEnd('/').removeSuffix(".app")
        val intent = host.activity.packageManager.getLaunchIntentForPackage(key)
            ?: resolveAndroidAppByLabel(host.activity, key)
            ?: return RomLaunchOutcome.AppNotInstalled(key)
        maybeLaunchCompanion(host, game, context)
        recentlyPlayedManager.record(game)
        host.onGameLaunching()
        if (options.strictAppStart) {
            val failure = startResolvedApp(
                context, intent, displayPreference, options.displayOverride
            )
            if (failure != null) return RomLaunchOutcome.Failed(failure)
        } else {
            launchApp(context = context, packageName = key, displayPreference = displayPreference)
        }
        host.onLaunchStarted()
        return RomLaunchOutcome.Launched(intent.`package` ?: intent.component?.packageName ?: key)
    }

    private fun startResolvedApp(
        context: Context,
        intent: Intent,
        displayPreference: DisplayPreference,
        displayOverride: Int?,
    ): LaunchFailure? {
        val displayId = displayOverride
            ?: PRIMARY_DISPLAY_ID.takeIf { displayPreference == DisplayPreference.PRIMARY_DISPLAY }
        return try {
            val bundle = displayId?.let {
                ActivityOptions.makeBasic().setLaunchDisplayId(it).toBundle()
            }
            context.startActivity(intent, bundle)
            val landedOn = displayId
                ?: runCatching { context.display?.displayId }.getOrNull()
                ?: PRIMARY_DISPLAY_ID
            ScreenFocusLockWriter.syncToDisplayId(context, landedOn)
            null
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Nothing handles ${intent.component ?: intent.`package`}", e)
            LaunchFailure.NoHandler(intent.`package` ?: intent.component?.packageName ?: "unknown", null)
        } catch (e: SecurityException) {
            Log.w(TAG, "Not permitted to start ${intent.component}", e)
            LaunchFailure.Unknown(e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Refused to start ${intent.component}", e)
            LaunchFailure.Unknown(e)
        }
    }

    /**
     * The shared launch sequence. Both entry points differ only in whether the
     * emulator package came from prefs or from the picker, and whether a saved
     * ES-DE command should be consulted.
     */
    private fun launchWithPackage(
        host: RomLaunchHost,
        game: GameInfo,
        context: Context,
        pkg: String,
        romPath: String,
        corePath: String?,
        useSavedCommand: Boolean,
        displayId: Int,
    ): RomLaunchOutcome {
        if (!EmulatorRegistry.isInstalled(context, pkg)) {
            return RomLaunchOutcome.Failed(LaunchFailure.EmulatorNotInstalled(pkg))
        }

        val needsExternalStorageUri =
            EmulatorRegistry.resolve(pkg)?.needsExternalStorageUri == true

        val effectiveContentUri: Uri = if (needsExternalStorageUri) {
            buildAetherDocUri(game.systemName, romPath, esdePrefs::getSafTreeUri, context)
                ?: return RomLaunchOutcome.NeedsFileAccess(game, safPickerHintFor(romPath))
        } else {
            when (val resolved = resolveContentUri(game, romPath, context)) {
                is ContentUriResult.Resolved -> resolved.uri
                ContentUriResult.NeedsAccess ->
                    return RomLaunchOutcome.NeedsFileAccess(game, safPickerHintFor(romPath))
            }
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
                maybeLaunchCompanion(host, game, context)
                return launchWithSavedCommand(
                    host, game, context, pkg, savedCommand, romPath, effectiveContentUri, displayId
                )
            }
        }

        // ID files (.steam, .titleid) and undocumented emulators (currently
        // GameHub Lite) need a specific ID extra sourced from the ROM file's
        // contents. The generic buildRomIntentFromPackage path below hands over
        // the file itself — the emulator opens empty ("Get Game") or refuses
        // the file. Pick the built-in command for this (system, pkg, extension)
        // tuple instead.
        val romExt = File(romPath).extension
        val autoCommand = EsdeCommandLauncher.builtinIdCommand(game.systemName, pkg, romExt)
            ?: if (EmulatorRegistry.resolve(pkg)?.launchContract is RomLaunchContract.Undocumented) {
                EsdeCommandLauncher.autoSelectBuiltinCommand(game.systemName, pkg, romExt)
            } else null
        if (autoCommand != null) {
            Log.d(TAG, "Auto-selected built-in command for $pkg ($romExt): $autoCommand")
            maybeLaunchCompanion(host, game, context)
            return launchWithSavedCommand(
                host, game, context, pkg, autoCommand, romPath, effectiveContentUri, displayId
            )
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
        maybeLaunchCompanion(host, game, context)
        recentlyPlayedManager.record(game)
        host.onGameLaunching()
        val failure = startFirstThatOpens(host.activity, candidates, pkg, displayId)
        if (failure != null) return RomLaunchOutcome.Failed(failure)
        host.onLaunchStarted()
        return RomLaunchOutcome.Launched(pkg)
    }

    private fun launchWithSavedCommand(
        host: RomLaunchHost,
        game: GameInfo,
        context: Context,
        pkg: String,
        savedCommand: String,
        romPath: String,
        contentUri: Uri,
        displayId: Int,
    ): RomLaunchOutcome {
        val activity = host.activity
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
        ) ?: return RomLaunchOutcome.IntentBuildFailed
        val options = ActivityOptions.makeBasic().apply { launchDisplayId = displayId }
        Log.d(TAG, "launchGame (command) | cmd=$savedCommand rom=$romPath")
        recentlyPlayedManager.record(game)
        host.onGameLaunching()
        ScreenFocusLockWriter.syncToDisplayId(activity, displayId)
        return try {
            activity.startActivity(intent, options.toBundle())
            host.onLaunchStarted()
            RomLaunchOutcome.Launched(intent.component?.packageName ?: pkg)
        } catch (e: Exception) {
            Log.e(TAG, "Saved-command launch failed", e)
            RomLaunchOutcome.Failed(LaunchFailure.Unknown(e))
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
        activity: Activity,
        candidates: List<Intent>,
        packageName: String,
        displayId: Int,
    ): LaunchFailure? {
        val options = ActivityOptions.makeBasic()
            .apply { launchDisplayId = displayId }
            .toBundle()
        ScreenFocusLockWriter.syncToDisplayId(activity, displayId)
        var failure: LaunchFailure? = null
        for (intent in candidates) {
            failure = startOrNull(activity, intent, options) ?: return null
        }
        // Then anywhere the system will have it. Pinning to a display is a
        // request, not a right, and a game on the near panel beats no game.
        for (intent in candidates) {
            if (startOrNull(activity, intent, null) == null) {
                Log.i(TAG, "$packageName refused the target display; opened on the default one")
                return null
            }
        }
        return failure
    }

    private fun startOrNull(
        activity: Activity,
        intent: Intent,
        options: Bundle?,
    ): LaunchFailure? = try {
        activity.startActivity(intent, options)
        null
    } catch (e: ActivityNotFoundException) {
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

    private sealed interface ContentUriResult {
        data class Resolved(val uri: Uri) : ContentUriResult
        data object NeedsAccess : ContentUriResult
    }

    private companion object {
        const val TAG = "RomGameLauncher"
    }
}
