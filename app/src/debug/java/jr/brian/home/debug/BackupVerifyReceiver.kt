package jr.brian.home.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.home.data.config.AppearanceBackup
import jr.brian.home.data.config.AppearanceConfig
import jr.brian.home.data.config.BackupParse
import jr.brian.home.data.config.ImportExportManager
import jr.brian.home.data.config.JarngreiprConfig
import jr.brian.home.data.config.JingleBackup
import jr.brian.home.data.config.RssFeedBackup
import jr.brian.home.data.config.RssFeedConfig
import jr.brian.home.data.config.parseBackup
import jr.brian.home.data.config.readBackupText
import jr.brian.home.data.database.RssFeedDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

private const val ACTION_PREFIX = "jr.brian.home.debug.backup."
private const val WORK_DIR = "backup-verify"
private const val EXTRA_FILE = "file"
private const val VERIFY_FEED = "https://verify.example/feed"
private const val VERIFY_REPO = "verify/jingles"
private val SafeFileName = Regex("^[a-z0-9_-]{1,40}\\.json$")

private val SeedLook = AppearanceConfig(
    themeId = "green_cyan",
    oledMode = true,
    keyboardOledExempt = true,
    iconShape = "SQUARE",
    tabAnimation = true,
)

private val MutatedLook = AppearanceConfig(
    themeId = "red_blue",
    oledMode = false,
    keyboardOledExempt = false,
    iconShape = "ROUNDED",
    tabAnimation = false,
)

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface BackupVerifyEntryPoint {
    fun importExportManager(): ImportExportManager
    fun appearanceBackup(): AppearanceBackup
    fun rssFeedBackup(): RssFeedBackup
    fun jingleBackup(): JingleBackup
    fun rssFeedDao(): RssFeedDao
}

/**
 * Debug builds only, for scripts/jarnfe-verify/verify-374-375-backup.sh. Only the shell (which holds
 * DUMP) can send these. Every file it reads or writes sits in files/backup-verify, and each action
 * ends by writing `<action>.done` there with "ok" or the reason it stopped.
 */
class BackupVerifyReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action?.removePrefix(ACTION_PREFIX) ?: return
        val app = context.applicationContext
        val deps = EntryPointAccessors.fromApplication(app, BackupVerifyEntryPoint::class.java)
        val dir = File(app.filesDir, WORK_DIR).apply { mkdirs() }
        val pending = goAsync()
        scope.launch {
            val result = runCatching { run(action, intent, app, deps, dir) }
                .getOrElse { "error: ${it.javaClass.simpleName}" }
            File(dir, "$action.done").writeText(result)
            pending.finish()
        }
    }

    private suspend fun run(
        action: String,
        intent: Intent,
        context: Context,
        deps: BackupVerifyEntryPoint,
        dir: File,
    ): String = when (action) {
        "seed" -> {
            deps.appearanceBackup().restore(SeedLook)
            deps.rssFeedBackup().restore(listOf(RssFeedConfig(VERIFY_FEED, 60)))
            deps.jingleBackup().restore(enabled = true, repos = listOf(VERIFY_REPO))
            "ok"
        }
        "export" -> export(intent.getStringExtra(EXTRA_FILE) ?: "export.json", deps.importExportManager(), dir)
        "mutate" -> {
            deps.appearanceBackup().restore(MutatedLook)
            deps.rssFeedDao().deleteFeed(VERIFY_FEED)
            deps.rssFeedDao().deleteItemsForFeed(VERIFY_FEED)
            context.getSharedPreferences("jingles_prefs", Context.MODE_PRIVATE).edit {
                putStringSet("jingle_repos", deps.jingleBackup().repos.toSet() - VERIFY_REPO)
            }
            "ok"
        }
        "restore" -> restore(intent.getStringExtra(EXTRA_FILE), deps.importExportManager(), dir)
        else -> "error: unknown action"
    }

    private suspend fun export(name: String, manager: ImportExportManager, dir: File): String {
        if (!SafeFileName.matches(name)) return "error: bad file name"
        File(dir, name).outputStream().use { manager.exportToStream(it) }
        return "ok"
    }

    private suspend fun restore(name: String?, manager: ImportExportManager, dir: File): String {
        if (name == null || !SafeFileName.matches(name)) return "error: bad file name"
        val file = File(dir, name).takeIf { it.isFile } ?: return "error: missing file"
        val text = file.inputStream().use { readBackupText(it) } ?: return "error: too large"
        return when (val parsed = parseBackup(text, JarngreiprConfig.CONFIG_VERSION, manager::decodeFromJson)) {
            is BackupParse.Rejected -> "rejected: ${parsed.problem}"
            is BackupParse.Ready -> {
                manager.applyConfig(parsed.config)
                "ok"
            }
        }
    }
}
