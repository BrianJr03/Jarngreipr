package jr.brian.home.data

import jr.brian.home.canvas.data.CanvasLayoutManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Container for page management-related managers.
 *
 * These managers handle multi-page functionality:
 * - Total page count
 * - Page types (Apps, Apps+Widgets, Drawer, Unified Canvas)
 * - Home page selection
 * - Widget page configuration
 * - Per-page Unified Canvas layouts
 */
@Singleton
data class PageManagers @Inject constructor(
    /** Tracks total number of pages */
    val pageCountManager: PageCountManager,

    /** Manages page types (Apps, Apps+Widgets, Drawer, Unified Canvas) */
    val pageTypeManager: PageTypeManager,

    /** Tracks which page is set as home */
    val homeTabManager: HomeTabManager,

    /** Manages apps shown on widget pages */
    val widgetPageAppManager: WidgetPageAppManager,

    /** Per-page layouts for Unified Canvas pages */
    val canvasLayoutManager: CanvasLayoutManager,

    /** Fan-out coordinator for pager-tab reorder / removal */
    val pageOrderCoordinator: PageOrderCoordinator
)
