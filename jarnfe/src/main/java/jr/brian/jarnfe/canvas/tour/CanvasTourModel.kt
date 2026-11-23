package jr.brian.jarnfe.canvas.tour

import jr.brian.home.canvas.model.GridRect

internal const val CANVAS_TOUR_COLUMNS = 4
internal const val CANVAS_TOUR_ROWS = 3
internal const val CANVAS_TOUR_PAGES = 2

internal enum class CanvasTourInput { Confirm, Context, Menu, Undo, Left, Right, Up, Down, Shrink, Grow }

internal enum class CanvasTourMode { Browse, TileMenu, Options, Arranging, Holding }

internal enum class CanvasTourTile { Game, Music, Photo }

internal data class CanvasTourPlace(val page: Int, val rect: GridRect)

private val GameHome = CanvasTourPlace(0, GridRect(1, 1, 1, 1))
private val GameAtEdge = CanvasTourPlace(0, GridRect(CANVAS_TOUR_COLUMNS - 1, 1, 1, 1))
private val GameNextPage = CanvasTourPlace(1, GridRect(0, 1, 1, 1))

/** [script] is the exact presses "Do it for me" plays, so it can never teach something untrue. */
internal enum class CanvasTourStep(
    val startMode: CanvasTourMode = CanvasTourMode.Browse,
    val startPlace: CanvasTourPlace = GameHome,
    val script: List<CanvasTourInput> = emptyList(),
) {
    Intro,
    Launch(script = listOf(CanvasTourInput.Confirm)),
    TileOptions(script = listOf(CanvasTourInput.Context)),
    OneShotMove(
        startMode = CanvasTourMode.TileMenu,
        script = listOf(CanvasTourInput.Confirm, CanvasTourInput.Right, CanvasTourInput.Confirm),
    ),
    Arrange(script = listOf(CanvasTourInput.Menu, CanvasTourInput.Confirm)),
    Resize(
        startMode = CanvasTourMode.Arranging,
        script = listOf(CanvasTourInput.Confirm, CanvasTourInput.Grow),
    ),
    PageEdge(
        startMode = CanvasTourMode.Holding,
        startPlace = GameAtEdge,
        script = listOf(CanvasTourInput.Right),
    ),
    Undo(
        startMode = CanvasTourMode.Holding,
        startPlace = GameNextPage,
        script = listOf(CanvasTourInput.Confirm, CanvasTourInput.Undo),
    ),
    Done;

    val isPractice: Boolean get() = script.isNotEmpty()
}

/** A private sample board. Nothing here is ever read from or written to the real Canvas. */
internal data class CanvasTourState(
    val step: CanvasTourStep = CanvasTourStep.Intro,
    val mode: CanvasTourMode = step.startMode,
    val game: CanvasTourPlace = step.startPlace,
    val practiced: Boolean = false,
    val undoTo: CanvasTourPlace? = null,
) {
    val position: Int get() = step.ordinal
    val total: Int get() = CanvasTourStep.entries.size

    fun placeOf(tile: CanvasTourTile): CanvasTourPlace = when (tile) {
        CanvasTourTile.Game -> game
        CanvasTourTile.Music -> MusicPlace
        CanvasTourTile.Photo -> PhotoPlace
    }

    /** Null once the last step is passed. */
    fun next(): CanvasTourState? =
        CanvasTourStep.entries.getOrNull(step.ordinal + 1)?.let { CanvasTourState(it) }

    /** Backs out of a half-done step first. Null means B left the tour from its first step. */
    fun back(): CanvasTourState? = when {
        isPartWay -> CanvasTourState(step)
        step.ordinal == 0 -> null
        else -> CanvasTourState(CanvasTourStep.entries[step.ordinal - 1])
    }

    /** [repeat] is a held direction, which may move the tile but never flips the page. */
    fun press(input: CanvasTourInput, repeat: Boolean = false): CanvasTourState {
        if (practiced) return this
        return when (step) {
            CanvasTourStep.Launch -> if (input == CanvasTourInput.Confirm) copy(practiced = true) else this
            CanvasTourStep.TileOptions ->
                if (input == CanvasTourInput.Context) copy(mode = CanvasTourMode.TileMenu, practiced = true) else this
            CanvasTourStep.OneShotMove -> pressOneShot(input)
            CanvasTourStep.Arrange -> pressArrange(input)
            CanvasTourStep.Resize -> pressResize(input)
            CanvasTourStep.PageEdge -> pressPageEdge(input, repeat)
            CanvasTourStep.Undo -> pressUndo(input)
            CanvasTourStep.Intro, CanvasTourStep.Done -> this
        }
    }

    fun doneForMe(): CanvasTourState =
        step.script.fold(CanvasTourState(step)) { state, input -> state.press(input) }

    private val isPartWay: Boolean
        get() = !practiced && (mode != step.startMode || game != step.startPlace || undoTo != null)

    private fun pressOneShot(input: CanvasTourInput): CanvasTourState = when {
        mode == CanvasTourMode.TileMenu ->
            if (input == CanvasTourInput.Confirm) copy(mode = CanvasTourMode.Holding) else this
        input == CanvasTourInput.Confirm ->
            if (game != step.startPlace) copy(mode = CanvasTourMode.Browse, practiced = true) else this
        else -> nudged(input)
    }

    private fun pressArrange(input: CanvasTourInput): CanvasTourState = when {
        mode == CanvasTourMode.Browse ->
            if (input == CanvasTourInput.Menu) copy(mode = CanvasTourMode.Options) else this
        input == CanvasTourInput.Confirm -> copy(mode = CanvasTourMode.Arranging, practiced = true)
        else -> this
    }

    private fun pressResize(input: CanvasTourInput): CanvasTourState {
        if (mode == CanvasTourMode.Arranging) {
            return if (input == CanvasTourInput.Confirm) copy(mode = CanvasTourMode.Holding) else this
        }
        val resized = nudged(input)
        val changedSize = resized.game.rect.colSpan != game.rect.colSpan
        return if (changedSize) resized.copy(practiced = true) else resized
    }

    private fun pressPageEdge(input: CanvasTourInput, repeat: Boolean): CanvasTourState {
        val atRightEdge = game.rect.right >= CANVAS_TOUR_COLUMNS
        if (input == CanvasTourInput.Right && atRightEdge) {
            return if (repeat) this else copy(game = GameNextPage, practiced = true)
        }
        return if (input == CanvasTourInput.Grow || input == CanvasTourInput.Shrink) this else nudged(input)
    }

    private fun pressUndo(input: CanvasTourInput): CanvasTourState = when {
        mode == CanvasTourMode.Holding ->
            if (input == CanvasTourInput.Confirm) {
                copy(mode = CanvasTourMode.Arranging, undoTo = GameAtEdge)
            } else {
                this
            }
        input == CanvasTourInput.Undo && undoTo != null -> copy(game = undoTo, undoTo = null, practiced = true)
        else -> this
    }

    private fun nudged(input: CanvasTourInput): CanvasTourState {
        val rect = game.rect
        val target = when (input) {
            CanvasTourInput.Left -> rect.copy(col = rect.col - 1)
            CanvasTourInput.Right -> rect.copy(col = rect.col + 1)
            CanvasTourInput.Up -> rect.copy(row = rect.row - 1)
            CanvasTourInput.Down -> rect.copy(row = rect.row + 1)
            CanvasTourInput.Grow -> rect.copy(colSpan = rect.colSpan + 1)
            CanvasTourInput.Shrink -> if (rect.colSpan > 1) rect.copy(colSpan = rect.colSpan - 1) else rect
            else -> rect
        }
        return if (fits(target)) copy(game = game.copy(rect = target)) else this
    }

    private fun fits(rect: GridRect): Boolean {
        val inside = rect.col >= 0 && rect.row >= 0 &&
            rect.right <= CANVAS_TOUR_COLUMNS &&
            rect.bottom <= CANVAS_TOUR_ROWS
        if (!inside) return false
        return listOf(MusicPlace, PhotoPlace).none { it.page == game.page && it.rect.overlaps(rect) }
    }

    private companion object {
        val MusicPlace = CanvasTourPlace(0, GridRect(0, 0, 2, 1))
        val PhotoPlace = CanvasTourPlace(0, GridRect(0, 2, 1, 1))
    }
}
