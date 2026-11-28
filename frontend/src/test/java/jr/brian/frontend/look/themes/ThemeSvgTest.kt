package jr.brian.frontend.look.themes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** One SVG rule for `.jarntheme` files and ES-DE imports: art only, nothing that reaches outside the file. */
class ThemeSvgTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun svg(body: String) = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10">$body</svg>""".toByteArray()

    @Test
    fun `plain vector art passes`() {
        assertTrue(isSafeThemeSvg(svg("""<rect width="10" height="10" fill="#123456"/>""")))
        assertTrue(isSafeThemeSvg("<svg/>".toByteArray()))
        assertTrue(isSafeThemeSvg(svg("""<defs><linearGradient id="g"/></defs><rect fill="url(#g)"/><use href="#g"/>""")))
        assertTrue(isSafeThemeSvg(svg("""<image xlink:href="data:image/png;base64,iVBORw0KGgo="/>""")))
    }

    @Test
    fun `entities, doctypes and scripts are refused`() {
        val hostile = listOf(
            """<?xml version="1.0"?><!DOCTYPE svg [<!ENTITY x SYSTEM "file:///etc/passwd">]><svg>&x;</svg>""",
            """<svg><!ENTITY lol "lol"></svg>""",
            String(svg("<script>alert(1)</script>")),
            String(svg("""<SCRIPT type="text/javascript"/>""")),
            String(svg("""<rect onload="steal()"/>""")),
            String(svg("""<foreignObject><iframe src="https://x"/></foreignObject>""")),
            String(svg("""<style>@import url(https://x/a.css);</style>""")),
            """<?xml-stylesheet href="https://x/a.css"?><svg/>""",
        )
        hostile.forEach { assertFalse(it, isSafeThemeSvg(it.toByteArray())) }
    }

    @Test
    fun `links that leave the file are refused`() {
        val outside = listOf(
            """<image href="https://example.com/a.png"/>""",
            """<image xlink:href="file:///sdcard/a.png"/>""",
            """<use xlink:href='other.svg#a'/>""",
            """<a href="javascript:alert(1)"><rect/></a>""",
            """<image href="data:text/html;base64,PHNjcmlwdD4="/>""",
            """<rect style="fill:url(https://x/p.png)"/>""",
            """<rect fill="url('../escape.svg#p')"/>""",
        )
        outside.forEach { assertFalse(it, isSafeThemeSvg(svg(it))) }
    }

    @Test
    fun `size, encoding and the svg root are checked`() {
        assertFalse(isSafeThemeSvg(ByteArray(0)))
        assertFalse(isSafeThemeSvg(ByteArray(MAX_THEME_SVG_BYTES + 1) { ' '.code.toByte() }))
        assertFalse(isSafeThemeSvg(byteArrayOf(0x3C, 0x73, 0x76, 0x67, 0x2F, 0x3E, 0xC3.toByte(), 0x28)))
        assertFalse(isSafeThemeSvg("<html><body/></html>".toByteArray()))
        assertFalse(isSafeThemeSvg("<svgfoo/>".toByteArray()))
    }

    @Test
    fun `a theme file keeps safe SVGs and deletes hostile ones`() {
        val root = temp.newFolder()
        root.resolve("logo.svg").writeBytes(svg("<circle r='4'/>"))
        root.resolve("bad.svg").writeBytes(svg("<script/>"))
        val passed = checkThemeAssets(root)
        assertEquals(mapOf("logo.svg" to ThemeAssetKind.Image), passed)
        assertFalse(root.resolve("bad.svg").exists())
        assertEquals(ThemeAssetKind.Image, themeAssetKind("logos/A.SVG"))
    }
}
