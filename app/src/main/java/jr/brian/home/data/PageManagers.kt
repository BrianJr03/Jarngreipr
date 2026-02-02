package jr.brian.home.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Container for page management-related managers.
 * 
 * These managers handle multi-page functionality:
 * - Total page count
 * - Page types (Apps, Apps+Widgets, Drawer)
 * - Home page selection
 * - Widget page configuration
 */
@Singleton
data class PageManagers @Inject constructor(
    /** Tracks total number of pages */
    val pageCountManager: PageCountManager,
    
    /** Manages page types (Apps, Apps+Widgets, Drawer) */
    val pageTypeManager: PageTypeManager,
    
    /** Tracks which page is set as home */
    val homeTabManager: HomeTabManager,
    
    /** Manages apps shown on widget pages */
    val widgetPageAppManager: WidgetPageAppManager
)
