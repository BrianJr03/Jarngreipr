package jr.brian.jarnfe.canvas

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import kotlinx.coroutines.flow.Flow

const val JARN_CANVAS_MAX_PAGES = 6

enum class JarnCanvasPageKind { Canvas, Apps, AppsAndWidgets, AppDrawer, Rss }

@Immutable
data class JarnCanvasApp(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val iconPath: String? = null,
    val iconVersion: Long = 0L,
)

@Immutable
data class JarnCanvasFolder(
    val id: String,
    val name: String,
    val apps: List<JarnCanvasApp>,
    val backgroundColorArgb: Int? = null,
    val backgroundImagePath: String? = null,
)

@Immutable
data class JarnCanvasRom(
    val key: String,
    val title: String,
    val systemName: String,
    val path: String,
    val artPath: String? = null,
    val emulatorPackage: String? = null,
    val launchCommand: String? = null,
)

@Immutable
data class JarnCanvasMusic(
    val title: String,
    val artist: String? = null,
    val imageUrl: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
)

/** [apps], [folders] and [roms] resolve the references held by this page's tiles. */
@Immutable
data class JarnCanvasPage(
    val index: Int,
    val kind: JarnCanvasPageKind,
    val layout: CanvasLayout = CanvasLayout(),
    val apps: Map<String, JarnCanvasApp> = emptyMap(),
    val folders: Map<String, JarnCanvasFolder> = emptyMap(),
    val roms: Map<String, JarnCanvasRom> = emptyMap(),
)

@Immutable
data class JarnCanvasSnapshot(
    val pages: List<JarnCanvasPage> = emptyList(),
    val homePage: Int = 0,
    val recent: List<JarnCanvasRom> = emptyList(),
    val music: JarnCanvasMusic? = null,
)

/** The saved pages Classic also reads. Writes keep the stored format exactly as it is. */
interface JarnCanvasData : JarnCanvasTiles, JarnCanvasFolders {
    val snapshot: Flow<JarnCanvasSnapshot>

    /** Writes one solved arrangement in a single save. The other orientation is left alone. */
    fun commitPlacements(
        pageIndex: Int,
        orientation: CanvasScrollOrientation,
        placements: Map<String, GridRect>,
    )

    /** The smallest (columns, rows) a widget can shrink to at this cell size. */
    fun widgetMinSpans(widgetId: Int, cellSizePx: Float): Pair<Int, Int>

    fun setHomePage(index: Int)

    /** Returns the new page's index, or null at the page limit. */
    suspend fun addCanvasPage(): Int?

    suspend fun deletePage(index: Int)

    suspend fun movePage(from: Int, to: Int)
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnCanvasDataProvider {
    val jarnCanvasData: JarnCanvasData
}

fun Context.jarnCanvasDataOrNull(): JarnCanvasData? =
    (applicationContext as? JarnCanvasDataProvider)?.jarnCanvasData

/** Old page indices in their new order after moving [from] to [to]. */
fun jarnPageOrderAfterMove(size: Int, from: Int, to: Int): List<Int> {
    val order = (0 until size).toMutableList()
    if (from !in order.indices || to !in order.indices) return order
    order.add(to, order.removeAt(from))
    return order
}

/** Where a selected page lands after page [deleted] is removed from [size] pages. */
fun jarnPageAfterDelete(selected: Int, deleted: Int, size: Int): Int {
    val shifted = if (selected > deleted) selected - 1 else selected
    return shifted.coerceIn(0, (size - 2).coerceAtLeast(0))
}
