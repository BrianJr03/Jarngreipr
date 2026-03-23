package jr.brian.home.esde.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.util.Xml
import androidx.core.content.FileProvider
import org.xmlpull.v1.XmlPullParser
import java.io.File
import androidx.core.net.toUri

data class EmulatorOption(
    val packageName: String,
    val displayName: String,
    val command: String? = null
)

object EsdeCommandLauncher {
    private const val TAG = "EsdeCommandLauncher"

    // Built-in ES-DE emulator rules: emulator name → list of "package/activity"
    private val BUILTIN_RULES: Map<String, List<String>> = mapOf(
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
            "org.citra_emu.citra/org.citra.citra_emu.ui.main.MainActivity"
        ),
        "AZAHARPLUS" to listOf(
            "io.github.lime3ds.android/org.citra.citra_emu.activities.EmulationActivity"
        ),
        "RPCS3" to listOf(
            "net.rpcs3.rpcs3/net.rpcs3.rpcs3.MainActivity"
        ),
        "AETHERSX2" to listOf(
            "xyz.aethersx2.android/xyz.aethersx2.android.EmulationActivity"
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
            "paulscode.android.mupen64plusae/paulscode.android.mupen64plusae.SplashActivity"
        ),
        "CEMU" to listOf(
            "info.cemu.cemu/info.cemu.cemu.MainActivity"
        ),
        "EDEN" to listOf(
            "dev.eden.eden_emulator/dev.eden.eden_emulator.ui.main.MainActivity"
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
        )
    )

    // package → Pair(display name, supported extensions)
    private val EMULATOR_EXTENSION_MAP: Map<String, Pair<String, List<String>>> = mapOf(
        "com.retroarch" to Pair(
            "RetroArch",
            listOf(
                "gb",
                "gbc",
                "gba",
                "nes",
                "sfc",
                "smc",
                "n64",
                "z64",
                "v64",
                "nds",
                "iso",
                "bin",
                "cue",
                "md",
                "smd",
                "gen",
                "sms",
                "gg",
                "32x",
                "pce",
                "ccd",
                "img",
                "mdf",
                "chd",
                "pbp",
                "cso"
            )
        ),
        "com.retroarch.aarch64" to Pair(
            "RetroArch 64",
            listOf(
                "gb",
                "gbc",
                "gba",
                "nes",
                "sfc",
                "smc",
                "n64",
                "z64",
                "v64",
                "nds",
                "iso",
                "bin",
                "cue",
                "md",
                "smd",
                "gen",
                "sms",
                "gg",
                "32x",
                "pce",
                "ccd",
                "img",
                "mdf",
                "chd",
                "pbp",
                "cso"
            )
        ),
        "com.retroarch.ra32" to Pair(
            "RetroArch 32",
            listOf(
                "gb",
                "gbc",
                "gba",
                "nes",
                "sfc",
                "smc",
                "n64",
                "z64",
                "v64",
                "nds",
                "iso",
                "bin",
                "cue",
                "md",
                "smd",
                "gen",
                "sms",
                "gg",
                "32x",
                "pce",
                "ccd",
                "img",
                "mdf",
                "chd",
                "pbp",
                "cso"
            )
        ),
        "org.ppsspp.ppsspp" to Pair("PPSSPP", listOf("iso", "cso", "pbp", "elf")),
        "org.ppsspp.ppssppgold" to Pair("PPSSPP Gold", listOf("iso", "cso", "pbp", "elf")),
        "com.emu.ppss22" to Pair("PPSS22", listOf("iso", "bin", "chd", "cso")),
        "me.magnum.melonds" to Pair("melonDS", listOf("nds", "bin")),
        "com.drastic.ds" to Pair("DraStic DS", listOf("nds", "bin", "zip")),
        "com.hydra.noods" to Pair("Noods", listOf("nds", "gba")),
        "org.dolphinemu.dolphinemu" to Pair(
            "Dolphin",
            listOf("gcm", "iso", "wbfs", "ciso", "gcz", "wad", "dol", "elf", "rvz")
        ),
        "com.github.stenzek.duckstation" to Pair(
            "DuckStation",
            listOf("bin", "cue", "img", "iso", "chd", "pbp", "exe", "psexe", "m3u")
        ),
        "xyz.aethersx2.android" to Pair(
            "AetherSX2",
            listOf("iso", "bin", "elf", "chd", "cso", "gz")
        ),
        "net.pcsx2.pcsx2" to Pair("PCSX2", listOf("iso", "bin", "elf", "chd", "cso", "gz")),
        "org.citra.citra_emu" to Pair("Citra", listOf("3ds", "cia", "cxi", "app", "cci")),
        "org.citra_emu.citra" to Pair("Citra (alt)", listOf("3ds", "cia", "cxi", "app", "cci")),
        "io.github.lime3ds.android" to Pair(
            "AzaharPlus",
            listOf("3ds", "cia", "cxi", "app", "cci")
        ),
        "org.yuzu.yuzu_emu" to Pair("Yuzu", listOf("nsp", "xci", "nca", "nso")),
        "org.mupen64plusae.v3.fzurita" to Pair("M64Plus FZ", listOf("n64", "v64", "z64", "zip")),
        "org.mupen64plusae.v3.fzurita.pro" to Pair(
            "M64Plus FZ Pro",
            listOf("n64", "v64", "z64", "zip")
        ),
        "info.cemu.cemu" to Pair("CEMU", listOf("wud", "wux", "iso", "rpx", "wua", "wad")),
        "org.flycast.Flycast" to Pair(
            "Flycast",
            listOf("gdi", "cdi", "chd", "cue", "bin", "dat", "zip", "7z")
        ),
        "com.fastemulator.gba" to Pair("My Boy!", listOf("gba", "zip")),
        "com.fastemulator.gbafree" to Pair("My Boy! Free", listOf("gba", "zip")),
        "com.fastemulator.gbc" to Pair("My OldBoy!", listOf("gb", "gbc", "zip")),
        "com.fastemulator.gbcfree" to Pair("My OldBoy! Free", listOf("gb", "gbc", "zip")),
        "it.dbtecno.pizzaboygba" to Pair("Pizza Boy GBA", listOf("gba", "gb", "gbc", "zip", "7z")),
        "it.dbtecno.pizzaboygbapro" to Pair(
            "Pizza Boy GBA Pro",
            listOf("gba", "gb", "gbc", "zip", "7z")
        ),
        "com.sky.SkyEmu" to Pair("SkyEmu", listOf("gba", "gb", "gbc", "nds")),
        "com.explusalpha.GbaEmu" to Pair("GBA.emu", listOf("gba", "zip", "7z")),
        "com.explusalpha.GbcEmu" to Pair("GBC.emu", listOf("gb", "gbc", "zip", "7z")),
        "com.explusalpha.LnkEmu" to Pair("Link.emu", listOf("gb", "gbc", "gba", "zip")),
        "net.rpcs3.rpcs3" to Pair("RPCS3", listOf("pkg", "iso", "bin", "ps3")),
        "dev.eden.eden_emulator" to Pair("Eden", listOf("nsp", "xci", "nca", "nso")),
    )

    fun parseSystemCommands(esSystemsFile: File, systemName: String): List<Pair<String, String>> {
        if (!esSystemsFile.exists()) return emptyList()
        val commands = mutableListOf<Pair<String, String>>()
        try {
            val parser = Xml.newPullParser()
            esSystemsFile.inputStream().use { input ->
                parser.setInput(input, "UTF-8")
                var inTargetSystem = false
                var depth = 0
                var systemDepth = -1
                var commandLabel = ""
                val textBuffer = StringBuilder()
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            depth++
                            textBuffer.clear()
                            when (parser.name) {
                                "system" -> systemDepth = depth
                                "command" -> if (inTargetSystem) {
                                    commandLabel = parser.getAttributeValue(null, "label") ?: ""
                                }
                            }
                        }

                        XmlPullParser.TEXT -> textBuffer.append(parser.text)
                        XmlPullParser.END_TAG -> {
                            when (parser.name) {
                                "name" -> {
                                    if (!inTargetSystem && depth == systemDepth + 1) {
                                        if (textBuffer.toString().trim()
                                                .equals(systemName, ignoreCase = true)
                                        ) {
                                            inTargetSystem = true
                                        }
                                    }
                                }

                                "command" -> if (inTargetSystem) {
                                    val cmd = textBuffer.toString().trim()
                                    if (cmd.isNotEmpty()) commands.add(commandLabel to cmd)
                                }

                                "system" -> if (inTargetSystem) return commands
                            }
                            depth--
                            textBuffer.clear()
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing es_systems.xml for system $systemName", e)
        }
        return commands
    }

    // Built-in command definitions for systems whose es_systems.xml lives inside the ES-DE APK
    // and is therefore not accessible to us. Used as a fallback when no custom file is found.
    private val BUILTIN_SYSTEM_COMMANDS: Map<String, List<Pair<String, String>>> = mapOf(
        "steam" to listOf(
            "GameNative" to
                "%EMULATOR_GAMENATIVE% %ACTION%=app.gamenative.LAUNCH_GAME %EXTRAINTEGER_app_id%=%INJECT%=%ROM%",
            "GameHub Lite (Steam)" to
                "%EMULATOR_GAMEHUB-LITE% %ACTION%=gamehub.lite.LAUNCH_GAME %EXTRABOOL_autoStartGame%=true %EXTRA_steamAppId%=%INJECT%=%ROM%",
            "GameHub Lite (Local)" to
                "%EMULATOR_GAMEHUB-LITE% %ACTION%=gamehub.lite.LAUNCH_GAME %EXTRABOOL_autoStartGame%=true %EXTRA_localGameId%=%INJECT%=%ROM%"
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

    fun getCompatibleEmulatorsFromSystem(
        context: Context,
        systemName: String,
        esSystemsFile: File,
        customRules: Map<String, List<String>> = emptyMap()
    ): List<EmulatorOption> {
        val parsed = parseSystemCommands(esSystemsFile, systemName)
        // Fall back to built-in commands when the custom es_systems.xml has no entry for this system
        val builtin = BUILTIN_SYSTEM_COMMANDS[systemName] ?: emptyList()
        val commands = if (parsed.isNotEmpty()) {
            // Merge: custom overrides built-in; add built-in labels not already present
            val customLabels = parsed.map { it.first }.toHashSet()
            parsed + builtin.filter { it.first !in customLabels }
        } else {
            builtin
        }
        if (commands.isEmpty()) return emptyList()
        val emulatorRegex = Regex("""%EMULATOR_([^%]+)%""")
        val result = mutableListOf<EmulatorOption>()
        val seenPackages = mutableSetOf<String>()
        for ((label, command) in commands) {
            val emulatorName = emulatorRegex.find(command)?.groupValues?.get(1) ?: continue
            val allEntries =
                (customRules[emulatorName] ?: emptyList()) + (BUILTIN_RULES[emulatorName]
                    ?: emptyList())
            val installedEntry = allEntries.firstOrNull { entry ->
                val pkg = entry.substringBefore("/")
                try {
                    context.packageManager.getPackageInfo(pkg, 0); true
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
            } ?: continue
            val packageName = installedEntry.substringBefore("/")
            if (seenPackages.add(packageName)) {
                result.add(EmulatorOption(packageName, label, command))
            }
        }
        return result
    }

    fun getCompatibleEmulators(context: Context, romExtension: String): List<EmulatorOption> {
        val ext = romExtension.lowercase()
        val candidates = EMULATOR_EXTENSION_MAP.filter { (_, pair) -> pair.second.contains(ext) }
        Log.d(TAG, "getCompatibleEmulators ext=$ext candidates=${candidates.keys}")
        return candidates.mapNotNull { (pkg, pair) ->
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                Log.d(TAG, "  FOUND installed: $pkg")
                EmulatorOption(pkg, pair.first)
            } catch (_: PackageManager.NameNotFoundException) {
                Log.d(TAG, "  NOT installed: $pkg")
                null
            }
        }
    }

    fun parseCustomRules(findRulesFile: File): Map<String, List<String>> {
        if (!findRulesFile.exists()) return emptyMap()
        val rules = mutableMapOf<String, MutableList<String>>()
        try {
            val parser = Xml.newPullParser()
            findRulesFile.inputStream().use { input ->
                parser.setInput(input, "UTF-8")
                var emulatorName: String? = null
                var eventType = parser.eventType
                val textBuffer = StringBuilder()
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            textBuffer.clear()
                            if (parser.name == "emulator") {
                                emulatorName = parser.getAttributeValue(null, "name")
                                if (emulatorName != null) rules[emulatorName] = mutableListOf()
                            }
                        }

                        XmlPullParser.TEXT -> textBuffer.append(parser.text)
                        XmlPullParser.END_TAG -> {
                            if (parser.name == "entry" && emulatorName != null) {
                                val entry = textBuffer.toString().trim()
                                if (entry.isNotEmpty()) rules[emulatorName]?.add(entry)
                            }
                            textBuffer.clear()
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing es_find_rules.xml", e)
        }
        return rules
    }

    fun resolvePackage(
        launchCommand: String?,
        context: Context,
        customRules: Map<String, List<String>> = emptyMap()
    ): String? {
        launchCommand ?: return null
        val emulatorName =
            Regex("""%EMULATOR_([^%]+)%""").find(launchCommand)?.groupValues?.get(1) ?: return null
        val allEntries = (customRules[emulatorName] ?: emptyList()) + (BUILTIN_RULES[emulatorName]
            ?: emptyList())
        return allEntries.firstOrNull { entry ->
            val pkg = entry.substringBefore("/")
            try {
                context.packageManager.getPackageInfo(pkg, 0); true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }?.substringBefore("/")
    }

    fun buildIntent(
        launchCommand: String,
        romAbsPath: String,
        context: Context,
        customRules: Map<String, List<String>> = emptyMap()
    ): Intent? {
        val emulatorRegex = Regex("""%EMULATOR_([^%]+)%""")
        val emulatorMatch = emulatorRegex.find(launchCommand) ?: return null
        val emulatorName = emulatorMatch.groupValues[1]

        val allEntries = (customRules[emulatorName] ?: emptyList()) + (BUILTIN_RULES[emulatorName]
            ?: emptyList())
        if (allEntries.isEmpty()) {
            Log.w(TAG, "No find rules for emulator: $emulatorName")
            return null
        }

        val pkgActivity = allEntries.firstOrNull { entry ->
            val pkg = entry.substringBefore("/")
            try {
                context.packageManager.getPackageInfo(pkg, 0); true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        } ?: return null

        val packageName = pkgActivity.substringBefore("/")
        val activityRaw = pkgActivity.substringAfter("/")
        val activityName =
            if (activityRaw.startsWith(".")) "$packageName$activityRaw" else activityRaw

        val intent = Intent()
        intent.component = ComponentName(packageName, activityName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val remaining = launchCommand.removeRange(emulatorMatch.range).trim()

        if (remaining.contains("%ACTIVITY_CLEAR_TASK%")) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (remaining.contains("%ACTIVITY_CLEAR_TOP%")) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val actionMatch = Regex("""%ACTION%=(\S+)""").find(remaining)
        if (actionMatch != null) intent.action = actionMatch.groupValues[1]

        val extraRegex = Regex("""%EXTRA_([^%]+)%=([^\s]+)""")
        for (match in extraRegex.findAll(remaining)) {
            val key = match.groupValues[1]
            val value = resolveToken(match.groupValues[2], romAbsPath, packageName)
            intent.putExtra(key, value)
        }

        val extraIntegerRegex = Regex("""%EXTRAINTEGER_([^%]+)%=([^\s]+)""")
        for (match in extraIntegerRegex.findAll(remaining)) {
            val key = match.groupValues[1]
            val value = resolveToken(match.groupValues[2], romAbsPath, packageName)
            intent.putExtra(key, value.toIntOrNull() ?: 0)
        }

        val extraBoolRegex = Regex("""%EXTRABOOL_([^%]+)%=([^\s]+)""")
        for (match in extraBoolRegex.findAll(remaining)) {
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            intent.putExtra(key, value.equals("true", ignoreCase = true))
        }

        val dataMatch = Regex("""%DATA%=([^\s]+)""").find(remaining)
        if (dataMatch != null) {
            val resolvedUri =
                resolveDataUri(dataMatch.groupValues[1], romAbsPath, packageName, context)
            if (resolvedUri != null) {
                intent.data = resolvedUri
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        return intent
    }

    /**
     * Builds a ROM-launch intent using per-emulator intent contracts sourced from dual-play-launcher.
     * This is the primary launch path since launchCommand is null for all default ES-DE systems
     * (their commands live inside the ES-DE APK, not in any user-accessible file).
     */
    fun buildRomIntentFromPackage(
        packageName: String,
        romAbsPath: String,
        context: Context,
        corePath: String? = null
    ): Intent {
        val romFile = File(romAbsPath)
        val contentUri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", romFile)
        } catch (_: Exception) {
            android.net.Uri.fromFile(romFile)
        }
        return buildRomIntentFromPackage(packageName, romAbsPath, contentUri, context, corePath)
    }

    /**
     * Builds a ROM-launch intent with a caller-supplied [contentUri].
     * Use this overload when the URI has already been resolved (e.g. SAF for SD card ROMs).
     */
    fun buildRomIntentFromPackage(
        packageName: String,
        romAbsPath: String,
        contentUri: android.net.Uri,
        context: Context,
        corePath: String? = null
    ): Intent {
        val romFile = File(romAbsPath)
        val romDirectory = romFile.parent ?: ""
        val romName = romFile.nameWithoutExtension

        fun grantUri(pkg: String) {
            // SAF URIs from ExternalStorageDocumentsProvider don't need manual grants.
            if (contentUri.authority == "com.android.externalstorage.documents") return
            try {
                context.grantUriPermission(pkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
        }

        // Resolve known activity from BUILTIN_RULES for the generic fallback
        val knownEntry = BUILTIN_RULES.values.flatten()
            .firstOrNull { it.substringBefore("/") == packageName }
        val knownActivity = knownEntry?.let { entry ->
            val raw = entry.substringAfter("/")
            if (raw.startsWith(".")) "$packageName$raw" else raw
        }

        return when {
            packageName == "me.magnum.melonds" -> {
                // melonDS: ACTION_VIEW with content URI as data
                grantUri(packageName)
                Intent(Intent.ACTION_VIEW).apply {
                    setClassName(packageName, "me.magnum.melonds.ui.emulator.EmulatorActivity")
                    data = contentUri
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            packageName.startsWith("com.retroarch") -> {
                Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(
                        packageName,
                        "com.retroarch.browser.retroactivity.RetroActivityFuture"
                    )
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    putExtra("ROM", romAbsPath)
                    corePath?.let { putExtra("LIBRETRO", it) }
                }
            }

            packageName == "org.ppsspp.ppsspp" || packageName == "org.ppsspp.ppssppgold" -> {
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", romFile)
                } catch (e: Exception) {
                    Log.e(TAG, "FileProvider failed, falling back: ${e.message}")
                    android.net.Uri.fromFile(romFile)
                }
                Intent(Intent.ACTION_VIEW).apply {
                    component = ComponentName(packageName, "org.ppsspp.ppsspp.PpssppActivity")
                    data = uri
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            packageName == "com.emu.ppss22" -> {
                Intent().apply {
                    setClassName(packageName, "$packageName.MainActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra("ROM_PATH", romAbsPath)
                }
            }

            packageName == "io.github.lime3ds.android" -> {
                grantUri(packageName)
                Intent(Intent.ACTION_VIEW).apply {
                    setClassName(packageName, "org.citra.citra_emu.activities.EmulationActivity")
                    setDataAndType(contentUri, "*/*")
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }

            packageName == "org.mupen64plusae.v3.fzurita" ||
                    packageName == "paulscode.android.mupen64plusae" -> {
                grantUri(packageName)
                Intent(Intent.ACTION_VIEW).apply {
                    setClassName(packageName, "$packageName.SplashActivity")
                    data = contentUri
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            packageName == "info.cemu.cemu" -> {
                grantUri(packageName)
                Intent(Intent.ACTION_VIEW).apply {
                    data = contentUri
                    setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            packageName == "xyz.aethersx2.android" -> {
                Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(packageName, "$packageName.EmulationActivity")
                    putExtra("bootPath", romFile.absolutePath)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            packageName == "dev.eden.eden_emulator" -> {
                Intent(Intent.ACTION_VIEW).apply {
                    setClassName(packageName, "dev.eden.eden_emulator.ui.main.MainActivity")
                    data = android.net.Uri.fromFile(romFile)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            packageName == "org.dolphinemu.dolphinemu" -> {
                grantUri(packageName)
                Intent(Intent.ACTION_MAIN).apply {
                    setClassName(packageName, "$packageName.ui.main.MainActivity")
                    putExtra("AutoStartFile", contentUri.toString())
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }

            packageName == "com.github.stenzek.duckstation" -> {
                grantUri(packageName)
                Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(
                        packageName,
                        "com.github.stenzek.duckstation.EmulationActivity"
                    )
                    putExtra("bootPath", contentUri.toString())
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }

            else -> {
                grantUri(packageName)
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, romMimeType(romFile.extension))
                    if (knownActivity != null) component = ComponentName(packageName, knownActivity)
                    else setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra("rom_path", romAbsPath)
                    putExtra("rom_directory", romDirectory)
                    putExtra("rom_name", romName)
                    putExtra("GAMEPATH", romAbsPath)
                    putExtra("ROMPATH", romDirectory)
                    putExtra("SDCARD", romDirectory)
                    putExtra("PATH", romAbsPath)
                }
            }
        }
    }

    private fun romMimeType(extension: String): String = when (extension.lowercase()) {
        "gba" -> "application/x-gba-rom"
        "gb", "gbc" -> "application/x-gameboy-rom"
        "nds", "dsi" -> "application/x-nintendo-ds-rom"
        "nes" -> "application/x-nes-rom"
        "sfc", "smc" -> "application/x-snes-rom"
        "n64", "z64", "v64" -> "application/x-n64-rom"
        "wud", "wux", "wua" -> "application/x-wiiu-rom"
        "rpx" -> "application/x-wiiu-rpx"
        "iso", "cso", "chd" -> "application/x-iso9660-image"
        "cue", "bin" -> "application/x-cue"
        else -> "application/octet-stream"
    }

    /**
     * Scans the cores directory of every installed RetroArch variant and returns
     * a list of (displayName, absolutePath) pairs for each .so found.
     * displayName is derived from the filename, e.g. "mgba_libretro_android.so" → "mGBA".
     */
    fun getInstalledCores(context: Context): List<Pair<String, String>> {
        val pkg = listOf("com.retroarch.aarch64", "com.retroarch", "com.retroarch.ra32")
            .firstOrNull { p ->
                try { context.packageManager.getPackageInfo(p, 0); true }
                catch (_: Exception) { false }
            } ?: return emptyList()

        // RetroArch always reverts its core directory to its private internal storage
        // (/data/data/{pkg}/cores/) which other apps cannot list. However, RetroArch CAN
        // load cores from that path when given it via the LIBRETRO intent extra. We construct
        // the expected path for each known core and let RetroArch resolve it at launch time.
        val internalCoresDir = "/data/data/$pkg/cores"
        return KNOWN_CORE_STEMS.map { (displayName, stem) ->
            displayName to "$internalCoresDir/${stem}_libretro_android.so"
        }
    }

    private val KNOWN_CORE_STEMS = listOf(
        "mGBA" to "mgba",
        "VBA Next" to "vba_next",
        "gpSP" to "gpsp",
        "Snes9x" to "snes9x",
        "Snes9x 2010" to "snes9x2010",
        "Nestopia" to "nestopia",
        "FCEUmm" to "fceumm",
        "Genesis Plus GX" to "genesis_plus_gx",
        "PicoDrive" to "picodrive",
        "Mupen64Plus-Next" to "mupen64plus_next",
        "ParaLLEl N64" to "parallel_n64",
        "PCSX ReARMed" to "pcsx_rearmed",
        "Beetle PSX HW" to "mednafen_psx_hw",
        "DeSmuME" to "desmume",
        "melonDS" to "melonds",
        "Citra" to "citra",
        "Dolphin" to "dolphin",
        "PPSSPP" to "ppsspp",
        "Flycast" to "flycast",
        "MAME" to "mame",
        "FinalBurn Neo" to "fbneo",
    )

    private fun coreDisplayName(stem: String): String {
        // "mgba_libretro_android" → "mgba" → pretty-print
        val stripped = stem.removeSuffix("_libretro_android").removeSuffix("_libretro")
        val knownNames = mapOf(
            "mgba" to "mGBA",
            "vba_next" to "VBA Next",
            "gpsp" to "gpSP",
            "snes9x" to "Snes9x",
            "snes9x2010" to "Snes9x 2010",
            "nestopia" to "Nestopia",
            "fceumm" to "FCEUmm",
            "genesis_plus_gx" to "Genesis Plus GX",
            "picodrive" to "PicoDrive",
            "mupen64plus_next" to "Mupen64Plus-Next",
            "parallel_n64" to "ParaLLEl N64",
            "pcsx_rearmed" to "PCSX ReARMed",
            "mednafen_psx_hw" to "Beetle PSX HW",
            "beetle_psx_hw" to "Beetle PSX HW",
            "desmume" to "DeSmuME",
            "melonds" to "melonDS",
            "citra" to "Citra",
            "dolphin" to "Dolphin",
            "ppsspp" to "PPSSPP",
            "flycast" to "Flycast",
            "mame" to "MAME",
            "fbneo" to "FinalBurn Neo",
            "opera" to "Opera (3DO)",
            "mednafen_saturn" to "Beetle Saturn",
            "yabause" to "Yabause",
            "bluemsx" to "BlueMSX",
            "fmsx" to "fMSX",
            "mesen" to "Mesen",
            "bsnes" to "bsnes",
            "bsnes_mercury_accuracy" to "bsnes-mercury Accuracy"
        )
        return knownNames[stripped]
            ?: stripped.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
    }

    private fun resolveToken(token: String, romAbsPath: String, packageName: String): String {
        // %INJECT%=<path_token> — read the file content instead of using the path
        if (token.startsWith("%INJECT%=")) {
            val pathToken = token.removePrefix("%INJECT%=")
            val filePath = resolveToken(pathToken, romAbsPath, packageName)
            return try { File(filePath).readText().trim() } catch (_: Exception) { filePath }
        }
        return token
            .replace("%ROM%", romAbsPath)
            .replace("%ANDROIDPACKAGE%", packageName)
    }

    private fun resolveDataUri(
        token: String,
        romAbsPath: String,
        packageName: String,
        context: Context
    ): android.net.Uri? {
        return when {
            token.contains("%ROMPROVIDER%") -> {
                try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        File(romAbsPath)
                    )
                } catch (_: Exception) {
                    android.net.Uri.fromFile(File(romAbsPath))
                }
            }

            token.contains("%ROMSAF%") -> {
                // %ROMSAF% hints at a SAF tree URI, but we can't reliably manufacture one
                // without knowing what tree root the target emulator was granted. Fall back to
                // FileProvider + FLAG_GRANT_READ_URI_PERMISSION (caller must add that flag).
                try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        File(romAbsPath)
                    )
                } catch (_: Exception) {
                    android.net.Uri.fromFile(File(romAbsPath))
                }
            }

            token.contains("%ROM%") -> {
                android.net.Uri.fromFile(File(resolveToken(token, romAbsPath, packageName)))
            }

            else -> {
                resolveToken(token, romAbsPath, packageName).toUri()
            }
        }
    }
}
