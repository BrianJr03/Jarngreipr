package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.canvas.data.CanvasLayoutManager

val LocalCanvasLayoutManager = staticCompositionLocalOf<CanvasLayoutManager> {
    error("No CanvasLayoutManager provided")
}
