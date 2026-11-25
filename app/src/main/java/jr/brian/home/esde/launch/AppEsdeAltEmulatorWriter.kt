package jr.brian.home.esde.launch

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.migrate.EsdeAltEmulatorResult
import jr.brian.frontend.library.migrate.EsdeAltEmulatorSkip
import jr.brian.frontend.library.migrate.EsdeAltEmulatorSkipReason
import jr.brian.frontend.library.migrate.EsdeAltEmulatorWriter
import jr.brian.frontend.library.migrate.EsdeAltEmulators
import jr.brian.frontend.library.systems.SystemCatalog
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.setGameLaunchMaps
import jr.brian.home.esde.data.setSystemEmulator
import jr.brian.home.esde.util.BundledEsdeSystems
import jr.brian.home.esde.util.EmulatorRegistry
import jr.brian.home.esde.util.EsdeBuiltinCommands
import jr.brian.home.esde.util.EsdeCommandLauncher
import jr.brian.home.esde.util.customSystemsFile
import jr.brian.jarnfe.launch.JarnEmulatorChoice
import jr.brian.jarnfe.systems.systemStorageKeys
import javax.inject.Inject
import javax.inject.Singleton

private const val RETROARCH_PREFIX = "com.retroarch"
private val EmulatorToken = Regex("""%EMULATOR_([^%]+)%""")
private val LibretroCore = Regex("""%EXTRA_LIBRETRO%=(\S+)""")
private val Placeholder = Regex("""%[A-Z_]+%""")
private const val INTERNAL_DATA = "/data/data"

/** Tokens Jarn's launcher fills in. Anything else would reach the emulator as literal text. */
private val LaunchTokens = setOf(
    "%ROM%", "%ROMSAF%", "%ROMPROVIDER%", "%ANDROIDPACKAGE%", "%INJECT%",
    "%ACTION%", "%DATA%", "%ACTIVITY_CLEAR_TASK%", "%ACTIVITY_CLEAR_TOP%",
)

/** What an ES-DE label means here. */
internal sealed interface EsdeLabelMatch {
    data class Found(val choice: JarnEmulatorChoice) : EsdeLabelMatch
    data object Unknown : EsdeLabelMatch
    data object NotInstalled : EsdeLabelMatch
    data object Unsupported : EsdeLabelMatch
}

/** The saved choices, and the new ones planned on top of them. */
internal data class LaunchChoices(
    val games: GameEmulatorMaps = GameEmulatorMaps(),
    val systems: Map<String, String> = emptyMap(),
)

internal data class EsdeAltEmulatorPlan(val choices: LaunchChoices, val result: EsdeAltEmulatorResult)

/**
 * A RetroArch choice saves its core and no command, the way the core picker does, because the launch
 * gate asks for a core until one is saved and a saved command would ignore it. ES-DE writes the core
 * as `%INTERNALDATA%/%ANDROIDPACKAGE%/cores/<file>`, which is RetroArch's own cores folder.
 * Null when the command can't be launched here.
 */
internal fun esdeChoice(packageName: String, command: String): JarnEmulatorChoice? {
    if (packageName.startsWith(RETROARCH_PREFIX)) {
        LibretroCore.find(command)?.groupValues?.get(1)?.let { core ->
            val path = core.replace("%INTERNALDATA%", INTERNAL_DATA).replace("%ANDROIDPACKAGE%", packageName)
                .takeIf { it.startsWith("/") && !Placeholder.containsMatchIn(it) }
                ?: "$INTERNAL_DATA/$packageName/cores/${core.substringAfterLast('/')}"
            return JarnEmulatorChoice(packageName, command = null, corePath = path)
        }
    }
    val tokens = Placeholder.findAll(command).map { it.value }
        .filterNot { it.startsWith("%EMULATOR_") || it.startsWith("%EXTRA") }
    return if (tokens.all { it in LaunchTokens }) JarnEmulatorChoice(packageName, command) else null
}

/**
 * Finds [label] among [commands] (label to command, custom ones first), then the first installed
 * package for its `%EMULATOR_X%`. [rules] lists a rule name's "package/activity" entries.
 */
internal fun matchEsdeLabel(
    label: String,
    commands: List<Pair<String, String>>,
    rules: (String) -> List<String>,
    isInstalled: (String) -> Boolean,
): EsdeLabelMatch {
    val command = commands.firstOrNull { it.first == label }?.second
        ?: commands.firstOrNull { it.first.trim().equals(label.trim(), ignoreCase = true) }?.second
        ?: return EsdeLabelMatch.Unknown
    val emulator = EmulatorToken.find(command)?.groupValues?.get(1) ?: return EsdeLabelMatch.Unknown
    val packageName = rules(emulator).map { it.substringBefore("/") }.firstOrNull(isInstalled)
        ?: return EsdeLabelMatch.NotInstalled
    return esdeChoice(packageName, command)?.let(EsdeLabelMatch::Found) ?: EsdeLabelMatch.Unsupported
}

private fun GameEmulatorMaps.hasChoice(key: String) = key in emulators || key in commands || key in cores

private fun GameEmulatorMaps.with(key: String, choice: JarnEmulatorChoice) = GameEmulatorMaps(
    emulators = emulators + (key to choice.packageName),
    commands = choice.command?.let { commands + (key to it) } ?: commands,
    cores = choice.corePath?.let { cores + (key to it) } ?: cores,
)

private fun GameEmulatorMaps.plus(added: GameEmulatorMaps) = GameEmulatorMaps(
    emulators = added.emulators + emulators,
    commands = added.commands + commands,
    cores = added.cores + cores,
)

/**
 * Game choices first, then each system's for its other games. A game or system that already has a
 * choice in Jarngreipr keeps it. A system choice is written per game too, as [planSystemEmulator] does.
 */
internal fun planEsdeAltEmulators(
    found: EsdeAltEmulators,
    saved: LaunchChoices,
    systemKeys: (String) -> List<String>,
    match: (esdeSystem: String, label: String) -> EsdeLabelMatch,
): EsdeAltEmulatorPlan {
    var games = saved.games
    var systems = saved.systems
    var gamesSet = 0
    var systemsSet = 0
    var kept = 0
    val skipped = LinkedHashSet<EsdeAltEmulatorSkip>()

    fun resolve(esdeSystem: String, label: String): JarnEmulatorChoice? =
        when (val result = match(esdeSystem, label)) {
            is EsdeLabelMatch.Found -> result.choice
            EsdeLabelMatch.Unknown -> null.also {
                skipped += EsdeAltEmulatorSkip(esdeSystem, label, EsdeAltEmulatorSkipReason.UnknownLabel)
            }
            EsdeLabelMatch.NotInstalled -> null.also {
                skipped += EsdeAltEmulatorSkip(esdeSystem, label, EsdeAltEmulatorSkipReason.NotInstalled)
            }
            EsdeLabelMatch.Unsupported -> null.also {
                skipped += EsdeAltEmulatorSkip(esdeSystem, label, EsdeAltEmulatorSkipReason.Unsupported)
            }
        }

    for (game in found.games) {
        if (saved.games.hasChoice(game.key)) {
            kept++
            continue
        }
        val choice = resolve(game.esdeSystem, game.label) ?: continue
        games = games.with(game.key, choice)
        gamesSet++
    }

    for (system in found.systems) {
        val keys = systemKeys(system.systemId)
        if (keys.any { it in saved.systems }) {
            kept++
            continue
        }
        val choice = resolve(system.esdeSystem, system.label) ?: continue
        val open = system.games.filterNot { games.hasChoice(it.key) }
            .map { SystemGameRow(it.key, it.rowEmulator) }
        games = games.plus(planSystemEmulator(GameEmulatorMaps(), keys, choice, open))
        systems = systems + keys.associateWith { choice.packageName }
        systemsSet++
    }

    return EsdeAltEmulatorPlan(
        LaunchChoices(games, systems),
        EsdeAltEmulatorResult(systemsSet, gamesSet, kept, skipped = skipped.toList()),
    )
}

/**
 * A rule name's entries in the order a launch tries them (custom, built-in, then bundled), so the
 * package saved here is the one the saved command opens.
 */
internal fun esdeRules(
    name: String,
    custom: Map<String, List<String>>,
    bundled: Map<String, List<String>>,
): List<String> = custom[name].orEmpty() + EsdeBuiltinCommands.RULES[name].orEmpty() + bundled[name].orEmpty()

/** Saves ES-DE's emulator choices into the prefs Jarn and Classic launch from. */
@Singleton
class AppEsdeAltEmulatorWriter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val esdePrefs: ESDEPreferencesManager,
    private val catalog: SystemCatalog,
) : EsdeAltEmulatorWriter {

    override fun write(found: EsdeAltEmulators): EsdeAltEmulatorResult {
        val state = esdePrefs.state.value
        val saved = LaunchChoices(
            GameEmulatorMaps(state.gameEmulatorMap, state.gameCommandMap, state.gameCoreMap),
            state.systemEmulatorMap,
        )
        val customSystems = customSystemsFile(context, "es_systems.xml")
        val customRules = EsdeCommandLauncher.parseCustomRules(customSystemsFile(context, "es_find_rules.xml"))
        val bundledRules = BundledEsdeSystems.rules(context)
        val commandsBySystem = HashMap<String, List<Pair<String, String>>>()
        val plan = planEsdeAltEmulators(
            found = found,
            saved = saved,
            systemKeys = { id -> systemStorageKeys(id, catalog.findById(id)?.folderAliases.orEmpty()) },
            match = { esdeSystem, label ->
                val commands = commandsBySystem.getOrPut(esdeSystem) {
                    EsdeCommandLauncher.parseSystemCommands(customSystems, esdeSystem) +
                        BundledEsdeSystems.commands(context, esdeSystem) +
                        EsdeBuiltinCommands.SYSTEM_COMMANDS[esdeSystem].orEmpty()
                }
                matchEsdeLabel(
                    label = label,
                    commands = commands,
                    rules = { name -> esdeRules(name, customRules, bundledRules) },
                    isInstalled = { pkg -> EmulatorRegistry.isInstalled(context, pkg) },
                )
            },
        )
        val planned = plan.choices
        if (planned.games != saved.games) {
            esdePrefs.setGameLaunchMaps(planned.games.emulators, planned.games.commands, planned.games.cores)
        }
        (planned.systems - saved.systems.keys).forEach { (key, pkg) -> esdePrefs.setSystemEmulator(key, pkg) }
        return plan.result
    }
}
