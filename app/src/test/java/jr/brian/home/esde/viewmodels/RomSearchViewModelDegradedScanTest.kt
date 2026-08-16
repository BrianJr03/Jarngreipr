package jr.brian.home.esde.viewmodels

import android.content.Context
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomIndexCache
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.esde.data.SystemCacheEntry
import jr.brian.home.esde.data.SystemStamp
import jr.brian.home.esde.data.addRomsPath
import jr.brian.home.esde.data.setGamelistDecorationEnabled
import jr.brian.home.esde.model.GameInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Locks the invariants the boot-time SD-card race fix has to enforce.
 *
 * Before the fix a scan whose SD-card root wasn't mounted yet pruned every SD
 * system out of the persisted cache (`RomIndexCache.saveAll` treats absence
 * from the save-set as a deletion). The failure was permanent for the process
 * lifetime because `loadGames`'s early return also latched. These tests hit
 * that path directly against the ViewModel's cache and prove:
 *
 *  1. an unreachable-root scan does NOT touch the cached entry for that root,
 *  2. a fully-mounted scan still prunes a system whose directory is gone,
 *  3. the two behaviours compose across scans so a mount-after-reboot ends
 *     with the SD systems back in the store.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RomSearchViewModelDegradedScanTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var app: android.app.Application
    private lateinit var esdePrefs: ESDEPreferencesManager
    private lateinit var setupPrefs: SetupPreferences
    private lateinit var stateHolder: RomSearchStateHolder
    private lateinit var vm: RomSearchViewModel
    private lateinit var cache: RomIndexCache

    // The ES-DE root the ViewModel derives from setupPreferences.scriptsPath.
    // The scan does read `custom_systems/es_systems.xml` if present, but we
    // don't create one — extension resolution falls back to the registry.
    private lateinit var esdeRoot: File

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        // Every test lives in the same JVM. Wipe both prefs blobs and the
        // filesDir cache so leftover state from a prior test can't pollute
        // this one's setup.
        app.getSharedPreferences("esde_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("SetupPrefs", Context.MODE_PRIVATE).edit().clear().commit()
        File(app.filesDir, "rom_index").deleteRecursively()

        esdeRoot = tempFolder.newFolder("ES-DE")
        setupPrefs = SetupPreferences(app).apply {
            scriptsPath = File(esdeRoot, "scripts").absolutePath
        }
        esdePrefs = ESDEPreferencesManager(app)
        // Decoration off keeps the scan filesystem-only — no gamelist.xml
        // needed and no metadata reads to mock.
        esdePrefs.setGamelistDecorationEnabled(false)

        stateHolder = RomSearchStateHolder()
        cache = RomIndexCache(app)
        vm = RomSearchViewModel(
            context = app,
            esdePreferencesManager = esdePrefs,
            setupPreferences = setupPrefs,
            store = stateHolder,
        )
    }

    @Test
    fun `cached system whose root is missing at scan time survives`() = runBlocking {
        val internalRoot = tempFolder.newFolder("Internal").also {
            writeRom(it, "nes/Mario.nes")
        }
        val missingSdRoot = File(tempFolder.root, "sdcard-not-mounted").absolutePath
        esdePrefs.addRomsPath(internalRoot.absolutePath)
        esdePrefs.addRomsPath(missingSdRoot)

        // Seed the cache with a psx entry whose stamp claims it came from the
        // SD card — this is the state left behind by the last mounted scan.
        cache.saveAll(mapOf("psx" to psxEntry(sourceRoot = missingSdRoot)))

        vm.parseAndStore(rootPath = esdeRoot.absolutePath, forceRescan = false)

        val persisted = cache.loadAll()
        assertTrue("psx must survive on-disk", "psx" in persisted)
        assertTrue("nes must be indexed", "nes" in persisted)
        val allGames = stateHolder.allGames.value
        assertTrue(
            "allGames must include the carried-over psx entry",
            allGames.any { it.systemName == "psx" },
        )
        assertTrue(
            "allGames must include the freshly scanned nes entry",
            allGames.any { it.systemName == "nes" },
        )
    }

    @Test
    fun `same system is pruned when its root is present and directory is gone`() = runBlocking {
        // The escape hatch has to keep working: if the "SD root" is actually
        // mounted and the psx directory really was deleted, the stale cache
        // entry must go — otherwise a manual refresh wouldn't clean anything up.
        val root = tempFolder.newFolder("Roms").also {
            writeRom(it, "nes/Mario.nes")
        }
        esdePrefs.addRomsPath(root.absolutePath)

        cache.saveAll(mapOf("psx" to psxEntry(sourceRoot = root.absolutePath)))

        vm.parseAndStore(rootPath = esdeRoot.absolutePath, forceRescan = false)

        val persisted = cache.loadAll()
        assertNull("psx must be pruned when its root is present and directory is gone", persisted["psx"])
        assertTrue("nes must remain", "nes" in persisted)
    }

    @Test
    fun `unreachable-then-restored root sequence ends with the full set of systems`() = runBlocking {
        val internalRoot = tempFolder.newFolder("Internal").also {
            writeRom(it, "nes/Mario.nes")
        }
        val sdRootPath = File(tempFolder.root, "sdcard").absolutePath
        esdePrefs.addRomsPath(internalRoot.absolutePath)
        esdePrefs.addRomsPath(sdRootPath)

        cache.saveAll(mapOf("psx" to psxEntry(sourceRoot = sdRootPath)))

        // First pass: SD isn't mounted. psx is carried forward from cache.
        vm.parseAndStore(rootPath = esdeRoot.absolutePath, forceRescan = false)
        assertTrue("psx must be carried forward while SD is missing", "psx" in cache.loadAll())

        // SD "mounts" — the directory becomes real and gains its psx system.
        File(sdRootPath).mkdirs()
        writeRom(File(sdRootPath), "psx/Metal Gear.chd")

        // Second pass: SD is available now. psx must be a real scan result,
        // AND nes must still be present.
        vm.parseAndStore(rootPath = esdeRoot.absolutePath, forceRescan = false)

        val persisted = cache.loadAll()
        assertEquals(setOf("nes", "psx"), persisted.keys)
        val psxGames = persisted["psx"]!!.games
        assertEquals(1, psxGames.size)
        assertTrue(
            "psx entry must reflect the newly mounted directory, not the seeded stub",
            psxGames.single().name == "Metal Gear",
        )
    }

    private fun psxEntry(sourceRoot: String): SystemCacheEntry = SystemCacheEntry(
        stamp = SystemStamp(
            systemDirLastModified = 1L,
            entryCount = 1,
            gamelistLastModified = -1L,
            decorationEnabled = false,
            romsRootUsed = sourceRoot,
        ),
        games = listOf(
            GameInfo(path = "./CarriedOver.iso", name = "CarriedOver", systemName = "psx")
        ),
    )

    private fun writeRom(root: File, relative: String): File {
        val file = File(root, relative)
        file.parentFile?.mkdirs()
        file.writeText("stub")
        return file
    }
}
