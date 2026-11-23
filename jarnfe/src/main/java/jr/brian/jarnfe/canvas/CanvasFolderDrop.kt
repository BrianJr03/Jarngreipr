package jr.brian.jarnfe.canvas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.GridRect
import kotlinx.coroutines.delay

private const val FOLDER_ARMED_SCALE = 1.08f
private val FolderHoverOutline = 2.dp
private val FolderArmedOutline = 3.dp

internal enum class CanvasFolderDropState { Hover, Armed }

/** A held app let go over an armed folder. Nothing has been saved yet. */
internal data class CanvasFolderDrop(
    val grab: CanvasGrab,
    val packageName: String,
    val folderTileId: String,
    val folderId: String,
)

/** Folders a held tile can go into, at their frozen spots so reflow can't pull one away. */
internal fun canvasFolderTargets(layout: CanvasLayout, grab: CanvasGrab): Map<String, GridRect> {
    if (grab.resizeCorner != null) return emptyMap()
    if (layout.items.none { it.id == grab.itemId && it is CanvasItem.AppItem }) return emptyMap()
    return layout.items
        .filterIsInstance<CanvasItem.FolderItem>()
        .mapNotNull { folder -> grab.baseline.placements[folder.id]?.let { folder.id to it } }
        .toMap()
}

/** True inside the tile's center, so brushing past a folder's edge never counts as aiming at it. */
internal fun CanvasBoardGeometry.inFolderCenter(rect: GridRect, point: Offset, inset: Float): Boolean {
    val origin = originOf(rect)
    val width = extentOf(rect.colSpan)
    val height = extentOf(rect.rowSpan)
    val insetX = width * inset
    val insetY = height * inset
    return point.x >= origin.x + insetX && point.x <= origin.x + width - insetX &&
        point.y >= origin.y + insetY && point.y <= origin.y + height - insetY
}

internal fun canvasFolderUnder(
    board: CanvasBoardGeometry,
    targets: Map<String, GridRect>,
    point: Offset,
    inset: Float,
): String? = targets.entries.firstOrNull { board.inFolderCenter(it.value, point, inset) }?.key

/** Times the hold over one folder. Moving to another folder or away starts it again. */
@Stable
internal class CanvasFolderHover(private val dwellMs: Long) {
    var targetId by mutableStateOf<String?>(null)
        private set

    var armed by mutableStateOf(false)
        private set

    private var since = 0L

    /** Returns true only on the update that arms it, so the haptic fires once. */
    fun update(id: String?, nowMs: Long): Boolean {
        if (id != targetId) {
            targetId = id
            armed = false
            since = nowMs
        }
        if (id == null || armed || nowMs - since < dwellMs) return false
        armed = true
        return true
    }

    fun remainingMs(nowMs: Long): Long = (dwellMs - (nowMs - since)).coerceAtLeast(0L)

    fun reset() {
        targetId = null
        armed = false
    }
}

internal fun canvasPendingFolderAdd(drop: CanvasFolderDrop, pages: List<JarnCanvasPage>): CanvasPendingFolderAdd =
    CanvasPendingFolderAdd(
        pageIndex = drop.grab.home.pageIndex,
        itemId = drop.grab.itemId,
        packageName = drop.packageName,
        folderPage = drop.grab.pageIndex,
        folderId = drop.folderId,
        app = pages.getOrNull(drop.grab.home.pageIndex)?.apps?.get(drop.packageName),
    )

/** Shows a pending app inside its folder, so the window reads the same as the finished move. */
internal fun JarnCanvasPage.withPendingFolderApps(pending: List<CanvasPendingFolderAdd>): JarnCanvasPage {
    val here = pending.filter { it.folderPage == index && it.app != null && it.folderId in folders }
    if (here.isEmpty()) return this
    val next = folders.toMutableMap()
    here.forEach { add ->
        val folder = next.getValue(add.folderId)
        val app = add.app ?: return@forEach
        if (folder.apps.none { it.packageName == app.packageName }) {
            next[add.folderId] = folder.copy(apps = folder.apps + app)
        }
    }
    return copy(folders = next)
}

/** A finger can rest on a folder without moving, so the dwell needs its own timer. */
@Composable
internal fun CanvasFolderDwellEffect(driver: CanvasTouchDriver) {
    val target = driver.folderHover.targetId
    LaunchedEffect(driver, target) {
        if (target == null) return@LaunchedEffect
        while (driver.folderHover.targetId == target && !driver.folderHover.armed) {
            delay(driver.folderDwellLeftMs().coerceAtLeast(1L))
            driver.tickFolderDwell()
        }
    }
}

/** A drop-target outline, separate from focus. Only the folder grows, and never in reduced motion. */
@Composable
internal fun Modifier.canvasFolderDropTarget(state: CanvasFolderDropState?, grows: Boolean): Modifier {
    val motion = JarnTheme.motion
    val colors = JarnTheme.colors
    val shape = JarnTheme.tileShape
    val target = if (grows && state == CanvasFolderDropState.Armed && !motion.reduced) FOLDER_ARMED_SCALE else 1f
    val scale by animateFloatAsState(target, motion.small.spec())
    if (state == null && scale == 1f) return this
    val outline = when (state) {
        CanvasFolderDropState.Hover -> Modifier.border(FolderHoverOutline, colors.accent, shape)
        CanvasFolderDropState.Armed -> Modifier.border(FolderArmedOutline, colors.success, shape)
        null -> Modifier
    }
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(outline)
}
