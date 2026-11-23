package jr.brian.jarnfe.library

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil.decode.BitmapFactoryDecoder
import coil.decode.SvgDecoder
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private const val WALL_CACHE_MAX_BYTES = 20 * 1024 * 1024
private const val UNAVAILABLE_KEYS_MAX = 16
private const val SVG_SUFFIX = ".svg"
private const val TITLE_TEXT_FRACTION = 0.085f
private const val TITLE_PADDING_FRACTION = 0.1f
private const val TITLE_MAX_LINES = 3

private val StillImageDecoder = BitmapFactoryDecoder.Factory()
private val VectorImageDecoder = SvgDecoder.Factory()

/** Walls are full-screen bitmaps, so only the latest two stay and memory pressure drops them. */
internal object LibraryCoverWallCache : ComponentCallbacks2 {
    private val walls = object : LruCache<LibraryCoverWallKey, ImageBitmap>(WALL_CACHE_MAX_BYTES) {
        override fun sizeOf(key: LibraryCoverWallKey, value: ImageBitmap): Int =
            value.asAndroidBitmap().allocationByteCount
    }
    private val unavailable = LruCache<LibraryCoverWallKey, Boolean>(UNAVAILABLE_KEYS_MAX)
    private var isRegistered = false

    fun attach(context: Context) {
        if (isRegistered) return
        context.applicationContext.registerComponentCallbacks(this)
        isRegistered = true
    }

    operator fun get(key: LibraryCoverWallKey): ImageBitmap? = walls.get(key)

    fun isUnavailable(key: LibraryCoverWallKey): Boolean = unavailable.get(key) == true

    fun store(key: LibraryCoverWallKey, wall: ImageBitmap?) {
        if (wall == null) unavailable.put(key, true) else walls.put(key, wall)
    }

    fun keepOnly(key: LibraryCoverWallKey) {
        walls.snapshot().keys.filter { it != key }.forEach(walls::remove)
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) walls.evictAll()
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() = walls.evictAll()
}

/** Null when too few covers could be read, so the caller's wallpaper stays visible. */
internal suspend fun renderLibraryCoverWall(context: Context, key: LibraryCoverWallKey): ImageBitmap? {
    val layout = libraryCoverWallLayout(key.size, key.covers.size)
    val cellWidth = layout.cellWidth.roundToInt().coerceAtLeast(1)
    val cellHeight = layout.cellHeight.roundToInt().coerceAtLeast(1)
    val images = key.covers.map { cover ->
        cover.path?.let { decodeWallCover(context, it, cellWidth, cellHeight) }
    }
    if (images.count { it != null } < LIBRARY_WALL_MIN_COVERS) return null
    return withContext(Dispatchers.Default) {
        val wall = Bitmap.createBitmap(key.size.width, key.size.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(wall)
        canvas.drawColor(key.palette.base)
        canvas.save()
        canvas.translate(key.size.width / 2f, key.size.height / 2f)
        canvas.rotate(layout.rotationDegrees)
        val painter = WallCellPainter(canvas, layout, key.palette)
        layout.cells.forEach { cell ->
            ensureActive()
            val cover = key.covers[cell.coverIndex]
            painter.draw(cell, images[cell.coverIndex], cover.title)
        }
        canvas.restore()
        canvas.drawColor(key.palette.overlay)
        wall.prepareToDraw()
        wall.asImageBitmap()
    }
}

private suspend fun decodeWallCover(context: Context, path: String, width: Int, height: Int): Bitmap? {
    val decoder = if (path.endsWith(SVG_SUFFIX, ignoreCase = true)) VectorImageDecoder else StillImageDecoder
    val request = ImageRequest.Builder(context)
        .data(path)
        .decoderFactory(decoder)
        .size(width, height)
        .scale(Scale.FILL)
        .allowHardware(false)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .build()
    val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
    return (result.drawable as? BitmapDrawable)?.bitmap
}

private class WallCellPainter(
    private val canvas: Canvas,
    private val layout: LibraryCoverWallLayout,
    palette: LibraryCoverWallPalette,
) {
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.card }
    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.text
        typeface = Typeface.DEFAULT_BOLD
        textSize = layout.cellHeight * TITLE_TEXT_FRACTION
    }
    private val titleLayouts = HashMap<String, StaticLayout>()
    private val source = Rect()
    private val target = RectF()

    fun draw(cell: LibraryWallCell, image: Bitmap?, title: String) {
        val halfWidth = layout.cellWidth / 2f
        val halfHeight = layout.cellHeight / 2f
        target.set(
            cell.centerX - halfWidth,
            cell.centerY - halfHeight,
            cell.centerX + halfWidth,
            cell.centerY + halfHeight,
        )
        if (image == null) drawTitleCard(title) else drawCover(image)
    }

    private fun drawCover(image: Bitmap) {
        val scale = max(target.width() / image.width, target.height() / image.height)
        val cropWidth = (target.width() / scale).roundToInt().coerceIn(1, image.width)
        val cropHeight = (target.height() / scale).roundToInt().coerceIn(1, image.height)
        val left = (image.width - cropWidth) / 2
        val top = (image.height - cropHeight) / 2
        source.set(left, top, left + cropWidth, top + cropHeight)
        canvas.drawBitmap(image, source, target, imagePaint)
    }

    private fun drawTitleCard(title: String) {
        canvas.drawRect(target, cardPaint)
        val text = titleLayouts.getOrPut(title) { titleLayout(title) }
        canvas.save()
        canvas.translate(
            target.centerX() - text.width / 2f,
            target.centerY() - text.height / 2f,
        )
        text.draw(canvas)
        canvas.restore()
    }

    private fun titleLayout(title: String): StaticLayout {
        val padding = layout.cellWidth * TITLE_PADDING_FRACTION
        val width = (layout.cellWidth - padding * 2f).roundToInt().coerceAtLeast(1)
        return StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setMaxLines(TITLE_MAX_LINES)
            .build()
    }
}
