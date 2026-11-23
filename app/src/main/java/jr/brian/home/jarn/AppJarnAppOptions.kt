package jr.brian.home.jarn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.hardware.display.DisplayManager
import android.provider.Settings
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.apps.JarnAppPinStore
import jr.brian.home.canvas.jarn.toJarnDisplay
import jr.brian.home.canvas.jarn.toPreference
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.AppVisibilityManager
import jr.brian.home.data.CustomAppNameManager
import jr.brian.home.data.CustomIconManager
import jr.brian.home.data.IconPackManager
import jr.brian.home.data.PageTypeManager
import jr.brian.home.ui.theme.managers.GlobalIconRefreshManager
import jr.brian.jarnfe.apps.JarnAppOptions
import jr.brian.jarnfe.apps.JarnAppOptionsData
import jr.brian.jarnfe.apps.JarnIconPack
import jr.brian.jarnfe.canvas.JarnAppDisplay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton
import jr.brian.home.util.launchApp as launchAppOnDisplay

/** Per-app options on the stores Classic reads, so a choice made in either experience holds. */
@Singleton
class AppJarnAppOptions @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val customNames: CustomAppNameManager,
    private val customIcons: CustomIconManager,
    private val displayPreferences: AppDisplayPreferenceManager,
    private val iconPackManager: IconPackManager,
    private val iconRefresh: GlobalIconRefreshManager,
    private val visibility: AppVisibilityManager,
    private val pageTypes: PageTypeManager,
    private val pins: JarnAppPinStore,
) : JarnAppOptionsData {
    private val packRevision = MutableStateFlow(0)

    override val selectedIconPack: Flow<String?> = iconPackManager.selectedIconPack

    /** Also fires once a change has finished, because the pack's caches clear after its save. */
    val iconPackChanges: Flow<String?> =
        combine(iconPackManager.selectedIconPack.distinctUntilChanged(), packRevision) { pack, _ -> pack }

    override fun appOptions(packageName: String) = JarnAppOptions(
        display = displayPreferences.getAppDisplayPreference(packageName).toJarnDisplay(),
        askOnLaunch = displayPreferences.getPromptForDisplayOnLaunch(packageName),
        customName = customNames.getCustomName(packageName),
        hasSecondDisplay = hasSecondDisplay(),
        pinned = packageName in pins.pinnedState.value,
        hidden = packageName in jarnHiddenPackages(pageTypes.pageTypes.value, visibility.hiddenAppsByPage.value),
    )

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

    private fun hasSecondDisplay(): Boolean =
        (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
            ?.displays?.size?.let { it > 1 } == true

    override fun setAppDisplay(packageName: String, display: JarnAppDisplay) {
        displayPreferences.setAppDisplayPreference(packageName, display.toPreference())
    }

    override fun setAskDisplayOnLaunch(packageName: String, ask: Boolean) {
        displayPreferences.setPromptForDisplayOnLaunch(packageName, ask)
    }

    override fun setAppName(packageName: String, name: String?) {
        if (name == null) {
            customNames.removeCustomName(packageName)
        } else {
            customNames.setCustomName(packageName, name)
        }
    }

    override suspend fun setAppIcon(packageName: String, uri: String): Boolean {
        val saved = customIcons.setCustomIcon(packageName, uri.toUri()).isSuccess
        if (saved) iconRefresh.triggerRefresh()
        return saved
    }

    override suspend fun setAppIconFromPack(packageName: String, iconPack: String, iconName: String): Boolean {
        val drawable = iconPackManager.getIconDrawable(iconPack, iconName) ?: return false
        val saved = customIcons.setCustomIcon(packageName, drawable).isSuccess
        if (saved) iconRefresh.triggerRefresh()
        return saved
    }

    override suspend fun clearAppIcon(packageName: String) {
        if (customIcons.removeCustomIcon(packageName)) iconRefresh.triggerRefresh()
    }

    override suspend fun iconPacks(): List<JarnIconPack> =
        iconPackManager.getInstalledIconPacks().map { JarnIconPack(it.packageName, it.name, it.icon) }

    override suspend fun setIconPack(packageName: String?) {
        iconPackManager.setSelectedIconPack(packageName)
        packRevision.value += 1
        iconRefresh.triggerRefresh()
    }

    override suspend fun iconPackIconNames(iconPack: String): List<String> =
        iconPackManager.getIconNames(iconPack)

    override suspend fun iconPackIcon(iconPack: String, iconName: String): Drawable? =
        iconPackManager.getIconDrawable(iconPack, iconName)

    override fun launchApp(host: Activity, packageName: String) {
        launchAppOnDisplay(host, packageName, displayPreferences.getAppDisplayPreference(packageName))
    }

    override fun launchAppOn(host: Activity, packageName: String, display: JarnAppDisplay) {
        launchAppOnDisplay(host, packageName, display.toPreference())
    }

    override fun openAppInfo(host: Activity, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
        runCatching { host.startActivity(intent) }
    }
}
