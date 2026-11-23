package jr.brian.home.esde.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.core.model.ScreenHost
import jr.brian.core.model.ScreenSlot
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.data.RecentlyPlayedManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.addGameCompanion
import jr.brian.home.esde.data.getGameEmulator
import jr.brian.home.esde.data.setGameEmulator
import jr.brian.home.esde.data.setGameLaunchCommand
import jr.brian.home.esde.launch.LaunchFailure
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.gameKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

private const val EMULATOR = "com.example.emu"
private const val DISPLAY_KEY = "android.activity.launchDisplayId"

/** Pins the observable order of Classic launch side effects across the extraction. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RomGameLauncherCharacterizationTest {

    open class RecordingActivity : ComponentActivity() {
        val events = mutableListOf<String>()
        var startFailure: ((Intent) -> Exception?)? = null

        override fun startActivity(intent: Intent, options: Bundle?) {
            val target = intent.`package` ?: intent.component?.packageName
            val display = options?.getInt(DISPLAY_KEY, -1) ?: -1
            events += "start:$target:$display"
            startFailure?.invoke(intent)?.let { throw it }
        }

        override fun finish() {
            events += "finish"
        }
    }

    class PinnedHostActivity : RecordingActivity(), ScreenHost {
        override val slot = ScreenSlot.entries.first()
        override fun finishIfOn(displayId: Int) {
            events += "finishIfOn:$displayId"
        }
    }

    private lateinit var app: Application
    private lateinit var prefs: ESDEPreferencesManager
    private lateinit var recents: RecentlyPlayedManager
    private lateinit var stateHolder: RomSearchStateHolder
    private val safHints = mutableListOf<Uri?>()

    private val game = GameInfo(
        path = "./game.sfc",
        name = "Game",
        systemName = "snes",
        romAbsolutePath = "/storage/emulated/0/Roms/snes/game.sfc",
    )

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        listOf("esde_prefs", "recently_played_prefs").forEach {
            app.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
        prefs = ESDEPreferencesManager(app)
        recents = RecentlyPlayedManager(app)
        recents.clear()
        stateHolder = RomSearchStateHolder()
        ShadowToast.reset()
    }

    private fun install(vararg packages: String) {
        val shadow = shadowOf(app.packageManager)
        packages.forEach { pkg ->
            shadow.installPackage(
                PackageInfo().apply {
                    packageName = pkg
                    applicationInfo = ApplicationInfo().apply { packageName = pkg }
                }
            )
        }
    }

    private fun installLaunchable(vararg packages: String) {
        install(*packages)
        val shadow = shadowOf(app.packageManager)
        packages.forEach { pkg ->
            val component = ComponentName(pkg, "$pkg.Main")
            shadow.addActivityIfNotPresent(component)
            shadow.addIntentFilterForActivity(
                component,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            )
        }
    }

    private fun <T : RecordingActivity> activity(type: Class<T>): T =
        Robolectric.buildActivity(type).setup().get()

    private fun launcher(activity: RecordingActivity) = RomGameLauncher(
        activity = activity,
        esdePrefs = prefs,
        appDisplayPreferenceManager = AppDisplayPreferenceManager(app),
        recentlyPlayedManager = recents,
        romSearchStateHolder = stateHolder,
        onSignalGameLaunch = {
            activity.events += "signal:recents=${recents.entries.value.size}"
        },
        onLaunchSafPicker = { safHints += it },
    )

    private fun launch(activity: RecordingActivity, target: GameInfo = game) = launcher(activity)
        .launchGame(target, activity, DisplayPreference.PRIMARY_DISPLAY)

    @Test
    fun `installed emulator records, signals, starts on primary, then finishes`() {
        val activity = activity(RecordingActivity::class.java)
        install(EMULATOR)
        prefs.setGameEmulator(gameKey(game), EMULATOR)

        launch(activity)

        assertEquals(
            listOf("signal:recents=1", "start:$EMULATOR:$PRIMARY_DISPLAY_ID", "finish"),
            activity.events,
        )
    }

    @Test
    fun `pinned host finishes only through finishIfOn primary`() {
        val activity = activity(PinnedHostActivity::class.java)
        install(EMULATOR)
        prefs.setGameEmulator(gameKey(game), EMULATOR)

        launch(activity)

        assertEquals("finishIfOn:$PRIMARY_DISPLAY_ID", activity.events.last())
    }

    @Test
    fun `no emulator shows the guidance toast and starts nothing`() {
        val activity = activity(RecordingActivity::class.java)

        launch(activity)

        assertEquals(LaunchFailure.NoEmulatorConfigured.message(), ShadowToast.getTextOfLatestToast())
        assertEquals(emptyList<String>(), activity.events)
        assertEquals(0, recents.entries.value.size)
    }

    @Test
    fun `saved emulator that is not installed reports it and does not record`() {
        val activity = activity(RecordingActivity::class.java)
        prefs.setGameEmulator(gameKey(game), EMULATOR)

        launch(activity)

        assertEquals("Not installed: $EMULATOR", ShadowToast.getTextOfLatestToast())
        assertEquals(emptyList<String>(), activity.events)
        assertEquals(0, recents.entries.value.size)
    }

    @Test
    fun `rejected launch retries each intent then toasts without finishing`() {
        val activity = activity(RecordingActivity::class.java)
        activity.startFailure = { ActivityNotFoundException() }
        install(EMULATOR)
        prefs.setGameEmulator(gameKey(game), EMULATOR)

        launch(activity)

        assertEquals(
            listOf(
                "signal:recents=1",
                "start:$EMULATOR:$PRIMARY_DISPLAY_ID",
                "start:$EMULATOR:$PRIMARY_DISPLAY_ID",
                "start:$EMULATOR:-1",
                "start:$EMULATOR:-1",
            ),
            activity.events,
        )
        assertEquals("$EMULATOR wouldn't accept this ROM", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `refused display falls back to the default display and finishes`() {
        val activity = activity(RecordingActivity::class.java)
        install(EMULATOR)
        prefs.setGameEmulator(gameKey(game), EMULATOR)
        var attempts = 0
        activity.startFailure = { if (++attempts <= 2) SecurityException("display") else null }

        launch(activity)

        assertEquals(
            listOf(
                "signal:recents=1",
                "start:$EMULATOR:$PRIMARY_DISPLAY_ID",
                "start:$EMULATOR:$PRIMARY_DISPLAY_ID",
                "start:$EMULATOR:-1",
                "finish",
            ),
            activity.events,
        )
    }

    @Test
    fun `saved command launches on primary and finishes`() {
        val activity = activity(RecordingActivity::class.java)
        install(EMULATOR, "com.retroarch.aarch64")
        prefs.setGameEmulator(gameKey(game), EMULATOR)
        prefs.setGameLaunchCommand(gameKey(game), "%EMULATOR_RETROARCH%")

        launch(activity)

        assertEquals(
            listOf(
                "signal:recents=1",
                "start:com.retroarch.aarch64:$PRIMARY_DISPLAY_ID",
                "finish",
            ),
            activity.events,
        )
    }

    @Test
    fun `unbuildable saved command toasts and neither records nor finishes`() {
        val activity = activity(RecordingActivity::class.java)
        install(EMULATOR)
        prefs.setGameEmulator(gameKey(game), EMULATOR)
        prefs.setGameLaunchCommand(gameKey(game), "not-a-command")

        launch(activity)

        assertEquals("Failed to build launch intent", ShadowToast.getTextOfLatestToast())
        assertEquals(emptyList<String>(), activity.events)
        assertEquals(0, recents.entries.value.size)
    }

    @Test
    fun `sd card rom without a grant opens the picker and stores the pending game`() {
        val activity = activity(RecordingActivity::class.java)
        val sdGame = game.copy(romAbsolutePath = "/storage/1234-ABCD/Roms/snes/game.sfc")
        install(EMULATOR)
        prefs.setGameEmulator(gameKey(sdGame), EMULATOR)
        val launcher = launcher(activity)

        launcher.launchGame(sdGame, activity, DisplayPreference.PRIMARY_DISPLAY)

        assertEquals(sdGame, launcher.pendingGameLaunch?.first)
        assertEquals(
            "content://com.android.externalstorage.documents/document/1234-ABCD%3ARoms%2Fsnes",
            safHints.single().toString(),
        )
        assertEquals(emptyList<String>(), activity.events)
        assertEquals(0, recents.entries.value.size)
    }

    @Test
    fun `two installed companions emit the picker signal before launching`() {
        val activity = activity(RecordingActivity::class.java)
        val emitted = mutableListOf<List<String>>()
        val collector = CoroutineScope(Dispatchers.Unconfined).launch {
            stateHolder.showCompanionLaunchPickerSignal.collect { emitted += it }
        }
        install(EMULATOR)
        installLaunchable("com.example.one", "com.example.two")
        prefs.setGameEmulator(gameKey(game), EMULATOR)
        prefs.addGameCompanion(gameKey(game), "com.example.one")
        prefs.addGameCompanion(gameKey(game), "com.example.two")

        launch(activity)

        collector.cancel()
        assertEquals(listOf("com.example.one", "com.example.two"), emitted.single())
        assertEquals(
            listOf("signal:recents=1", "start:$EMULATOR:$PRIMARY_DISPLAY_ID", "finish"),
            activity.events,
        )
    }

    @Test
    fun `android game starts the app and finishes`() {
        val activity = activity(RecordingActivity::class.java)
        val androidGame = game.copy(
            systemName = "androidgames",
            path = "com.example.game.app",
            romAbsolutePath = null,
        )
        installLaunchable("com.example.game")

        launch(activity, androidGame)

        assertEquals(
            listOf("signal:recents=1", "start:com.example.game:$PRIMARY_DISPLAY_ID", "finish"),
            activity.events,
        )
    }

    @Test
    fun `missing android app toasts and still finishes the host`() {
        val activity = activity(RecordingActivity::class.java)
        val androidGame = game.copy(
            systemName = "androidapps",
            path = "com.example.gone.app",
            romAbsolutePath = null,
        )

        launch(activity, androidGame)

        assertEquals("App not installed: com.example.gone", ShadowToast.getTextOfLatestToast())
        assertEquals(listOf("finish"), activity.events)
        assertEquals(0, recents.entries.value.size)
    }

    @Test
    fun `picker launch persists the emulator and skips the saved command`() {
        val activity = activity(RecordingActivity::class.java)
        install(EMULATOR)
        prefs.setGameLaunchCommand(gameKey(game), "not-a-command")

        launcher(activity).launchGameWithEmulator(game, EMULATOR)

        assertEquals(EMULATOR, prefs.getGameEmulator(gameKey(game)))
        assertEquals(
            listOf("signal:recents=1", "start:$EMULATOR:$PRIMARY_DISPLAY_ID", "finish"),
            activity.events,
        )
        assertNull(ShadowToast.getTextOfLatestToast())
    }
}
