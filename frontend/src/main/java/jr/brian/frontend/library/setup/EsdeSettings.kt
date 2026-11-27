package jr.brian.frontend.library.setup

import java.io.File

/** Where ES-DE keeps its settings, relative to its folder. The first one found wins. */
internal val ESDE_SETTINGS_FILES = listOf("settings/es_settings.xml", "es_settings.xml")

/**
 * The `es_settings.xml` keys Jarn reads. ES-DE writes the theme under `Theme`; older
 * EmulationStation builds wrote `ThemeSet`, so both are read with `Theme` first.
 */
internal enum class EsdeSettingKey(vararg val names: String) {
    ROMDirectory("ROMDirectory"),
    ThemeSet("Theme", "ThemeSet"),
    ThemeVariant("ThemeVariant"),
    ThemeColorScheme("ThemeColorScheme"),
    ThemeAspectRatio("ThemeAspectRatio"),
}

/** The ES-DE settings Jarn uses. Each is null when missing or blank. Paths are as written, not resolved. */
data class EsdeSettings(
    val romDirectory: String? = null,
    val themeSet: String? = null,
    val themeVariant: String? = null,
    val themeColorScheme: String? = null,
    val themeAspectRatio: String? = null,
)

private val entryPatterns: Map<String, Regex> = EsdeSettingKey.entries.flatMap { it.names.asList() }
    .associateWith { name ->
        Regex("""<string\s+name\s*=\s*"${Regex.escape(name)}"\s+value\s*=\s*"([^"]*)"""")
    }

/** One `<string>` value from an `es_settings.xml`, or null when it is missing or blank. */
internal fun parseEsdeSetting(xml: String, key: EsdeSettingKey): String? = key.names.firstNotNullOfOrNull { name ->
    entryPatterns.getValue(name).find(xml)?.groupValues?.get(1)?.let(::unescapeXml)?.trim()?.takeIf { it.isNotEmpty() }
}

internal fun parseEsdeSettings(xml: String) = EsdeSettings(
    romDirectory = parseEsdeSetting(xml, EsdeSettingKey.ROMDirectory),
    themeSet = parseEsdeSetting(xml, EsdeSettingKey.ThemeSet),
    themeVariant = parseEsdeSetting(xml, EsdeSettingKey.ThemeVariant),
    themeColorScheme = parseEsdeSetting(xml, EsdeSettingKey.ThemeColorScheme),
    themeAspectRatio = parseEsdeSetting(xml, EsdeSettingKey.ThemeAspectRatio),
)

/** The settings in [esdeDir], or null when it has no readable settings file. Only reads. */
internal fun readEsdeSettings(esdeDir: File, readText: (File) -> String?): EsdeSettings? =
    ESDE_SETTINGS_FILES.firstNotNullOfOrNull { name -> readText(File(esdeDir, name)) }?.let(::parseEsdeSettings)

internal fun unescapeXml(value: String): String = value
    .replace("&quot;", "\"")
    .replace("&apos;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&amp;", "&")
