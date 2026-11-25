package jr.brian.home.jarn

import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.setGamelistDecorationEnabled
import jr.brian.jarnfe.settings.JarnGamelistSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Classic's "Read gamelist.xml" for Jarn. Already in JarngreiprConfig, and
 * LibraryStoreSync pushes it into the library, so nothing new is stored here.
 */
@Singleton
class AppJarnGamelistSetting @Inject constructor(
    private val prefs: ESDEPreferencesManager,
) : JarnGamelistSetting {
    override val readGamelist: Flow<Boolean> =
        prefs.state.map { it.gamelistDecorationEnabled }.distinctUntilChanged()

    override val isReadingGamelist: Boolean get() = prefs.state.value.gamelistDecorationEnabled

    override fun setReadGamelist(read: Boolean) = prefs.setGamelistDecorationEnabled(read)
}
