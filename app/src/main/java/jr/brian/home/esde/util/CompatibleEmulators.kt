package jr.brian.home.esde.util

import android.content.Context
import java.io.File

private const val ES_DE_CUSTOM_SYSTEMS = "ES-DE/custom_systems"
private const val SHARED_CUSTOM_SYSTEMS = "/storage/emulated/0/ES-DE/custom_systems"

internal fun customSystemsFile(context: Context, name: String): File =
    File(context.filesDir.parent ?: "", "$ES_DE_CUSTOM_SYSTEMS/$name")
        .takeIf { it.exists() }
        ?: File(SHARED_CUSTOM_SYSTEMS, name)

/** Installed emulators for a game: the system's own list, then matches by file extension. */
fun compatibleEmulators(
    context: Context,
    systemName: String,
    romExtension: String,
): List<EmulatorOption> {
    val customRules =
        EsdeCommandLauncher.parseCustomRules(customSystemsFile(context, "es_find_rules.xml"))
    val fromSystem = EsdeCommandLauncher.getCompatibleEmulatorsFromSystem(
        context, systemName, customSystemsFile(context, "es_systems.xml"), customRules
    )
    val fromExtension = EsdeCommandLauncher.getCompatibleEmulators(context, romExtension)
    val seenPackages = fromSystem.map { it.packageName }.toHashSet()
    return fromSystem + fromExtension.filter { it.packageName !in seenPackages }
}
