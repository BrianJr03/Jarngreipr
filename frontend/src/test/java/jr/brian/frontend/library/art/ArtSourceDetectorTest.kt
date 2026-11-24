package jr.brian.frontend.library.art

import android.app.Application
import jr.brian.core.model.ArtSource
import jr.brian.frontend.library.art.ArtMediaPresence.Empty
import jr.brian.frontend.library.art.ArtMediaPresence.HasMedia
import jr.brian.frontend.library.art.ArtMediaPresence.Unknown
import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ArtSourceDetectorTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val catalog = StubSystemCatalog(
        SystemDef(id = "gba", displayName = "GBA", extensions = setOf("gba")),
    )
    private var access = true

    private lateinit var esde: File
    private lateinit var retroHrai: File
    private lateinit var shared: File
    private var esdeRoot: File? = null
    private lateinit var store: ArtSourceStore
    private lateinit var detector: ArtSourceDetector

    @Before
    fun setUp() {
        esde = temp.newFolder("esde")
        retroHrai = temp.newFolder("hrai")
        shared = temp.newFolder("shared")
        esdeRoot = esde
        val roots = ArtRoots({ esdeRoot }, retroHrai, { shared }, temp.newFolder("jarn"))
        store = ArtSourceStore(RuntimeEnvironment.getApplication())
        detector = ArtSourceDetector(
            ArtIndexes(roots, catalog),
            roots,
            store,
            CoroutineScope(Dispatchers.Unconfined),
        ) { access }
    }

    @Test
    fun `ES-DE wins, then RetroHrai, and nothing else decides`() {
        assertEquals(ArtSource.EsDe, decideArtSource(HasMedia, HasMedia))
        assertEquals(ArtSource.EsDe, decideArtSource(HasMedia, Unknown))
        assertEquals(ArtSource.RetroHrai, decideArtSource(Empty, HasMedia))
        assertNull(decideArtSource(Unknown, HasMedia))
        assertNull(decideArtSource(Unknown, Unknown))
        assertNull(decideArtSource(Empty, Unknown))
        assertNull(decideArtSource(Empty, Empty))
    }

    @Test
    fun `shared storage without access is unknown, not empty`() {
        val root = File("/storage/emulated/0/ES-DE/downloaded_media")

        assertEquals(Unknown, mediaPresence(0, listOf(root), { false }))
        assertEquals(HasMedia, mediaPresence(3, listOf(root), { false }))
    }

    @Test
    fun `a missing SD card is unknown and a missing internal folder is empty`() {
        val onCard = File("/storage/ABCD-1234/ES-DE/downloaded_media")
        val internal = File("/storage/emulated/0/ES-DE/downloaded_media")

        assertEquals(Unknown, mediaPresence(0, listOf(onCard), { true }))
        assertEquals(Empty, mediaPresence(0, listOf(internal), { true }))
        assertEquals(Empty, mediaPresence(0, emptyList(), { true }))
    }

    @Test
    fun `detects ES-DE when both have media`() = runBlocking {
        write(esde, "gba/covers/A.png")
        write(retroHrai, "gba/covers/A.png")

        detector.detect()

        assertEquals(ArtSource.EsDe, store.source)
        assertEquals(ArtSource.EsDe, detector.detected.value)
    }

    @Test
    fun `detects RetroHrai and updates the live source`() = runBlocking {
        write(retroHrai, "gba/covers/A.png")

        detector.detect()

        assertEquals(ArtSource.RetroHrai, store.state.value)
        assertEquals(ArtSource.RetroHrai, store.savedSource)
    }

    @Test
    fun `saves nothing when no source has media and checks again later`() = runBlocking {
        detector.detect()
        assertNull(store.savedSource)

        write(retroHrai, "gba/covers/A.png")
        detector.detect(mediaChanged = true)

        assertEquals(ArtSource.RetroHrai, store.savedSource)
    }

    @Test
    fun `a change in storage access re-reads the folders`() = runBlocking {
        access = false
        detector.detect()
        write(retroHrai, "gba/covers/A.png")
        access = true

        detector.detect()

        assertEquals(ArtSource.RetroHrai, store.savedSource)
    }

    @Test
    fun `a saved result does not flip when the other media appears`() = runBlocking {
        write(retroHrai, "gba/covers/A.png")
        detector.detect()

        write(esde, "gba/covers/A.png")
        detector.detect(mediaChanged = true)

        assertEquals(ArtSource.RetroHrai, store.source)
    }

    @Test
    fun `never overrides a choice, including the default`() = runBlocking {
        store.setSource(ArtSource.EsDe)
        write(retroHrai, "gba/covers/A.png")

        detector.detect()

        assertEquals(ArtSource.EsDe, store.source)
        assertNull(detector.detected.value)
    }

    @Test
    fun `picking a source clears the detected result`() = runBlocking {
        write(retroHrai, "gba/covers/A.png")
        detector.detect()

        store.setSource(ArtSource.Scraped)

        assertNull(detector.detected.value)
    }

    @Test
    fun `scraped art is never selected`() = runBlocking {
        write(shared, "gba/covers/A.png")

        detector.detect()

        assertNull(store.savedSource)
    }

    @Test
    fun `an ES-DE path pointed at scraped art does not count as ES-DE`() = runBlocking {
        esdeRoot = shared
        write(shared, "gba/covers/A.png")

        detector.detect()

        assertNull(store.savedSource)
    }

    @Test
    fun `an ES-DE path pointed at RetroHrai detects RetroHrai`() = runBlocking {
        esdeRoot = retroHrai
        write(retroHrai, "gba/covers/A.png")

        detector.detect()

        assertEquals(ArtSource.RetroHrai, store.savedSource)
    }

    @Test
    fun `the detected result survives a restart`() = runBlocking {
        write(retroHrai, "gba/covers/A.png")
        detector.detect()

        val reloaded = ArtSourceStore(RuntimeEnvironment.getApplication())

        assertEquals(ArtSource.RetroHrai, reloaded.source)
        assertEquals(ArtSource.RetroHrai, reloaded.detectedState.value)
    }

    private fun write(root: File, path: String) {
        File(root, path).apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1))
        }
    }
}
