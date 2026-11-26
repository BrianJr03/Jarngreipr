package jr.brian.jarnfe.audio

/**
 * Whether Jarn FE may make sound right now. One gate serves both windows. A covered window means
 * something else, usually a game, is on that screen, even if the other window is tapped.
 * [previewGameId] is the game whose preview will play with sound, which silences music and
 * jingles from the moment it is wanted, not just once it is audible.
 */
internal data class JarnAudioGate(
    val topWindows: Set<Int> = emptySet(),
    val coveredWindows: Set<Int> = emptySet(),
    val videoOpen: Boolean = false,
    val otherMediaPlaying: Boolean = false,
    val previewGameId: String? = null,
) {
    /** Everything but a preview's own claim, so a claimed preview can tell whether it may sound. */
    val previewMayPlay: Boolean
        get() = topWindows.isNotEmpty() && coveredWindows.isEmpty() && !videoOpen && !otherMediaPlaying

    val allowed: Boolean
        get() = previewMayPlay && previewGameId == null

    fun withTop(window: Int, isTop: Boolean): JarnAudioGate =
        copy(topWindows = if (isTop) topWindows + window else topWindows - window)

    fun withCovered(window: Int, isCovered: Boolean): JarnAudioGate =
        copy(coveredWindows = if (isCovered) coveredWindows + window else coveredWindows - window)
}

internal sealed interface JarnJingleStep {
    data object None : JarnJingleStep
    data object Stop : JarnJingleStep
    data class Play(val gameId: String) : JarnJingleStep
}

/**
 * Turns the selected game into one jingle per selection. Returning from a game or a hold keeps
 * the selection, so the same jingle does not play again until the player moves away and back.
 * A game whose preview plays with sound counts as played, so its jingle never follows the preview.
 */
internal class JarnJingleTrigger {
    private var lastGameId: String? = null
    private var sounding = false

    fun next(gameId: String?, allowed: Boolean, previewGameId: String? = null): JarnJingleStep = when {
        gameId != null && gameId == previewGameId -> {
            lastGameId = gameId
            silence()
        }
        !allowed -> silence()
        gameId == null -> {
            lastGameId = null
            silence()
        }
        gameId == lastGameId -> JarnJingleStep.None
        else -> {
            lastGameId = gameId
            sounding = true
            JarnJingleStep.Play(gameId)
        }
    }

    private fun silence(): JarnJingleStep {
        if (!sounding) return JarnJingleStep.None
        sounding = false
        return JarnJingleStep.Stop
    }
}
