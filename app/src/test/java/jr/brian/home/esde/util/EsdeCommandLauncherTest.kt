package jr.brian.home.esde.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric-backed tests for the ROM-launch intent shape. Real Intent / Uri
 * behaviour matters here: mockk stubs would let a broken code path assert
 * whatever we tell it to, and the whole point is to lock down what the
 * emulator actually receives.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class EsdeCommandLauncherTest {

    private val romPath = "/storage/emulated/0/Roms/ps2/Game.chd"
    private val bootUri: Uri = Uri.parse(
        "content://com.android.externalstorage.documents/tree/" +
            "primary%3ARoms%2Fps2/document/primary%3ARoms%2Fps2%2FGame.chd"
    )

    private fun contextWithInstalled(vararg installedPackages: String): Context {
        val real = RuntimeEnvironment.getApplication()
        val pm = mockk<PackageManager>(relaxed = true)
        val installed = installedPackages.toSet()
        every { pm.getPackageInfo(any<String>(), any<Int>()) } answers {
            val pkg = firstArg<String>()
            if (pkg in installed) PackageInfo().also { it.packageName = pkg }
            else throw PackageManager.NameNotFoundException(pkg)
        }
        val context = mockk<Context>(relaxed = true)
        every { context.packageManager } returns pm
        every { context.contentResolver } returns real.contentResolver
        return context
    }

    @Test
    fun `AetherSX2 intent carries bootPath, ClipData, no data, and read-grant flag`() {
        val context = contextWithInstalled("xyz.aethersx2.android")
        val intent = EsdeCommandLauncher.buildRomIntentFromPackage(
            packageName = "xyz.aethersx2.android",
            romAbsPath = romPath,
            contentUri = bootUri,
            context = context,
        )

        // Extra is the URI as a string under `bootPath`, matching ES-DE's
        // %EXTRA_bootPath%=%ROMSAF%.
        assertEquals(bootUri.toString(), intent.getStringExtra("bootPath"))
        // ClipData carries the read grant so a receiver reading only the
        // extra still resolves the URI.
        assertNotNull("ClipData must be populated to forward the grant", intent.clipData)
        assertEquals(1, intent.clipData!!.itemCount)
        assertEquals(bootUri, intent.clipData!!.getItemAt(0).uri)
        // intent.data would be an unexpected argument to AetherSX2 —
        // ExternalStorageProvider URI is delivered via extra only.
        assertNull("intent.data must remain unset for the UriExtra contract", intent.data)
        // ACTION_MAIN matches ES-DE's shipped es_systems.xml for AetherSX2.
        assertEquals(Intent.ACTION_MAIN, intent.action)
        // Explicit component so the intent doesn't resolve to a file browser
        // if a rebuild registered one under the same package.
        assertEquals("xyz.aethersx2.android", intent.component!!.packageName)
        assertEquals(
            "xyz.aethersx2.android.EmulationActivity",
            intent.component!!.className,
        )
        // Grant flag must be set for the ClipData-carried URI to be readable.
        assertTrue(
            "FLAG_GRANT_READ_URI_PERMISSION must be set",
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0,
        )
    }

    @Test
    fun `aX360e command intent has custom action, game_uri extra, ClipData, and read-grant flag`() {
        val context = contextWithInstalled("aenu.ax360e")
        val romUri: Uri = Uri.parse(
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3ARoms%2Fxbox360/document/primary%3ARoms%2Fxbox360%2FGame.iso"
        )
        val command =
            "%EMULATOR_AX360E% %ACTION%=aenu.intent.action.AX360E " +
                "%EXTRA_game_uri%=%ROMSAF%"
        val intent = EsdeCommandLauncher.buildIntent(
            launchCommand = command,
            romAbsPath = "/storage/emulated/0/Roms/xbox360/Game.iso",
            context = context,
            contentUri = romUri,
        )
        assertNotNull("Saved-command build must succeed for aX360e", intent)
        intent!!

        // Custom action from the emulator's own intent-filter — VIEW would be
        // silently ignored by the receiver that matches only on this action.
        assertEquals("aenu.intent.action.AX360E", intent.action)
        // Extra key is the URI string, matching %EXTRA_game_uri%=%ROMSAF%.
        assertEquals(romUri.toString(), intent.getStringExtra("game_uri"))
        // No %DATA%, so intent.data must not be set — an unexpected URI in
        // .data would divert some builds away from the extra-driven code path.
        assertNull(intent.data)
        // ClipData carries the read grant so a receiver reading only the
        // extra can still resolve the URI under scoped storage.
        assertNotNull("ClipData must be populated to forward the grant", intent.clipData)
        assertEquals(1, intent.clipData!!.itemCount)
        assertEquals(romUri, intent.clipData!!.getItemAt(0).uri)
        assertTrue(
            "FLAG_GRANT_READ_URI_PERMISSION must be set",
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0,
        )
        assertEquals("aenu.ax360e", intent.component!!.packageName)
        assertEquals(
            "aenu.ax360e.EmulatorActivity",
            intent.component!!.className,
        )
    }

    @Test
    fun `AetherSX2 registry and saved-command intents agree on the essentials`() {
        val context = contextWithInstalled("xyz.aethersx2.android")

        val registryIntent = EsdeCommandLauncher.buildRomIntentFromPackage(
            packageName = "xyz.aethersx2.android",
            romAbsPath = romPath,
            contentUri = bootUri,
            context = context,
        )

        val savedCommand =
            "%EMULATOR_AETHERSX2% %ACTIVITY_CLEAR_TASK% %ACTIVITY_CLEAR_TOP% " +
                "%ACTION%=android.intent.action.MAIN %EXTRA_bootPath%=%ROMSAF%"
        val commandIntent = EsdeCommandLauncher.buildIntent(
            launchCommand = savedCommand,
            romAbsPath = romPath,
            context = context,
            contentUri = bootUri,
        )
        assertNotNull("Saved-command build must succeed", commandIntent)

        // Same action.
        assertEquals(commandIntent!!.action, registryIntent.action)
        // Same explicit component — divergence here means the two paths route
        // to different activities, which is exactly the "works for me, broken
        // for them" symptom this reconciliation is meant to prevent.
        assertEquals(commandIntent.component, registryIntent.component)
        // Same bootPath extra.
        assertEquals(
            commandIntent.getStringExtra("bootPath"),
            registryIntent.getStringExtra("bootPath"),
        )
        // Both must carry the read-grant flag.
        val readGrant = Intent.FLAG_GRANT_READ_URI_PERMISSION
        assertTrue(commandIntent.flags and readGrant != 0)
        assertTrue(registryIntent.flags and readGrant != 0)
        // Both must attach ClipData carrying the ROM URI, so a receiver that
        // reads only the extra still gets a readable URI in both cases.
        assertNotNull(commandIntent.clipData)
        assertNotNull(registryIntent.clipData)
        assertEquals(bootUri, commandIntent.clipData!!.getItemAt(0).uri)
        assertEquals(bootUri, registryIntent.clipData!!.getItemAt(0).uri)
        // Neither must set intent.data — %DATA% is absent from the built-in
        // command, and the registry UriExtra branch deliberately leaves it null.
        assertNull(commandIntent.data)
        assertNull(registryIntent.data)
    }
}
