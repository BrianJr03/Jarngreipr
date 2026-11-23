package jr.brian.jarnfe.wallpaper

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.core.ui.theme.JarnColors
import jr.brian.jarnfe.session.JarnSession

@Composable
internal fun rememberJarnWallpaperIsBottomScreen(): Boolean {
    val windowDisplayId = LocalView.current.display?.displayId
    val hasBothScreens by remember {
        derivedStateOf {
            val state = JarnSession.state
            state.deckDisplayAvailable && state.deckHostReady
        }
    }
    return jarnWallpaperIsBottomScreen(windowDisplayId, PRIMARY_DISPLAY_ID, hasBothScreens)
}

internal fun JarnColors.toJarnWallpaperPalette(): JarnWallpaperPalette = jarnWallpaperPalette(
    bg = bg.toArgb(),
    accent = accent.toArgb(),
    focus = focus.toArgb(),
    textMid = textMid.toArgb(),
    textLow = textLow.toArgb(),
)

/** The dim bands that protect each screen's header and hints. */
internal class JarnWallpaperBand(palette: JarnWallpaperPalette, hasDeckUi: Boolean) {
    private val stops = jarnWallpaperBandStops(hasDeckUi)
    private val colors = jarnWallpaperBandColors(palette, hasDeckUi)
    private val paint = Paint()
    private var shaderHeight = -1f

    fun draw(canvas: Canvas, width: Float, height: Float) {
        if (shaderHeight != height) {
            paint.shader = LinearGradient(0f, 0f, 0f, height, colors, stops, Shader.TileMode.CLAMP)
            shaderHeight = height
        }
        canvas.drawRect(0f, 0f, width, height, paint)
    }
}
