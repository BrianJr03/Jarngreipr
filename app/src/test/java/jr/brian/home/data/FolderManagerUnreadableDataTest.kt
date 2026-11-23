package jr.brian.home.data

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class FolderManagerUnreadableDataTest {
    private lateinit var context: Context
    private lateinit var manager: FolderManager

    private val pageKey = stringPreferencesKey("folders_page_$PAGE")

    private val edits: List<Pair<String, suspend FolderManager.() -> Boolean>> = listOf(
        "create" to { createFolder(PAGE, folder("new")) },
        "update" to { updateFolder(PAGE, folder("a", name = "Changed")) },
        "rename" to { renameFolder(PAGE, "a", "Changed") },
        "updateApps" to { updateFolderApps(PAGE, "a", listOf("x")) },
        "delete" to { deleteFolder(PAGE, "a") },
        "background" to { updateFolderBackground(PAGE, "a", 7, null) },
        "position" to { updateFolderPosition(PAGE, "a", AppPosition("a", 9f, 8f, 70f)) },
        "removeApp" to { removeAppFromFolders(PAGE, "one") },
    )

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.folderDataStore.edit { it.clear() }
        manager = FolderManager(context)
    }

    @Test
    fun `every edit leaves each kind of unreadable data byte-identical and reports failure`() = runBlocking {
        UNREADABLE.forEach { stored ->
            edits.forEach { (name, edit) ->
                store(pageKey.name, stored)
                assertFalse("$name on <$stored>", manager.edit())
                assertEquals("$name on <$stored>", stored, raw(pageKey.name))
            }
        }
    }

    @Test
    fun `a refused edit leaves other pages alone and announces the refusal`() = runBlocking {
        val otherPage = "folders_page_${PAGE + 1}"
        manager.createFolder(PAGE + 1, folder("kept"))
        val otherBytes = raw(otherPage)
        store(pageKey.name, UNREADABLE.first())
        val announced = async(start = CoroutineStart.UNDISPATCHED) {
            manager.editRefused.first()
        }

        assertFalse(manager.deleteFolder(PAGE, "a"))

        withTimeout(2_000) { announced.await() }
        assertEquals(otherBytes, raw(otherPage))
    }

    @Test
    fun `refused delete and background edits keep the managed image file`() = runBlocking {
        val image = managedImage("kept.png")
        val truncated = """[{"id":"a","name":"A","backgroundImagePath":"${image.absolutePath}""""
        store(pageKey.name, truncated)

        assertFalse(manager.deleteFolder(PAGE, "a"))
        assertFalse(manager.updateFolderBackground(PAGE, "a", 1, null))

        assertTrue(image.exists())
        assertEquals(truncated, raw(pageKey.name))
    }

    @Test
    fun `a missing key still behaves as an empty list`() = runBlocking {
        assertTrue(manager.createFolder(PAGE, folder("a")))
        assertEquals(listOf("a"), ids())
    }

    @Test
    fun `valid data still updates through every edit`() = runBlocking {
        seed()
        assertTrue(manager.createFolder(PAGE, folder("c")))
        assertEquals(listOf("a", "b", "c"), ids())

        assertTrue(manager.updateFolder(PAGE, folder("a", name = "Updated")))
        assertEquals("Updated", stored("a").name)

        assertTrue(manager.renameFolder(PAGE, "a", "Renamed"))
        assertEquals("Renamed", stored("a").name)

        assertTrue(manager.updateFolderApps(PAGE, "a", listOf("x", "y")))
        assertEquals(listOf("x", "y"), stored("a").appPackageNames)

        assertTrue(manager.updateFolderBackground(PAGE, "a", 42, null))
        assertEquals(42, stored("a").backgroundColorArgb)

        assertTrue(manager.updateFolderPosition(PAGE, "a", AppPosition("a", 3f, 4f, 80f)))
        assertEquals(3f, stored("a").position.x)
        assertEquals(4f, stored("a").position.y)
        assertEquals(80f, stored("a").position.iconSize)

        assertTrue(manager.removeAppFromFolders(PAGE, "two"))
        assertEquals(listOf("a", "c"), ids().filter { it != "b" })
        assertNull(manager.getFolder(PAGE, "b"))

        assertTrue(manager.deleteFolder(PAGE, "c"))
        assertEquals(listOf("a"), ids())
    }

    @Test
    fun `emptying a folder through updateFolderApps still removes it`() = runBlocking {
        seed()
        assertTrue(manager.updateFolderApps(PAGE, "a", emptyList()))
        assertEquals(listOf("b"), ids())
    }

    @Test
    fun `valid delete and background change release the old managed image`() = runBlocking {
        val deleted = managedImage("deleted.png")
        val replaced = managedImage("replaced.png")
        manager.createFolder(PAGE, folder("a", image = deleted.absolutePath))
        manager.createFolder(PAGE, folder("b", image = replaced.absolutePath))

        assertTrue(manager.deleteFolder(PAGE, "a"))
        assertTrue(manager.updateFolderBackground(PAGE, "b", 5, null))

        assertFalse(deleted.exists())
        assertFalse(replaced.exists())
    }

    @Test
    fun `reordering pages carries unreadable bytes to the new index`() = runBlocking {
        val unreadable = UNREADABLE.first()
        store("folders_page_0", unreadable)
        manager.createFolder(1, folder("a"))
        val readable = raw("folders_page_1")

        manager.reorderPages(mapOf(0 to 1, 1 to 0))

        assertEquals(readable, raw("folders_page_0"))
        assertEquals(unreadable, raw("folders_page_1"))
    }

    @Test
    fun `removing another page shifts unreadable bytes without changing them`() = runBlocking {
        val unreadable = UNREADABLE.first()
        manager.createFolder(0, folder("a"))
        store("folders_page_1", unreadable)

        manager.removePage(0)

        assertEquals(unreadable, raw("folders_page_0"))
        assertNull(raw("folders_page_1"))
    }

    private suspend fun seed() {
        manager.createFolder(PAGE, folder("a"))
        manager.createFolder(PAGE, folder("b", apps = listOf("two")))
    }

    private suspend fun ids() = manager.getFolders(PAGE).first().map { it.id }

    private suspend fun stored(id: String) = manager.getFolders(PAGE).first().first { it.id == id }

    private suspend fun store(key: String, value: String) {
        context.folderDataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    private suspend fun raw(key: String): String? =
        context.folderDataStore.data.first()[stringPreferencesKey(key)]

    private fun managedImage(name: String): File {
        val dir = File(context.filesDir, "folder_backgrounds").apply { mkdirs() }
        return File(dir, name).apply { writeText("image") }
    }

    private fun folder(
        id: String,
        name: String = id,
        apps: List<String> = listOf("one"),
        image: String? = null,
    ) = Folder(
        id = id,
        name = name,
        appPackageNames = apps,
        position = AppPosition(id, 1f, 2f, 64f),
        backgroundImagePath = image,
    )

    private companion object {
        const val PAGE = 3
        val UNREADABLE = listOf(
            "not json",
            "",
            "null",
            """{"id":"a"}""",
            """[{"id":"a","name":"A"}]""",
            """[{"id":"a","name":"A","appPackageNames":["one"],"x":1.0""",
        )
    }
}
