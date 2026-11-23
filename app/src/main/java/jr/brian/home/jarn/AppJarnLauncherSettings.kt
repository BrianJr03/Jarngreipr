package jr.brian.home.jarn

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.R
import jr.brian.home.data.AppVisibilityManager
import jr.brian.home.data.DockManager
import jr.brian.home.util.PatchNotesUtil
import jr.brian.jarnfe.settings.JarnLauncherPrefs
import jr.brian.jarnfe.settings.JarnLauncherSettings
import jr.brian.jarnfe.settings.JarnSupportLink
import jr.brian.jarnfe.settings.jarnNextDockSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** Reuses Classic's stores, so every value here is already covered by backup and restore. */
@Singleton
class AppJarnLauncherSettings @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val visibility: AppVisibilityManager,
    private val dock: DockManager,
) : JarnLauncherSettings {
    override val prefs: Flow<JarnLauncherPrefs> = combine(
        snapshotFlow { visibility.appLabelFontSize },
        dock.isDockVisible,
        dock.dockApps,
        dock.maxDockApps,
    ) { labelSize, dockVisible, dockApps, maxDockApps ->
        JarnLauncherPrefs(labelSize, dockVisible, dockApps, maxDockApps)
    }.distinctUntilChanged()

    override fun readPrefs() = JarnLauncherPrefs(
        appLabelSize = visibility.appLabelFontSize,
        dockVisible = dock.isDockVisible.value,
        dockApps = dock.dockApps.value,
        maxDockApps = dock.maxDockApps.value,
    )

    override fun setAppLabelSize(size: Int) = visibility.updateAppLabelFontSize(size)

    override fun setDockVisible(visible: Boolean) = dock.setDockVisibility(visible)

    override fun setDockApp(packageName: String, inDock: Boolean): Boolean {
        if (!inDock) {
            dock.removeAppFromDock(packageName)
            return true
        }
        if (dock.isAppInDock(packageName)) return true
        val slot = jarnNextDockSlot(dock.dockApps.value, dock.maxDockApps.value) ?: return false
        dock.addAppToDock(packageName, slot)
        return dock.isAppInDock(packageName)
    }

    override suspend fun whatsNew(): String = PatchNotesUtil.fetchPatchNotesWithFallback(
        context = context,
        currentVersionName = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty(),
    )

    override fun openSupportLink(host: Activity, link: JarnSupportLink): Boolean = try {
        host.startActivity(
            Intent(Intent.ACTION_VIEW, context.getString(link.urlRes).toUri())
                .addCategory(Intent.CATEGORY_BROWSABLE),
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

@get:StringRes
private val JarnSupportLink.urlRes: Int
    get() = when (this) {
        JarnSupportLink.Coffee -> R.string.settings_buy_me_coffee_url
        JarnSupportLink.Kofi -> R.string.settings_kofi_url
        JarnSupportLink.Discord -> R.string.settings_discord_url
    }
