package jr.brian.home.util

import android.content.pm.PackageInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Guards the "sharpen the dialog copy when Mjolnir is actually installed" branch
 * of [jr.brian.home.ui.components.settings.HomeInterceptionSettingItem].
 *
 * The dialog wording is user-facing and legally sensitive — if this detector
 * silently returns false on a real Mjolnir install, the user gets softer generic
 * copy that lets them enable two conflicting interceptors simultaneously.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MjolnirDetectionTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val shadowPm get() = shadowOf(context.packageManager)

    @Test
    fun `returns false when Mjolnir is not installed`() {
        assertFalse(MjolnirDetection.isInstalled(context))
    }

    @Test
    fun `returns true when Mjolnir is installed`() {
        val info = PackageInfo().apply {
            packageName = MjolnirDetection.MJOLNIR_PACKAGE
            versionName = "1.0"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                longVersionCode = 1
            }
        }
        shadowPm.installPackage(info)

        assertTrue(MjolnirDetection.isInstalled(context))
    }

    @Test
    fun `does not match a similarly-named package`() {
        val info = PackageInfo().apply {
            packageName = "xyz.blacksheep.mjolnir.beta"
            versionName = "1.0"
        }
        shadowPm.installPackage(info)

        assertFalse(MjolnirDetection.isInstalled(context))
    }

    @Test
    fun `package constant is stable`() {
        assertTrue(MjolnirDetection.MJOLNIR_PACKAGE == "xyz.blacksheep.mjolnir")
    }
}
