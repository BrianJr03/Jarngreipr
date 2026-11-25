package jr.brian.home.esde.util

import android.content.Context
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

private const val TAG = "BundledEsdeSystems"
private const val SYSTEMS_ASSET = "esde/android/es_systems.xml"
private const val FIND_RULES_ASSET = "esde/android/es_find_rules.xml"

/**
 * ES-DE's Android `es_systems.xml` and `es_find_rules.xml`, bundled unmodified from ES-DE v3.5.0
 * (commit 50e4b600ae533d772bae3ff880d11a09b05dbe84, gitlab.com/es-de/emulationstation-de).
 * MIT licensed: Copyright (c) 2024-2026 Northwestern Software AB, (c) 2020-2024 Leon Styhre,
 * (c) 2014 Alec Lofquist. See NOTICE.md.
 *
 * ES-DE ships these inside its APK, where no other app can read them, so this copy is how ES-DE's
 * emulator labels and rule names resolve. A user's own `custom_systems` files always come first.
 * Each file is parsed once, on first use. Call [prewarm] from a background thread.
 */
object BundledEsdeSystems {
    @Volatile private var commands: Map<String, List<Pair<String, String>>>? = null
    @Volatile private var rules: Map<String, List<String>>? = null

    /** Label to command for each system, keyed by ES-DE's lowercase system name. */
    fun commands(context: Context, systemName: String): List<Pair<String, String>> =
        allCommands(context)[systemName.lowercase()].orEmpty()

    fun allCommands(context: Context): Map<String, List<Pair<String, String>>> =
        commands ?: synchronized(this) {
            commands ?: read(context, SYSTEMS_ASSET, ::parseSystems).also { commands = it }
        }

    /** Emulator name to its "package/activity" entries. */
    fun rules(context: Context): Map<String, List<String>> =
        rules ?: synchronized(this) {
            rules ?: read(context, FIND_RULES_ASSET, EsdeCommandLauncher::parseRules).also { rules = it }
        }

    /** Loads the find rules, which launches read, so a launch never parses them on the main thread. */
    fun prewarm(context: Context) {
        rules(context)
    }

    private fun <T : Any> read(context: Context, asset: String, parse: (InputStream) -> Map<String, T>): Map<String, T> =
        runCatching { context.assets.open(asset).use(parse) }
            .onFailure { Log.e(TAG, "Could not read $asset", it) }
            .getOrDefault(emptyMap())

    /** Every `<system>`'s `<command label>` entries, in file order. */
    internal fun parseSystems(input: InputStream): Map<String, List<Pair<String, String>>> {
        val systems = LinkedHashMap<String, List<Pair<String, String>>>()
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        var name: String? = null
        var label = ""
        var current = mutableListOf<Pair<String, String>>()
        val text = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    text.clear()
                    when (parser.name) {
                        "system" -> { name = null; current = mutableListOf() }
                        "command" -> label = parser.getAttributeValue(null, "label").orEmpty()
                    }
                }
                XmlPullParser.TEXT -> text.append(parser.text)
                XmlPullParser.END_TAG -> {
                    val value = text.toString().trim()
                    when (parser.name) {
                        "name" -> if (name == null) name = value.lowercase()
                        "command" -> if (value.isNotEmpty()) current += label to value
                        "system" -> name?.let { systems[it] = current }
                    }
                    text.clear()
                }
            }
            event = parser.next()
        }
        return systems
    }
}
