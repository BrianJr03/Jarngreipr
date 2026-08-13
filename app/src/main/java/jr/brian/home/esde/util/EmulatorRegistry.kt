package jr.brian.home.esde.util

import android.content.pm.PackageManager
import android.content.Context

/** The exact public intent shape an emulator accepts for a ROM launch. */
sealed interface RomLaunchContract {

    /** ACTION_VIEW carrying the content URI as intent data. The common case. */
    data object ContentUriView : RomLaunchContract

    /**
     * The content URI, as a string, under a documented extra key.
     *
     * The read grant is carried on `intent.clipData` — `FLAG_GRANT_READ_URI_PERMISSION`
     * forwards to any URI in the clip, and ClipData is not used for intent
     * matching, so a receiver that only reads the extra never sees it. This
     * avoids setting `intent.data`, which some builds (Dolphin, DuckStation,
     * AetherSX2) treat as an unexpected argument and reject.
     *
     * [action] is the intent action to fire. Defaults to VIEW; set to MAIN for
     * emulators whose public entry point sits behind an ACTION_MAIN filter
     * (AetherSX2 / NetherSX2, matching ES-DE's shipped es_systems.xml).
     */
    data class UriExtra(
        val key: String,
        val action: String = android.content.Intent.ACTION_VIEW,
    ) : RomLaunchContract

    /** A raw shared-storage path under a documented extra key. */
    data class PathExtra(val key: String) : RomLaunchContract

    /** RetroArch's ACTION_MAIN + `ROM` path extra; core supplied via `LIBRETRO`. */
    data object RetroArch : RomLaunchContract

    /**
     * Nothing documented is known about how this build takes a ROM.
     *
     * Not a refusal — [ContentUriView] is attempted anyway and [hint] is only
     * shown if every attempt fails. These builds are the most likely to have
     * gained a VIEW filter since the row was written.
     */
    data class Undocumented(val hint: String) : RomLaunchContract
}

/**
 * One emulator, and how to hand it a ROM.
 *
 * @param activityName explicit component. Null means resolve the package's own
 *   default VIEW handler. Getting this wrong is recoverable — the launcher
 *   retries without the component before giving up.
 * @param extensions file extensions this emulator can open, for the picker.
 * @param isFrontEnd many cores behind one app (RetroArch). Offered last on an
 *   automatic pick, because it needs a core downloaded and assigned first.
 */
data class EmulatorSpec(
    val packageName: String,
    val displayName: String,
    val activityName: String? = null,
    val launchContract: RomLaunchContract = RomLaunchContract.ContentUriView,
    val extensions: List<String> = emptyList(),
    val isFrontEnd: Boolean = false,
    /**
     * The emulator can only read an ExternalStorageProvider document URI (not a
     * FileProvider URI from another authority, not a raw path). Callers must
     * build the URI via [buildAetherDocUri] and forward the persisted tree
     * grant so multi-file ROMs (.cue+.bin, .m3u+chunks) can resolve siblings.
     */
    val needsExternalStorageUri: Boolean = false,
)

object EmulatorRegistry {

    private val RETRO_EXTS = listOf(
        "gb", "gbc", "gba", "nes", "sfc", "smc", "n64", "z64", "v64", "nds",
        "iso", "bin", "cue", "md", "smd", "gen", "sms", "gg", "32x", "pce",
        "ccd", "img", "mdf", "chd", "pbp", "cso",
    )
    private const val RETRO_ACTIVITY =
        "com.retroarch.browser.retroactivity.RetroActivityFuture"
    private const val AETHER_ACTIVITY = "xyz.aethersx2.android.EmulationActivity"

    val KNOWN: List<EmulatorSpec> = listOf(
        // ---- RetroArch (front-ends, offered last) --------------------------
        EmulatorSpec("com.retroarch.aarch64", "RetroArch 64", RETRO_ACTIVITY,
            RomLaunchContract.RetroArch, RETRO_EXTS, isFrontEnd = true),
        EmulatorSpec("com.retroarch.ra32", "RetroArch 32", RETRO_ACTIVITY,
            RomLaunchContract.RetroArch, RETRO_EXTS, isFrontEnd = true),
        EmulatorSpec("com.retroarch", "RetroArch", RETRO_ACTIVITY,
            RomLaunchContract.RetroArch, RETRO_EXTS, isFrontEnd = true),
        EmulatorSpec("com.swordfish.lemuroid", "Lemuroid",
            extensions = RETRO_EXTS, isFrontEnd = true),

        // ---- PSP -----------------------------------------------------------
        EmulatorSpec("org.ppsspp.ppsspp", "PPSSPP", "org.ppsspp.ppsspp.PpssppActivity",
            extensions = listOf("iso", "cso", "pbp", "elf")),
        EmulatorSpec("org.ppsspp.ppssppgold", "PPSSPP Gold", "org.ppsspp.ppsspp.PpssppActivity",
            extensions = listOf("iso", "cso", "pbp", "elf")),
        EmulatorSpec("com.emu.ppss22", "PPSS22", "com.emu.ppss22.MainActivity",
            RomLaunchContract.PathExtra("ROM_PATH"),
            listOf("iso", "bin", "chd", "cso")),

        // ---- PS2 -----------------------------------------------------------
        // bootPath must be an ExternalStorageProvider document URI (ES-DE passes
        // %ROMSAF% here); these builds cannot read a raw /storage path under
        // scoped storage, nor a FileProvider URI from an unrelated authority.
        EmulatorSpec("xyz.aethersx2.android", "AetherSX2", AETHER_ACTIVITY,
            RomLaunchContract.UriExtra("bootPath", android.content.Intent.ACTION_MAIN),
            listOf("iso", "bin", "elf", "chd", "cso", "gz"),
            needsExternalStorageUri = true),
        EmulatorSpec("xyz.aethersx2.tturnip", "NetherSX2 Turnip", AETHER_ACTIVITY,
            RomLaunchContract.UriExtra("bootPath", android.content.Intent.ACTION_MAIN),
            listOf("iso", "bin", "elf", "chd", "cso", "gz"),
            needsExternalStorageUri = true),
        EmulatorSpec("xyz.aethersx2.cturnip", "NetherSX2 Turnip Classic", AETHER_ACTIVITY,
            RomLaunchContract.UriExtra("bootPath", android.content.Intent.ACTION_MAIN),
            listOf("iso", "bin", "elf", "chd", "cso", "gz"),
            needsExternalStorageUri = true),
        EmulatorSpec("net.pcsx2.pcsx2", "PCSX2", "net.pcsx2.pcsx2.NativeActivity",
            extensions = listOf("iso", "bin", "elf", "chd", "cso", "gz")),
        EmulatorSpec("com.armsx2", "ARMSX2 Refresh", "com.armsx2.Main",
            extensions = listOf("iso", "bin", "chd", "cso")),
        EmulatorSpec("com.sbro.emucorex", "EmuCoreX", "com.sbro.emucorex.MainActivity",
            extensions = listOf("iso", "bin", "chd", "cso")),
        EmulatorSpec("com.play.emu", "Play!",
            extensions = listOf("iso", "bin", "chd", "cso", "gz", "elf")),

        // ---- PSX -----------------------------------------------------------
        EmulatorSpec("com.github.stenzek.duckstation", "DuckStation",
            "com.github.stenzek.duckstation.EmulationActivity",
            RomLaunchContract.UriExtra("bootPath"),
            listOf("bin", "cue", "img", "iso", "chd", "pbp", "exe", "psexe", "m3u")),
        EmulatorSpec("com.epsxe.ePSXe", "ePSXe",
            extensions = listOf("bin", "cue", "img", "iso", "chd", "pbp", "exe", "psexe", "m3u")),
        EmulatorSpec("com.emulator.fpse", "FPse",
            extensions = listOf("bin", "cue", "img", "iso", "chd", "pbp", "m3u")),
        EmulatorSpec("com.emulator.fpse64", "FPse64",
            extensions = listOf("bin", "cue", "img", "iso", "chd", "pbp", "m3u")),

        // ---- Nintendo home consoles -----------------------------------------
        EmulatorSpec("org.dolphinemu.dolphinemu", "Dolphin",
            "org.dolphinemu.dolphinemu.ui.main.MainActivity",
            RomLaunchContract.UriExtra("AutoStartFile"),
            listOf("gcm", "iso", "wbfs", "ciso", "gcz", "wad", "dol", "elf", "rvz")),
        // MMJR builds have no documented ROM-launch extra: give the default
        // ContentUriView contract a chance rather than refusing outright, since
        // these builds are the most likely to have quietly gained a VIEW filter.
        EmulatorSpec("org.dolphinemu.dolphinemu.mmjr", "Dolphin MMJR",
            extensions = listOf("gcm", "iso", "wbfs", "ciso", "gcz", "wad", "dol", "elf", "rvz")),
        EmulatorSpec("org.mmjr.dolphinemu", "Dolphin MMJR (fork)",
            extensions = listOf("gcm", "iso", "wbfs", "ciso", "gcz", "wad", "dol", "elf", "rvz")),
        EmulatorSpec("info.cemu.cemu", "CEMU",
            extensions = listOf("wud", "wux", "iso", "rpx", "wua", "wad")),
        EmulatorSpec("net.rpcs3.rpcs3", "RPCS3", "net.rpcs3.rpcs3.MainActivity",
            extensions = listOf("pkg", "iso", "bin", "ps3")),
        EmulatorSpec("aenu.aps3e", "aPS3e",
            extensions = listOf("pkg", "iso", "bin", "ps3")),
        EmulatorSpec("aenu.aps3e.premium", "aPS3e Premium",
            extensions = listOf("pkg", "iso", "bin", "ps3")),

        // ---- N64 -------------------------------------------------------------
        // FZ kept its parent's paulscode namespace when it changed application
        // id, so one component serves the family. Without it the intent resolves
        // to the package's file browser rather than the game.
        EmulatorSpec("org.mupen64plusae.v3.fzurita.pro", "M64Plus FZ Pro",
            "paulscode.android.mupen64plusae.SplashActivity",
            extensions = listOf("n64", "v64", "z64", "zip")),
        EmulatorSpec("org.mupen64plusae.v3.fzurita", "M64Plus FZ",
            "paulscode.android.mupen64plusae.SplashActivity",
            extensions = listOf("n64", "v64", "z64", "zip")),
        EmulatorSpec("paulscode.android.mupen64plusae", "Mupen64Plus AE",
            "paulscode.android.mupen64plusae.SplashActivity",
            extensions = listOf("n64", "v64", "z64", "zip")),
        EmulatorSpec("org.mupen64plusae.v3.alpha", "Mupen64Plus AE Alpha",
            "paulscode.android.mupen64plusae.SplashActivity",
            extensions = listOf("n64", "v64", "z64", "zip")),

        // ---- DS / 3DS ---------------------------------------------------------
        EmulatorSpec("me.magnum.melonds", "melonDS",
            "me.magnum.melonds.ui.emulator.EmulatorActivity",
            extensions = listOf("nds", "bin")),
        EmulatorSpec("com.dsemu.drastic", "DraStic DS",
            extensions = listOf("nds", "bin", "zip")),
        EmulatorSpec("com.hydra.noods", "NooDS", "com.hydra.noods.MainActivity",
            extensions = listOf("nds", "gba")),
        EmulatorSpec("us.rewrite.nds", "Rewrite DS",
            extensions = listOf("nds")),
        EmulatorSpec("io.github.lime3ds.android", "Azahar (Play build)",
            "org.citra.citra_emu.activities.EmulationActivity",
            extensions = listOf("3ds", "cia", "cxi", "app", "cci")),
        EmulatorSpec("org.azahar_emu.azahar", "Azahar",
            extensions = listOf("3ds", "cia", "cxi", "app", "cci")),
        EmulatorSpec("org.citra.citra_emu", "Citra",
            extensions = listOf("3ds", "cia", "cxi", "app", "cci")),
        EmulatorSpec("org.citra.citra_emu.canary", "Citra Canary",
            extensions = listOf("3ds", "cia", "cxi", "app", "cci")),
        EmulatorSpec("org.citra.emu", "Citra MMJ",
            extensions = listOf("3ds", "cia", "cxi", "app", "cci")),
        EmulatorSpec("io.github.mandarine3ds.mandarine", "Mandarine",
            extensions = listOf("3ds", "cia", "cxi", "app", "cci")),
        EmulatorSpec("com.panda3ds.pandroid", "Panda3DS",
            extensions = listOf("3ds", "cia", "cxi", "app", "cci")),

        // ---- Switch ------------------------------------------------------------
        // Eden previously used Uri.fromFile, which throws FileUriExposedException
        // on API 24+. Moved to the standard content-URI VIEW.
        EmulatorSpec("dev.eden.eden_emulator", "Eden",
            "dev.eden.eden_emulator.ui.main.MainActivity",
            extensions = listOf("nsp", "xci", "nca", "nso")),
        EmulatorSpec("org.yuzu.yuzu_emu", "Yuzu",
            extensions = listOf("nsp", "xci", "nca", "nso")),
        EmulatorSpec("org.yuzu.yuzu_emu.ea", "Yuzu EA",
            extensions = listOf("nsp", "xci", "nca", "nso")),
        EmulatorSpec("org.ryujinx.android", "Ryujinx",
            extensions = listOf("nsp", "xci", "nca", "nso")),
        EmulatorSpec("org.citron.citron_emu", "Citron",
            extensions = listOf("nsp", "xci", "nca", "nso")),
        EmulatorSpec("org.sudachi.sudachi_emu", "Sudachi",
            extensions = listOf("nsp", "xci", "nca", "nso")),
        EmulatorSpec("org.stratoemu.strato", "Strato",
            extensions = listOf("nsp", "xci", "nca", "nso")),
        EmulatorSpec("skyline.emu", "Skyline",
            extensions = listOf("nsp", "xci", "nca", "nso")),

        // ---- Handhelds ---------------------------------------------------------
        EmulatorSpec("com.fastemulator.gba", "My Boy!",
            "com.fastemulator.gba.GPActivity", extensions = listOf("gba", "zip")),
        EmulatorSpec("com.fastemulator.gbafree", "My Boy! Free",
            "com.fastemulator.gba.GPActivity", extensions = listOf("gba", "zip")),
        EmulatorSpec("com.fastemulator.gbc", "My OldBoy!",
            "com.fastemulator.gbc.GPActivity", extensions = listOf("gb", "gbc", "zip")),
        EmulatorSpec("com.fastemulator.gbcfree", "My OldBoy! Free",
            "com.fastemulator.gbc.GPActivity", extensions = listOf("gb", "gbc", "zip")),
        EmulatorSpec("it.dbtecno.pizzaboygba", "Pizza Boy GBA",
            "it.dbtecno.pizzaboygba.MainActivity",
            extensions = listOf("gba", "gb", "gbc", "zip", "7z")),
        EmulatorSpec("it.dbtecno.pizzaboygbc", "Pizza Boy GBC",
            "it.dbtecno.pizzaboygbc.MainActivity",
            extensions = listOf("gb", "gbc", "zip", "7z")),
        EmulatorSpec("com.explusalpha.GbaEmu", "GBA.emu",
            "com.explusalpha.GbaEmu.MainActivity", extensions = listOf("gba", "zip", "7z")),
        EmulatorSpec("com.explusalpha.GbcEmu", "GBC.emu",
            "com.explusalpha.GbcEmu.MainActivity", extensions = listOf("gb", "gbc", "zip", "7z")),
        EmulatorSpec("com.explusalpha.LnkEmu", "Link.emu",
            "com.explusalpha.LnkEmu.MainActivity", extensions = listOf("gb", "gbc", "gba", "zip")),
        EmulatorSpec("com.sky.SkyEmu", "SkyEmu", "com.sky.SkyEmu.MainActivity",
            extensions = listOf("gba", "gb", "gbc", "nds")),

        // ---- explusalpha .emu family --------------------------------------------
        EmulatorSpec("com.explusalpha.A2600Emu", "A2600.emu",
            "com.explusalpha.A2600Emu.MainActivity",
            extensions = listOf("a26", "bin", "zip", "7z")),
        EmulatorSpec("com.explusalpha.C64Emu", "C64.emu",
            "com.explusalpha.C64Emu.MainActivity",
            extensions = listOf("d64", "t64", "prg", "tap", "crt", "zip", "7z")),
        EmulatorSpec("com.explusalpha.LynxEmu", "Lynx.emu",
            "com.explusalpha.LynxEmu.MainActivity",
            extensions = listOf("lnx", "lyx", "zip", "7z")),
        EmulatorSpec("com.explusalpha.MdEmu", "MD.emu",
            "com.explusalpha.MdEmu.MainActivity",
            extensions = listOf("md", "smd", "gen", "bin", "32x", "zip", "7z")),
        EmulatorSpec("com.explusalpha.MsxEmu", "MSX.emu",
            "com.explusalpha.MsxEmu.MainActivity",
            extensions = listOf("rom", "mx1", "mx2", "dsk", "cas", "zip", "7z")),
        EmulatorSpec("com.explusalpha.NeoEmu", "NEO.emu",
            "com.explusalpha.NeoEmu.MainActivity",
            extensions = listOf("neo", "zip", "7z")),
        EmulatorSpec("com.explusalpha.NesEmu", "NES.emu",
            "com.explusalpha.NesEmu.MainActivity",
            extensions = listOf("nes", "fds", "unf", "unif", "zip", "7z")),
        EmulatorSpec("com.explusalpha.NgpEmu", "NGP.emu",
            "com.explusalpha.NgpEmu.MainActivity",
            extensions = listOf("ngp", "ngc", "zip", "7z")),
        EmulatorSpec("com.explusalpha.PceEmu", "PCE.emu",
            "com.explusalpha.PceEmu.MainActivity",
            extensions = listOf("pce", "sgx", "zip", "7z")),
        EmulatorSpec("com.explusalpha.Saturn", "Saturn.emu",
            "com.explusalpha.Saturn.MainActivity",
            extensions = listOf("cue", "bin", "iso", "chd", "mds", "ccd", "img", "m3u")),
        EmulatorSpec("com.explusalpha.Snes9xPlus", "Snes9x+",
            "com.explusalpha.Snes9xPlus.MainActivity",
            extensions = listOf("sfc", "smc", "fig", "swc", "zip", "7z")),
        EmulatorSpec("com.explusalpha.SwanEmu", "Swan.emu",
            "com.explusalpha.SwanEmu.MainActivity",
            extensions = listOf("ws", "wsc", "zip", "7z")),

        // ---- FMS Ltd. classic-computer emulators --------------------------------
        EmulatorSpec("com.fms.colem", "ColEm",
            extensions = listOf("rom", "col", "zip")),
        EmulatorSpec("com.fms.colem.deluxe", "ColEm Deluxe",
            extensions = listOf("rom", "col", "zip")),
        EmulatorSpec("com.fms.fmsx", "fMSX",
            extensions = listOf("rom", "mx1", "mx2", "dsk", "cas", "zip")),
        EmulatorSpec("com.fms.fmsx.deluxe", "fMSX Deluxe",
            extensions = listOf("rom", "mx1", "mx2", "dsk", "cas", "zip")),
        EmulatorSpec("com.fms.ines.free", "iNES Free",
            extensions = listOf("nes", "fds", "unf", "zip")),
        EmulatorSpec("com.fms.mg", "Master Gear",
            extensions = listOf("sms", "gg", "zip")),
        EmulatorSpec("com.fms.speccy", "Speccy",
            extensions = listOf("tap", "tzx", "z80", "sna", "dsk", "trd", "scl", "zip")),
        EmulatorSpec("com.fms.speccy.deluxe", "Speccy Deluxe",
            extensions = listOf("tap", "tzx", "z80", "sna", "dsk", "trd", "scl", "zip")),

        // ---- John emulators (John GBA / GBC / NES family) -----------------------
        EmulatorSpec("com.johnemulators.johngba", "John GBA",
            extensions = listOf("gba", "zip", "7z")),
        EmulatorSpec("com.johnemulators.johngbac", "John GBAC",
            extensions = listOf("gba", "gb", "gbc", "zip", "7z")),
        EmulatorSpec("com.johnemulators.johngbalite", "John GBA Lite",
            extensions = listOf("gba", "zip", "7z")),
        EmulatorSpec("com.johnemulators.johngbc", "John GBC",
            extensions = listOf("gb", "gbc", "zip", "7z")),
        EmulatorSpec("com.johnemulators.johngbclite", "John GBC Lite",
            extensions = listOf("gb", "gbc", "zip", "7z")),
        EmulatorSpec("com.johnemulators.johnness", "John NESS",
            extensions = listOf("nes", "fds", "unf", "zip", "7z")),

        // ---- PC / Windows -------------------------------------------------------
        // Launched by shortcut, not by executable: a Windows program needs a Wine
        // prefix, driver and DLL overrides, and the .desktop file is what names
        // all of that. Set the game up in Winlator once, then it launches here.
        EmulatorSpec("com.winlator", "Winlator",
            "com.winlator.XServerDisplayActivity",
            RomLaunchContract.PathExtra("shortcut_path")),
        EmulatorSpec("com.loki.winlator", "Winlator Loki",
            "com.winlator.cmod.XServerDisplayActivity",
            RomLaunchContract.PathExtra("shortcut_path")),
        EmulatorSpec("com.winlator.cmod", "Winlator Cmod", "com.winlator.MainActivity",
            RomLaunchContract.PathExtra("shortcut_path")),
        EmulatorSpec("com.winlator.ludashi", "Winlator Ludashi", "com.winlator.MainActivity",
            RomLaunchContract.PathExtra("shortcut_path")),
        EmulatorSpec("com.winlator.glibc", "Winlator Glibc", "com.winlator.MainActivity",
            RomLaunchContract.PathExtra("shortcut_path")),
        EmulatorSpec("com.winlator.proot", "Winlator PRoot", "com.winlator.MainActivity",
            RomLaunchContract.PathExtra("shortcut_path")),
        EmulatorSpec("app.gamenative", "GameNative", "app.gamenative.MainActivity",
            RomLaunchContract.Undocumented(
                "GameNative launches by Steam app id; set the game up there first.")),
        // GameHub Lite ships under two application ids sharing one activity —
        // one looks like a rename, the other a rebuild. Both are registered so
        // whichever the user has installed resolves; the picker's dedupe
        // (package + command) still keeps the two GameHub Lite commands
        // (Steam id vs local id) distinct within a single system. Extensions
        // are listed for picker correctness — the launcher hands the app an id
        // read from the file's contents, not a file path.
        EmulatorSpec("gamehub.lite", "GameHub Lite",
            "com.xj.landscape.launcher.ui.gamedetail.GameDetailActivity",
            RomLaunchContract.Undocumented(
                "GameHub Lite launches by Steam app id or local game id; set the game up there first."),
            extensions = listOf("steam", "localgameid")),
        EmulatorSpec("com.xj.landscape.launcher", "GameHub Lite (Landscape)",
            "com.xj.landscape.launcher.ui.gamedetail.GameDetailActivity",
            RomLaunchContract.Undocumented(
                "GameHub Lite launches by Steam app id or local game id; set the game up there first."),
            extensions = listOf("steam", "localgameid")),

        // ---- Dreamcast ----------------------------------------------------------
        EmulatorSpec("org.flycast.Flycast", "Flycast",
            extensions = listOf("gdi", "cdi", "chd", "cue", "bin", "dat", "zip", "7z")),
        EmulatorSpec("com.flycast.emulator", "Flycast Emulator",
            extensions = listOf("gdi", "cdi", "chd", "cue", "bin", "dat", "zip", "7z")),
        EmulatorSpec("com.reicast.emulator", "Reicast",
            extensions = listOf("gdi", "cdi", "chd", "cue", "bin", "dat", "zip", "7z")),
        EmulatorSpec("io.recompiled.redream", "reDream",
            extensions = listOf("gdi", "cdi", "chd", "cue", "bin", "iso")),

        // ---- Saturn -------------------------------------------------------------
        EmulatorSpec("org.uoyabause.uranus", "Yaba Sanshiro",
            extensions = listOf("iso", "cue", "bin", "chd", "mds", "ccd", "img", "m3u")),

        // ---- Vita ---------------------------------------------------------------
        EmulatorSpec("org.vita3k.emulator", "Vita3K",
            extensions = listOf("vpk")),

        // ---- Arcade -------------------------------------------------------------
        EmulatorSpec("com.seleuco.mame4droid", "MAME4droid",
            extensions = listOf("zip", "7z", "chd")),
        EmulatorSpec("com.seleuco.mame4d2024", "MAME4droid 2024",
            extensions = listOf("zip", "7z", "chd")),

        // ---- Misc ---------------------------------------------------------------
        EmulatorSpec("org.scummvm.scummvm", "ScummVM",
            launchContract = RomLaunchContract.Undocumented(
                "ScummVM may need the game's folder adding in its own library first.")),
    )

    private val byPackage: Map<String, EmulatorSpec> = KNOWN.associateBy { it.packageName }

    /**
     * The spec for an installed package, allowing for builds this table predates.
     *
     * Emulators ship under ids that cannot be enumerated in advance: Dolphin's
     * nightlies are `org.dolphinemu.dolphinemu.debug`, a fork appends its own
     * name, a store build appends the store's. An exact match reports every one
     * of those as not installed. A suffixed id is the same project, accepts the
     * same intent, and runs the same systems.
     *
     * Longest matching base wins, and the separating dot is required so
     * `com.fastemulator.gbafree` is not mistaken for a variant of
     * `com.fastemulator.gba` — a different application listed in its own right.
     */
    fun resolve(packageName: String): EmulatorSpec? =
        byPackage[packageName] ?: KNOWN
            .filter { packageName.startsWith("${it.packageName}.") }
            .maxByOrNull { it.packageName.length }

    /** Distinguishes a nightly from the build the table named. */
    fun displayNameFor(packageName: String): String {
        val spec = resolve(packageName) ?: return packageName
        if (spec.packageName == packageName) return spec.displayName
        val suffix = packageName.removePrefix("${spec.packageName}.")
            .replace('.', ' ')
            .replaceFirstChar(Char::uppercaseChar)
        return "${spec.displayName} ($suffix)"
    }

    /**
     * Installed emulators that can open [extension], dedicated ones first.
     *
     * Front-ends last on purpose: RetroArch claims almost every extension but
     * needs a core downloaded and assigned before it runs anything, so someone
     * with both RetroArch and melonDS installed should get melonDS for a DS game.
     */
    fun candidatesForExtension(context: Context, extension: String): List<EmulatorSpec> {
        val ext = extension.lowercase()
        return KNOWN
            .filter { ext in it.extensions && isInstalled(context, it.packageName) }
            .sortedBy { it.isFrontEnd }
    }

    fun isInstalled(context: Context, packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
