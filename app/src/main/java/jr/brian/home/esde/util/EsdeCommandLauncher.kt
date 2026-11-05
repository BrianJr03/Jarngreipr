package jr.brian.home.esde.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.util.Xml
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import org.xmlpull.v1.XmlPullParser
import java.io.File

data class EmulatorOption(
    val packageName: String,
    val displayName: String,
    val command: String? = null,
    /**
     * Extension the command's file-content extra is namespaced against, or null
     * when the command is extension-agnostic. Distinct commands for one package
     * can require different extensions (Steam app id vs GameHub-internal id);
     * the picker uses this to mark, but not filter, options whose extension
     * does not match the ROM.
     */
    val requiredExtension: String? = null,
)

object EsdeCommandLauncher {
    private const val TAG = "EsdeCommandLauncher"

    /**
     * ROMs have no registered MIME types, and emulators match on a wildcard
     * rather than on any specific type. The previous per-extension types
     * (`application/x-gba-rom` and friends) matched no intent filter on any
     * emulator, so resolveActivity returned null and the launch fell back to
     * opening the emulator's own file browser.
     */
    private const val MIME_ANY = "*/*"

    /** RetroArch's public activity reads the ROM from this extra. */
    private const val RETROARCH_ROM_EXTRA = "ROM"
    private const val RETROARCH_CORE_EXTRA = "LIBRETRO"

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

    /**
     * Union of extensions across every emulator that [EsdeBuiltinCommands.SYSTEM_COMMANDS] and
     * [EsdeBuiltinCommands.RULES] resolve for [systemName]. Used by the ROM scanner when
     * es_systems.xml is missing or lacks an `<extension>` element for this system.
     *
     * Emulator-level extensions are the authoritative source elsewhere in the app
     * (the picker filters by them, RetroArch config uses them); reusing that set
     * for scanning keeps the two views of "what is a ROM" identical.
     */
    fun systemExtensionsFallback(systemName: String): Set<String> {
        val commands = EsdeBuiltinCommands.SYSTEM_COMMANDS[systemName].orEmpty()
        val emulatorRegex = Regex("""%EMULATOR_([^%]+)%""")
        val extensions = mutableSetOf<String>()
        for ((_, command) in commands) {
            val emulatorName = emulatorRegex.find(command)?.groupValues?.get(1) ?: continue
            val entries = EsdeBuiltinCommands.RULES[emulatorName] ?: continue
            for (entry in entries) {
                val pkg = entry.substringBefore("/")
                val spec = EmulatorRegistry.resolve(pkg) ?: continue
                extensions += spec.extensions.map { it.lowercase() }
            }
        }
        // For systems that don't appear in EsdeBuiltinCommands.SYSTEM_COMMANDS at all (most of
        // them — that table only covers the built-in cases that ES-DE ships
        // without a distributable XML), fall back to the union of extensions
        // across every registered emulator. The ALWAYS_SKIPPED_EXTENSIONS filter
        // in the scanner catches sidecar files (.sav, .png, etc.), so a broad
        // set here is safe: false positives happen only for files whose extension
        // is unique to a niche emulator we don't ship, which is rare.
        if (extensions.isEmpty()) {
            EmulatorRegistry.KNOWN.forEach { spec ->
                extensions += spec.extensions.map { it.lowercase() }
            }
        }
        return extensions
    }

    /**
     * Extra keys whose value carries a namespaced ID rather than a file path,
     * mapped to the extension the app writes that ID into. A `.steam` file
     * holds a Steam app ID; a `.localgameid` file holds a GameHub-internal ID.
     * The picker uses this to detect commands whose expected content does not
     * match the ROM's extension and de-emphasise them, without filtering — a
     * misinferred mapping should not hide the only working option.
     *
     * Add a new entry here when introducing a command whose extra reads a
     * different ID namespace.
     */
    private val EXTRA_KEY_TO_REQUIRED_EXTENSION: Map<String, String> = mapOf(
        "localGameId" to "localgameid",
        "steamAppId" to "steam",
        "app_id" to "steam",
        "title_id" to "titleid",
    )

    private fun requiredExtensionFor(command: String): String? {
        val extraRegex = Regex("""%EXTRA(?:INTEGER|BOOL)?_([^%]+)%""")
        for (match in extraRegex.findAll(command)) {
            val key = match.groupValues[1]
            EXTRA_KEY_TO_REQUIRED_EXTENSION[key]?.let { return it }
        }
        return null
    }

    /**
     * The built-in command for [systemName] whose emulator resolves to [pkg]
     * and whose required extension matches [romExtension] (or that is
     * extension-agnostic). Returns null if nothing matches.
     *
     * This exists for emulators marked [RomLaunchContract.Undocumented] whose
     * launch cannot be reconstructed from the generic ContentUriView fallback:
     * GameHub Lite reads the game id from an extra populated by `%INJECT%=%ROM%`,
     * which the fallback has no way to produce. Without this, a user who picked
     * only the emulator (and never a specific command) lands on an empty
     * `GameDetailActivity` with a "Get Game" button and no game loaded.
     */
    fun autoSelectBuiltinCommand(
        systemName: String,
        pkg: String,
        romExtension: String,
    ): String? {
        val commands = EsdeBuiltinCommands.SYSTEM_COMMANDS[systemName] ?: return null
        val emulatorRegex = Regex("""%EMULATOR_([^%]+)%""")
        for ((_, command) in commands) {
            val emulatorName = emulatorRegex.find(command)?.groupValues?.get(1) ?: continue
            val entries = EsdeBuiltinCommands.RULES[emulatorName] ?: continue
            val matchesPkg = entries.any { it.substringBefore("/") == pkg }
            if (!matchesPkg) continue
            val required = requiredExtensionFor(command)
            if (required == null || required.equals(romExtension, ignoreCase = true)) {
                return command
            }
        }
        return null
    }

    /**
     * The built-in command for [systemName] whose emulator resolves to [pkg] and
     * whose extra reads an ID from files of [romExtension] (see
     * [EXTRA_KEY_TO_REQUIRED_EXTENSION]). Null when the extension is an ordinary
     * ROM extension.
     *
     * An ID file is never a ROM the emulator can open, so the generic contract
     * (which hands over the file's path or URI) cannot launch it whatever the
     * emulator's declared contract is. A `.titleid` for Banana Native must go
     * through its `title_id` command, not its `game` path extra.
     */
    fun builtinIdCommand(
        systemName: String,
        pkg: String,
        romExtension: String,
    ): String? {
        val commands = EsdeBuiltinCommands.SYSTEM_COMMANDS[systemName] ?: return null
        val emulatorRegex = Regex("""%EMULATOR_([^%]+)%""")
        return commands.firstOrNull { (_, command) ->
            val emulatorName = emulatorRegex.find(command)?.groupValues?.get(1)
            val matchesPkg = EsdeBuiltinCommands.RULES[emulatorName].orEmpty()
                .any { it.substringBefore("/") == pkg }
            matchesPkg && requiredExtensionFor(command)
                ?.equals(romExtension, ignoreCase = true) == true
        }?.second
    }

    /**
     * The first installed package named by a built-in command of [systemName].
     *
     * The automatic pick matches emulators by extension, and a game that is a
     * folder (a Switch 2 dump found by its `main.npdm`) has none. The system's
     * own built-in commands still say which emulator runs it.
     */
    fun builtinSystemEmulator(context: Context, systemName: String): String? {
        val emulatorRegex = Regex("""%EMULATOR_([^%]+)%""")
        return EsdeBuiltinCommands.SYSTEM_COMMANDS[systemName].orEmpty()
            .asSequence()
            .mapNotNull { (_, command) -> emulatorRegex.find(command)?.groupValues?.get(1) }
            .flatMap { EsdeBuiltinCommands.RULES[it].orEmpty() }
            .map { it.substringBefore("/") }
            .firstOrNull { EmulatorRegistry.isInstalled(context, it) }
    }

    fun getCompatibleEmulatorsFromSystem(
        context: Context,
        systemName: String,
        esSystemsFile: File,
        customRules: Map<String, List<String>> = emptyMap()
    ): List<EmulatorOption> {
        val parsed = parseSystemCommands(esSystemsFile, systemName)
        // Fall back to built-in commands when the custom es_systems.xml has no entry for this system
        val builtin = EsdeBuiltinCommands.SYSTEM_COMMANDS[systemName] ?: emptyList()
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
        // Dedupe by (package, command) so two distinct commands that share one
        // package — e.g. GameHub Lite reading a Steam id vs a local id — both
        // survive. Package-only dedupe would silently drop the second.
        val seenPackageCommands = mutableSetOf<Pair<String, String>>()
        for ((label, command) in commands) {
            val emulatorName = emulatorRegex.find(command)?.groupValues?.get(1) ?: continue
            val allEntries =
                (customRules[emulatorName] ?: emptyList()) + (EsdeBuiltinCommands.RULES[emulatorName]
                    ?: emptyList())
            val installedEntry = allEntries.firstOrNull { entry ->
                val pkg = entry.substringBefore("/")
                EmulatorRegistry.resolve(pkg) != null &&
                    EmulatorRegistry.isInstalled(context, pkg)
            } ?: continue
            val packageName = installedEntry.substringBefore("/")
            if (seenPackageCommands.add(packageName to command)) {
                result.add(
                    EmulatorOption(
                        packageName = packageName,
                        displayName = label,
                        command = command,
                        requiredExtension = requiredExtensionFor(command),
                    )
                )
            }
        }
        return result
    }

    fun getCompatibleEmulators(context: Context, romExtension: String): List<EmulatorOption> =
        EmulatorRegistry.candidatesForExtension(context, romExtension)
            .map { EmulatorOption(it.packageName, it.displayName) }

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

    fun buildIntent(
        launchCommand: String,
        romAbsPath: String,
        context: Context,
        customRules: Map<String, List<String>> = emptyMap(),
        contentUri: Uri? = null,
    ): Intent? {
        val emulatorRegex = Regex("""%EMULATOR_([^%]+)%""")
        val emulatorMatch = emulatorRegex.find(launchCommand) ?: return null
        val emulatorName = emulatorMatch.groupValues[1]

        val allEntries = (customRules[emulatorName] ?: emptyList()) + (EsdeBuiltinCommands.RULES[emulatorName]
            ?: emptyList())
        if (allEntries.isEmpty()) {
            Log.w(TAG, "No find rules for emulator: $emulatorName")
            return null
        }

        val pkgActivity = allEntries.firstOrNull { entry ->
            val pkg = entry.substringBefore("/")
            EmulatorRegistry.isInstalled(context, pkg)
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

        var grantedUri = false

        val extraRegex = Regex("""%EXTRA_([^%]+)%=([^\s]+)""")
        for (match in extraRegex.findAll(remaining)) {
            val key = match.groupValues[1]
            val rawToken = match.groupValues[2]
            val value = resolveToken(rawToken, romAbsPath, packageName, contentUri)
            intent.putExtra(key, value)
            if (contentUri != null && tokenReferencesRomUri(rawToken)) grantedUri = true
        }

        val extraIntegerRegex = Regex("""%EXTRAINTEGER_([^%]+)%=([^\s]+)""")
        for (match in extraIntegerRegex.findAll(remaining)) {
            val key = match.groupValues[1]
            val value = resolveToken(match.groupValues[2], romAbsPath, packageName, contentUri)
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
            val rawToken = dataMatch.groupValues[1]
            val resolvedUri =
                resolveDataUri(rawToken, romAbsPath, packageName, context, contentUri)
            if (resolvedUri != null) {
                intent.data = resolvedUri
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (contentUri != null && tokenReferencesRomUri(rawToken)) grantedUri = true
            }
        }

        if (grantedUri) {
            try {
                context.grantUriPermission(
                    packageName, contentUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w(TAG, "grantUriPermission($packageName, $contentUri) failed", e)
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Match the registry path: ClipData forwards the read grant to
            // receivers that only consult the extra, without altering intent
            // matching. Only attached when the command did not already set
            // intent.data — that path already carries the grant on its own.
            if (intent.data == null) {
                intent.clipData = android.content.ClipData.newRawUri("ROM", contentUri!!)
            }
        }

        return intent
    }

    /**
     * Builds a ROM-launch intent for [packageName].
     *
     * @param withComponent name the spec's explicit activity. Pass false for the
     *   retry: a stored activity name is a snapshot and a build that has updated
     *   since may have renamed it, in which case resolving the package's own VIEW
     *   handler still works.
     */
    fun buildRomIntentFromPackage(
        packageName: String,
        romAbsPath: String,
        contentUri: Uri,
        context: Context,
        corePath: String? = null,
        withComponent: Boolean = true,
    ): Intent {
        val spec = EmulatorRegistry.resolve(packageName)
        val declared = spec?.launchContract ?: RomLaunchContract.ContentUriView()
        // An undocumented row still gets the ordinary contract tried. Refusing on
        // sight charges the table's ignorance to the user, and these are exactly
        // the builds most likely to have gained a VIEW filter since the row was
        // written. The hint survives as the message if every attempt fails.
        val contract = if (declared is RomLaunchContract.Undocumented) {
            RomLaunchContract.ContentUriView()
        } else {
            declared
        }

        // Extras do not carry a URI grant the way intent.data does, so anything
        // reading a URI out of an extra needs an explicit grant. Log rather
        // than swallow: silent failure here manifests downstream as the
        // emulator booting to a black screen when it cannot open the URI.
        fun grant() = try {
            context.grantUriPermission(
                packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            Log.w(TAG, "grantUriPermission($packageName, $contentUri) failed", e)
        }

        val action = when (contract) {
            RomLaunchContract.RetroArch -> Intent.ACTION_MAIN
            is RomLaunchContract.UriExtra -> contract.action
            is RomLaunchContract.ContentUriView -> contract.action
            else -> Intent.ACTION_VIEW
        }

        return Intent(action).apply {
            setPackage(packageName)
            if (withComponent) spec?.activityName?.let { setClassName(packageName, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            when (contract) {
                is RomLaunchContract.ContentUriView -> {
                    grant()
                    setDataAndType(contentUri, MIME_ANY)
                    // clipData carries the grant for receivers that read it there.
                    clipData = android.content.ClipData.newRawUri("ROM", contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putRomHintExtras(romAbsPath)
                }

                is RomLaunchContract.UriExtra -> {
                    grant()
                    putExtra(contract.key, contentUri.toString())
                    // ClipData carries the read grant without polluting
                    // intent.data (which some receivers reject); it is not used
                    // for intent matching, so a receiver that only reads its
                    // own extra is unaffected.
                    clipData = android.content.ClipData.newRawUri("ROM", contentUri)
                    addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                is RomLaunchContract.PathExtra -> {
                    type = MIME_ANY
                    putExtra(contract.key, romAbsPath)
                    addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }

                RomLaunchContract.RetroArch -> {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    putExtra(RETROARCH_ROM_EXTRA, romAbsPath)
                    corePath?.let { putExtra(RETROARCH_CORE_EXTRA, it) }
                }

                is RomLaunchContract.Undocumented ->
                    error("Undocumented is resolved to a real contract above")
            }
        }
    }

    /**
     * The legacy extra shotgun, kept for emulators that read a path from one of
     * these and are not in the table. Unknown extras are ignored by everything
     * else, so this costs nothing.
     */
    private fun Intent.putRomHintExtras(romAbsPath: String) {
        val file = File(romAbsPath)
        putExtra("rom_path", romAbsPath)
        putExtra("rom_directory", file.parent ?: "")
        putExtra("rom_name", file.nameWithoutExtension)
        putExtra("GAMEPATH", romAbsPath)
        putExtra("ROMPATH", file.parent ?: "")
        putExtra("PATH", romAbsPath)
    }

    /** The hint to show when every launch attempt for [packageName] has failed. */
    fun undocumentedHint(packageName: String): String? =
        (EmulatorRegistry.resolve(packageName)?.launchContract
            as? RomLaunchContract.Undocumented)?.hint

    /**
     * Scans the cores directory of every installed RetroArch variant and returns
     * a list of (displayName, absolutePath) pairs for each .so found.
     * displayName is derived from the filename, e.g. "mgba_libretro_android.so" → "mGBA".
     */
    @SuppressLint("SdCardPath")
    fun getInstalledCores(context: Context): List<Pair<String, String>> {
        val pkg = listOf("com.retroarch.aarch64", "com.retroarch", "com.retroarch.ra32")
            .firstOrNull { p -> EmulatorRegistry.isInstalled(context, p) } ?: return emptyList()

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

    private fun tokenReferencesRomUri(token: String): Boolean =
        token.contains("%ROMSAF%") || token.contains("%ROMPROVIDER%")

    private fun resolveToken(
        token: String,
        romAbsPath: String,
        packageName: String,
        contentUri: Uri? = null,
    ): String {
        // %INJECT%=<path_token> — read the file content instead of using the path
        if (token.startsWith("%INJECT%=")) {
            val pathToken = token.removePrefix("%INJECT%=")
            val filePath = resolveToken(pathToken, romAbsPath, packageName, contentUri)
            return try { File(filePath).readText().trim() } catch (_: Exception) { filePath }
        }
        // %ROMSAF% / %ROMPROVIDER% resolve to a content URI when the caller has
        // one — that is what the target emulator actually needs under scoped
        // storage. Falls back to the absolute ROM path only if no URI is known.
        val romUriString = contentUri?.toString() ?: romAbsPath
        return token
            .replace("%ROMSAF%", romUriString)
            .replace("%ROMPROVIDER%", romUriString)
            .replace("%ROM%", romAbsPath)
            .replace("%ANDROIDPACKAGE%", packageName)
    }

    private fun resolveDataUri(
        token: String,
        romAbsPath: String,
        packageName: String,
        context: Context,
        contentUri: Uri? = null,
    ): Uri? {
        return when {
            token.contains("%ROMPROVIDER%") || token.contains("%ROMSAF%") -> {
                // Prefer the caller-supplied URI: it already carries the correct
                // read grant (from SAF or FileProvider), and rebuilding a
                // FileProvider URI here would drop that.
                contentUri ?: try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        File(romAbsPath)
                    )
                } catch (_: Exception) {
                    Uri.fromFile(File(romAbsPath))
                }
            }

            token.contains("%ROM%") -> {
                Uri.fromFile(File(resolveToken(token, romAbsPath, packageName, contentUri)))
            }

            else -> {
                resolveToken(token, romAbsPath, packageName, contentUri).toUri()
            }
        }
    }
}
