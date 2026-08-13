package jr.brian.home.esde.util

import android.content.Context
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmulatorRegistryTest {

    @Test
    fun `resolve returns Dolphin spec for suffixed debug build`() {
        val spec = EmulatorRegistry.resolve("org.dolphinemu.dolphinemu.debug")
        assertNotNull("debug variant should resolve to Dolphin", spec)
        assertEquals("org.dolphinemu.dolphinemu", spec!!.packageName)
        assertEquals("Dolphin", spec.displayName)
    }

    @Test
    fun `resolve returns exact My Boy Free row, not a My Boy variant`() {
        val spec = EmulatorRegistry.resolve("com.fastemulator.gbafree")
        assertNotNull(spec)
        // gbafree is a separate application listed in its own right — resolving
        // it as a variant of com.fastemulator.gba would mislabel and misroute.
        assertEquals("com.fastemulator.gbafree", spec!!.packageName)
        assertEquals("My Boy! Free", spec.displayName)
    }

    @Test
    fun `resolve returns null for an unrelated package`() {
        assertNull(EmulatorRegistry.resolve("com.unrelated.app"))
    }

    @Test
    fun `resolve picks longest matching base for a deeply nested suffix`() {
        // fzurita.pro is its own row; a further suffix should still land there
        // rather than on fzurita (its shorter sibling).
        val spec = EmulatorRegistry.resolve("org.mupen64plusae.v3.fzurita.pro.debug")
        assertNotNull(spec)
        assertEquals("org.mupen64plusae.v3.fzurita.pro", spec!!.packageName)
    }

    @Test
    fun `suffixed NetherSX2 package inherits needsExternalStorageUri`() {
        // A rebuild of NetherSX2 published as `xyz.aethersx2.tturnip.<suffix>`
        // must resolve to the same launch contract as the base row — else the
        // launcher skips the ExternalStorageProvider URI path and hands the
        // emulator something it cannot read.
        val spec = EmulatorRegistry.resolve("xyz.aethersx2.tturnip.nightly")
        assertNotNull(spec)
        assertEquals("xyz.aethersx2.tturnip", spec!!.packageName)
        assertTrue(
            "Suffixed variant must inherit needsExternalStorageUri",
            spec.needsExternalStorageUri,
        )
    }

    @Test
    fun `candidatesForExtension puts RetroArch after dedicated emulators for nds`() {
        val context = mockContextWithInstalled(
            "me.magnum.melonds",
            "com.retroarch",
            "com.retroarch.aarch64",
        )
        val candidates = EmulatorRegistry.candidatesForExtension(context, "nds")
        assertTrue("melonDS should be present", candidates.any { it.packageName == "me.magnum.melonds" })
        val melonIdx = candidates.indexOfFirst { it.packageName == "me.magnum.melonds" }
        val retroIdx = candidates.indexOfFirst { it.isFrontEnd }
        assertTrue("melonDS should come before any RetroArch front-end", melonIdx < retroIdx)
    }

    private fun mockContextWithInstalled(vararg installedPackages: String): Context {
        val pm = mockk<PackageManager>()
        val installed = installedPackages.toSet()
        every { pm.getPackageInfo(any<String>(), any<Int>()) } answers {
            val pkg = firstArg<String>()
            if (pkg in installed) mockk(relaxed = true)
            else throw PackageManager.NameNotFoundException(pkg)
        }
        val context = mockk<Context>()
        every { context.packageManager } returns pm
        return context
    }
}
