package jr.brian.jarnfe.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.jarnfe.library.LibraryBuiltInThemes
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.StageRoute
import jr.brian.jarnfe.themekit.ThemeLayoutIssue
import jr.brian.jarnfe.themes.JarnUserThemeStore
import jr.brian.jarnfe.themes.ThemeVersion

private const val UNDO_LIMIT = 100

/**
 * The one theme editing session, shared by the canvas on Stage and the tools on the Deck. It
 * outlives the editor route while the player tries the theme, and ends when they leave.
 * Undo covers the whole session: every change to the draft, with a held piece counting once.
 */
internal object JarnThemeEditor {
    var state by mutableStateOf<ThemeEditorState?>(null)
        private set

    private val undoStack = ArrayDeque<JarnThemeSpec>()
    private val redoStack = ArrayDeque<JarnThemeSpec>()
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    /** What the layout engine flagged on the canvas, published by the canvas for the tools. */
    var layoutIssues by mutableStateOf<List<ThemeLayoutIssue>>(emptyList())

    /** How many repeats the current held direction or shoulder has run, so it speeds up. */
    private var holdRepeat = 0

    /** Where the draft, saves and history go. Null in tests and previews, which keep nothing. */
    var store: JarnUserThemeStore? = null
        private set

    /** The theme's saved versions, newest first. */
    var versions by mutableStateOf<List<ThemeVersion>>(emptyList())

    /** Grows each time Start > History asks the tools to bring History into view. */
    var historyRequests by mutableIntStateOf(0)
        private set

    /** Opens the tools at History: on the Deck when it has a window, else in the sheet. */
    fun showHistory(toolsOnDeck: Boolean) {
        update { copy(tab = ThemeEditorTab.Preview, sheetFocused = !toolsOnDeck) }
        if (toolsOnDeck) JarnSession.update { activate(JarnScreen.Deck) }
        historyRequests += 1
    }

    fun open(themeId: String, spec: JarnThemeSpec, store: JarnUserThemeStore? = null) {
        this.store = store
        versions = emptyList()
        state = ThemeEditorState(themeId, spec.withBothViews(LibraryBuiltInThemes.Grid.spec))
        undoStack.clear()
        resetHoldSteps()
        redoStack.clear()
        syncHistory()
        JarnSession.update { openThemeEditor() }
    }

    /** A change that doesn't touch the draft: selection, tab, axis and the like. */
    fun update(transform: ThemeEditorState.() -> ThemeEditorState) {
        state = state?.transform()
    }

    /** A change to the draft, which undo can take back. */
    fun edit(transform: ThemeEditorState.() -> ThemeEditorState) {
        val current = state ?: return
        val next = current.transform()
        if (next.spec != current.spec) remember(current.spec)
        state = next
    }

    fun pickUp() {
        resetHoldSteps()
        update { pickedUp() }
    }

    /** One undo step for whatever changed since the hold began, or since its last stick push. */
    fun drop() {
        endStickGesture()
        val before = holdCommitted ?: state?.held?.before ?: return
        update { dropped() }
        if (state?.spec != before) remember(before)
        resetHoldSteps()
    }

    /** Puts the piece back where it was picked up, stick pushes included. */
    fun cancel() {
        repeat(holdSteps) { undoStack.removeLastOrNull() }
        resetHoldSteps()
        update { cancelled() }
        syncHistory()
    }

    /** Where the current stick push began, while one is under way. */
    private var stickStart: JarnThemeSpec? = null

    /** The draft after the hold's last stick push, which [drop] measures from. */
    private var holdCommitted: JarnThemeSpec? = null

    /** Undo steps the current hold's stick pushes added, which [cancel] takes back. */
    private var holdSteps = 0

    fun beginStickGesture() {
        if (stickStart == null && state?.held != null) stickStart = state?.spec
    }

    /** A stick push that moved the held piece becomes one undo step. */
    fun endStickGesture() {
        val start = stickStart ?: return
        stickStart = null
        val spec = state?.spec ?: return
        if (state?.held == null || spec == start) return
        remember(start)
        holdSteps += 1
        holdCommitted = spec
    }

    private fun resetHoldSteps() {
        stickStart = null
        holdCommitted = null
        holdSteps = 0
    }

    /** A [fresh] press moves one step; each repeat of a held press moves a little further. */
    fun nudge(dx: Int, dy: Int, fresh: Boolean) {
        holdRepeat = if (fresh) 0 else holdRepeat + 1
        update { nudged(dx, dy, holdSteps(holdRepeat)) }
    }

    fun resize(direction: Int, fresh: Boolean) {
        holdRepeat = if (fresh) 0 else holdRepeat + 1
        update { resized(direction, holdSteps(holdRepeat)) }
    }

    fun undo() = step(from = undoStack, to = redoStack)

    fun redo() = step(from = redoStack, to = undoStack)

    /** Ends the session without touching the routes, as when Try it uses the theme. */
    fun close() {
        state = null
        store = null
    }

    /** Back on the canvas, or the tools: the editor closes and the route underneath returns. */
    fun leave() {
        state = null
        if (JarnSession.state.stageRoute == StageRoute.ThemeEditor) JarnSession.update { back() }
    }

    private fun step(from: ArrayDeque<JarnThemeSpec>, to: ArrayDeque<JarnThemeSpec>) {
        val current = state ?: return
        if (current.held != null) return
        val spec = from.removeLastOrNull() ?: return
        to.addLast(current.spec)
        state = current.copy(spec = spec).keepingSelection()
        syncHistory()
    }

    private fun remember(spec: JarnThemeSpec) {
        undoStack.addLast(spec)
        if (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
        redoStack.clear()
        syncHistory()
    }

    private fun syncHistory() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }
}

/** A selected layer that an undo took away falls back to the navigator. */
private fun ThemeEditorState.keepingSelection(): ThemeEditorState =
    if (selected in targets) this else copy(selected = ThemeEditorTarget.Navigator)
