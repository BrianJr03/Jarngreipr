package jr.brian.home

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import jr.brian.home.util.CrashLogger

@HiltAndroidApp
class JarngreiprApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.initialize(this)
    }
}
