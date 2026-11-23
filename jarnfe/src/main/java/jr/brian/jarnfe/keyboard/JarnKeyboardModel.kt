package jr.brian.jarnfe.keyboard

internal enum class JarnKeyboardPage { Lower, Upper, Symbols }

internal sealed interface JarnKeyboardKey {
    data class Letter(val char: Char) : JarnKeyboardKey
    data object Shift : JarnKeyboardKey
    data object Symbols : JarnKeyboardKey
    data object Space : JarnKeyboardKey
    data object Delete : JarnKeyboardKey
    data object Clear : JarnKeyboardKey
    data object Done : JarnKeyboardKey
}

internal val JarnKeyboardControls = listOf(
    JarnKeyboardKey.Shift,
    JarnKeyboardKey.Symbols,
    JarnKeyboardKey.Space,
    JarnKeyboardKey.Delete,
    JarnKeyboardKey.Done,
)

private val LowerRows = listOf("1234567890", "qwertyuiop", "asdfghjkl-", "zxcvbnm_.@")
private val SymbolRows = listOf("!@#\$%^&*()", "-_=+[]{}\\|", ";:'\",.<>/?", "`~")

internal fun jarnKeyboardRows(page: JarnKeyboardPage): List<List<JarnKeyboardKey.Letter>> {
    val rows = when (page) {
        JarnKeyboardPage.Lower -> LowerRows
        JarnKeyboardPage.Upper -> LowerRows.map { it.uppercase() }
        JarnKeyboardPage.Symbols -> SymbolRows
    }
    return rows.map(::jarnKeyRow)
}

internal fun jarnKeyRow(row: String): List<JarnKeyboardKey.Letter> = row.map(JarnKeyboardKey::Letter)

/** A key's place in the grid, kept so focus survives a page with shorter rows. */
internal data class JarnKeyPosition(val row: Int, val column: Int) {
    fun clampedTo(rows: List<List<*>>): JarnKeyPosition {
        if (rows.isEmpty()) return this
        val clampedRow = row.coerceIn(rows.indices)
        return JarnKeyPosition(clampedRow, column.coerceIn(0, (rows[clampedRow].size - 1).coerceAtLeast(0)))
    }
}

/** Text entry state for a controller. Its text form is redacted because it may hold a secret. */
internal class JarnKeyboardState(
    text: String = "",
    val page: JarnKeyboardPage = JarnKeyboardPage.Lower,
    private val maxLength: Int = MAX_LENGTH,
) {
    val text: String = text.take(maxLength)

    fun press(key: JarnKeyboardKey): JarnKeyboardState = when (key) {
        is JarnKeyboardKey.Letter -> append(key.char)
        JarnKeyboardKey.Space -> append(' ')
        JarnKeyboardKey.Delete -> copy(text = text.dropLast(1))
        JarnKeyboardKey.Clear -> copy(text = "")
        JarnKeyboardKey.Shift -> copy(
            page = if (page == JarnKeyboardPage.Lower) JarnKeyboardPage.Upper else JarnKeyboardPage.Lower,
        )
        JarnKeyboardKey.Symbols -> copy(
            page = if (page == JarnKeyboardPage.Symbols) JarnKeyboardPage.Lower else JarnKeyboardPage.Symbols,
        )
        JarnKeyboardKey.Done -> this
    }

    private fun append(char: Char) = if (text.length >= maxLength) this else copy(text = text + char)

    private fun copy(text: String = this.text, page: JarnKeyboardPage = this.page) =
        JarnKeyboardState(text, page, maxLength)

    override fun toString(): String = "JarnKeyboardState(redacted)"

    companion object {
        const val MAX_LENGTH = 128
    }
}
