package jr.brian.jarnfe.library.parity

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.ProvideJarnInput
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import java.io.File
import kotlin.math.roundToInt

/**
 * The D-pad walk every parity case takes. It reaches the second row, comes back, and crosses
 * both ends, so wrap and edge stops are part of the snapshot.
 */
internal val BrowseParitySteps: List<Key> = listOf(
    Key.DirectionRight,
    Key.DirectionRight,
    Key.DirectionDown,
    Key.DirectionRight,
    Key.DirectionDown,
    Key.DirectionLeft,
    Key.DirectionLeft,
    Key.DirectionUp,
    Key.DirectionLeft,
    Key.DirectionLeft,
    Key.DirectionLeft,
    Key.DirectionUp,
)

private val GOLDEN_DIR = File("src/test/resources/parity")

/** Every tile and every piece of text on screen, focusable or not. */
private val isSnapshotNode = SemanticsMatcher("is focusable or labeled") { node ->
    SemanticsProperties.Focused in node.config || node.label().isNotEmpty()
}

/**
 * Renders [render] inside Jarn's theme, records every focusable node's label and bounds, then
 * walks [steps] and records where focus lands after each one. The result must equal the checked
 * in golden `parity/<golden>.txt`. A missing golden is written from this run and the test fails,
 * so a new baseline is always a deliberate, reviewed change: delete the file and run again.
 *
 * The theme engine rebuild calls this with its own [render] and today's [golden] to prove that
 * browse looks and moves the same.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeContentTestRule.assertBrowseParity(
    golden: String,
    steps: List<Key> = BrowseParitySteps,
    render: @Composable () -> Unit,
) {
    setContent { JarnTheme { ProvideJarnInput(JarnInputRouter(), JarnScreen.Stage) { render() } } }
    waitForIdle()
    val actual = captureBrowseSnapshot(golden, steps)
    val file = File(GOLDEN_DIR, "$golden.txt")
    if (!file.exists()) {
        file.parentFile?.mkdirs()
        file.writeText(actual)
        fail("Wrote a new browse parity golden to ${file.absolutePath}. Review it and run again.")
    }
    assertEquals("Browse parity changed for $golden", file.readText(), actual)
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeContentTestRule.captureBrowseSnapshot(golden: String, steps: List<Key>): String =
    buildString {
        appendLine("# Browse parity golden: $golden")
        appendLine("# Bounds are px in root, left,top,right,bottom. * marks a focusable node.")
        appendLine("[nodes]")
        snapshotNodes().forEach { appendLine(it.describe(markFocusable = true)) }
        appendLine("[focus]")
        appendLine("start -> ${focusedNode()}")
        steps.forEach { key ->
            onRoot().performKeyInput { pressKey(key) }
            waitForIdle()
            appendLine("${key.stepName()} -> ${focusedNode()}")
        }
    }

private fun ComposeContentTestRule.snapshotNodes(): List<SemanticsNode> =
    onAllNodes(isSnapshotNode).fetchSemanticsNodes()
        .sortedWith(compareBy({ it.label() }, { it.boundsInRoot.top }, { it.boundsInRoot.left }))

private fun ComposeContentTestRule.focusedNode(): String =
    onAllNodes(isFocused()).fetchSemanticsNodes().singleOrNull()?.describe() ?: "<none>"

private fun SemanticsNode.describe(markFocusable: Boolean = false): String {
    val mark = if (markFocusable && SemanticsProperties.Focused in config) "* " else ""
    return "$mark${label().ifEmpty { "<unlabeled>" }} ${boundsInRoot.format()}"
}

private fun SemanticsNode.label(): String {
    val config = config
    config.getOrNull(SemanticsProperties.TestTag)?.let { return "tag:$it" }
    config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" | ")?.let { return it }
    return config.getOrNull(SemanticsProperties.Text)?.joinToString(" | ") { it.text }.orEmpty()
}

private fun Rect.format(): String =
    "[${left.roundToInt()},${top.roundToInt()},${right.roundToInt()},${bottom.roundToInt()}]"

private fun Key.stepName(): String = when (this) {
    Key.DirectionRight -> "Right"
    Key.DirectionLeft -> "Left"
    Key.DirectionUp -> "Up"
    Key.DirectionDown -> "Down"
    else -> toString()
}
