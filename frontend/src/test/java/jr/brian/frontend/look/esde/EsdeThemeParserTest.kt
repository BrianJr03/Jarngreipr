package jr.brian.frontend.look.esde

import android.app.Application
import jr.brian.frontend.look.esde.EsdeParseIssue.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EsdeThemeParserTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var fixture: EsdeThemeFixture
    private val parser = EsdeThemeParser()
    private val nes = EsdeSystemRef(name = "nes", fullName = "Nintendo Entertainment System")
    private val snes = EsdeSystemRef(name = "snes", fullName = "Super Nintendo")

    @Before
    fun setUp() {
        fixture = EsdeThemeFixture(temp.newFolder("theme"))
        fixture.capabilities()
    }

    private fun parse(
        vararg systems: EsdeSystemRef = arrayOf(nes),
        selection: EsdeThemeSelection = EsdeThemeSelection(),
    ) = parser.parse(fixture.root, systems.toList(), selection)

    private fun EsdeThemeModel.props(type: String, name: String, view: EsdeView = EsdeView.Gamelist, system: String = "nes") =
        systems.getValue(system).views[view]?.get(type, name)

    private fun EsdeThemeModel.kinds() = issues.map { it.kind }

    @Test
    fun `elements merge by type and name and the last definition wins`() {
        fixture.theme(
            body = fixture.element("gamelist", "image", "logo", "pos" to "0.1 0.1", "size" to "0.2 0.2") +
                fixture.element("system, gamelist", "image", "logo", "pos" to "0.5 0.5") +
                fixture.element("gamelist", "text", "logo", "color" to "FFFFFF"),
        )

        val model = parse()

        assertEquals(mapOf("pos" to "0.5 0.5", "size" to "0.2 0.2"), model.props("image", "logo"))
        assertEquals(mapOf("pos" to "0.5 0.5"), model.props("image", "logo", EsdeView.System))
        assertEquals(mapOf("color" to "FFFFFF"), model.props("text", "logo"))
        assertTrue(model.issues.isEmpty())
    }

    @Test
    fun `one element tag can name several elements`() {
        fixture.theme(body = fixture.element("gamelist", "text", "title, developer", "fontSize" to "0.03"))

        val model = parse()

        assertEquals("0.03", model.props("text", "title")?.get("fontSize"))
        assertEquals("0.03", model.props("text", "developer")?.get("fontSize"))
    }

    @Test
    fun `includes resolve relative to the including file`() {
        fixture.theme(body = "<include>./parts/layout.xml</include>")
        fixture.theme("parts/layout.xml", body = "<include>./colors.xml</include>" + fixture.element("gamelist", "image", "bg", "path" to "./art/bg.png"))
        fixture.theme("parts/colors.xml", body = fixture.element("gamelist", "image", "bg", "color" to "112233"))

        val model = parse()

        assertEquals(mapOf("color" to "112233", "path" to "./parts/art/bg.png"), model.props("image", "bg"))
        assertTrue(model.issues.isEmpty())
    }

    @Test
    fun `include cycles stop and are reported`() {
        fixture.theme(body = "<include>./a.xml</include>")
        fixture.theme("a.xml", body = "<include>./b.xml</include>" + fixture.element("gamelist", "text", "a", "color" to "A"))
        fixture.theme("b.xml", body = "<include>./a.xml</include>" + fixture.element("gamelist", "text", "b", "color" to "B"))

        val model = parse()

        assertEquals("A", model.props("text", "a")?.get("color"))
        assertEquals("B", model.props("text", "b")?.get("color"))
        assertEquals(listOf(Kind.IncludeCycle), model.kinds())
    }

    @Test
    fun `includes stop past the depth cap`() {
        fixture.theme(body = "<include>./level1.xml</include>")
        (1..ESDE_MAX_INCLUDE_DEPTH + 2).forEach { level ->
            fixture.theme(
                "level$level.xml",
                body = "<include>./level${level + 1}.xml</include>" + fixture.element("gamelist", "text", "l$level", "z" to "$level"),
            )
        }

        val model = parse()

        assertEquals("$ESDE_MAX_INCLUDE_DEPTH", model.props("text", "l$ESDE_MAX_INCLUDE_DEPTH")?.get("z"))
        assertNull(model.props("text", "l${ESDE_MAX_INCLUDE_DEPTH + 1}"))
        assertTrue(Kind.IncludeTooDeep in model.kinds())
    }

    @Test
    fun `oversized files are skipped`() {
        fixture.theme(body = "<include>./big.xml</include>" + fixture.element("gamelist", "text", "kept", "z" to "1"))
        fixture.theme("big.xml", body = "<!--" + "x".repeat(ESDE_MAX_FILE_BYTES.toInt()) + "-->")

        val model = parse()

        assertEquals("1", model.props("text", "kept")?.get("z"))
        assertEquals(listOf(Kind.FileTooLarge), model.kinds())
    }

    @Test
    fun `paths outside the theme folder are refused`() {
        temp.newFile("secret.xml").writeText("<theme>" + fixture.element("gamelist", "text", "leak", "z" to "1") + "</theme>")
        fixture.theme(
            body = "<include>../secret.xml</include><include>/etc/hosts</include>" +
                fixture.element("gamelist", "image", "bg", "path" to "../../outside.png", "tile" to "true") +
                fixture.element("gamelist", "image", "home", "path" to "~/pic.png"),
        )

        val model = parse()

        assertNull(model.props("text", "leak"))
        assertEquals(mapOf("tile" to "true"), model.props("image", "bg"))
        assertEquals(emptyMap<String, String>(), model.props("image", "home"))
        assertEquals(4, model.issues.count { it.kind == Kind.PathOutsideTheme })
    }

    @Test
    fun `variables resolve, nest and substitute per system`() {
        fixture.theme(
            body = "<variables><accent>FF8800</accent><accentDim>\${accent}80</accentDim>" +
                "<logo>./logos/\${system.theme}.svg</logo></variables>" +
                fixture.element(
                    "system",
                    "image",
                    "logo",
                    "path" to "\${logo}",
                    "color" to "\${accentDim}",
                ) +
                fixture.element("system", "text", "label", "text" to "\${system.fullName} (\${system.name})"),
        )

        val model = parse(nes, snes)

        assertEquals(mapOf("path" to "./logos/nes.svg", "color" to "FF880080"), model.props("image", "logo", EsdeView.System))
        assertEquals("./logos/snes.svg", model.props("image", "logo", EsdeView.System, "snes")?.get("path"))
        assertEquals("Super Nintendo (snes)", model.props("text", "label", EsdeView.System, "snes")?.get("text"))
        assertTrue(model.issues.isEmpty())
    }

    @Test
    fun `later variables replace earlier ones and empty values are ignored`() {
        fixture.theme(
            body = "<variables><panel>111111</panel><keep>AAAAAA</keep></variables><include>./inc.xml</include>" +
                fixture.element("gamelist", "text", "t", "color" to "\${panel}", "background" to "\${keep}"),
        )
        fixture.theme("inc.xml", body = "<variables><panel>222222</panel><keep></keep></variables>")

        assertEquals(mapOf("color" to "222222", "background" to "AAAAAA"), parse().props("text", "t"))
    }

    @Test
    fun `unknown variables are reported per system and read as empty`() {
        fixture.theme(body = fixture.element("gamelist", "text", "t", "color" to "\${missing}"))

        val model = parse()

        assertEquals("", model.props("text", "t")?.get("color"))
        assertEquals(EsdeParseIssue.Kind.UnknownVariable, model.issues.single().kind)
        assertEquals("nes", model.issues.single().systemId)
    }

    @Test
    fun `a system folder theme wins over the root theme`() {
        fixture.theme(body = fixture.element("system", "image", "bg", "path" to "./root.png"))
        fixture.theme("snes/theme.xml", body = fixture.element("system", "image", "bg", "path" to "./snes.png"))

        val model = parse(nes, snes)

        assertEquals("./root.png", model.props("image", "bg", EsdeView.System)?.get("path"))
        assertEquals("./snes/snes.png", model.props("image", "bg", EsdeView.System, "snes")?.get("path"))
    }

    @Test
    fun `selection picks the chosen variant, color scheme, font size and aspect ratio`() {
        fixture.capabilities(
            variants = listOf("carousel", "grid"),
            colorSchemes = listOf("dark", "light"),
            fontSizes = listOf("medium", "large"),
            aspectRatios = listOf("16:9", "4:3"),
        )
        fixture.theme(
            body = """
                <colorScheme name="dark"><variables><bg>000000</bg></variables></colorScheme>
                <colorScheme name="light"><variables><bg>FFFFFF</bg></variables></colorScheme>
                <fontSize name="medium"><variables><size>0.03</size></variables></fontSize>
                <fontSize name="large"><variables><size>0.05</size></variables></fontSize>
                <view name="gamelist"><text name="t"><color>${'$'}{bg}</color><fontSize>${'$'}{size}</fontSize></text></view>
                <variant name="carousel"><view name="gamelist"><carousel name="list"><type>horizontal</type></carousel></view></variant>
                <variant name="grid, all"><view name="gamelist"><text name="shared"><z>1</z></text></view></variant>
                <variant name="grid"><view name="gamelist"><grid name="list"><columns>4</columns></grid></view></variant>
                <aspectRatio name="4:3"><view name="gamelist"><text name="t"><pos>0 0.1</pos></text></view></aspectRatio>
            """.trimIndent(),
        )

        val defaults = parse()
        assertEquals(EsdeThemeSelection("carousel", "dark", "medium", "16:9"), defaults.selection)
        assertEquals(mapOf("color" to "000000", "fontSize" to "0.03"), defaults.props("text", "t"))
        assertEquals("horizontal", defaults.props("carousel", "list")?.get("type"))
        assertNull(defaults.props("grid", "list"))
        assertEquals("1", defaults.props("text", "shared")?.get("z"))

        val chosen = parse(selection = EsdeThemeSelection("grid", "light", "large", "4:3"))
        assertEquals(mapOf("color" to "FFFFFF", "fontSize" to "0.05", "pos" to "0 0.1"), chosen.props("text", "t"))
        assertEquals("4", chosen.props("grid", "list")?.get("columns"))
        assertNull(chosen.props("carousel", "list"))
        assertTrue(chosen.issues.isEmpty())
        assertEquals(listOf("carousel", "grid"), chosen.capabilities.variants.map { it.name })
        assertEquals("dark label", chosen.capabilities.colorSchemes.first().label)
    }

    @Test
    fun `unknown selections fall back to the first option`() {
        fixture.capabilities(variants = listOf("a", "b"))
        fixture.theme(body = "")

        val model = parse(selection = EsdeThemeSelection(variant = "zzz"))

        assertEquals("a", model.selection.variant)
        assertEquals(listOf(Kind.UnknownSelection), model.kinds())
    }

    @Test
    fun `parse order lets variants override views and color schemes override variables`() {
        fixture.capabilities(variants = listOf("v"), colorSchemes = listOf("c"))
        fixture.theme(
            body = """
                <variant name="v"><view name="gamelist"><text name="t"><z>variant</z></text></view></variant>
                <view name="gamelist"><text name="t"><z>general</z><color>${'$'}{c}</color></text></view>
                <colorScheme name="c"><variables><c>scheme</c></variables></colorScheme>
                <variables><c>base</c></variables>
            """.trimIndent(),
        )

        assertEquals(mapOf("z" to "variant", "color" to "scheme"), parse().props("text", "t"))
    }

    @Test
    fun `malformed files are reported and the rest still loads`() {
        fixture.theme(body = "<include>./broken.xml</include><include>./entity.xml</include>" + fixture.element("gamelist", "text", "ok", "z" to "1"))
        fixture.file("broken.xml", "<theme><view name=\"gamelist\"><text name=\"x\"></view>")
        fixture.file("entity.xml", "<?xml version=\"1.0\"?><!DOCTYPE t [<!ENTITY a \"aaaa\">]><theme>&a;</theme>")

        val model = parse()

        assertEquals("1", model.props("text", "ok")?.get("z"))
        assertEquals(setOf(Kind.Malformed, Kind.DocTypeRefused), model.kinds().toSet())
    }

    @Test
    fun `missing pieces are reported without throwing`() {
        fixture.root.resolve("capabilities.xml").delete()
        fixture.theme(body = "<include>./nope.xml</include>")

        val model = parse()

        assertEquals(setOf(Kind.MissingCapabilities, Kind.MissingInclude), model.kinds().toSet())
        assertEquals(EsdeThemeCapabilities(), model.capabilities)
    }

    @Test
    fun `a system with no theme file is reported`() {
        val model = parse()

        assertEquals(Kind.MissingThemeFile, model.issues.single().kind)
        assertEquals("nes", model.issues.single().systemId)
        assertEquals(emptyMap<EsdeView, EsdeViewModel>(), model.systems.getValue("nes").views)
    }

    @Test
    fun `legacy themes are reported as unsupported`() {
        fixture.theme(body = "<formatVersion>7</formatVersion>")

        val model = parse()

        assertTrue(model.isLegacy)
        assertTrue(model.systems.isEmpty())
    }
}
