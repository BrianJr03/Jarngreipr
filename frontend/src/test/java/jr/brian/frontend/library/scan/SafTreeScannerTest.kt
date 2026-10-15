package jr.brian.frontend.library.scan

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Covers [SafTreeScanner]. A real `DocumentsContract` provider is
 * stubbed via Robolectric so the scanner can be exercised end-to-end
 * (projection query, cursor read, URI construction) without touching
 * the actual Storage Access Framework.
 *
 * Mirrors the scenarios in [FastWalkScannerTest] so a scenario that
 * holds on files holds on SAF too.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SafTreeScannerTest {

    private val catalog = StubSystemCatalog(
        SystemDef(
            id = "psx",
            displayName = "PlayStation",
            folderAliases = listOf("ps1"),
            extensions = setOf("iso", "bin", "cue", "chd", "pbp", "m3u"),
        ),
    )

    private lateinit var context: Context
    private lateinit var provider: FakeDocumentsProvider

    private val treeUri: Uri by lazy {
        DocumentsContract.buildTreeDocumentUri(AUTHORITY, "primary:Roms")
    }

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        provider = Robolectric.setupContentProvider(
            FakeDocumentsProvider::class.java,
            AUTHORITY,
        ) as FakeDocumentsProvider
    }

    @Test
    fun `scan emits one entry per file in a system directory`() = runBlocking {
        provider.addDir("primary:Roms", "primary:Roms/psx")
        provider.addFile("primary:Roms/psx", "Game.chd", mime = "application/octet-stream", size = 1024L)
        provider.addFile("primary:Roms/psx", "Other.iso", mime = "application/octet-stream", size = 2048L)

        val results = SafTreeScanner(context, catalog).scan(treeUri.toString())

        assertEquals(2, results.size)
        assertTrue(results.all { it.systemId == "psx" })
        assertTrue(results.all { it.absolutePath == null })
        assertTrue(results.all { it.documentUri != null })
        assertEquals(setOf("Game.chd", "Other.iso"), results.map { it.relativePath }.toSet())
        assertEquals(setOf(1024L, 2048L), results.map { it.fileSizeBytes }.toSet())
    }

    @Test
    fun `scan descends into subdirectories and preserves relative path`() = runBlocking {
        provider.addDir("primary:Roms", "primary:Roms/psx")
        provider.addDir("primary:Roms/psx", "primary:Roms/psx/Genre")
        provider.addDir("primary:Roms/psx/Genre", "primary:Roms/psx/Genre/RPG")
        provider.addFile("primary:Roms/psx/Genre/RPG", "Xenogears.iso", mime = "application/octet-stream")

        val results = SafTreeScanner(context, catalog).scan(treeUri.toString())
        assertEquals(1, results.size)
        assertEquals("Genre/RPG/Xenogears.iso", results.single().relativePath)
    }

    @Test
    fun `directories whose name ends in a ROM extension are one entry`() = runBlocking {
        provider.addDir("primary:Roms", "primary:Roms/psx")
        provider.addDir("primary:Roms/psx", "primary:Roms/psx/Xenogears.m3u")
        provider.addFile("primary:Roms/psx/Xenogears.m3u", "Disc1.bin", mime = "application/octet-stream")
        provider.addFile("primary:Roms/psx/Xenogears.m3u", "Disc2.bin", mime = "application/octet-stream")

        val results = SafTreeScanner(context, catalog).scan(treeUri.toString())
        assertEquals(1, results.size)
        val entry = results.single()
        assertEquals("Xenogears.m3u", entry.relativePath)
        assertTrue(entry.isDirectoryAsFile)
        assertFalse(results.any { it.relativePath.startsWith("Xenogears.m3u/") })
    }

    @Test
    fun `scan skips sidecar extensions and media directories`() = runBlocking {
        provider.addDir("primary:Roms", "primary:Roms/psx")
        provider.addFile("primary:Roms/psx", "Game.chd", mime = "application/octet-stream")
        provider.addFile("primary:Roms/psx", ".DS_Store", mime = "application/octet-stream")
        provider.addFile("primary:Roms/psx", "Game.chd.sav", mime = "application/octet-stream")
        provider.addDir("primary:Roms/psx", "primary:Roms/psx/media")
        provider.addFile("primary:Roms/psx/media", "Game.png", mime = "image/png")

        val results = SafTreeScanner(context, catalog).scan(treeUri.toString())
        assertEquals(1, results.size)
        assertEquals("Game.chd", results.single().relativePath)
    }

    @Test
    fun `unknown system folders are not walked`() = runBlocking {
        provider.addDir("primary:Roms", "primary:Roms/notreal")
        provider.addFile("primary:Roms/notreal", "Game.iso", mime = "application/octet-stream")

        val results = SafTreeScanner(context, catalog).scan(treeUri.toString())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `scan reports zero children when the tree URI is unparseable`() = runBlocking {
        val results = SafTreeScanner(context, catalog).scan("not-a-uri")
        assertTrue(results.isEmpty())
    }

    private companion object {
        const val AUTHORITY = "com.example.docs"
    }
}

/**
 * In-memory `DocumentsContract` provider used by the SAF scanner
 * tests. Only implements the query path for `buildChildDocumentsUriUsingTree`;
 * every other provider method throws so a scanner call that
 * accidentally reaches outside the projection path fails fast.
 */
class FakeDocumentsProvider : ContentProvider() {
    private data class Row(
        val parentDocId: String,
        val docId: String,
        val displayName: String,
        val mimeType: String,
        val size: Long,
        val lastModified: Long,
    )

    private val rows = mutableListOf<Row>()

    fun addDir(parentDocId: String, docId: String) {
        rows += Row(
            parentDocId = parentDocId,
            docId = docId,
            displayName = docId.substringAfterLast('/'),
            mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
            size = -1L,
            lastModified = 0L,
        )
    }

    fun addFile(parentDocId: String, name: String, mime: String, size: Long = 0L, mtime: Long = 0L) {
        rows += Row(
            parentDocId = parentDocId,
            docId = "$parentDocId/$name",
            displayName = name,
            mimeType = mime,
            size = size,
            lastModified = mtime,
        )
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val parentDocId = DocumentsContract.getDocumentId(uri)
        val cursor = MatrixCursor(
            projection ?: arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            )
        )
        rows.filter { it.parentDocId == parentDocId }.forEach { row ->
            val builder = cursor.newRow()
            (projection ?: arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            )).forEach { col ->
                val value: Any? = when (col) {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID -> row.docId
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME -> row.displayName
                    DocumentsContract.Document.COLUMN_MIME_TYPE -> row.mimeType
                    DocumentsContract.Document.COLUMN_SIZE -> row.size
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED -> row.lastModified
                    else -> null
                }
                builder.add(value)
            }
        }
        return cursor
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException()
}
