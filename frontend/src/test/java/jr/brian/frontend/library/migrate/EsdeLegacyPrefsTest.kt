package jr.brian.frontend.library.migrate

import android.app.Application
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Decodes legacy `esde_prefs` into the shapes the migration consumes.
 * All reads are forgiving; the tests pin the "malformed input →
 * empty, never throw" contract that keeps one bad field from
 * blocking the whole migration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EsdeLegacyPrefsTest {

    private lateinit var app: Application
    private lateinit var reader: EsdeLegacyPrefs

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("esde_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        reader = EsdeLegacyPrefs(app)
    }

    @Test
    fun `missing keys produce empty results`() {
        assertTrue(reader.romsPaths().isEmpty())
        assertTrue(reader.safTreeUris().isEmpty())
        assertTrue(reader.systemFolderMappings().isEmpty())
        assertTrue(reader.hiddenSystems().isEmpty())
        assertTrue(reader.hiddenGames().isEmpty())
        assertTrue(reader.gameEmulatorMap().isEmpty())
        assertTrue(reader.systemOrder().isEmpty())
    }

    @Test
    fun `romsPaths decodes a JSONArray of strings`() {
        write("roms_paths", """["/storage/emulated/0/Roms","/sdcard/Roms"]""")
        assertEquals(listOf("/storage/emulated/0/Roms", "/sdcard/Roms"), reader.romsPaths())
    }

    @Test
    fun `safTreeUris decodes a JSONObject of system to URI`() {
        write(
            "saf_tree_uris",
            """{"psx":"content://com.android.externalstorage.documents/tree/primary%3ARoms%2Fpsx"}""",
        )
        val map = reader.safTreeUris()
        assertEquals(1, map.size)
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary%3ARoms%2Fpsx",
            map["psx"],
        )
    }

    @Test
    fun `systemFolderMappings decodes serialized Mapping objects`() {
        write(
            "system_folder_mappings",
            """[{"systemName":"ps2","treeUri":"content://foo","displayPath":"SD:/MyGames"}]""",
        )
        val mappings = reader.systemFolderMappings()
        assertEquals(1, mappings.size)
        assertEquals("ps2", mappings.single().systemName)
        assertEquals("content://foo", mappings.single().treeUri)
        assertEquals("SD:/MyGames", mappings.single().displayPath)
    }

    @Test
    fun `hiddenSystems and hiddenGames decode string arrays`() {
        write("hidden_systems", """["3do","pc"]""")
        write("hidden_games", """["nes|Mario","snes|Zelda"]""")
        assertEquals(setOf("3do", "pc"), reader.hiddenSystems())
        assertEquals(setOf("nes|Mario", "snes|Zelda"), reader.hiddenGames())
    }

    @Test
    fun `gameEmulatorMap decodes per-game overrides`() {
        write(
            "game_emulator_map",
            """{"n64|Mario 64":"org.mupen64plusae.v3.fzurita.pro","snes|Zelda":"com.explusalpha.Snes9xPlus"}""",
        )
        val map = reader.gameEmulatorMap()
        assertEquals("org.mupen64plusae.v3.fzurita.pro", map["n64|Mario 64"])
        assertEquals("com.explusalpha.Snes9xPlus", map["snes|Zelda"])
    }

    @Test
    fun `malformed JSON yields empty without throwing`() {
        write("roms_paths", "not even json")
        write("saf_tree_uris", "{\"broken")
        write("system_folder_mappings", "garbage")
        assertTrue(reader.romsPaths().isEmpty())
        assertTrue(reader.safTreeUris().isEmpty())
        assertTrue(reader.systemFolderMappings().isEmpty())
    }

    private fun write(key: String, value: String) {
        app.getSharedPreferences("esde_prefs", Context.MODE_PRIVATE)
            .edit().putString(key, value).commit()
    }
}
