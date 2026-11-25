package jr.brian.home.esde.launch

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import io.mockk.mockk
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.JinglesManager
import jr.brian.home.data.RecentlyPlayedManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.addGameCompanion
import jr.brian.home.esde.data.getGameCore
import jr.brian.home.esde.data.getGameEmulator
import jr.brian.home.esde.data.getGameLaunchCommand
import jr.brian.home.esde.data.setGameCore
import jr.brian.home.esde.data.setGameEmulator
import jr.brian.jarnfe.launch.JarnEmulatorChoice
import jr.brian.jarnfe.launch.JarnEmulatorChoiceKind
import jr.brian.jarnfe.launch.JarnGameRef
import jr.brian.jarnfe.launch.JarnLaunchFailure
import jr.brian.jarnfe.launch.JarnLaunchRequest
import jr.brian.jarnfe.launch.JarnLaunchResult
import jr.brian.jarnfe.launch.JarnLaunchTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
private const val RETROARCH = "com.retroarch.aarch64"
private const val DISPLAY_KEY = "android.activity.launchDisplayId"
private const val KEY = "snes/./game.sfc"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppJarnGameLauncherTest {

    class HostActivity : ComponentActivity() {
        val starts = mutableListOf<Pair<String?, Int>>()
        var finished = false
        var startFailure: Exception? = null

        override fun startActivity(intent: Intent, options: Bundle?) {
            starts += (intent.`package` ?: intent.component?.packageName) to
                (options?.getInt(DISPLAY_KEY, -1) ?: -1)
            startFailure?.let { throw it }
        }

        override fun finish() {
            finished = true
        }
    }

    private lateinit var app: Application
    private lateinit var prefs: ESDEPreferencesManager
    private lateinit var recents: RecentlyPlayedManager
    private lateinit var launcher: AppJarnGameLauncher
    private lateinit var host: HostActivity

    private val game = JarnGameRef(
        systemName = "snes",
        path = "./game.sfc",
        title = "Game",
        absolutePath = "/storage/emulated/0/Roms/snes/game.sfc",
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
        val displayPrefs = AppDisplayPreferenceManager(app)
        val jingles = mockk<JinglesManager>(relaxed = true)
        launcher = AppJarnGameLauncher(
            engine = RomLaunchEngine(prefs, displayPrefs, recents),
            esdePrefs = prefs,
            appDisplayPreferenceManager = displayPrefs,
            romSearchStateHolder = RomSearchStateHolder(),
            jinglesManager = { jingles },
        )
        host = Robolectric.buildActivity(HostActivity::class.java).setup().get()
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

    private fun launch(
        target: JarnLaunchTarget = JarnLaunchTarget.ClassicDefault,
        choice: JarnEmulatorChoice? = null,
        ref: JarnGameRef = game,
    ) = launcher.launch(host, JarnLaunchRequest(ref, target, choice))

    private fun assertNoUi() {
        assertEquals(0, ShadowToast.shownToastCount())
        assertFalse(host.finished)
    }

    @Test
    fun `launches on the Classic display, records it, and keeps the host open`() {
        install(EMULATOR)
        prefs.setGameEmulator(KEY, EMULATOR)

        assertEquals(JarnLaunchResult.Launched(EMULATOR, displayId = PRIMARY_DISPLAY_ID), launch())

        assertEquals(EMULATOR to PRIMARY_DISPLAY_ID, host.starts.single())
        assertEquals(1, recents.entries.value.size)
        assertNoUi()
    }

    @Test
    fun `explicit display target is honored`() {
        install(EMULATOR)
        prefs.setGameEmulator(KEY, EMULATOR)

        launch(target = JarnLaunchTarget.Display(7))

        assertEquals(EMULATOR to 7, host.starts.single())
    }

    @Test
    fun `no emulator asks for a choice and starts nothing`() {
        assertEquals(
            JarnLaunchResult.NeedsEmulatorChoice(JarnEmulatorChoiceKind.Emulator),
            launch(),
        )
        assertTrue(host.starts.isEmpty())
        assertNoUi()
    }

    @Test
    fun `retroarch without a core asks for the core`() {
        install(RETROARCH)
        prefs.setGameEmulator(KEY, RETROARCH)

        assertEquals(
            JarnLaunchResult.NeedsEmulatorChoice(JarnEmulatorChoiceKind.RetroArchCore),
            launch(),
        )
        assertTrue(host.starts.isEmpty())
    }

    @Test
    fun `emulator choice is saved like Classic and launch continues`() {
        install(EMULATOR)

        val result = launch(choice = JarnEmulatorChoice(EMULATOR, corePath = "/core.so"))

        assertEquals(JarnLaunchResult.Launched(EMULATOR, displayId = PRIMARY_DISPLAY_ID), result)
        assertEquals(EMULATOR, prefs.getGameEmulator(KEY))
        assertEquals("/core.so", prefs.getGameCore(KEY))
        assertEquals(null, prefs.getGameLaunchCommand(KEY))
    }

    @Test
    fun `sd card rom without a grant asks for file access`() {
        install(EMULATOR)
        prefs.setGameEmulator("snes/./sd.sfc", EMULATOR)
        val sdGame = game.copy(
            path = "./sd.sfc",
            absolutePath = "/storage/1234-ABCD/Roms/snes/sd.sfc",
        )

        assertEquals(
            JarnLaunchResult.NeedsFileAccess(
                "snes",
                "content://com.android.externalstorage.documents/document/1234-ABCD%3ARoms%2Fsnes",
            ),
            launch(ref = sdGame),
        )
        assertTrue(host.starts.isEmpty())
        assertNoUi()
    }

    @Test
    fun `rom is missing only when all-files access can prove it`() {
        assertTrue(isRomDefinitelyMissing(canSeeAllFiles = true, exists = false))
        assertFalse(isRomDefinitelyMissing(canSeeAllFiles = false, exists = false))
        assertFalse(isRomDefinitelyMissing(canSeeAllFiles = true, exists = true))
    }

    @Test
    fun `uninstalled emulator is an explicit failure`() {
        prefs.setGameEmulator(KEY, EMULATOR)

        assertEquals(
            JarnLaunchResult.Failed(JarnLaunchFailure.EmulatorNotInstalled, EMULATOR),
            launch(),
        )
        assertNoUi()
    }

    @Test
    fun `rejected start is an explicit failure without UI`() {
        install(EMULATOR)
        prefs.setGameEmulator(KEY, EMULATOR)
        host.startFailure = android.content.ActivityNotFoundException()

        assertEquals(
            JarnLaunchResult.Failed(JarnLaunchFailure.EmulatorRejected, EMULATOR),
            launch(),
        )
        assertNoUi()
    }

    @Test
    fun `two companions are returned for the caller to choose`() {
        install(EMULATOR)
        installLaunchable("com.example.one", "com.example.two")
        prefs.setGameEmulator(KEY, EMULATOR)
        prefs.addGameCompanion(KEY, "com.example.one")
        prefs.addGameCompanion(KEY, "com.example.two")

        assertEquals(
            JarnLaunchResult.Launched(EMULATOR, listOf("com.example.one", "com.example.two"), PRIMARY_DISPLAY_ID),
            launch(),
        )
    }

    @Test
    fun `android game honors an explicit display and missing app is a failure`() {
        installLaunchable("com.example.game")
        val androidGame = game.copy(
            systemName = "androidgames",
            path = "com.example.game.app",
            absolutePath = null,
        )

        assertEquals(
            JarnLaunchResult.Launched("com.example.game", displayId = 7),
            launch(target = JarnLaunchTarget.Display(7), ref = androidGame),
        )
        assertEquals("com.example.game" to 7, host.starts.single())
        assertEquals(
            JarnLaunchResult.Failed(JarnLaunchFailure.AppNotInstalled, "com.example.gone"),
            launch(ref = androidGame.copy(path = "com.example.gone.app")),
        )
        assertNoUi()
    }

    @Test
    fun `saved core satisfies the retroarch gate`() {
        install(RETROARCH)
        prefs.setGameEmulator(KEY, RETROARCH)
        prefs.setGameCore(KEY, "/core.so")

        assertEquals(JarnLaunchResult.Launched(RETROARCH, displayId = PRIMARY_DISPLAY_ID), launch())
    }

    @Test
    fun `gate matches the Classic grid order`() {
        assertEquals(RomLaunchGate.Launch, romLaunchGate(true, true, false, false))
        assertEquals(RomLaunchGate.PickCore, romLaunchGate(false, true, false, true))
        assertEquals(RomLaunchGate.Launch, romLaunchGate(false, true, true, true))
        assertEquals(RomLaunchGate.Launch, romLaunchGate(false, false, false, true))
        assertEquals(RomLaunchGate.PickEmulator, romLaunchGate(false, false, false, false))
    }
}
