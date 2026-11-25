package jr.brian.home.esde.launch

import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplayManager

private const val COMPANION = "com.example.companion"

/** ActivityOptions writes setLaunchDisplayId under this key. */
private const val LAUNCH_DISPLAY_KEY = "android.activity.launchDisplayId"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CompanionLaunchTest {
    private lateinit var app: Application

    @Before
    fun setup() {
        app = RuntimeEnvironment.getApplication()
        LastGameDisplay.id = null
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(COMPANION)
        shadowOf(app.packageManager).apply {
            installPackage(PackageInfo().apply {
                packageName = COMPANION
                applicationInfo = ApplicationInfo().apply { packageName = COMPANION }
            })
            addResolveInfoForIntent(
                launcher,
                ResolveInfo().apply { activityInfo = ActivityInfo().apply { packageName = COMPANION; name = "$COMPANION.Main" } },
            )
        }
    }

    @After
    fun tearDown() {
        LastGameDisplay.id = null
    }

    private fun launchedDisplay(): Int? {
        val started = shadowOf(app).nextStartedActivityForResult ?: return null
        assertEquals(COMPANION, started.intent.component?.packageName ?: started.intent.`package`)
        return started.options?.getInt(LAUNCH_DISPLAY_KEY, -1)
    }

    @Test
    fun `the companion goes to the display the game is not on`() {
        assertEquals(4, companionDisplayId(gameDisplayId = 0, displayIds = listOf(0, 4)))
        assertEquals(0, companionDisplayId(gameDisplayId = 4, displayIds = listOf(0, 4)))
    }

    @Test
    fun `a game with no known display counts as on the primary one`() {
        assertEquals(4, companionDisplayId(gameDisplayId = null, displayIds = listOf(0, 4)))
        assertEquals(4, companionDisplayId(gameDisplayId = 9, displayIds = listOf(0, 4)))
    }

    @Test
    fun `one display has no other screen`() {
        assertNull(companionDisplayId(gameDisplayId = 0, displayIds = listOf(0)))
        assertNull(companionDisplayId(gameDisplayId = 0, displayIds = emptyList()))
    }

    @Test
    fun `with two screens the companion opens on the bottom one even when the app prefers the top`() {
        val bottom = ShadowDisplayManager.addDisplay("w1080dp-h1240dp")

        launchCompanion(app, COMPANION, DisplayPreference.PRIMARY_DISPLAY, gameDisplayId = 0)

        assertEquals(bottom, launchedDisplay())
    }

    @Test
    fun `a companion picked after the launch uses the game's display`() {
        val bottom = ShadowDisplayManager.addDisplay("w1080dp-h1240dp")
        LastGameDisplay.id = bottom

        launchCompanion(app, COMPANION, DisplayPreference.CURRENT_DISPLAY)

        assertEquals(0, launchedDisplay())
    }

    @Test
    fun `with one screen the companion keeps its own preference`() {
        launchCompanion(app, COMPANION, DisplayPreference.CURRENT_DISPLAY, gameDisplayId = 0)

        assertEquals(-1, launchedDisplay() ?: -1)
    }
}
