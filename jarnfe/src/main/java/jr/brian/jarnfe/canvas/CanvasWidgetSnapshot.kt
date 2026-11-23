package jr.brian.jarnfe.canvas

import android.graphics.Picture
import android.view.View
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/** A recording rather than a bitmap, so hardware bitmaps inside a widget still draw. */
private fun View.recordPicture(): Picture? {
    if (width <= 0 || height <= 0) return null
    return runCatching {
        Picture().also { picture ->
            val canvas = picture.beginRecording(width, height)
            try {
                draw(canvas)
            } finally {
                picture.endRecording()
            }
        }
    }.getOrNull()
}

/**
 * Shows a still of the widget while it rides above the board, and hides the live view with alpha
 * so it stays attached and measured and keeps the finger. If the still fails, the live view shows.
 */
@Composable
internal fun BoxScope.CanvasWidgetFreeze(view: View?, frozen: Boolean) {
    val picture = remember(view, frozen) { if (frozen) view?.recordPicture() else null }
    DisposableEffect(view, picture) {
        if (picture != null) view?.alpha = 0f
        onDispose { view?.alpha = 1f }
    }
    picture?.let { still ->
        Spacer(
            Modifier
                .matchParentSize()
                .drawBehind { drawIntoCanvas { it.nativeCanvas.drawPicture(still) } },
        )
    }
}
