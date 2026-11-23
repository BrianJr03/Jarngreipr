package jr.brian.home.canvas.jarn

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.CustomAppNameManager
import jr.brian.home.data.FolderManager
import jr.brian.home.data.HomeTabManager
import jr.brian.home.data.IconPackManager
import jr.brian.home.data.NowPlayingManager
import jr.brian.home.data.PageCountManager
import jr.brian.home.data.PageOrderCoordinator
import jr.brian.home.data.PageTypeManager
import jr.brian.home.data.PinnedRomManager
import jr.brian.home.data.RecentlyPlayedEntry
import jr.brian.home.data.RecentlyPlayedManager
import jr.brian.home.data.WidgetPageAppManager
import jr.brian.home.data.WidgetPreferences
import jr.brian.home.model.PageType
import jr.brian.home.model.app.Folder
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.model.rom.resolveDisplayPath
import jr.brian.home.model.widget.WidgetConfig
import jr.brian.home.viewmodels.WidgetViewModel
import jr.brian.jarnfe.canvas.JARN_CANVAS_MAX_PAGES
import jr.brian.jarnfe.canvas.JarnCanvasApp
import jr.brian.jarnfe.canvas.JarnCanvasData
import jr.brian.jarnfe.canvas.JarnCanvasFolder
import jr.brian.jarnfe.canvas.JarnCanvasMusic
import jr.brian.jarnfe.canvas.JarnCanvasPage
import jr.brian.jarnfe.canvas.JarnCanvasPageKind
import jr.brian.jarnfe.canvas.JarnCanvasRom
import jr.brian.jarnfe.canvas.JarnCanvasSnapshot
import jr.brian.jarnfe.canvas.jarnPageOrderAfterMove
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.math.max
import jr.brian.home.util.launchApp as launchAppOnDisplay

/** Writes [placements] into one orientation only. Items and every other field stay as stored. */
internal fun mergeJarnPlacements(
    layout: CanvasLayout,
    orientation: CanvasScrollOrientation,
    placements: Map<String, GridRect>,
): CanvasLayout = layout.withArrangement(
    orientation,
    layout.arrangementFor(orientation).mapValues { (id, rect) -> placements[id] ?: rect },
)

/** Widgets kept after widget page [deleted] goes away, and the ids to release. */
internal fun widgetConfigsAfterPageDelete(
    configs: List<WidgetConfig>,
    deleted: Int,
): Pair<List<WidgetConfig>, List<Int>> {
    val (removed, kept) = configs.partition { it.pageIndex == deleted }
    val shifted = kept.map {
        if (it.pageIndex > deleted) it.copy(pageIndex = it.pageIndex - 1) else it
    }
    return shifted to removed.map { it.widgetId }
}

internal fun PageType.toJarnKind(): JarnCanvasPageKind = when (this) {
    PageType.APPS_TAB -> JarnCanvasPageKind.Apps
    PageType.APPS_AND_WIDGETS_TAB -> JarnCanvasPageKind.AppsAndWidgets
    PageType.APP_DRAWER_TAB -> JarnCanvasPageKind.AppDrawer
    PageType.RSS_TAB -> JarnCanvasPageKind.Rss
    PageType.UNIFIED_CANVAS -> JarnCanvasPageKind.Canvas
}

internal fun PinnedRomInfo.toJarnRom() = JarnCanvasRom(
    key = key,
    title = name,
    systemName = systemName,
    path = path,
    artPath = resolveDisplayPath(),
    emulatorPackage = emulatorPackage,
    launchCommand = launchCommand,
)

private data class CanvasSources(
    val types: List<PageType>,
    val home: Int,
    val layouts: Map<Int, CanvasLayout>,
    val names: Map<String, String>,
    val recent: List<RecentlyPlayedEntry>,
)

private data class PageRefs(val folders: List<Folder>, val roms: List<PinnedRomInfo>)

/** Bridges Jarn FE to the same stores Classic reads, so switching needs no import. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AppJarnCanvasData @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val layouts: CanvasLayoutManager,
    private val pageTypes: PageTypeManager,
    private val pageCount: PageCountManager,
    private val homeTab: HomeTabManager,
    private val pageOrder: PageOrderCoordinator,
    private val folders: FolderManager,
    private val pinnedRoms: PinnedRomManager,
    private val recentlyPlayed: RecentlyPlayedManager,
    private val customNames: CustomAppNameManager,
    private val iconPacks: IconPackManager,
    private val displayPreferences: AppDisplayPreferenceManager,
    private val widgetPreferences: WidgetPreferences,
    private val widgetPageApps: WidgetPageAppManager,
    private val nowPlaying: Provider<NowPlayingManager>,
) : JarnCanvasData {

    private val pageEdits = Mutex()
    private val icons = ConcurrentHashMap<String, Drawable>()

    override val snapshot: Flow<JarnCanvasSnapshot> = combine(
        pageTypes.pageTypes,
        homeTab.homeTabIndex,
        layouts.layoutsByPage,
        customNames.customNames,
        recentlyPlayed.entries,
    ) { types, home, saved, names, recent ->
        CanvasSources(types, home, saved, names, recent)
    }.flatMapLatest { sources ->
        combine(pageRefs(sources), music(sources)) { refs, music ->
            buildSnapshot(sources, refs, music)
        }
    }.flowOn(Dispatchers.IO)

    override fun commitPlacements(
        pageIndex: Int,
        orientation: CanvasScrollOrientation,
        placements: Map<String, GridRect>,
    ) {
        if (pageTypes.pageTypes.value.getOrNull(pageIndex) != PageType.UNIFIED_CANVAS) return
        val current = layouts.getLayout(pageIndex)
        layouts.replaceLayout(pageIndex, mergeJarnPlacements(current, orientation, placements))
    }

    override fun widgetMinSpans(widgetId: Int, cellSizePx: Float): Pair<Int, Int> {
        if (cellSizePx <= 0f) return 1 to 1
        val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId) ?: return 1 to 1
        val minWidth = max(info.minResizeWidth, info.minWidth)
        val minHeight = max(info.minResizeHeight, info.minHeight)
        val columns = if (minWidth > 0) ((minWidth + cellSizePx - 1) / cellSizePx).toInt() else 1
        val rows = if (minHeight > 0) ((minHeight + cellSizePx - 1) / cellSizePx).toInt() else 1
        return columns.coerceAtLeast(1) to rows.coerceAtLeast(1)
    }

    override fun launchApp(host: Activity, packageName: String) {
        launchAppOnDisplay(host, packageName, displayPreferences.getAppDisplayPreference(packageName))
    }

    override fun setHomePage(index: Int) {
        if (index in pageTypes.pageTypes.value.indices) homeTab.setHomeTabIndex(index)
    }

    override suspend fun addCanvasPage(): Int? = editPages {
        if (pageTypes.pageTypes.value.size >= JARN_CANVAS_MAX_PAGES) return@editPages null
        pageTypes.addPage(PageType.UNIFIED_CANVAS)
        pageCount.addPage()
        pageTypes.pageTypes.value.lastIndex
    }

    override suspend fun deletePage(index: Int) = editPages {
        val types = pageTypes.pageTypes.value
        if (types.size <= 1 || index !in types.indices) return@editPages
        pageOrder.removePage(index, ::removeWidgetPage)
    }

    override suspend fun movePage(from: Int, to: Int) = editPages {
        val types = pageTypes.pageTypes.value
        if (from == to || from !in types.indices || to !in types.indices) return@editPages
        val order = jarnPageOrderAfterMove(types.size, from, to)
        val home = order.indexOf(homeTab.homeTabIndex.value).coerceAtLeast(0)
        pageOrder.reorder(order.map { types[it] }, order, home)
    }

    /** A page edit touches many stores, so leaving the screen must never stop one halfway. */
    private suspend fun <T> editPages(edit: suspend () -> T): T =
        withContext(NonCancellable) { pageEdits.withLock { edit() } }

    private suspend fun removeWidgetPage(widgetPage: Int) {
        val (kept, released) = widgetConfigsAfterPageDelete(
            widgetPreferences.widgetConfigs.first(),
            widgetPage,
        )
        val host = AppWidgetHost(context, WidgetViewModel.APPWIDGET_HOST_ID)
        released.forEach { id -> runCatching { host.deleteAppWidgetId(id) } }
        widgetPreferences.saveWidgetConfigs(kept)
        widgetPageApps.removePage(widgetPage)
    }

    private fun pageRefs(sources: CanvasSources): Flow<Map<Int, PageRefs>> {
        val canvasPages = sources.types.indices.filter { sources.types[it] == PageType.UNIFIED_CANVAS }
        if (canvasPages.isEmpty()) return flowOf(emptyMap())
        val pages = canvasPages.map { index ->
            combine(
                folders.getFolders(index, CanvasTabType.VALUE),
                pinnedRoms.getPinnedRoms(index, CanvasTabType.VALUE),
            ) { pageFolders, roms -> index to PageRefs(pageFolders, roms) }
        }
        return combine(pages) { it.toMap() }
    }

    /** The music service only starts when a saved page actually has a music tile. */
    private fun music(sources: CanvasSources): Flow<JarnCanvasMusic?> {
        val hasMusicTile = sources.layouts.values.any { layout ->
            layout.items.any { it is CanvasItem.RssMusicItem }
        }
        if (!hasMusicTile) return flowOf(null)
        return nowPlaying.get().nowPlaying.map { info ->
            info?.let { JarnCanvasMusic(it.title, it.artist, it.imageUrl, it.isPlaying) }
        }
    }

    private suspend fun buildSnapshot(
        sources: CanvasSources,
        refs: Map<Int, PageRefs>,
        music: JarnCanvasMusic?,
    ): JarnCanvasSnapshot {
        val pages = sources.types.mapIndexed { index, type ->
            val layout = sources.layouts[index] ?: CanvasLayout()
            val pageRefs = refs[index]
            if (type != PageType.UNIFIED_CANVAS || pageRefs == null) {
                JarnCanvasPage(index, type.toJarnKind(), layout)
            } else {
                resolvePage(index, layout, pageRefs, sources.names)
            }
        }
        return JarnCanvasSnapshot(
            pages = pages,
            homePage = sources.home,
            recent = sources.recent.map { it.rom.toJarnRom() },
            music = music,
        )
    }

    private suspend fun resolvePage(
        index: Int,
        layout: CanvasLayout,
        refs: PageRefs,
        names: Map<String, String>,
    ): JarnCanvasPage {
        val folderIds = layout.items.filterIsInstance<CanvasItem.FolderItem>().map { it.folderId }.toSet()
        val placedFolders = refs.folders.filter { it.id in folderIds }
        val packages = layout.items.filterIsInstance<CanvasItem.AppItem>().map { it.packageName } +
            placedFolders.flatMap { it.appPackageNames }
        val apps = packages.distinct().mapNotNull { resolveApp(it, names) }.associateBy { it.packageName }
        return JarnCanvasPage(
            index = index,
            kind = JarnCanvasPageKind.Canvas,
            layout = layout,
            apps = apps,
            folders = placedFolders.associate { folder ->
                folder.id to JarnCanvasFolder(
                    id = folder.id,
                    name = folder.name,
                    apps = folder.appPackageNames.mapNotNull(apps::get),
                )
            },
            roms = refs.roms.associate { it.key to it.toJarnRom() },
        )
    }

    private suspend fun resolveApp(packageName: String, names: Map<String, String>): JarnCanvasApp? {
        val manager = context.packageManager
        val info = try {
            manager.getApplicationInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        val icon = icons[packageName] ?: loadIcon(packageName, info.loadIcon(manager))
            .also { icons[packageName] = it }
        return JarnCanvasApp(
            packageName = packageName,
            label = names[packageName] ?: info.loadLabel(manager).toString(),
            icon = icon,
        )
    }

    private suspend fun loadIcon(packageName: String, fallback: Drawable): Drawable {
        val activity = context.packageManager.getLaunchIntentForPackage(packageName)?.component?.className
        return iconPacks.getIconForApp(packageName, activity)
            ?: iconPacks.applyIconMask(fallback)
            ?: fallback
    }
}
