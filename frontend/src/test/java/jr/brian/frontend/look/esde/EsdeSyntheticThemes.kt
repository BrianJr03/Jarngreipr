package jr.brian.frontend.look.esde

import java.io.File

private val FIXTURES_DIR = File("src/test/resources/esde-fixtures")

/** Past [ESDE_MAX_FILE_BYTES], so the parser must skip it. Written at install time, not checked in. */
private const val OVERSIZE_BYTES = ESDE_MAX_FILE_BYTES.toInt() + 4 * 1024

/**
 * The synthetic ES-DE themes in `src/test/resources/esde-fixtures`, each copying only the
 * structure of a common theme style. [systems] and [selection] are what the goldens were made with.
 */
enum class EsdeSyntheticTheme(
    val id: String,
    val selection: EsdeThemeSelection = EsdeThemeSelection(),
) {
    HorizonCarousel("horizon-carousel"),
    WheelVideo("wheel-video"),
    CoverGrid("cover-grid", EsdeThemeSelection(fontSize = "large")),
    ListDetail("list-detail"),
    PerSystem("per-system"),
    ColorSchemes("color-schemes", EsdeThemeSelection(colorScheme = "paper")),
    LegacyFormat("legacy-format"),
    Hostile("hostile"),
    ;

    val systems: List<EsdeSystemRef> get() = EsdeFixtureSystems

    /** The checked-in folder, for scripts that push it to a device as-is. */
    val source: File get() = File(FIXTURES_DIR, id)

    /**
     * Copies the theme to `<parent>/<id>` and returns that folder. The hostile theme also gets
     * an oversize include and a real file just outside the theme, so its escape is a true one.
     */
    fun install(parent: File): File {
        val target = File(parent, id)
        source.copyRecursively(target, overwrite = true)
        if (this == Hostile) {
            File(target, "_inc/huge.xml").writeText(oversizeTheme())
            File(parent, "outside.xml").writeText("<theme><view name=\"system\"><text name=\"leak\"/></view></theme>")
        }
        return target
    }

    private fun oversizeTheme(): String = buildString(OVERSIZE_BYTES + 64) {
        append("<theme><!--")
        while (length < OVERSIZE_BYTES) append("padding ")
        append("--></theme>")
    }
}

/** Every fixture is read for these three: two with their own folders in [EsdeSyntheticTheme.PerSystem], one without. */
val EsdeFixtureSystems = listOf(
    EsdeSystemRef(name = "nes", fullName = "Nintendo Entertainment System"),
    EsdeSystemRef(name = "snes", fullName = "Super Nintendo"),
    EsdeSystemRef(name = "n64", fullName = "Nintendo 64"),
)
