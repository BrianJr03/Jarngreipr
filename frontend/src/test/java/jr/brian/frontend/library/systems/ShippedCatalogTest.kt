package jr.brian.frontend.library.systems

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Pins the invariants of the actual files under
 * `frontend/src/main/assets/systems/`. The catalog loader eats errors
 * silently (so one bad file doesn't block everyone else at runtime);
 * this test is where authoring mistakes get caught before merge.
 *
 * Required invariants:
 *  1. Every file in the asset dir parses as a SystemDef.
 *  2. Every file's `id` matches its filename stem (lowercase).
 *  3. `displayName` is non-blank.
 *  4. `extensions` is non-empty — a system with no extensions can't
 *     match anything on scan, so there's no reason to ship an empty one.
 *  5. No two systems share an id or a folder alias — the scanner's
 *     `findByFolderAlias` is first-match-wins, and collisions would
 *     make ownership of a folder non-deterministic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ShippedCatalogTest {

    private val app = RuntimeEnvironment.getApplication()
    private val catalog = AssetSystemCatalog(app)

    @Test
    fun `every shipped system has a non-blank display name`() {
        val all = catalog.all()
        assertFalse(
            "No systems loaded — either the asset dir is empty or every file failed to parse. " +
                "Check frontend/src/main/assets/systems/*.json",
            all.isEmpty(),
        )
        for (def in all) {
            assertFalse(
                "System ${def.id} has a blank displayName",
                def.displayName.isBlank(),
            )
        }
    }

    @Test
    fun `every shipped system declares at least one extension`() {
        for (def in catalog.all()) {
            assertFalse(
                "System ${def.id} has no extensions — the scanner cannot match anything to it",
                def.extensions.isEmpty(),
            )
        }
    }

    @Test
    fun `shipped ids and folder aliases are unique across the catalog`() {
        val all = catalog.all()
        val seenIds = mutableSetOf<String>()
        val seenAliases = mutableMapOf<String, String>()
        for (def in all) {
            val id = def.id.lowercase()
            assertTrue("Duplicate id: ${def.id}", seenIds.add(id))

            // The id itself is implicitly an alias (via findByFolderAlias's
            // "direct id match first" branch), so include it in the uniqueness
            // check — one system's id can't collide with another's alias.
            val aliases = listOf(id) + def.folderAliases.map { it.lowercase() }
            for (alias in aliases) {
                val existing = seenAliases[alias]
                assertTrue(
                    "Folder alias '$alias' is claimed by both '${existing ?: "?"}' and '${def.id}' — " +
                        "the scanner's findByFolderAlias is first-match-wins, so one system " +
                        "would silently absorb the other's games",
                    existing == null,
                )
                seenAliases[alias] = def.id
            }
        }
    }

    @Test
    fun `every shipped file loads via findById under its canonical id`() {
        for (def in catalog.all()) {
            val roundTripped = catalog.findById(def.id)
            assertNotNull("System ${def.id} did not round-trip via findById", roundTripped)
            assertEquals(def, roundTripped)
        }
    }
}
