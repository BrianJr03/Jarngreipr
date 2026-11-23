package jr.brian.jarnfe.scraper

internal enum class JarnKeyboardPage { Lower, Upper, Symbols }

internal sealed interface JarnKeyboardKey {
    data class Letter(val char: Char) : JarnKeyboardKey
    data object Shift : JarnKeyboardKey
    data object Symbols : JarnKeyboardKey
    data object Space : JarnKeyboardKey
    data object Delete : JarnKeyboardKey
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
    return rows.map { row -> row.map(JarnKeyboardKey::Letter) }
}

/** Text entry state for a controller. Its text form is redacted because it may hold a secret. */
internal class JarnKeyboardState(
    val text: String = "",
    val page: JarnKeyboardPage = JarnKeyboardPage.Lower,
) {
    fun press(key: JarnKeyboardKey): JarnKeyboardState = when (key) {
        is JarnKeyboardKey.Letter -> append(key.char)
        JarnKeyboardKey.Space -> append(' ')
        JarnKeyboardKey.Delete -> JarnKeyboardState(text.dropLast(1), page)
        JarnKeyboardKey.Shift -> JarnKeyboardState(
            text,
            if (page == JarnKeyboardPage.Lower) JarnKeyboardPage.Upper else JarnKeyboardPage.Lower,
        )
        JarnKeyboardKey.Symbols -> JarnKeyboardState(
            text,
            if (page == JarnKeyboardPage.Symbols) JarnKeyboardPage.Lower else JarnKeyboardPage.Symbols,
        )
        JarnKeyboardKey.Done -> this
    }

    private fun append(char: Char) =
        if (text.length >= MAX_LENGTH) this else JarnKeyboardState(text + char, page)

    override fun toString(): String = "JarnKeyboardState(redacted)"

    companion object {
        const val MAX_LENGTH = 128
    }
}
