package jr.brian.home.canvas.data

import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.canvas.model.satisfiesInvariant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins migration of stored v1 layouts now that the v2 model lives in :core-model. */
class CanvasLegacyFormatCompatibilityTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val v1Blob = """
        {
          "orientation": "VERTICAL",
          "columns": 4,
          "rows": 6,
          "items": [
            {"type":"app","id":"a","col":3,"row":0,"colSpan":1,"rowSpan":1,"packageName":"com.legacy.a"},
            {"type":"folder","id":"f","col":0,"row":1,"colSpan":1,"rowSpan":1,"folderId":"folder-1"},
            {"type":"rom","id":"r","col":1,"row":1,"colSpan":1,"rowSpan":1,"romKey":"snes/game.smc"},
            {"type":"widget","id":"w","col":2,"row":2,"colSpan":2,"rowSpan":2,"widgetId":7}
          ],
          "version": 1
        }
    """.trimIndent()

    @Test
    fun `v1 blob is classified and migrated with placements intact`() {
        assertEquals(CanvasLayoutMigration.Schema.V1, CanvasLayoutMigration.classify(v1Blob, json))

        val migrated = CanvasLayoutMigration.tryMigrateV1(v1Blob, json)

        assertNotNull(migrated)
        val layout = migrated!!
        assertEquals(CanvasLayout.CURRENT_VERSION, layout.version)
        assertEquals(CanvasScrollOrientation.VERTICAL, layout.activeOrientation)
        assertTrue(layout.satisfiesInvariant())
        assertEquals(GridRect(3, 0, 1, 1), layout.verticalArrangement.getValue("a"))
        assertEquals(GridRect(2, 2, 2, 2), layout.verticalArrangement.getValue("w"))
        assertEquals(
            CanvasItem.AppItem(id = "a", packageName = "com.legacy.a"),
            layout.items.first { it.id == "a" }
        )
    }

    @Test
    fun `current blob is classified as v2`() {
        val blob = """{"verticalArrangement":{},"horizontalArrangement":{},"items":[]}"""
        assertEquals(CanvasLayoutMigration.Schema.V2, CanvasLayoutMigration.classify(blob, json))
    }
}
