package jr.brian.home.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.frontend.library.migrate.EsdeAltEmulatorImport
import jr.brian.frontend.library.migrate.EsdeAltEmulatorResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "EsdeAltEmulatorDebug"
private const val WORK_DIR = "esde-alt-verify"
private const val RESULT_FILE = "result.txt"

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface EsdeAltEmulatorDebugEntryPoint {
    fun esdeAltEmulatorImport(): EsdeAltEmulatorImport
}

/**
 * Debug builds only, for the #468 device check until the import is wired into the ES-DE import flow.
 * Runs the import and writes the result to files/esde-alt-verify/result.txt, one `name=value` per line.
 */
class EsdeAltEmulatorDebugReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val import = EntryPointAccessors.fromApplication(app, EsdeAltEmulatorDebugEntryPoint::class.java)
            .esdeAltEmulatorImport()
        val dir = File(app.filesDir, WORK_DIR).apply { mkdirs() }
        val pending = goAsync()
        scope.launch {
            val text = runCatching { import.run().describe() }
                .getOrElse { "error=${it.javaClass.simpleName}" }
            Log.i(TAG, text.replace('\n', ' '))
            File(dir, RESULT_FILE).writeText(text)
            pending.finish()
        }
    }
}

private fun EsdeAltEmulatorResult.describe(): String = buildString {
    appendLine("systemsSet=$systemsSet")
    appendLine("gamesSet=$gamesSet")
    appendLine("keptExisting=$keptExisting")
    appendLine("gamesNotInLibrary=$gamesNotInLibrary")
    skipped.forEach { appendLine("skipped=${it.reason}|${it.esdeSystem}|${it.label}") }
    append("done=ok")
}
