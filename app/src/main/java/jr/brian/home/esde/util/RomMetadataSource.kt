package jr.brian.home.esde.util

/**
 * Optional decoration on top of the filesystem-scanned ROM set.
 *
 * The scanner ([RomScanner]) decides what exists; a metadata source only
 * enriches those entries with title, description, rating, box art, and so on.
 * An entry that has no metadata still appears — titled from its filename stem —
 * so a broken metadata source cannot cause games to disappear from the library.
 *
 * Implementations are expected to be pure functions over `(system, scans)` —
 * they must not mutate the passed-in list or issue side effects beyond reading
 * their own configured source (gamelist.xml, remote scraper API, etc.).
 */
interface RomMetadataSource {
    /**
     * Look up decoration for [scans] under [systemName].
     *
     * @return a map keyed by the scan whose metadata is described. Scans not
     *   present in the map get filename-derived titles and no other decoration.
     */
    suspend fun metadataFor(
        systemName: String,
        scans: List<ScannedRom>,
    ): Map<ScannedRom, RomMetadata>
}

/**
 * Everything a metadata source can contribute about one game. All fields are
 * optional — a source that only knows a title returns [RomMetadata] with just
 * the title set and every other field null.
 *
 * Notably absent: `absolutePath` and `systemName`. Those come from the scanned
 * entry the metadata is attached to. A metadata source must never override
 * where a ROM lives on disk.
 */
data class RomMetadata(
    val displayName: String? = null,
    val description: String? = null,
    val rating: Float = 0f,
    val releaseDate: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val genre: String? = null,
    val players: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val playTimeMinutes: Int = 0,
    val lastPlayed: String? = null,
    val artworkPath: String? = null,
    val physicalMediaPath: String? = null,
    val marqueeImagePath: String? = null,
    val screenshotPath: String? = null,
    val fanartPath: String? = null,
    val titlescreenPath: String? = null,
    val miximagePath: String? = null,
)

/**
 * A metadata source that never contributes anything. Used when the user turns
 * gamelist reading off and no other source is configured — the library still
 * displays every scanned ROM, titled from its filename.
 */
object NoOpMetadataSource : RomMetadataSource {
    override suspend fun metadataFor(
        systemName: String,
        scans: List<ScannedRom>,
    ): Map<ScannedRom, RomMetadata> = emptyMap()
}
