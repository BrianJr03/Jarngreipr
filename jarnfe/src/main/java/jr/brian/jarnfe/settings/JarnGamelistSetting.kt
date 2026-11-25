package jr.brian.jarnfe.settings

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** Classic's "Read gamelist.xml" setting, shared with Jarn. Backed up with Classic's settings. */
interface JarnGamelistSetting {
    val readGamelist: Flow<Boolean>
    val isReadingGamelist: Boolean
    fun setReadGamelist(read: Boolean)
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnGamelistSettingProvider {
    val jarnGamelistSetting: JarnGamelistSetting
}

internal fun Context.jarnGamelistSettingOrNull(): JarnGamelistSetting? =
    (applicationContext as? JarnGamelistSettingProvider)?.jarnGamelistSetting
