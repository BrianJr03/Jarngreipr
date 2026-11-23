package jr.brian.home.canvas.jarn

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.GameImageType
import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.EsdeContentScale
import jr.brian.home.data.CustomAppNameManager
import jr.brian.home.data.FolderManager
import jr.brian.home.data.NowPlayingManager
import jr.brian.home.data.PageTypeManager
import jr.brian.home.data.PinnedRomManager
import jr.brian.home.data.RssRepository
import jr.brian.home.data.WidgetPreferences
import jr.brian.home.model.PageType
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.model.rss.RssItem
import jr.brian.home.util.WallpaperUtils
import jr.brian.home.viewmodels.WidgetViewModel
import jr.brian.jarnfe.canvas.JarnCanvasApp
import jr.brian.jarnfe.canvas.JarnCanvasNewTile
import jr.brian.jarnfe.canvas.JarnCanvasRom
import jr.brian.jarnfe.canvas.JarnCanvasRssItem
import jr.brian.jarnfe.canvas.JarnCanvasTileMove
import jr.brian.jarnfe.canvas.JarnCanvasTileMoved
import jr.brian.jarnfe.canvas.JarnCanvasTiles
import jr.brian.jarnfe.canvas.JarnWidgetBind
import jr.brian.jarnfe.canvas.JarnWidgetHealth
import jr.brian.jarnfe.canvas.JarnWidgetLease
import jr.brian.jarnfe.canvas.JarnWidgetProvider
import jr.brian.jarnfe.canvas.jarnLayoutWithArrival
import jr.brian.jarnfe.canvas.jarnTileMayChangePage
import jr.brian.jarnfe.canvas.showJarnFolderEditFailed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val PHOTO_DIR = "canvas_photos"
private const val PHOTO_PREFIX = "photo_"
private const val FILE_SCHEME = "file://"
private const val WIDGET_CELL_PX = 80f
private const val WIDGET_MAX_SPAN = 6

/** True while any saved tile or widget page still points at [widgetId]. */
internal fun isWidgetIdReferenced(
    layouts: Collection<CanvasLayout>,
    widgetPageIds: Collection<Int>,
    widgetId: Int,
): Boolean = widgetId in widgetPageIds || layouts.any { layout ->
    layout.items.any { it is CanvasItem.WidgetItem && it.widgetId == widgetId }
}

internal fun isPhotoReferenced(layouts: Collection<CanvasLayout>, uri: String): Boolean =
    layouts.any { layout -> layout.items.any { it is CanvasItem.PhotoContainer && it.imageUri == uri } }

/** The file behind [uri], only when it sits inside [photoDir]. Anything else is not ours to delete. */
internal fun ownedPhotoFile(photoDir: File, uri: String?): File? {
    if (uri == null || !uri.startsWith(FILE_SCHEME)) return null
    val file = File(uri.removePrefix(FILE_SCHEME))
    val root = photoDir.canonicalPath + File.separator
    return file.takeIf { it.canonicalPath.startsWith(root) }
}

internal fun AppWidgetProviderInfo.canvasSpan(): Pair<Int, Int> =
    (minWidth / WIDGET_CELL_PX).toInt().coerceIn(1, WIDGET_MAX_SPAN) to
        (minHeight / WIDGET_CELL_PX).toInt().coerceIn(1, WIDGET_MAX_SPAN)

@Singleton
class AppJarnCanvasTiles @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val layouts: CanvasLayoutManager,
    private val pageTypes: PageTypeManager,
    private val folders: FolderManager,
    private val pinnedRoms: PinnedRomManager,
    private val customNames: CustomAppNameManager,
    private val widgetPreferences: WidgetPreferences,
    private val nowPlaying: Provider<NowPlayingManager>,
    private val rss: Provider<RssRepository>,
) : JarnCanvasTiles {

    /** Shared with page edits, so a tile change can never land on a page that just shifted. */
    internal val edits = Mutex()

    private val widgetManager: AppWidgetManager by lazy { AppWidgetManager.getInstance(context) }

    override val rssItems: Flow<List<JarnCanvasRssItem>> = flow {
        emitAll(rss.get().allItems.map { items -> items.map { it.toJarnRssItem() } })
    }.flowOn(Dispatchers.IO)

    override suspend fun addTile(pageIndex: Int, tile: JarnCanvasNewTile): String? = edit {
        if (!isCanvasPage(pageIndex)) return@edit null
        when (tile) {
            is JarnCanvasNewTile.App -> place(
                pageIndex,
                CanvasItem.AppItem("app-${tile.packageName}-${UUID.randomUUID()}", tile.packageName),
            )
            is JarnCanvasNewTile.Folder -> addFolder(pageIndex, tile)
            is JarnCanvasNewTile.Rom -> addRom(pageIndex, tile.key)
            is JarnCanvasNewTile.Widget -> {
                val item = CanvasItem.WidgetItem("widget-${tile.widgetId}", tile.widgetId)
                layouts.addItem(pageIndex, item, colSpan = tile.columns, rowSpan = tile.rows)
                item.id
            }
            JarnCanvasNewTile.Art -> place(
                pageIndex,
                CanvasItem.EsdeArtItem(
                    id = "esde-${GameImageType.Fanart.name.lowercase()}-${UUID.randomUUID()}",
                    imageType = GameImageType.Fanart,
                    contentScale = EsdeContentScale.CROP,
                ),
            )
            JarnCanvasNewTile.LastPlayed ->
                place(pageIndex, CanvasItem.LastPlayedItem("last-played-${UUID.randomUUID()}"))
            JarnCanvasNewTile.Photo ->
                place(pageIndex, CanvasItem.PhotoContainer("photo-${UUID.randomUUID()}"))
            JarnCanvasNewTile.RssLauncher ->
                place(pageIndex, CanvasItem.RssLauncherItem("rss-${UUID.randomUUID()}"))
            JarnCanvasNewTile.RssMusic ->
                place(pageIndex, CanvasItem.RssMusicItem("rss-music-${UUID.randomUUID()}"))
        }
    }

    override fun updateTile(pageIndex: Int, item: CanvasItem) {
        if (!isCanvasPage(pageIndex)) return
        if (layouts.getLayout(pageIndex).items.none { it.id == item.id }) return
        layouts.addItem(pageIndex, item)
    }

    override suspend fun removeTile(pageIndex: Int, itemId: String) = edit {
        if (!isCanvasPage(pageIndex)) return@edit
        val item = layouts.getLayout(pageIndex).items.firstOrNull { it.id == itemId } ?: return@edit
        if (item is CanvasItem.FolderItem &&
            !folders.deleteFolder(pageIndex, item.folderId, CanvasTabType.VALUE)
        ) {
            showJarnFolderEditFailed()
            return@edit
        }
        layouts.removeItem(pageIndex, itemId)
        releaseAfterRemoval(pageIndex, item)
    }

    /** Never releases anything: the tile keeps its id, widget and photo on the new page. */
    override suspend fun moveTile(move: JarnCanvasTileMove): JarnCanvasTileMoved? = edit {
        if (move.fromPage == move.toPage || !isCanvasPage(move.fromPage) || !isCanvasPage(move.toPage)) {
            return@edit null
        }
        val source = layouts.getLayout(move.fromPage)
        val destination = layouts.getLayout(move.toPage)
        val item = source.items.firstOrNull { it.id == move.itemId } ?: return@edit null
        if (move.itemId !in move.placements || !jarnTileMayChangePage(item, destination)) return@edit null
        val romKey = (item as? CanvasItem.RomItem)?.romKey
        val rom = romKey?.let { key ->
            pinOn(move.fromPage, key) ?: pinnedRoms.allPinnedRoms.first().firstOrNull { it.key == key }
                ?: return@edit null
        }
        val pinAdded = romKey != null && pinOn(move.toPage, romKey) == null
        if (pinAdded && rom != null) pinnedRoms.addPinnedRom(move.toPage, rom, CanvasTabType.VALUE)
        layouts.replaceLayout(move.toPage, jarnLayoutWithArrival(destination, source, item, move))
        layouts.removeItem(move.fromPage, move.itemId)
        val sourceAfter = layouts.getLayout(move.fromPage)
        val pinRemoved = romKey != null &&
            sourceAfter.items.none { it is CanvasItem.RomItem && it.romKey == romKey }
        if (pinRemoved && romKey != null) {
            pinnedRoms.removePinnedRom(move.fromPage, romKey, CanvasTabType.VALUE)
        }
        JarnCanvasTileMoved(
            move = move,
            sourceBefore = source,
            sourceAfter = sourceAfter,
            destinationBefore = destination,
            destinationAfter = layouts.getLayout(move.toPage),
            romKey = romKey,
            pinAdded = pinAdded,
            pinRemoved = pinRemoved,
        )
    }

    /** Restores the source first, so this direction can't lose the tile either. */
    override suspend fun undoTileMove(moved: JarnCanvasTileMoved): Boolean = edit {
        val move = moved.move
        val untouched = isCanvasPage(move.fromPage) && isCanvasPage(move.toPage) &&
            layouts.getLayout(move.fromPage) == moved.sourceAfter &&
            layouts.getLayout(move.toPage) == moved.destinationAfter
        if (!untouched) return@edit false
        val romKey = moved.romKey
        if (romKey != null && moved.pinRemoved) {
            val rom = pinOn(move.toPage, romKey) ?: return@edit false
            pinnedRoms.addPinnedRom(move.fromPage, rom, CanvasTabType.VALUE)
        }
        layouts.replaceLayout(move.fromPage, moved.sourceBefore)
        layouts.replaceLayout(move.toPage, moved.destinationBefore)
        if (romKey != null && moved.pinAdded) {
            pinnedRoms.removePinnedRom(move.toPage, romKey, CanvasTabType.VALUE)
        }
        true
    }

    private suspend fun pinOn(pageIndex: Int, key: String) =
        pinnedRoms.getPinnedRoms(pageIndex, CanvasTabType.VALUE).first().firstOrNull { it.key == key }

    /** Runs after the tile is gone, so a failure can never leave a tile on a released resource. */
    private suspend fun releaseAfterRemoval(pageIndex: Int, item: CanvasItem) {
        val saved = layouts.layoutsByPage.value
        when (item) {
            is CanvasItem.RomItem -> {
                val stillPlaced = saved[pageIndex]?.items.orEmpty()
                    .any { it is CanvasItem.RomItem && it.romKey == item.romKey }
                if (!stillPlaced) pinnedRoms.removePinnedRom(pageIndex, item.romKey, CanvasTabType.VALUE)
            }
            is CanvasItem.PhotoContainer -> {
                val uri = item.imageUri
                if (uri != null && !isPhotoReferenced(saved.values, uri)) {
                    runCatching { ownedPhotoFile(File(context.filesDir, PHOTO_DIR), uri)?.delete() }
                }
            }
            is CanvasItem.WidgetItem -> {
                val widgetPages = widgetPreferences.widgetConfigs.first().map { it.widgetId }
                if (!isWidgetIdReferenced(saved.values, widgetPages, item.widgetId)) {
                    runCatching { newHost().deleteAppWidgetId(item.widgetId) }
                }
            }
            else -> Unit
        }
    }

    override suspend fun installedApps(): List<JarnCanvasApp> = withContext(Dispatchers.IO) {
        val manager = context.packageManager
        val names = customNames.customNames.value
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        manager.queryIntentActivities(launcher, 0)
            .map { info ->
                val packageName = info.activityInfo.packageName
                JarnCanvasApp(packageName, names[packageName] ?: info.loadLabel(manager).toString())
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    override suspend fun pinnedRoms(): List<JarnCanvasRom> =
        pinnedRoms.allPinnedRoms.first().map { it.toJarnRom() }.sortedBy { it.title.lowercase() }

    override fun widgetHealth(widgetId: Int): JarnWidgetHealth = when {
        widgetManager.getAppWidgetInfo(widgetId) != null -> JarnWidgetHealth.Live
        runCatching { widgetId in newHost().appWidgetIds }.getOrDefault(false) -> JarnWidgetHealth.Unbound
        else -> JarnWidgetHealth.Gone
    }

    override fun acquireWidgetHost(): JarnWidgetLease =
        JarnWidgetHostHandoff.acquire(widgetManager, ::newHost)

    override suspend fun widgetProviders(): List<JarnWidgetProvider> = withContext(Dispatchers.IO) {
        val manager = context.packageManager
        widgetManager.installedProviders.map { info ->
            val (columns, rows) = info.canvasSpan()
            JarnWidgetProvider(
                key = info.provider.flattenToString(),
                label = info.loadLabel(manager),
                appName = runCatching {
                    manager.getApplicationInfo(info.provider.packageName, 0).loadLabel(manager).toString()
                }.getOrDefault(info.provider.packageName),
                columns = columns,
                rows = rows,
            )
        }.sortedWith(compareBy({ it.appName.lowercase() }, { it.label.lowercase() }))
    }

    override fun bindWidget(provider: JarnWidgetProvider, existingId: Int?): JarnWidgetBind {
        val info = widgetManager.installedProviders
            .firstOrNull { it.provider.flattenToString() == provider.key }
            ?: return JarnWidgetBind.Failed
        val host = newHost()
        if (existingId != null && runCatching { existingId !in host.appWidgetIds }.getOrDefault(true)) {
            return JarnWidgetBind.Failed
        }
        val id = existingId ?: runCatching { host.allocateAppWidgetId() }.getOrNull()
            ?: return JarnWidgetBind.Failed
        val bound = runCatching { widgetManager.bindAppWidgetIdIfAllowed(id, info.provider) }
        return when {
            bound.getOrNull() == true -> JarnWidgetBind.Bound(id, provider.columns, provider.rows)
            bound.isSuccess -> JarnWidgetBind.NeedsPermission(
                widgetId = id,
                columns = provider.columns,
                rows = provider.rows,
                isNewId = existingId == null,
                intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider),
            )
            else -> {
                if (existingId == null) runCatching { host.deleteAppWidgetId(id) }
                JarnWidgetBind.Failed
            }
        }
    }

    override fun cancelWidgetBind(bind: JarnWidgetBind.NeedsPermission) {
        if (bind.isNewId) runCatching { newHost().deleteAppWidgetId(bind.widgetId) }
    }

    override suspend fun importPhoto(uri: String): String? = withContext(Dispatchers.IO) {
        val source = uri.toUri()
        WallpaperUtils.copyMediaToInternalStorage(context, source, PHOTO_DIR, PHOTO_PREFIX)
            ?: runCatching {
                context.contentResolver.takePersistableUriPermission(
                    source,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                uri
            }.getOrNull()
    }

    override fun toggleMusic() = nowPlaying.get().togglePlayPause()

    override fun skipMusic(forward: Boolean) {
        if (forward) nowPlaying.get().skipToNext() else nowPlaying.get().skipToPrevious()
    }

    override suspend fun startMusic(): Boolean {
        val queue = audioQueue()
        if (queue.isEmpty()) return false
        nowPlaying.get().play(queue, 0)
        return true
    }

    override suspend fun openRssItem(host: Activity, item: JarnCanvasRssItem): Boolean {
        if (item.hasAudio) {
            val queue = audioQueue()
            val index = queue.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                nowPlaying.get().play(queue, index)
                return true
            }
        }
        if (item.link.isBlank()) return false
        return try {
            host.startActivity(Intent(Intent.ACTION_VIEW, item.link.toUri()))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private suspend fun audioQueue(): List<RssItem> =
        rss.get().allItems.first().filter { it.audioUrl.isNotEmpty() }

    private fun place(pageIndex: Int, item: CanvasItem): String {
        layouts.addItem(pageIndex, item)
        return item.id
    }

    private suspend fun addFolder(pageIndex: Int, tile: JarnCanvasNewTile.Folder): String? {
        val first = tile.packageNames.firstOrNull() ?: return null
        val folder = Folder(
            id = UUID.randomUUID().toString(),
            name = tile.name,
            appPackageNames = tile.packageNames,
            position = AppPosition(first, 0f, 0f),
        )
        if (!folders.createFolder(pageIndex, folder, CanvasTabType.VALUE)) return null
        return place(pageIndex, CanvasItem.FolderItem("folder-${folder.id}", folder.id))
    }

    private suspend fun addRom(pageIndex: Int, key: String): String? {
        val existing = layouts.getLayout(pageIndex).items
            .firstOrNull { it is CanvasItem.RomItem && it.romKey == key }
        if (existing != null) return existing.id
        val rom = pinnedRoms.allPinnedRoms.first().firstOrNull { it.key == key } ?: return null
        pinnedRoms.addPinnedRom(pageIndex, rom, CanvasTabType.VALUE)
        return place(pageIndex, CanvasItem.RomItem("rom-$key", key))
    }

    private fun isCanvasPage(pageIndex: Int): Boolean =
        pageTypes.pageTypes.value.getOrNull(pageIndex) == PageType.UNIFIED_CANVAS

    private fun newHost() = AppWidgetHost(context, WidgetViewModel.APPWIDGET_HOST_ID)

    /** A tile edit touches several stores, so leaving the screen must never stop one halfway. */
    internal suspend fun <T> edit(block: suspend () -> T): T =
        withContext(NonCancellable) { edits.withLock { block() } }

    private fun RssItem.toJarnRssItem() = JarnCanvasRssItem(
        id = id,
        title = title,
        link = link,
        hasAudio = audioUrl.isNotEmpty(),
    )
}
