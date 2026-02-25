package jr.brian.home.ui.components.konfetti

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.xml.image.ImageUtil

object KonfettiShapeFactory {
    private const val BITMAP_SIZE = 128
    private const val TEXT_SIZE = 96f

    fun createCharShape(
        context: Context,
        char: Char,
        tint: Boolean = true,
        applyAlpha: Boolean = true
    ): Shape.DrawableShape {
        val bitmap = createBitmap(BITMAP_SIZE, BITMAP_SIZE)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = TEXT_SIZE
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val x = BITMAP_SIZE / 2f
        val y = BITMAP_SIZE / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(char.toString(), x, y, paint)

        val drawable = bitmap.toDrawable(context.resources)
        return ImageUtil.loadDrawable(drawable, tint, applyAlpha)
    }
}
