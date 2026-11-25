package jr.brian.home.jarn

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.JinglesManager
import jr.brian.home.data.RecentlyPlayedManager
import jr.brian.home.data.ScreenAssignmentStore
import jr.brian.home.data.ScreenFocusLockWriter
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.addGameCompanion
import jr.brian.home.esde.data.setGameEmulator
import jr.brian.home.esde.data.setGameLaunchCommand
import jr.brian.home.esde.launch.AppJarnGameLauncher
import jr.brian.home.esde.launch.AppJarnGameOptions
import jr.brian.home.esde.launch.RomLaunchEngine
import jr.brian.home.util.ThorDetection
import jr.brian.jarnfe.canvas.JarnAppDisplay
import jr.brian.jarnfe.launch.JarnGameRef
import jr.brian.jarnfe.launch.JarnLaunchRequest
import jr.brian.jarnfe.launch.JarnLaunchResult
import jr.brian.jarnfe.launch.JarnLaunchTarget
import jr.brian.jarnfe.shell.JarnScreenSwap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private const val EMULATOR = "com.example.emu"
private const val RETROARCH = "com.retroarch.aarch64"
private const val COMPANION = "com.example.companion"
private const val ANDROID_GAME = "com.example.game"
private const val KEY = "snes/./game.sfc"
private const val TARGET_DISPLAY = 7

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnFocusLockTest {

    class HostActivity : ComponentActivity() {
        override fun startActivity(intent: Intent, options: Bundle?) = Unit
        override fun startActivity(intent: Intent) = Unit
    }

    private lateinit var app: Application
    private lateinit var prefs: ESDEPreferencesManager
    private lateinit var displayPrefs: AppDisplayPreferenceManager
    private lateinit var engine: RomLaunchEngine
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
        displayPrefs = AppDisplayPreferenceManager(app)
        engine = RomLaunchEngine(prefs, displayPrefs, RecentlyPlayedManager(app))
        val jingles = mockk<JinglesManager>(relaxed = true)
        launcher = AppJarnGameLauncher(
            engine = engine,
            esdePrefs = prefs,
            appDisplayPreferenceManager = displayPrefs,
            romSearchStateHolder = RomSearchStateHolder(),
            jinglesManager = { jingles },
        )
        host = Robolectric.buildActivity(HostActivity::class.java).setup().get()
        mockkObject(ScreenFocusLockWriter, ThorDetection)
        every { ThorDetection.isThor() } returns true
        every { ScreenFocusLockWriter.syncToDisplayId(any(), any()) } just runs
        every { ScreenFocusLockWriter.syncToAssignment(any(), any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkObject(ScreenFocusLockWriter, ThorDetection)
    }

    private fun installLaunchable(vararg packages: String) {
        val shadow = shadowOf(app.packageManager)
        packages.forEach { pkg ->
            shadow.installPackage(
                PackageInfo().apply {
                    packageName = pkg
                    applicationInfo = ApplicationInfo().apply { packageName = pkg }
                },
            )
            val component = ComponentName(pkg, "$pkg.Main")
            shadow.addActivityIfNotPresent(component)
            shadow.addIntentFilterForActivity(
                component,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            )
        }
    }

    private fun launch(ref: JarnGameRef = game): JarnLaunchResult =
        launcher.launch(host, JarnLaunchRequest(ref, JarnLaunchTarget.Display(TARGET_DISPLAY)))

    private fun assertNoFocusLockWrite() {
        verify(exactly = 0) { ScreenFocusLockWriter.syncToDisplayId(any(), any()) }
        verify(exactly = 0) { ScreenFocusLockWriter.syncToAssignment(any(), any()) }
    }

    private fun assertSyncedToTarget() {
        verify(exactly = 1) { ScreenFocusLockWriter.syncToDisplayId(host, TARGET_DISPLAY) }
    }

    private fun appOptions() = AppJarnAppOptions(
        context = app,
        customNames = mockk(relaxed = true),
        customIcons = mockk(relaxed = true),
        displayPreferences = displayPrefs,
        iconPackManager = mockk(relaxed = true),
        iconRefresh = mockk(relaxed = true),
        visibility = mockk(relaxed = true),
        pageTypes = mockk(relaxed = true),
        pins = mockk(relaxed = true),
    )

    private fun gameOptions() = AppJarnGameOptions(
        context = app,
        esdePrefs = prefs,
        engine = engine,
        gameDao = mockk(relaxed = true),
        sourceDao = mockk(relaxed = true),
        catalog = mockk(relaxed = true),
    )

    @Test
    fun `screen switch contract has no way to request a focus lock write`() {
        val names = JarnScreenSwap::class.java.methods.map { it.name }
        assertFalse(names.any { it.contains("sync", ignoreCase = true) })
    }

    @Test
    fun `screen swap flips the assignment and writes nothing`() {
        val store = mockk<ScreenAssignmentStore>(relaxed = true)

        AppJarnScreenSwap(store).requestSwap()

        verify(exactly = 1) { store.flip() }
        assertNoFocusLockWrite()
    }

    @Test
    fun `emulator launch syncs focus lock`() {
        installLaunchable(EMULATOR)
        prefs.setGameEmulator(KEY, EMULATOR)

        assertEquals(JarnLaunchResult.Launched(EMULATOR, displayId = TARGET_DISPLAY), launch())

        assertSyncedToTarget()
    }

    @Test
    fun `saved command launch syncs focus lock`() {
        installLaunchable(EMULATOR, RETROARCH)
        prefs.setGameEmulator(KEY, EMULATOR)
        prefs.setGameLaunchCommand(KEY, "%EMULATOR_RETROARCH%")

        assertEquals(JarnLaunchResult.Launched(RETROARCH, displayId = TARGET_DISPLAY), launch())

        assertSyncedToTarget()
    }

    @Test
    fun `android game launch syncs focus lock`() {
        installLaunchable(ANDROID_GAME)
        val androidGame = game.copy(
            systemName = "androidgames",
            path = "$ANDROID_GAME.app",
            absolutePath = null,
        )

        assertEquals(JarnLaunchResult.Launched(ANDROID_GAME, displayId = TARGET_DISPLAY), launch(androidGame))

        assertSyncedToTarget()
    }

    @Test
    fun `automatic companion launch syncs focus lock with the game`() {
        installLaunchable(EMULATOR, COMPANION)
        prefs.setGameEmulator(KEY, EMULATOR)
        prefs.addGameCompanion(KEY, COMPANION)

        launch()

        verify(exactly = 2) { ScreenFocusLockWriter.syncToDisplayId(host, any()) }
        assertSyncedToTarget()
    }

    @Test
    fun `opening a companion syncs focus lock`() {
        installLaunchable(COMPANION)

        gameOptions().openCompanion(host, COMPANION)

        verify(exactly = 1) { ScreenFocusLockWriter.syncToDisplayId(host, any()) }
    }

    @Test
    fun `app launch syncs focus lock`() {
        installLaunchable(COMPANION)

        appOptions().launchApp(host, COMPANION)

        verify(exactly = 1) { ScreenFocusLockWriter.syncToDisplayId(host, any()) }
    }

    @Test
    fun `open on other display syncs focus lock to that display`() {
        installLaunchable(COMPANION)

        appOptions().launchAppOn(host, COMPANION, JarnAppDisplay.Primary)

        verify(exactly = 1) { ScreenFocusLockWriter.syncToDisplayId(host, PRIMARY_DISPLAY_ID) }
    }
}
