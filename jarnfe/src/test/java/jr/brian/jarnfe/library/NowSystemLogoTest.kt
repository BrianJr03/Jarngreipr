package jr.brian.jarnfe.library

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NowSystemLogoTest {
    private val plate = Color(0xFF0E1419)
    private val purple = 0xFF3B2A8C.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val clear = 0x00000000

    @Test
    fun darkLogoGetsBacking() = assertTrue(nowLogoNeedsBacking(intArrayOf(purple, purple, clear), plate))

    @Test
    fun lightLogoDoesNot() = assertFalse(nowLogoNeedsBacking(intArrayOf(white, white, clear), plate))

    @Test
    fun transparentPixelsAreIgnored() = assertFalse(nowLogoNeedsBacking(intArrayOf(white, clear, clear, clear), plate))

    @Test
    fun blankLogoDoesNot() = assertFalse(nowLogoNeedsBacking(intArrayOf(clear, clear), plate))
}
