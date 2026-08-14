package jr.brian.home.esde.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // ---- Additive-row coverage -------------------------------------------------

    /**
     * Every package added to the registry must be reachable via [EmulatorRegistry.resolve]
     * as an exact match — a typo on `packageName` would otherwise silently misroute
     * everything through the suffix fallback (or nothing at all).
     */
    @Test
    fun `every newly added package resolves to itself`() {
        for (pkg in NEWLY_ADDED_PACKAGES) {
            val spec = EmulatorRegistry.resolve(pkg)
            assertNotNull("Registry is missing an exact row for $pkg", spec)
            assertEquals(
                "Registry row for $pkg does not resolve to itself (typo in packageName?)",
                pkg,
                spec!!.packageName,
            )
        }
    }

    @Test
    fun `no duplicate packageName in KNOWN`() {
        val packages = EmulatorRegistry.KNOWN.map { it.packageName }
        val duplicates = packages.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        assertTrue(
            "Duplicate packageName entries in registry: $duplicates",
            duplicates.isEmpty(),
        )
    }

    /**
     * The ROM scanner treats each emulator's [EmulatorSpec.extensions] list as
     * authoritative for what a ROM looks like on disk. An empty list on a row
     * that is NOT a shortcut launcher or undocumented builds silently drops
     * every game from every affected system — the scanner reports zero ROMs
     * and the UI shows an empty tab. Rows whose contract is [PathExtra] (Wine
     * shortcuts) or [Undocumented] (opaque launchers like GameNative / ScummVM)
     * legitimately have nothing to scan.
     */
    @Test
    fun `every scannable row declares at least one extension`() {
        val offenders = EmulatorRegistry.KNOWN.filter { spec ->
            spec.extensions.isEmpty() &&
                spec.launchContract !is RomLaunchContract.PathExtra &&
                spec.launchContract !is RomLaunchContract.Undocumented
        }.map { it.packageName }
        assertTrue(
            "Rows without extensions that would scan nothing: $offenders",
            offenders.isEmpty(),
        )
    }

    // ---- Winlator additions ----------------------------------------------------

    @Test
    fun `com winlator base row uses XServerDisplayActivity and shortcut_path extra`() {
        val spec = EmulatorRegistry.resolve("com.winlator")
        assertNotNull(spec)
        assertEquals("com.winlator.XServerDisplayActivity", spec!!.activityName)
        val contract = spec.launchContract
        assertTrue(
            "com.winlator should use PathExtra contract",
            contract is RomLaunchContract.PathExtra,
        )
        assertEquals("shortcut_path", (contract as RomLaunchContract.PathExtra).key)
    }

    @Test
    fun `com loki winlator row uses cmod XServerDisplayActivity and shortcut_path extra`() {
        val spec = EmulatorRegistry.resolve("com.loki.winlator")
        assertNotNull(spec)
        assertEquals("com.winlator.cmod.XServerDisplayActivity", spec!!.activityName)
        val contract = spec.launchContract
        assertTrue(
            "com.loki.winlator should use PathExtra contract",
            contract is RomLaunchContract.PathExtra,
        )
        assertEquals("shortcut_path", (contract as RomLaunchContract.PathExtra).key)
    }

    // ---- Frozen contracts ------------------------------------------------------
    // These four rows are the ones with the most brittle downstream behaviour:
    // AetherSX2 needs the ExternalStorageProvider path or it black-screens;
    // DuckStation rejects any intent.data and must read a URI extra; RetroArch
    // fires ACTION_MAIN with the ROM path in a bespoke extra; PPSSPP resolves
    // through a fixed activity. Assert them explicitly so a future edit that
    // "cleans up" one of these regresses the whole launch path with a red bar
    // rather than a silent runtime failure.

    @Test
    fun `AetherSX2 row keeps ExternalStorageProvider bootPath contract`() {
        val spec = EmulatorRegistry.resolve("xyz.aethersx2.android")
        assertNotNull(spec)
        assertEquals("xyz.aethersx2.android.EmulationActivity", spec!!.activityName)
        assertTrue("AetherSX2 must retain needsExternalStorageUri", spec.needsExternalStorageUri)
        val contract = spec.launchContract
        assertTrue(
            "AetherSX2 must use UriExtra, not PathExtra",
            contract is RomLaunchContract.UriExtra,
        )
        contract as RomLaunchContract.UriExtra
        assertEquals("bootPath", contract.key)
        assertEquals(Intent.ACTION_MAIN, contract.action)
    }

    @Test
    fun `DuckStation row keeps bootPath UriExtra contract`() {
        val spec = EmulatorRegistry.resolve("com.github.stenzek.duckstation")
        assertNotNull(spec)
        assertEquals(
            "com.github.stenzek.duckstation.EmulationActivity",
            spec!!.activityName,
        )
        assertFalse(
            "DuckStation reads a FileProvider URI, not an ExternalStorageProvider URI",
            spec.needsExternalStorageUri,
        )
        val contract = spec.launchContract
        assertTrue(
            "DuckStation must use UriExtra, not PathExtra",
            contract is RomLaunchContract.UriExtra,
        )
        contract as RomLaunchContract.UriExtra
        assertEquals("bootPath", contract.key)
        assertEquals(Intent.ACTION_VIEW, contract.action)
    }

    @Test
    fun `RetroArch rows keep the RetroArch launch contract and front-end flag`() {
        for (pkg in listOf("com.retroarch", "com.retroarch.aarch64", "com.retroarch.ra32")) {
            val spec = EmulatorRegistry.resolve(pkg)
            assertNotNull("RetroArch variant missing: $pkg", spec)
            assertEquals(
                "com.retroarch.browser.retroactivity.RetroActivityFuture",
                spec!!.activityName,
            )
            assertTrue(
                "$pkg must remain flagged as a front-end",
                spec.isFrontEnd,
            )
            assertTrue(
                "$pkg must use the RetroArch launch contract",
                spec.launchContract === RomLaunchContract.RetroArch,
            )
        }
    }

    @Test
    fun `PPSSPP rows keep the default ContentUriView contract and PpssppActivity`() {
        for (pkg in listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold")) {
            val spec = EmulatorRegistry.resolve(pkg)
            assertNotNull(spec)
            assertEquals("org.ppsspp.ppsspp.PpssppActivity", spec!!.activityName)
            assertTrue(
                "$pkg must use the default ContentUriView contract",
                spec.launchContract === RomLaunchContract.ContentUriView,
            )
        }
    }

    // ---- GameHub Lite additions -----------------------------------------------

    @Test
    fun `gamehub lite package resolves through the registry`() {
        val spec = EmulatorRegistry.resolve("gamehub.lite")
        assertNotNull("gamehub.lite must be reachable via resolve()", spec)
        assertEquals("gamehub.lite", spec!!.packageName)
        assertEquals(
            "com.xj.landscape.launcher.ui.gamedetail.GameDetailActivity",
            spec.activityName,
        )
        assertTrue(
            "GameHub Lite should carry an Undocumented hint like GameNative",
            spec.launchContract is RomLaunchContract.Undocumented,
        )
        assertEquals(listOf("steam", "localgameid"), spec.extensions)
    }

    @Test
    fun `com xj landscape launcher package resolves through the registry`() {
        val spec = EmulatorRegistry.resolve("com.xj.landscape.launcher")
        assertNotNull(spec)
        assertEquals("com.xj.landscape.launcher", spec!!.packageName)
    }

    // ---- Dedupe by (package, command) -----------------------------------------

    @Test
    fun `both GameHub Lite commands survive dedupe when the package is installed`() {
        val context = mockContextWithInstalled("gamehub.lite")
        val options = EsdeCommandLauncher.getCompatibleEmulatorsFromSystem(
            context, "steam", NON_EXISTENT_FILE,
        )
        val labels = options.map { it.displayName }
        assertTrue(
            "GameHub Lite (Steam) should appear for the steam system: $labels",
            "GameHub Lite (Steam)" in labels,
        )
        assertTrue(
            "GameHub Lite (Local) should appear for the steam system: $labels",
            "GameHub Lite (Local)" in labels,
        )
    }

    @Test
    fun `steam system returns all three commands in built-in order`() {
        val context = mockContextWithInstalled("app.gamenative", "gamehub.lite")
        val options = EsdeCommandLauncher.getCompatibleEmulatorsFromSystem(
            context, "steam", NON_EXISTENT_FILE,
        )
        // Membership and order are load-bearing — the deliberate decision is to
        // mark, not filter, so anything that changes count or order regresses it.
        assertEquals(3, options.size)
        assertEquals(
            listOf("GameNative", "GameHub Lite (Steam)", "GameHub Lite (Local)"),
            options.map { it.displayName },
        )
    }

    // ---- requiredExtension inference -----------------------------------------

    @Test
    fun `requiredExtension marks Steam-id and localgameid commands, agnostic elsewhere`() {
        val context = mockContextWithInstalled("app.gamenative", "gamehub.lite")
        val steamOptions = EsdeCommandLauncher.getCompatibleEmulatorsFromSystem(
            context, "steam", NON_EXISTENT_FILE,
        ).associateBy { it.displayName }
        assertEquals("steam", steamOptions["GameNative"]?.requiredExtension)
        assertEquals("steam", steamOptions["GameHub Lite (Steam)"]?.requiredExtension)
        assertEquals("localgameid", steamOptions["GameHub Lite (Local)"]?.requiredExtension)
    }

    @Test
    fun `extension-agnostic system leaves requiredExtension null on every command`() {
        // ps2 commands all read the ROM path, not a namespaced id — no command
        // should carry a requiredExtension, or the picker will mislabel every
        // ps2 ROM as needing a special extension.
        val context = mockContextWithInstalled(
            "xyz.aethersx2.android",
            "xyz.aethersx2.tturnip",
            "xyz.aethersx2.cturnip",
            "net.pcsx2.pcsx2",
            "com.armsx2",
            "com.sbro.emucorex",
        )
        val options = EsdeCommandLauncher.getCompatibleEmulatorsFromSystem(
            context, "ps2", NON_EXISTENT_FILE,
        )
        assertTrue("ps2 should return at least one command", options.isNotEmpty())
        val offenders = options.filter { it.requiredExtension != null }
            .map { "${it.displayName}=${it.requiredExtension}" }
        assertTrue(
            "No ps2 command should have a requiredExtension: $offenders",
            offenders.isEmpty(),
        )
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

    private companion object {
        /**
         * A path that will not exist on any test host. `parseSystemCommands`
         * returns an empty list for a missing file, so
         * `getCompatibleEmulatorsFromSystem` falls back to the built-in
         * command table — which is what these tests exercise.
         */
        val NON_EXISTENT_FILE = File("/does/not/exist/es_systems.xml")

        /**
         * Packages added by the emulator-registry expansion. Enumerated so a typo
         * on `packageName` in the registry is caught by the resolve-round-trip
         * test above.
         */
        val NEWLY_ADDED_PACKAGES = listOf(
            // Multi-system front-end
            "com.swordfish.lemuroid",
            // PS2
            "com.play.emu",
            // PSX
            "com.epsxe.ePSXe",
            "com.emulator.fpse",
            "com.emulator.fpse64",
            // GameCube / Wii
            "org.dolphinemu.dolphinemu.mmjr",
            "org.mmjr.dolphinemu",
            // PS3
            "aenu.aps3e",
            "aenu.aps3e.premium",
            // N64
            "org.mupen64plusae.v3.alpha",
            // NDS
            "us.rewrite.nds",
            // 3DS
            "org.citra.citra_emu.canary",
            "org.citra.emu",
            "io.github.mandarine3ds.mandarine",
            "com.panda3ds.pandroid",
            // Switch
            "org.yuzu.yuzu_emu.ea",
            "org.stratoemu.strato",
            "skyline.emu",
            // explusalpha .emu family
            "com.explusalpha.A2600Emu",
            "com.explusalpha.C64Emu",
            "com.explusalpha.LynxEmu",
            "com.explusalpha.MdEmu",
            "com.explusalpha.MsxEmu",
            "com.explusalpha.NeoEmu",
            "com.explusalpha.NesEmu",
            "com.explusalpha.NgpEmu",
            "com.explusalpha.PceEmu",
            "com.explusalpha.Saturn",
            "com.explusalpha.Snes9xPlus",
            "com.explusalpha.SwanEmu",
            // FMS Ltd. family
            "com.fms.colem",
            "com.fms.colem.deluxe",
            "com.fms.fmsx",
            "com.fms.fmsx.deluxe",
            "com.fms.ines.free",
            "com.fms.mg",
            "com.fms.speccy",
            "com.fms.speccy.deluxe",
            // John emulators
            "com.johnemulators.johngba",
            "com.johnemulators.johngbac",
            "com.johnemulators.johngbalite",
            "com.johnemulators.johngbc",
            "com.johnemulators.johngbclite",
            "com.johnemulators.johnness",
            // PC / Windows
            "com.winlator",
            "com.loki.winlator",
            // Dreamcast
            "com.flycast.emulator",
            "com.reicast.emulator",
            "io.recompiled.redream",
            // Saturn
            "org.uoyabause.uranus",
            // Vita
            "org.vita3k.emulator",
            // Arcade
            "com.seleuco.mame4droid",
            "com.seleuco.mame4d2024",
            // Xbox / Xbox 360
            "com.izzy2lost.x1box",
            "com.rfandango.haku_x",
            "aenu.ax360e",
            "aenu.ax360e.free",
            "xendroid.compose",
            "emu.x360.mobile",
        )
    }

    // ---- xbox / xbox360 --------------------------------------------------------

    @Test
    fun `aX360e row uses custom action UriExtra with game_uri key`() {
        val spec = EmulatorRegistry.resolve("aenu.ax360e")
        assertNotNull(spec)
        assertEquals("aenu.ax360e.EmulatorActivity", spec!!.activityName)
        val contract = spec.launchContract
        assertTrue(
            "aX360e must use a UriExtra contract, not the default ContentUriView",
            contract is RomLaunchContract.UriExtra,
        )
        contract as RomLaunchContract.UriExtra
        assertEquals("game_uri", contract.key)
        assertEquals("aenu.intent.action.AX360E", contract.action)
    }

    @Test
    fun `systemExtensionsFallback for xbox360 returns iso xex zar`() {
        assertEquals(
            setOf("iso", "xex", "zar"),
            EsdeCommandLauncher.systemExtensionsFallback("xbox360"),
        )
    }

    @Test
    fun `systemExtensionsFallback for xbox returns the union of X1 BOX and hakuX extensions`() {
        assertEquals(
            setOf("iso", "xiso", "cso", "cci"),
            EsdeCommandLauncher.systemExtensionsFallback("xbox"),
        )
    }

    @Test
    fun `xbox360 system offers aX360e, X360 Mobile, and XenDroid in built-in order`() {
        val context = mockContextWithInstalled(
            "aenu.ax360e",
            "emu.x360.mobile",
            "xendroid.compose",
        )
        val options = EsdeCommandLauncher.getCompatibleEmulatorsFromSystem(
            context, "xbox360", NON_EXISTENT_FILE,
        )
        assertEquals(
            listOf("aX360e", "X360 Mobile", "XenDroid"),
            options.map { it.displayName },
        )
        assertEquals(
            listOf("aenu.ax360e", "emu.x360.mobile", "xendroid.compose"),
            options.map { it.packageName },
        )
    }

    @Test
    fun `xbox360 falls back to aX360e Free when only the free build is installed`() {
        // The AX360E find rule lists the paid package first and the .free
        // package second; if only .free is present, the command still needs to
        // resolve — otherwise a legitimate open-source install shows "no rule".
        val context = mockContextWithInstalled("aenu.ax360e.free")
        val options = EsdeCommandLauncher.getCompatibleEmulatorsFromSystem(
            context, "xbox360", NON_EXISTENT_FILE,
        )
        assertEquals(1, options.size)
        assertEquals("aX360e", options[0].displayName)
        assertEquals("aenu.ax360e.free", options[0].packageName)
    }

    @Test
    fun `xbox system offers X1 BOX and hakuX in built-in order`() {
        val context = mockContextWithInstalled(
            "com.izzy2lost.x1box",
            "com.rfandango.haku_x",
        )
        val options = EsdeCommandLauncher.getCompatibleEmulatorsFromSystem(
            context, "xbox", NON_EXISTENT_FILE,
        )
        assertEquals(
            listOf("X1 BOX", "hakuX"),
            options.map { it.displayName },
        )
    }
}
