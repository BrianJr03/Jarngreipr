package jr.brian.jarnfe.editor

import jr.brian.core.model.MediaKind
import jr.brian.core.model.theme.GameTextField
import jr.brian.core.model.theme.ThemeDateField
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ThemeVisibleWhen

/** The kinds of piece the player can add, each with a sensible first look. */
internal enum class ThemeEditorNewLayer { Picture, Text, Rating, Date, Panel, Video }

private const val NEW_SIZE = 0.2f
private const val NEW_TEXT_HEIGHT = 0.08f
private const val DUPLICATE_OFFSET = 0.02f
private const val GRID_COLUMNS = 5
private const val LIST_ROWS = 8

/** Adds [kind] in the middle of the screen, on top of everything, and selects it. */
internal fun ThemeEditorState.withNewLayer(kind: ThemeEditorNewLayer): ThemeEditorState {
    val id = freeId(kind.name.lowercase())
    val z = (viewSpec?.layers?.maxOfOrNull { it.z } ?: 0) + 1
    val height = if (kind == ThemeEditorNewLayer.Text || kind == ThemeEditorNewLayer.Date) NEW_TEXT_HEIGHT else NEW_SIZE
    val rect = ThemeRect(x = 0.5f, y = 0.5f, width = NEW_SIZE, height = height, originX = 0.5f, originY = 0.5f)
    val layer = when (kind) {
        ThemeEditorNewLayer.Picture -> ThemeLayer.Picture(id, rect, z, source = ThemePicture.GameMedia(MediaKind.Cover))
        ThemeEditorNewLayer.Text -> ThemeLayer.Text(id, rect, z, content = ThemeText.Game(GameTextField.Title))
        ThemeEditorNewLayer.Rating -> ThemeLayer.Rating(id, rect, z)
        ThemeEditorNewLayer.Date -> ThemeLayer.Date(id, rect, z, field = ThemeDateField.ReleaseDate)
        ThemeEditorNewLayer.Panel -> ThemeLayer.Panel(id, rect, z)
        ThemeEditorNewLayer.Video -> ThemeLayer.Video(id, rect, z)
    }
    return withViewSpec { it.copy(layers = it.layers + layer) }.copy(selected = ThemeEditorTarget.Layer(id))
}

/** A copy just below and right of the original, on top, and selected. */
internal fun ThemeEditorState.withDuplicate(id: String): ThemeEditorState {
    val original = layer(id) ?: return this
    val copyId = freeId(id)
    val z = (viewSpec?.layers?.maxOfOrNull { it.z } ?: 0) + 1
    val rect = original.rect.copy(x = original.rect.x + DUPLICATE_OFFSET, y = original.rect.y + DUPLICATE_OFFSET)
    val copy = original.withRect(rect).withIdAndZ(copyId, z)
    return withViewSpec { it.copy(layers = it.layers + copy) }.copy(selected = ThemeEditorTarget.Layer(copyId))
}

internal fun ThemeEditorState.withoutLayer(id: String): ThemeEditorState =
    withViewSpec { it.copy(layers = it.layers.filterNot { layer -> layer.id == id }) }
        .copy(selected = ThemeEditorTarget.Navigator)

/**
 * Swaps [id] with its neighbor one step up ([step] 1) or down (-1). The list sits at 0, so a layer
 * sent back past the lowest one goes below the list.
 */
internal fun ThemeEditorState.withLayerOrder(id: String, step: Int): ThemeEditorState {
    val ordered = viewSpec?.layers?.sortedBy { it.z } ?: return this
    val index = ordered.indexOfFirst { it.id == id }
    val moving = ordered.getOrNull(index) ?: return this
    val neighbor = ordered.getOrNull(index + step)
        ?: return withLayer(id) { it.withIdAndZ(it.id, moving.z + step) }
    val movingZ = if (neighbor.z == moving.z) moving.z + step else neighbor.z
    return withLayer(id) { it.withIdAndZ(it.id, movingZ) }
        .withLayer(neighbor.id) { it.withIdAndZ(it.id, moving.z) }
}

internal fun ThemeEditorState.withLayer(id: String, transform: (ThemeLayer) -> ThemeLayer): ThemeEditorState =
    withViewSpec { view -> view.copy(layers = view.layers.map { if (it.id == id) transform(it) else it }) }

internal fun ThemeEditorState.withPrimary(transform: (ThemePrimary) -> ThemePrimary): ThemeEditorState =
    withViewSpec { it.copy(primary = transform(it.primary)) }

/** Changes the list's type, keeping its box and art. */
internal fun ThemePrimary.asType(next: ThemePrimary): ThemePrimary = when (next) {
    is ThemePrimary.Shelf -> ThemePrimary.Shelf(box = box, art = art)
    is ThemePrimary.Grid -> ThemePrimary.Grid(box = box, art = art, columns = GRID_COLUMNS)
    is ThemePrimary.List -> ThemePrimary.List(box = box, art = art, rows = LIST_ROWS)
}

/** One of each list type, in the order A steps through them. */
internal val PrimaryTypes: List<ThemePrimary> = listOf(
    ThemePrimary.Shelf(ThemeRect(0f, 0f, 1f, 1f)),
    ThemePrimary.Grid(ThemeRect(0f, 0f, 1f, 1f)),
    ThemePrimary.List(ThemeRect(0f, 0f, 1f, 1f)),
)

internal fun ThemePrimary.nextType(): ThemePrimary {
    val index = PrimaryTypes.indexOfFirst { it::class == this::class }
    return asType(PrimaryTypes[(index + 1) % PrimaryTypes.size])
}

private fun ThemeEditorState.freeId(base: String): String {
    val taken = viewSpec?.layers?.mapTo(HashSet()) { it.id }.orEmpty()
    return generateSequence(1) { it + 1 }.map { "$base-$it" }.first { it !in taken }
}

private fun ThemeLayer.withIdAndZ(id: String, z: Int): ThemeLayer = when (this) {
    is ThemeLayer.Picture -> copy(id = id, z = z)
    is ThemeLayer.Text -> copy(id = id, z = z)
    is ThemeLayer.Rating -> copy(id = id, z = z)
    is ThemeLayer.Date -> copy(id = id, z = z)
    is ThemeLayer.Panel -> copy(id = id, z = z)
    is ThemeLayer.Video -> copy(id = id, z = z)
}

internal fun ThemeLayer.withOpacity(opacity: Float): ThemeLayer = when (this) {
    is ThemeLayer.Picture -> copy(opacity = opacity)
    is ThemeLayer.Text -> copy(opacity = opacity)
    is ThemeLayer.Rating -> copy(opacity = opacity)
    is ThemeLayer.Date -> copy(opacity = opacity)
    is ThemeLayer.Panel -> copy(opacity = opacity)
    is ThemeLayer.Video -> copy(opacity = opacity)
}

internal fun ThemeLayer.withVisibleWhen(rule: ThemeVisibleWhen?): ThemeLayer = when (this) {
    is ThemeLayer.Picture -> copy(visibleWhen = rule)
    is ThemeLayer.Text -> copy(visibleWhen = rule)
    is ThemeLayer.Rating -> copy(visibleWhen = rule)
    is ThemeLayer.Date -> copy(visibleWhen = rule)
    is ThemeLayer.Panel -> copy(visibleWhen = rule)
    is ThemeLayer.Video -> copy(visibleWhen = rule)
}
