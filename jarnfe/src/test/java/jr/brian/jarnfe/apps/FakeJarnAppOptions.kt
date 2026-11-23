package jr.brian.jarnfe.apps

import android.app.Activity
import android.graphics.drawable.Drawable
import jr.brian.jarnfe.canvas.JarnAppDisplay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeJarnAppOptions : JarnAppOptionsData {
    var current = JarnAppOptions(JarnAppDisplay.Current, false, null, false)

    override val selectedIconPack: Flow<String?> = flowOf(null)

    override fun appOptions(packageName: String) = current

    override fun setPinned(packageName: String, pinned: Boolean) = Unit

    override fun setHidden(packageName: String, hidden: Boolean) = Unit

    override fun setAppDisplay(packageName: String, display: JarnAppDisplay) = Unit

    override fun setAskDisplayOnLaunch(packageName: String, ask: Boolean) = Unit

    override fun setAppName(packageName: String, name: String?) = Unit

    override suspend fun setAppIcon(packageName: String, uri: String) = true

    override suspend fun setAppIconFromPack(packageName: String, iconPack: String, iconName: String) = true

    override suspend fun clearAppIcon(packageName: String) = Unit

    override suspend fun iconPacks() = emptyList<JarnIconPack>()

    override suspend fun setIconPack(packageName: String?) = Unit

    override suspend fun iconPackIconNames(iconPack: String) = emptyList<String>()

    override suspend fun iconPackIcon(iconPack: String, iconName: String): Drawable? = null

    override fun launchApp(host: Activity, packageName: String) = Unit

    override fun launchAppOn(host: Activity, packageName: String, display: JarnAppDisplay) = Unit

    override fun openAppInfo(host: Activity, packageName: String) = Unit
}
