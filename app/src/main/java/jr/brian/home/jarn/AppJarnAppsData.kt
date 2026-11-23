package jr.brian.home.jarn

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.apps.JarnAppPinStore
import jr.brian.home.canvas.jarn.AppJarnCanvasData
import jr.brian.home.data.AppVisibilityManager
import jr.brian.home.data.CustomAppNameManager
import jr.brian.home.data.IconPackManager
import jr.brian.home.data.PageTypeManager
import jr.brian.home.model.PageType
import jr.brian.jarnfe.apps.JarnAppsData
import jr.brian.jarnfe.apps.JarnAppsSnapshot
import jr.brian.jarnfe.dock.JarnDockApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val INSTALLED_APPS_KEEP_ALIVE_MS = 30_000L

private val AppListingPages = setOf(PageType.APPS_TAB, PageType.APPS_AND_WIDGETS_TAB, PageType.APP_DRAWER_TAB)

/**
 * Classic stores hidden apps per page, so "hidden" here means no page that lists apps still shows
 * it. With no such page, every page counts. An app hidden on only some pages stays visible.
 */
internal fun jarnHiddenPackages(pageTypes: List<PageType>, hiddenByPage: Map<Int, Set<String>>): Set<String> {
    val listing = pageTypes.indices.filter { pageTypes[it] in AppListingPages }
    val pages = listing.ifEmpty { pageTypes.indices.toList() }
    if (pages.isEmpty()) return emptySet()
    return pages.map { hiddenByPage[it].orEmpty() }.reduce { shared, page -> shared intersect page }
}

/** Unhiding also clears pages no longer in use, so a removed page can't bring a hide back. */
internal fun jarnUnhidePages(pageCount: Int, hiddenByPage: Map<Int, Set<String>>, packageName: String): Set<Int> =
    (0 until pageCount).toSet() + hiddenByPage.filterValues { packageName in it }.keys

@Singleton
class AppJarnAppsData @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val visibility: AppVisibilityManager,
    private val pageTypes: PageTypeManager,
    private val customNames: CustomAppNameManager,
    private val iconPacks: IconPackManager,
    private val pins: JarnAppPinStore,
    private val canvas: Provider<AppJarnCanvasData>,
) : JarnAppsData {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val installed = packageChanges()
        .mapLatest { loadInstalled() }
        .shareIn(scope, SharingStarted.WhileSubscribed(INSTALLED_APPS_KEEP_ALIVE_MS), replay = 1)

    override val snapshot: Flow<JarnAppsSnapshot> = combine(
        installed,
        customNames.customNames,
        visibility.hiddenAppsByPage,
        pageTypes.pageTypes,
        pins.pinnedState,
    ) { apps, names, hiddenByPage, types, pinned ->
        JarnAppsSnapshot(
            apps = apps.map { app -> names[app.packageName]?.let { app.copy(label = it) } ?: app },
            pinned = pinned,
            hidden = jarnHiddenPackages(types, hiddenByPage),
        )
    }.flowOn(Dispatchers.Default)

    override fun launch(host: Activity, packageName: String) {
        canvas.get().launchApp(host, packageName)
    }

    override fun setPinned(packageName: String, pinned: Boolean) {
        pins.setPinned(packageName, pinned)
    }

    override fun setHidden(packageName: String, hidden: Boolean) {
        val pageCount = pageTypes.pageTypes.value.size
        if (hidden) {
            repeat(pageCount) { visibility.hideApp(it, packageName) }
        } else {
            jarnUnhidePages(pageCount, visibility.hiddenAppsByPage.value, packageName)
                .forEach { visibility.showApp(it, packageName) }
        }
    }

    private fun packageChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        trySend(Unit)
        awaitClose { context.unregisterReceiver(receiver) }
    }.buffer(Channel.CONFLATED)

    private suspend fun loadInstalled(): List<JarnDockApp> {
        val manager = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return manager.queryIntentActivities(launcher, 0)
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                val packageName = info.activityInfo.packageName
                val fallback = info.loadIcon(manager)
                val icon = iconPacks.getIconForApp(packageName, info.activityInfo.name)
                    ?: iconPacks.applyIconMask(fallback)
                    ?: fallback
                JarnDockApp(packageName, info.loadLabel(manager).toString(), icon)
            }
    }
}
