package jr.brian.home.esde.util

/**
 * The ES-DE emulator rules and system commands Jarngreipr ships with, for
 * emulators and systems whose definitions live inside the ES-DE APK and so
 * cannot be read from disk. [EsdeCommandLauncher] merges these with the
 * user's custom `es_find_rules.xml` / `es_systems.xml`.
 */
internal object EsdeBuiltinCommands {

    // Built-in ES-DE emulator rules: emulator name → list of "package/activity"
    val RULES: Map<String, List<String>> = mapOf(
        "RETROARCH" to listOf(
            "com.retroarch.aarch64/com.retroarch.browser.retroactivity.RetroActivityFuture",
            "com.retroarch/com.retroarch.browser.retroactivity.RetroActivityFuture"
        ),
        "DOLPHIN" to listOf(
            "org.dolphinemu.dolphinemu/org.dolphinemu.dolphinemu.ui.main.MainActivity"
        ),
        "PPSSPP" to listOf(
            "org.ppsspp.ppsspp/org.ppsspp.ppsspp.PpssppActivity",
            "org.ppsspp.ppssppgold/org.ppsspp.ppsspp.PpssppActivity"
        ),
        "MELONDS" to listOf(
            "me.magnum.melonds/me.magnum.melonds.ui.emulator.EmulatorActivity"
        ),
        "CITRA" to listOf(
            "org.citra.citra_emu/org.citra.citra_emu.ui.main.MainActivity",
            "org.citra.citra_emu.canary/org.citra.citra_emu.ui.main.MainActivity",
            "org.citra_emu.citra/org.citra.citra_emu.ui.main.MainActivity"
        ),
        "AZAHARPLUS" to listOf(
            "io.github.lime3ds.android/org.citra.citra_emu.activities.EmulationActivity"
        ),
        "RPCS3" to listOf(
            "net.rpcs3.rpcs3/net.rpcs3.rpcs3.MainActivity"
        ),
        "AETHERSX2" to listOf(
            "xyz.aethersx2.android/xyz.aethersx2.android.EmulationActivity",
            // Fall back to the NetherSX2 rebuilds if only they are installed;
            // a user picking the "AetherSX2" command shouldn't hit
            // "no rule installed" when a compatible fork is present.
            "xyz.aethersx2.tturnip/xyz.aethersx2.android.EmulationActivity",
            "xyz.aethersx2.cturnip/xyz.aethersx2.android.EmulationActivity"
        ),
        "NETHERSX2-TURNIP" to listOf(
            "xyz.aethersx2.tturnip/xyz.aethersx2.android.EmulationActivity"
        ),
        "NETHERSX2-TURNIP-CLASSIC" to listOf(
            "xyz.aethersx2.cturnip/xyz.aethersx2.android.EmulationActivity"
        ),
        "ARMSX2R" to listOf(
            "com.armsx2/com.armsx2.Main"
        ),
        "EMUCOREX" to listOf(
            "com.sbro.emucorex/com.sbro.emucorex.MainActivity"
        ),
        "DUCKSTATION" to listOf(
            "com.github.stenzek.duckstation/com.github.stenzek.duckstation.EmulationActivity"
        ),
        "PCSX2" to listOf(
            "net.pcsx2.pcsx2/net.pcsx2.pcsx2.NativeActivity"
        ),
        "GBA-EMU" to listOf(
            "com.explusalpha.GbaEmu/com.explusalpha.GbaEmu.MainActivity"
        ),
        "GBC-EMU" to listOf(
            "com.explusalpha.GbcEmu/com.explusalpha.GbcEmu.MainActivity"
        ),
        "MY-BOY" to listOf(
            "com.fastemulator.gba/com.fastemulator.gba.GPActivity",
            "com.fastemulator.gbafree/com.fastemulator.gba.GPActivity"
        ),
        "MY-OLDBOY" to listOf(
            "com.fastemulator.gbc/com.fastemulator.gbc.GPActivity",
            "com.fastemulator.gbcfree/com.fastemulator.gbc.GPActivity"
        ),
        "NOODS" to listOf(
            "com.hydra.noods/com.hydra.noods.MainActivity"
        ),
        "SKYEMU" to listOf(
            "com.sky.SkyEmu/com.sky.SkyEmu.MainActivity"
        ),
        "PIZZA-BOY-GBA" to listOf(
            "it.dbtecno.pizzaboygba/it.dbtecno.pizzaboygba.MainActivity",
            "air.com.pizzaboy.gba/air.com.pizzaboy.gba.AppEntry"
        ),
        "PIZZA-BOY-GBC" to listOf(
            "it.dbtecno.pizzaboygbc/it.dbtecno.pizzaboygbc.MainActivity"
        ),
        "LINKBOY" to listOf(
            "com.explusalpha.LnkEmu/com.explusalpha.LnkEmu.MainActivity"
        ),
        "MUPEN64PLUS" to listOf(
            "org.mupen64plusae.v3.fzurita/org.mupen64plusae.v3.fzurita.SplashActivity",
            "org.mupen64plusae.v3.alpha/paulscode.android.mupen64plusae.SplashActivity",
            "paulscode.android.mupen64plusae/paulscode.android.mupen64plusae.SplashActivity"
        ),
        "CEMU" to listOf(
            "info.cemu.cemu/info.cemu.cemu.MainActivity"
        ),
        "EDEN" to listOf(
            "dev.eden.eden_emulator/dev.eden.eden_emulator.ui.main.MainActivity"
        ),
        "BANANA-NATIVE" to listOf(
            "dev.bananative.android.plus/dev.bananative.android.plus.Launch",
            "dev.bananative.android/dev.bananative.android.Launch",
            "dev.nxnative.android/dev.nxnative.android.Launch"
        ),
        "GAMENATIVE" to listOf(
            "app.gamenative/app.gamenative.MainActivity"
        ),
        "GAMEHUB-LITE" to listOf(
            "gamehub.lite/com.xj.landscape.launcher.ui.gamedetail.GameDetailActivity",
            "com.xj.landscape.launcher/com.xj.landscape.launcher.ui.gamedetail.GameDetailActivity"
        ),
        "WINLATOR-CMOD" to listOf(
            "com.winlator.cmod/com.winlator.MainActivity"
        ),
        "WINLATOR-LUDASHI" to listOf(
            "com.winlator.ludashi/com.winlator.MainActivity"
        ),
        "WINLATOR-GLIBC" to listOf(
            "com.winlator.glibc/com.winlator.MainActivity"
        ),
        "WINLATOR-PROOT" to listOf(
            "com.winlator.proot/com.winlator.MainActivity"
        ),
        "X1-BOX" to listOf(
            "com.izzy2lost.x1box/com.izzy2lost.x1box.LauncherActivity"
        ),
        "HAKUX" to listOf(
            "com.rfandango.haku_x/com.rfandango.haku_x.LauncherActivity"
        ),
        "AX360E" to listOf(
            "aenu.ax360e/aenu.ax360e.EmulatorActivity",
            "aenu.ax360e.free/aenu.ax360e.EmulatorActivity"
        ),
        "X360MOBILE" to listOf(
            "emu.x360mobile.com/emu.x360mobile.com.MainActivity",
            "emu.x360.mobile/emu.x360.mobile.X360MobileGameLaunchActivity"
        ),
        "XENDROID" to listOf(
            "xendroid.compose/xendroid.compose.EmulatorHostActivity"
        )
    )

    // Built-in command definitions for systems whose es_systems.xml lives inside the ES-DE APK
    // and is therefore not accessible to us. Used as a fallback when no custom file is found.
    val SYSTEM_COMMANDS: Map<String, List<Pair<String, String>>> = mapOf(
        "psp" to listOf(
            "PPSSPP" to
                "%EMULATOR_PPSSPP% %ACTIVITY_CLEAR_TASK% %ACTIVITY_CLEAR_TOP% %ACTION%=android.intent.action.VIEW %DATA%=%ROMSAF%"
        ),
        "ps2" to listOf(
            "AetherSX2" to
                "%EMULATOR_AETHERSX2% %ACTIVITY_CLEAR_TASK% %ACTIVITY_CLEAR_TOP% %ACTION%=android.intent.action.MAIN %EXTRA_bootPath%=%ROMSAF%",
            "NetherSX2-Turnip" to
                "%EMULATOR_NETHERSX2-TURNIP% %ACTIVITY_CLEAR_TASK% %ACTIVITY_CLEAR_TOP% %ACTION%=android.intent.action.MAIN %EXTRA_bootPath%=%ROMSAF%",
            "NetherSX2-Turnip Classic" to
                "%EMULATOR_NETHERSX2-TURNIP-CLASSIC% %ACTIVITY_CLEAR_TASK% %ACTIVITY_CLEAR_TOP% %ACTION%=android.intent.action.MAIN %EXTRA_bootPath%=%ROMSAF%",
            "PCSX2" to
                "%EMULATOR_PCSX2% %ACTION%=android.intent.action.VIEW %DATA%=%ROMSAF%",
            "ARMSX2 Refresh" to
                "%EMULATOR_ARMSX2R% %ACTION%=android.intent.action.VIEW %DATA%=%ROMSAF%",
            "EmuCoreX" to
                "%EMULATOR_EMUCOREX% %ACTION%=android.intent.action.VIEW %DATA%=%ROMSAF%"
        ),
        "switch2" to listOf(
            "Banana Native" to
                "%EMULATOR_BANANA-NATIVE% %EXTRA_game%=%ROM%",
            "Banana Native (URI)" to
                "%EMULATOR_BANANA-NATIVE% %ACTION%=android.intent.action.VIEW %DATA%=%ROMSAF%",
            "Banana Native (Title ID)" to
                "%EMULATOR_BANANA-NATIVE% %EXTRA_title_id%=%INJECT%=%ROM%"
        ),
        "steam" to listOf(
            "GameNative" to
                "%EMULATOR_GAMENATIVE% %ACTION%=app.gamenative.LAUNCH_GAME %EXTRAINTEGER_app_id%=%INJECT%=%ROM%",
            "GameHub Lite (Steam)" to
                "%EMULATOR_GAMEHUB-LITE% %ACTION%=gamehub.lite.LAUNCH_GAME %EXTRABOOL_autoStartGame%=true %EXTRA_steamAppId%=%INJECT%=%ROM%",
            "GameHub Lite (Local)" to
                "%EMULATOR_GAMEHUB-LITE% %ACTION%=gamehub.lite.LAUNCH_GAME %EXTRABOOL_autoStartGame%=true %EXTRA_localGameId%=%INJECT%=%ROM%"
        ),
        "xbox" to listOf(
            "X1 BOX" to
                "%EMULATOR_X1-BOX% %ACTION%=android.intent.action.VIEW %DATA%=%ROMSAF%",
            "hakuX" to
                "%EMULATOR_HAKUX% %ACTION%=android.intent.action.VIEW %DATA%=%ROMSAF%"
        ),
        "xbox360" to listOf(
            "aX360e" to
                "%EMULATOR_AX360E% %ACTION%=aenu.intent.action.AX360E %EXTRA_game_uri%=%ROMSAF%",
            "X360 Mobile" to
                "%EMULATOR_X360MOBILE% %DATA%=%ROMSAF%",
            "XenDroid" to
                "%EMULATOR_XENDROID% %ACTION%=xendroid.intent.action.xendroid %DATA%=%ROMSAF%"
        ),
        "windows" to listOf(
            "Winlator Cmod" to
                "%EMULATOR_WINLATOR-CMOD% %ACTIVITY_CLEAR_TASK% %ACTIVITY_CLEAR_TOP% %EXTRA_shortcut_path%=%ROM%",
            "Winlator Ludashi" to
                "%EMULATOR_WINLATOR-LUDASHI% %ACTIVITY_CLEAR_TASK% %ACTIVITY_CLEAR_TOP% %EXTRA_shortcut_path%=%ROM%",
            "Winlator Glibc" to
                "%EMULATOR_WINLATOR-GLIBC% %ACTIVITY_CLEAR_TASK% %ACTIVITY_CLEAR_TOP% %EXTRA_shortcut_path%=%ROM%",
            "Winlator PRoot" to
                "%EMULATOR_WINLATOR-PROOT% %ACTIVITY_CLEAR_TASK% %ACTIVITY_CLEAR_TOP% %EXTRA_shortcut_path%=%ROM%",
            "GameNative" to
                "%EMULATOR_GAMENATIVE% %ACTION%=app.gamenative.LAUNCH_GAME %EXTRAINTEGER_app_id%=%INJECT%=%ROM%",
            "GameHub Lite (Steam)" to
                "%EMULATOR_GAMEHUB-LITE% %ACTION%=gamehub.lite.LAUNCH_GAME %EXTRABOOL_autoStartGame%=true %EXTRA_steamAppId%=%INJECT%=%ROM%",
            "GameHub Lite (Local)" to
                "%EMULATOR_GAMEHUB-LITE% %ACTION%=gamehub.lite.LAUNCH_GAME %EXTRABOOL_autoStartGame%=true %EXTRA_localGameId%=%INJECT%=%ROM%"
        )
    )
}
