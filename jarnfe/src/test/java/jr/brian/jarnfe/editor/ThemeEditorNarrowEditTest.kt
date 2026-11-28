package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemePrimaryPatch
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.model.theme.ThemeViews
import jr.brian.core.model.theme.ViewSpec
import jr.brian.jarnfe.library.LibraryBuiltInThemes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** #576: with the narrow canvas on, every edit lands in the view's narrow patch. */
class ThemeEditorNarrowEditTest {
    private val title = ThemeLayer.Text(id = "title", rect = ThemeRect(0.1f, 0.1f, 0.4f, 0.1f), z = 2, content = jr.brian.core.model.theme.ThemeText.Static("T"))
    private val panel = ThemeLayer.Panel(id = "panel", rect = ThemeRect(0f, 0f, 1f, 0.3f), z = -1)
    private val wide = ViewSpec(primary = ThemePrimary.Grid(ThemeRect(0f, 0.3f, 1f, 0.6f), columns = 5), layers = listOf(panel, title))
    private val spec: JarnThemeSpec = LibraryBuiltInThemes.Grid.spec.copy(views = ThemeViews(systems = wide, games = wide))

    private fun narrowEditor(selected: ThemeEditorTarget = ThemeEditorTarget.Layer("title")) =
        ThemeEditorState(themeId = "t", spec = spec, view = ThemeView.Systems, selected = selected, snap = false, narrow = true)

    @After
    fun closeEditor() = JarnThemeEditor.close()

    @Test
    fun `moving a piece writes a replacement into the patch and keeps the wide layout`() {
        val moved = narrowEditor().nudged(1, 0)
        val view = moved.wideViewSpec!!
        assertEquals(wide.layers, view.layers)
        assertEquals(listOf("title"), view.narrow?.layers?.map { it.id })
        assertTrue(view.narrow!!.layers.single().rect.x > title.rect.x)
        assertTrue(moved.differsOnNarrow(ThemeEditorTarget.Layer("title")))
        assertFalse(moved.differsOnNarrow(ThemeEditorTarget.Layer("panel")))
    }

    @Test
    fun `the wide canvas still edits the wide layout`() {
        val moved = narrowEditor().copy(narrow = false).nudged(1, 0)
        assertNull(moved.wideViewSpec?.narrow)
        assertTrue(moved.wideViewSpec!!.layers.single { it.id == "title" }.rect.x > title.rect.x)
    }

    @Test
    fun `deleting hides on narrow only, and adding adds on narrow only`() {
        val deleted = narrowEditor().withoutLayer("panel")
        assertEquals(listOf("panel"), deleted.wideViewSpec?.narrow?.hide)
        assertEquals(wide.layers, deleted.wideViewSpec?.layers)
        assertFalse(ThemeEditorTarget.Layer("panel") in deleted.targets)

        val added = deleted.withNewLayer(ThemeEditorNewLayer.Rating)
        val newId = (added.selected as ThemeEditorTarget.Layer).id
        assertTrue(added.wideViewSpec!!.narrow!!.layers.any { it.id == newId })
        assertFalse(added.wideViewSpec!!.layers.any { it.id == newId })
        assertTrue(newId != "panel")
    }

    @Test
    fun `list settings go to the patch's primary`() {
        val edited = narrowEditor(ThemeEditorTarget.Navigator).withPrimary { (it as ThemePrimary.Grid).copy(columns = 3) }
        assertEquals(ThemePrimaryPatch(columns = 3), edited.wideViewSpec?.narrow?.primary)
        assertEquals(5, (edited.wideViewSpec?.primary as ThemePrimary.Grid).columns)
        assertEquals(3, (edited.viewSpec?.primary as ThemePrimary.Grid).columns)
        assertTrue(edited.differsOnNarrow(ThemeEditorTarget.Navigator))
    }

    @Test
    fun `reset puts a piece or the whole view back to wide`() {
        val edited = narrowEditor().nudged(1, 0).withoutLayer("panel")
            .withPrimary { (it as ThemePrimary.Grid).copy(columns = 3) }
        val titleReset = edited.withNarrowReset(ThemeEditorTarget.Layer("title"))
        assertEquals(emptyList<ThemeLayer>(), titleReset.wideViewSpec?.narrow?.layers)
        assertEquals(listOf("panel"), titleReset.wideViewSpec?.narrow?.hide)

        val panelReset = titleReset.withNarrowReset(ThemeEditorTarget.Layer("panel"))
        assertTrue(ThemeEditorTarget.Layer("panel") in panelReset.targets)

        assertNull(edited.withNarrowCleared().wideViewSpec?.narrow)
    }

    @Test
    fun `editing back to the wide values leaves no patch`() {
        val there = narrowEditor().nudged(1, 0)
        val back = there.nudged(-1, 0)
        assertNull(back.wideViewSpec?.narrow)
    }

    @Test
    fun `undo and redo step narrow edits`() {
        JarnThemeEditor.open("t", spec)
        JarnThemeEditor.update { narrowEditor() }
        JarnThemeEditor.edit { nudged(1, 0) }
        assertTrue(JarnThemeEditor.state?.wideViewSpec?.narrow != null)
        JarnThemeEditor.undo()
        assertNull(JarnThemeEditor.state?.wideViewSpec?.narrow)
        JarnThemeEditor.redo()
        assertEquals(listOf("title"), JarnThemeEditor.state?.wideViewSpec?.narrow?.layers?.map { it.id })
    }

    @Test
    fun `turning the narrow canvas on drops a selection the narrow layout hides`() {
        val hidden = narrowEditor(ThemeEditorTarget.Layer("panel")).withoutLayer("panel")
            .copy(narrow = false, selected = ThemeEditorTarget.Layer("panel"))
        assertEquals(ThemeEditorTarget.Navigator, hidden.withNarrowCanvas(true).selected)
    }

    @Test
    fun `compare counts changes for the bottom screen`() {
        val now = narrowEditor().nudged(1, 0).withoutLayer("panel").spec
        val diff = themeVersionDiff(then = spec, now = now)
        assertEquals(2, diff.narrowChanged)
        assertEquals(0, diff.changed)
        assertFalse(diff.listChanged)
        assertFalse(diff.same)
    }
}
