package jr.brian.home.esde.launch

import jr.brian.jarnfe.launch.JarnEmulatorChoice

private const val RETROARCH_PREFIX = "com.retroarch"

internal data class GameEmulatorMaps(
    val emulators: Map<String, String> = emptyMap(),
    val commands: Map<String, String> = emptyMap(),
    val cores: Map<String, String> = emptyMap(),
)

/** [key] is Classic's per-game key. [rowEmulator] is the emulator imported with the game. */
internal data class SystemGameRow(val key: String, val rowEmulator: String?)

/** A saved command would ignore the picked core, so a core choice launches without one. */
private fun JarnEmulatorChoice.effectiveCommand(): String? = command.takeIf { corePath == null }

private fun Map<String, String>.withoutSystem(prefixes: List<String>) =
    filterKeys { key -> prefixes.none(key::startsWith) }

private fun systemPrefixes(systemKeys: List<String>) = systemKeys.map { "$it/" }

/** Games whose saved emulator, command, or core differs from [choice]. */
internal fun countSystemConflicts(
    maps: GameEmulatorMaps,
    systemKeys: List<String>,
    choice: JarnEmulatorChoice,
): Int {
    val prefixes = systemPrefixes(systemKeys)
    fun Map<String, String>.differing(expected: String?) =
        filter { (key, value) -> value != expected && prefixes.any(key::startsWith) }.keys
    return (
        maps.emulators.differing(choice.packageName) +
            maps.commands.differing(choice.effectiveCommand()) +
            maps.cores.differing(choice.corePath)
        ).size
}

/**
 * Classic has no system-wide command or core, so those are written per game. A game imported with
 * RetroArch keeps an explicit emulator when the choice is not RetroArch, because Classic's core
 * prompt reads the imported emulator before the system's.
 */
internal fun planSystemEmulator(
    maps: GameEmulatorMaps,
    systemKeys: List<String>,
    choice: JarnEmulatorChoice,
    games: List<SystemGameRow>,
): GameEmulatorMaps {
    val prefixes = systemPrefixes(systemKeys)
    val command = choice.effectiveCommand()
    val choseRetroArch = choice.packageName.startsWith(RETROARCH_PREFIX)
    val pinned = if (choseRetroArch) {
        emptyList()
    } else {
        games.filter { it.rowEmulator?.startsWith(RETROARCH_PREFIX) == true }
    }
    return GameEmulatorMaps(
        emulators = maps.emulators.withoutSystem(prefixes) +
            pinned.associate { it.key to choice.packageName },
        commands = maps.commands.withoutSystem(prefixes) +
            command?.let { value -> games.associate { it.key to value } }.orEmpty(),
        cores = maps.cores.withoutSystem(prefixes) +
            choice.corePath?.let { value -> games.associate { it.key to value } }.orEmpty(),
    )
}
