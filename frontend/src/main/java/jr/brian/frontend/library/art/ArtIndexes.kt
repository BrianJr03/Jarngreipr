package jr.brian.frontend.library.art

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.ArtSource
import jr.brian.core.model.SystemArtSource
import jr.brian.frontend.library.media.EsdeLayout
import jr.brian.frontend.library.media.JarngreiprLayout
import jr.brian.frontend.library.media.MediaLayout
import jr.brian.frontend.library.media.RetroHraiLayout
import jr.brian.frontend.library.systems.SystemCatalog
import jr.brian.frontend.scrape.sharedScrapedRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where each art source lives. Jarn FE scrapes into [jarnScrapedRoot], which
 * no Classic media root contains, so its downloads can never change Classic
 * art and never land in the user's RetroHrai or ES-DE folders.
 */
@Singleton
class ArtRoots internal constructor(
    private val esdeRoot: EsdeMediaRootProvider,
    private val retroHraiRoot: File,
    private val sharedScraped: () -> File,
    val jarnScrapedRoot: File,
    private val bundledLogoFiles: () -> List<String> = { emptyList() },
) {
    private val bundledLogos: Set<String> by lazy { bundledLogoFiles().toSet() }

    /** The bundled logo for the first of [folders] that has one. */
    fun bundledLogoFor(folders: List<String>): String? = bundledLogo(folders, bundledLogos)

    @Inject
    constructor(
        @ApplicationContext context: Context,
        esdeRoot: EsdeMediaRootProvider,
    ) : this(
        esdeRoot = esdeRoot,
        retroHraiRoot = File(RETRO_HRAI_MEDIA_PATH),
        sharedScraped = { sharedScrapedRoot(context) },
        jarnScrapedRoot = File(context.getExternalFilesDir(null) ?: context.filesDir, JARN_MEDIA_DIR),
        bundledLogoFiles = { context.assets.list(BUNDLED_LOGOS_DIR)?.toList().orEmpty() },
    )

    fun rootsFor(source: ArtSource): List<File> = when (source) {
        ArtSource.EsDe -> listOfNotNull(esdeRoot.esdeMediaRoot())
        ArtSource.RetroHrai -> listOf(retroHraiRoot)
        ArtSource.Scraped -> listOf(jarnScrapedRoot, sharedScraped()).distinct()
    }

    fun sourceOf(path: String): ArtSource? = ArtSource.entries.firstOrNull { source ->
        rootsFor(source).any { path.startsWith(it.path.trimEnd('/') + "/") }
    }

    private companion object {
        const val RETRO_HRAI_MEDIA_PATH = "/storage/emulated/0/RetroHrai!/media"
        const val JARN_MEDIA_DIR = "Jarn FE Media"
    }
}

/** Lazily built, cached indexes for every source. Reads the disk, never writes. */
@Singleton
class ArtIndexes @Inject constructor(
    private val roots: ArtRoots,
    private val catalog: SystemCatalog,
) {
    private val mutex = Mutex()
    @Volatile private var cached: Map<ArtSource, ArtFolderIndex>? = null
    private val _revision = MutableStateFlow(0L)

    /** Bumps whenever the indexes are invalidated, so observers re-resolve art. */
    val revision: StateFlow<Long> = _revision.asStateFlow()

    internal suspend fun current(): Map<ArtSource, ArtFolderIndex> = mutex.withLock {
        cached?.let { return@withLock it }
        val startedAt = _revision.value
        val built = withContext(Dispatchers.IO) {
            ArtSource.entries.associateWith { source ->
                ArtFolderIndex.build(roots.rootsFor(source), layoutsFor(source), allFolders(source))
            }
        }
        if (_revision.value == startedAt) cached = built
        built
    }

    fun invalidate() {
        cached = null
        _revision.update { it + 1 }
    }

    /** Folders to probe for [systemId] in [source], canonical id first. */
    fun foldersFor(systemId: String, source: ArtSource): List<String> {
        val def = catalog.findById(systemId)
        return buildList {
            add(def?.id ?: systemId)
            def?.folderAliases?.let(::addAll)
            if (source == ArtSource.RetroHrai) {
                RetroHraiLayout.EXTRA_SYSTEM_FOLDERS[def?.id ?: systemId]?.let(::addAll)
            }
        }.distinct()
    }

    /** System tile art for [systemIds], from RetroHrai's platform folders and the bundled logos. */
    suspend fun systemArt(
        systemIds: Collection<String>,
        source: SystemArtSource,
    ): Map<String, SystemArt> = withContext(Dispatchers.IO) {
        val mediaRoots = roots.rootsFor(ArtSource.RetroHrai)
        systemIds.associateWith { id ->
            val folders = foldersFor(id, ArtSource.RetroHrai)
            val dirs = folders.flatMap { folder -> mediaRoots.map { File(it, folder) } }
            val retroHrai = SystemArt(
                backgroundPath = platformImage(dirs, PLATFORM_BACKGROUNDS_FOLDER),
                logoPath = platformImage(dirs, PLATFORM_LOGOS_FOLDER),
            )
            pickSystemArt(source, retroHrai, roots.bundledLogoFor(folders))
        }
    }

    private fun allFolders(source: ArtSource): Set<String> =
        catalog.all().flatMapTo(HashSet()) { foldersFor(it.id, source) }

    private fun layoutsFor(source: ArtSource): List<MediaLayout> = when (source) {
        ArtSource.EsDe -> listOf(EsdeLayout(), JarngreiprLayout())
        ArtSource.RetroHrai -> listOf(RetroHraiLayout())
        ArtSource.Scraped -> listOf(JarngreiprLayout())
    }
}
