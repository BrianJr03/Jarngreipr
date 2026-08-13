package jr.brian.home.esde.util

import jr.brian.home.esde.data.SystemStamp
import jr.brian.home.esde.model.GameInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Joins the filesystem scan to an optional metadata source and yields the game
 * list the app uses. This is the single entry point for building the library.
 *
 * The scan output is authoritative: every scanned ROM produces one entry.
 * A metadata source decorates entries it can match; entries it cannot match
 * are still emitted, titled from their filename stem.
 */
object RomIndexBuilder {

    /**
     * Build the full library across every system reachable from [romsPaths].
     *
     * @param romsPaths ordered ROMs roots.
     * @param esSystemsFile optional es_systems.xml, used only for its
     *   `<extension>` element per system.
     * @param metadataSource the decoration layer; [NoOpMetadataSource] when the
     *   user has turned gamelist reading off and no scraper is configured.
     * @param launchCommandFor called once per (system, first-scan). Returning a
     *   command string wires it into every game in that system so the launcher
     *   can still consult the saved custom command per-game later.
     * @param emulatorPackageFor per-system emulator preference, when set.
     */
    suspend fun buildIndex(
        romsPaths: List<String>,
        esSystemsFile: File?,
        metadataSource: RomMetadataSource,
        launchCommandFor: (systemName: String) -> String? = { null },
        emulatorPackageFor: (systemName: String) -> String? = { null },
    ): List<GameInfo> = withContext(Dispatchers.IO) {
        val scans = RomScanner.scan(romsPaths, esSystemsFile)
        if (scans.isEmpty()) return@withContext emptyList()

        coroutineScope {
            scans.map { (systemName, systemScans) ->
                async {
                    val metadata = metadataSource.metadataFor(systemName, systemScans)
                    systemScans.map { scan ->
                        toGameInfo(
                            scan = scan,
                            metadata = metadata[scan],
                            launchCommand = launchCommandFor(systemName),
                            emulatorPackage = emulatorPackageFor(systemName),
                        )
                    }
                }
            }.awaitAll().flatten()
        }
    }

    /**
     * Build a validity stamp for [systemName] given the roots and gamelist file.
     * The stamp is what the cache uses to decide whether a persisted entry can
     * be reused or must be rescanned. See [SystemStamp] for the fields and
     * their limits (notably: directory-lastModified propagation is unreliable
     * on some filesystems — the entry count catches most misses).
     */
    fun stamp(
        systemName: String,
        romsPaths: List<String>,
        esdeRootPath: String?,
        decorationEnabled: Boolean,
    ): SystemStamp? {
        for (root in romsPaths.distinct()) {
            val systemDir = File(root, systemName)
            if (!systemDir.exists() || !systemDir.isDirectory) continue
            val entryCount = systemDir.listFiles()?.size ?: 0
            val gamelistMtime = if (decorationEnabled && esdeRootPath != null) {
                val gl = File(esdeRootPath, "gamelists/$systemName/gamelist.xml")
                if (gl.exists()) gl.lastModified() else -1L
            } else -1L
            return SystemStamp(
                systemDirLastModified = systemDir.lastModified(),
                entryCount = entryCount,
                gamelistLastModified = gamelistMtime,
                decorationEnabled = decorationEnabled,
                romsRootUsed = root,
            )
        }
        return null
    }

    /**
     * Convenience overload for a single-system rebuild — used by the cache
     * layer when only one system's stamp has changed.
     */
    suspend fun buildForSystem(
        systemName: String,
        romsPaths: List<String>,
        esSystemsFile: File?,
        metadataSource: RomMetadataSource,
        launchCommand: String? = null,
        emulatorPackage: String? = null,
    ): List<GameInfo> = withContext(Dispatchers.IO) {
        val allScans = RomScanner.scan(romsPaths, esSystemsFile)
        val systemScans = allScans[systemName] ?: return@withContext emptyList()
        val metadata = metadataSource.metadataFor(systemName, systemScans)
        systemScans.map { scan ->
            toGameInfo(scan, metadata[scan], launchCommand, emulatorPackage)
        }
    }

    private fun toGameInfo(
        scan: ScannedRom,
        metadata: RomMetadata?,
        launchCommand: String?,
        emulatorPackage: String?,
    ): GameInfo {
        val name = metadata?.displayName
            ?: File(scan.relativePath).nameWithoutExtension
        return GameInfo(
            // Keep the ES-DE-style leading "./" on `path` so downstream code
            // (media-basename candidates, gameKey) matches the shape it used to
            // see from the gamelist parse.
            path = "./${scan.relativePath}",
            name = name,
            description = metadata?.description,
            rating = metadata?.rating ?: 0f,
            releaseDate = metadata?.releaseDate,
            developer = metadata?.developer,
            publisher = metadata?.publisher,
            genre = metadata?.genre,
            players = metadata?.players,
            isFavorite = metadata?.isFavorite == true,
            playCount = metadata?.playCount ?: 0,
            playTimeMinutes = metadata?.playTimeMinutes ?: 0,
            lastPlayed = metadata?.lastPlayed,
            systemName = scan.systemName,
            artworkPath = metadata?.artworkPath,
            physicalMediaPath = metadata?.physicalMediaPath,
            marqueeImagePath = metadata?.marqueeImagePath,
            screenshotPath = metadata?.screenshotPath,
            fanartPath = metadata?.fanartPath,
            titlescreenPath = metadata?.titlescreenPath,
            miximagePath = metadata?.miximagePath,
            emulatorPackage = emulatorPackage,
            // Always the scanner's answer. A metadata source must never
            // override where a ROM lives on disk.
            romAbsolutePath = scan.absolutePath,
            launchCommand = launchCommand,
        )
    }
}
