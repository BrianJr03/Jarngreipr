package jr.brian.home.jarn

import android.app.Activity
import android.view.KeyEvent
import jr.brian.home.data.ExperienceSwitcher
import jr.brian.home.data.HomeKeyFallback
import jr.brian.jarnfe.shell.JarnExperienceSwitch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppJarnExperienceSwitch @Inject constructor(
    private val switcher: ExperienceSwitcher,
) : JarnExperienceSwitch {
    override fun switchToClassic(host: Activity): Boolean = switcher.switchToClassic(host)

    override fun handleHomeKey(host: Activity, event: KeyEvent): Boolean =
        HomeKeyFallback.handle(host, event)
}
