package jr.brian.home.data

import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.model.PageType
import jr.brian.home.model.widget.WidgetConfig
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single fan-out point for "pager-tab order changed" and "pager tab removed."
 *
 * Every per-`pageIndex` store in the launcher must be remapped together — apps
 * positions, hidden apps, folders (×3 tab types), pinned ROMs (×3 tab types),
 * canvas layouts, widget configs, dock/fab/floaty visibility sets, and the
 * notification shade page. Forgetting any one of them reintroduces the bug
 * where reordering / removing tabs orphans or shifts page content.
 */
@Singleton
class PageOrderCoordinator @Inject constructor(
    private val pageTypeManager: PageTypeManager,
    private val pageCountManager: PageCountManager,
    private val homeTabManager: HomeTabManager,
    private val appVisibilityManager: AppVisibilityManager,
    private val appPositionManager: AppPositionManager,
    private val folderManager: FolderManager,
    private val pinnedRomManager: PinnedRomManager,
    private val canvasLayoutManager: CanvasLayoutManager,
    private val widgetPageAppManager: WidgetPageAppManager,
    private val widgetPreferences: WidgetPreferences,
    private val dockManager: DockManager,
    private val appDrawerFabManager: AppDrawerFabManager,
    private val floatyModeManager: FloatyModeManager,
    private val notificationManager: NotificationManager
) {
    private val tabTypes = listOf(
        PinnedRomManager.TAB_TYPE_APPS,
        PinnedRomManager.TAB_TYPE_WIDGETS,
        CanvasTabType.VALUE
    )

    suspend fun reorder(
        newOrder: List<PageType>,
        oldIndicesInNewOrder: List<Int>,
        newCurrentTabIndex: Int
    ) {
        val pagerMap: Map<Int, Int> = oldIndicesInNewOrder
            .withIndex()
            .associate { (newIdx, oldIdx) -> newIdx to oldIdx }

        val oldPageTypes = pageTypeManager.pageTypes.value
        val widgetMap = computeWidgetPagePermutation(oldPageTypes, newOrder, pagerMap)

        pageTypeManager.reorderPages(newOrder)

        appVisibilityManager.reorderPages(pagerMap)
        appPositionManager.reorderPages(pagerMap)
        canvasLayoutManager.reorderPages(pagerMap)
        dockManager.reorderPages(pagerMap)
        appDrawerFabManager.reorderPages(pagerMap)
        floatyModeManager.reorderPages(pagerMap)
        notificationManager.reorderPages(pagerMap)

        tabTypes.forEach { tabType ->
            folderManager.reorderPages(pagerMap, tabType)
            pinnedRomManager.reorderPages(pagerMap, tabType)
        }

        widgetPageAppManager.reorderPages(widgetMap)
        rewriteWidgetConfigPageIndices(widgetMap)

        homeTabManager.setHomeTabIndex(newCurrentTabIndex)
    }

    suspend fun removePage(
        pagerPageIndex: Int,
        onDeleteWidgetPage: (suspend (widgetPageIndex: Int) -> Unit)? = null
    ) {
        val oldPageTypes = pageTypeManager.pageTypes.value
        val removedType = oldPageTypes.getOrNull(pagerPageIndex)
        if (removedType != null && removedType != PageType.APPS_TAB) {
            val widgetPageIndex = widgetPageIndexOf(oldPageTypes, pagerPageIndex)
            onDeleteWidgetPage?.invoke(widgetPageIndex)
        }

        appVisibilityManager.removePage(pagerPageIndex)
        appPositionManager.removePage(pagerPageIndex)
        canvasLayoutManager.removePage(pagerPageIndex)
        dockManager.removePage(pagerPageIndex)
        appDrawerFabManager.removePage(pagerPageIndex)
        floatyModeManager.removePage(pagerPageIndex)
        notificationManager.removePage(pagerPageIndex)

        tabTypes.forEach { tabType ->
            folderManager.removePage(pagerPageIndex, tabType)
            pinnedRomManager.removePage(pagerPageIndex, tabType)
        }

        pageTypeManager.removePage(pagerPageIndex)
        pageCountManager.removePage()

        val currentHomeTabIndex = homeTabManager.homeTabIndex.value
        when {
            pagerPageIndex == currentHomeTabIndex -> homeTabManager.setHomeTabIndex(0)
            pagerPageIndex < currentHomeTabIndex ->
                homeTabManager.setHomeTabIndex(currentHomeTabIndex - 1)
        }
    }

    /**
     * Maps each new widget-page slot to its old widget-page index. Widget pages
     * are positionally indexed against the count of non-APPS_TAB pages before
     * them; reordering pager tabs shifts those sub-indices even though the
     * widget-hosting pages themselves all survive.
     */
    private fun computeWidgetPagePermutation(
        oldPageTypes: List<PageType>,
        newOrder: List<PageType>,
        pagerMap: Map<Int, Int>
    ): Map<Int, Int> {
        val oldWidgetIndexByPager = mutableMapOf<Int, Int>()
        var oldWidgetCount = 0
        oldPageTypes.forEachIndexed { pagerIdx, type ->
            if (type != PageType.APPS_TAB) {
                oldWidgetIndexByPager[pagerIdx] = oldWidgetCount
                oldWidgetCount++
            }
        }

        val widgetMap = mutableMapOf<Int, Int>()
        var newWidgetIdx = 0
        newOrder.forEachIndexed { newPagerIdx, type ->
            if (type == PageType.APPS_TAB) return@forEachIndexed
            val oldPagerIdx = pagerMap[newPagerIdx] ?: return@forEachIndexed
            val oldWidgetIdx = oldWidgetIndexByPager[oldPagerIdx] ?: return@forEachIndexed
            widgetMap[newWidgetIdx] = oldWidgetIdx
            newWidgetIdx++
        }
        return widgetMap
    }

    private fun widgetPageIndexOf(pageTypes: List<PageType>, pagerPageIndex: Int): Int {
        var count = 0
        for (i in 0 until pagerPageIndex) {
            if (pageTypes.getOrNull(i) != PageType.APPS_TAB) count++
        }
        return count
    }

    /**
     * Rewrite [WidgetConfig.pageIndex] for every saved widget to its new
     * widget-page slot. The [WidgetViewModel]'s `widgetConfigs` collector will
     * pick up these edits and refresh its UI state without further coupling.
     */
    private suspend fun rewriteWidgetConfigPageIndices(widgetMap: Map<Int, Int>) {
        if (widgetMap.isEmpty()) return
        val oldToNew = widgetMap.entries.associate { (newIdx, oldIdx) -> oldIdx to newIdx }
        val configs = widgetPreferences.widgetConfigs.first()
        val updated = configs.map { config ->
            val newIdx = oldToNew[config.pageIndex] ?: return@map config
            if (newIdx == config.pageIndex) config else config.copy(pageIndex = newIdx)
        }
        if (updated == configs) return
        widgetPreferences.saveWidgetConfigs(updated)
    }
}
