package jr.brian.frontend.library.migrate

/** What one gamelist.xml changed. */
data class GamelistFileCounts(
    /** Games with any field changed. */
    val gamesUpdated: Int = 0,
    /** Games that became favorites. */
    val favoritesAdded: Int = 0,
    /** Games whose play count, play time or last played went up. */
    val playStatsUpdated: Int = 0,
) {
    operator fun plus(other: GamelistFileCounts) = GamelistFileCounts(
        gamesUpdated = gamesUpdated + other.gamesUpdated,
        favoritesAdded = favoritesAdded + other.favoritesAdded,
        playStatsUpdated = playStatsUpdated + other.playStatsUpdated,
    )
}

/**
 * What an ES-DE import did. Every count is exact. New parts of the import
 * (such as emulator choices) add their own fields with defaults, so older
 * callers and saved summaries keep working.
 *
 * @property esdeFound an ES-DE folder is on the device.
 * @property accessMissing all-files access is missing, so nothing was read.
 * @property gamelistsFound gamelist files that apply to this library, one
 *   per source and system.
 * @property systemsMatched systems with a gamelist and games here.
 * @property unrecognisedSystems gamelist folder names no known system uses,
 *   as ES-DE names them. Sorted.
 * @property unreadable gamelists that couldn't be read or parsed.
 * @property skippedUnchanged gamelists left alone because they hadn't
 *   changed since the last import. Always 0 on a forced run.
 * @property emulatorSystemsSet systems given ES-DE's emulator. Only a
 *   user-started import brings emulators over.
 * @property emulatorGamesSet games given their own ES-DE emulator.
 * @property emulatorKeptExisting ES-DE picks left alone because one was
 *   already chosen in Jarngreipr.
 * @property emulatorSkipped ES-DE picks Jarngreipr can't use (unknown,
 *   not installed or not supported yet).
 */
data class EsdeImportSummary(
    val esdeFound: Boolean = true,
    val accessMissing: Boolean = false,
    val gamelistsFound: Int = 0,
    val systemsMatched: Int = 0,
    val unrecognisedSystems: List<String> = emptyList(),
    val gamesUpdated: Int = 0,
    val favoritesAdded: Int = 0,
    val playStatsUpdated: Int = 0,
    val unreadable: Int = 0,
    val skippedUnchanged: Int = 0,
    val emulatorSystemsSet: Int = 0,
    val emulatorGamesSet: Int = 0,
    val emulatorKeptExisting: Int = 0,
    val emulatorSkipped: Int = 0,
) {
    /** This summary with what the emulator import did. */
    fun withEmulators(result: EsdeAltEmulatorResult): EsdeImportSummary = copy(
        emulatorSystemsSet = result.systemsSet,
        emulatorGamesSet = result.gamesSet,
        emulatorKeptExisting = result.keptExisting,
        emulatorSkipped = result.skipped.size,
    )

    /** One line for logcat. The device checks grep for its start. Emulators get [emulatorLogLine]. */
    fun logLine(): String =
        "Gamelist import: $gamelistsFound gamelists, $skippedUnchanged skipped as unchanged, " +
            "$gamesUpdated games updated, $favoritesAdded favorites added, $playStatsUpdated stats updated, " +
            "$unreadable unreadable, $systemsMatched systems matched, ${unrecognisedSystems.size} not recognised"

    /** The emulator import's line, logged after it runs. Only user-started imports have one. */
    fun emulatorLogLine(): String =
        "ES-DE emulator import: $emulatorSystemsSet systems, $emulatorGamesSet games, " +
            "$emulatorKeptExisting kept, $emulatorSkipped skipped"
}

/** How far an import is: [done] of [total] gamelists, now on [systemLabel]. */
data class EsdeImportProgress(val done: Int, val total: Int, val systemLabel: String)
