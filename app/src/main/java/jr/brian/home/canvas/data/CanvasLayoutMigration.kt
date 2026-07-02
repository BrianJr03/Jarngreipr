package jr.brian.home.canvas.data

import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.grid.firstFreeRect
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.canvas.model.defaultSpanFor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * On-disk schema migration for Unified Canvas layouts.
 *
 * v1 (initial Unified Canvas release): one `items` list per page; each item
 *   carried its own `col`/`row`/`colSpan`/`rowSpan`. A single `orientation`
 *   field decided which axis was bounded — switching orientations rendered
 *   the same single placement under the other axis bound, which is what
 *   caused items to drift off-screen.
 *
 * v2 (this change): items carry only entity references. Placement lives in
 *   per-orientation `verticalArrangement` / `horizontalArrangement` maps —
 *   independent grids over a shared content set. See [CanvasLayout].
 *
 * The migration:
 *   1. Decodes the v1 blob into [LegacyCanvasLayoutV1] to recover the
 *      placements the user actually had.
 *   2. Builds the v2 [CanvasLayout]:
 *      - shared `items` copied over (entity refs only)
 *      - the v1 `orientation`'s arrangement keeps every v1 placement exactly
 *      - the OTHER orientation gets each item auto-placed at its grid's next
 *        free cell via [firstFreeRect]
 *   3. Bumps version to [CanvasLayout.CURRENT_VERSION].
 *
 * Idempotent: re-running on already-v2 JSON is a parse-error fall-through —
 * the caller already has the v2 value and skips this path.
 */
internal object CanvasLayoutMigration {

    /**
     * Attempt to decode [rawJson] as a v1 blob and convert to v2. Returns null
     * if the input is not a recognizable v1 shape (no items, missing/invalid
     * fields, or a v2 blob — the caller decodes those directly).
     */
    fun tryMigrateV1(rawJson: String, json: Json): CanvasLayout? {
        val legacy = runCatching { json.decodeFromString<LegacyCanvasLayoutV1>(rawJson) }
            .getOrElse { return null }
        if (legacy.items.isEmpty()) return null
        return migrate(legacy)
    }

    /**
     * Classify [rawJson] as v1 or v2 by field shape, not by the `version`
     * field (the serializer omits default values, so v2 blobs typically have
     * no `version` key). v2 is recognized by the presence of any field
     * unique to the new schema; v1 by the `orientation` key (renamed to
     * `activeOrientation` in v2) or items with embedded `col`/`row`; an
     * empty JSON object is treated as a v2 default.
     */
    fun classify(rawJson: String, json: Json): Schema {
        val element = runCatching { json.parseToJsonElement(rawJson) }.getOrNull()
            ?: return Schema.Unknown
        val obj = element as? JsonObject ?: return Schema.Unknown
        if (V2_MARKERS.any { it in obj }) return Schema.V2
        if (V1_MARKERS.any { it in obj }) return Schema.V1
        val version = obj["version"]?.jsonPrimitive?.intOrNull
        if (version != null) {
            return if (version >= CanvasLayout.CURRENT_VERSION) Schema.V2 else Schema.V1
        }
        return Schema.V2
    }

    enum class Schema { V1, V2, Unknown }

    private val V2_MARKERS = setOf(
        "activeOrientation",
        "verticalArrangement",
        "horizontalArrangement",
        "verticalColumns",
        "horizontalRows"
    )

    private val V1_MARKERS = setOf("orientation", "columns", "rows")

    private fun migrate(legacy: LegacyCanvasLayoutV1): CanvasLayout {
        val orientation = legacy.orientation
        val items = legacy.items.map { it.toV2() }

        val preservedArrangement: MutableMap<String, GridRect> = LinkedHashMap()
        legacy.items.forEach { item ->
            preservedArrangement[item.id] = GridRect(
                col = item.col.coerceAtLeast(0),
                row = item.row.coerceAtLeast(0),
                colSpan = item.colSpan.coerceAtLeast(1),
                rowSpan = item.rowSpan.coerceAtLeast(1)
            )
        }

        val otherCrossAxis: Int
        val otherPushDirection: PushDirection
        when (orientation) {
            CanvasScrollOrientation.VERTICAL -> {
                otherCrossAxis = legacy.rows
                otherPushDirection = PushDirection.RIGHT
            }
            CanvasScrollOrientation.HORIZONTAL -> {
                otherCrossAxis = legacy.columns
                otherPushDirection = PushDirection.DOWN
            }
        }
        val otherArrangement: MutableMap<String, GridRect> = LinkedHashMap()
        legacy.items.forEach { item ->
            val (cs, rs) = defaultSpanFor(items.first { it.id == item.id })
            otherArrangement[item.id] = firstFreeRect(
                occupied = otherArrangement.values,
                crossAxisCount = otherCrossAxis,
                pushDirection = otherPushDirection,
                colSpan = cs,
                rowSpan = rs
            )
        }

        val (vertical, horizontal) = when (orientation) {
            CanvasScrollOrientation.VERTICAL -> preservedArrangement to otherArrangement
            CanvasScrollOrientation.HORIZONTAL -> otherArrangement to preservedArrangement
        }
        return CanvasLayout(
            activeOrientation = orientation,
            verticalColumns = legacy.columns,
            horizontalRows = legacy.rows,
            items = items,
            verticalArrangement = vertical,
            horizontalArrangement = horizontal,
            editMode = legacy.editMode,
            version = CanvasLayout.CURRENT_VERSION
        )
    }
}

@Serializable
internal data class LegacyCanvasLayoutV1(
    val orientation: CanvasScrollOrientation = CanvasScrollOrientation.VERTICAL,
    val columns: Int = CanvasLayout.DEFAULT_COLUMNS,
    val rows: Int = CanvasLayout.DEFAULT_ROWS,
    val items: List<LegacyCanvasItemV1> = emptyList(),
    val editMode: Boolean = false,
    val version: Int = 1
)

@Serializable
internal sealed class LegacyCanvasItemV1 {
    abstract val id: String
    abstract val col: Int
    abstract val row: Int
    abstract val colSpan: Int
    abstract val rowSpan: Int

    fun toV2(): CanvasItem = when (this) {
        is App -> CanvasItem.AppItem(id = id, packageName = packageName)
        is Folder -> CanvasItem.FolderItem(id = id, folderId = folderId)
        is Rom -> CanvasItem.RomItem(id = id, romKey = romKey)
        is Widget -> CanvasItem.WidgetItem(id = id, widgetId = widgetId)
        is RssLauncher -> CanvasItem.RssLauncherItem(id = id)
    }

    @Serializable
    @SerialName("app")
    data class App(
        override val id: String,
        override val col: Int = 0,
        override val row: Int = 0,
        override val colSpan: Int = 1,
        override val rowSpan: Int = 1,
        val packageName: String
    ) : LegacyCanvasItemV1()

    @Serializable
    @SerialName("folder")
    data class Folder(
        override val id: String,
        override val col: Int = 0,
        override val row: Int = 0,
        override val colSpan: Int = 1,
        override val rowSpan: Int = 1,
        val folderId: String
    ) : LegacyCanvasItemV1()

    @Serializable
    @SerialName("rom")
    data class Rom(
        override val id: String,
        override val col: Int = 0,
        override val row: Int = 0,
        override val colSpan: Int = 1,
        override val rowSpan: Int = 1,
        val romKey: String
    ) : LegacyCanvasItemV1()

    @Serializable
    @SerialName("widget")
    data class Widget(
        override val id: String,
        override val col: Int = 0,
        override val row: Int = 0,
        override val colSpan: Int = 2,
        override val rowSpan: Int = 2,
        val widgetId: Int
    ) : LegacyCanvasItemV1()

    @Serializable
    @SerialName("rss_launcher")
    data class RssLauncher(
        override val id: String,
        override val col: Int = 0,
        override val row: Int = 0,
        override val colSpan: Int = 1,
        override val rowSpan: Int = 1
    ) : LegacyCanvasItemV1()
}
