package jr.brian.jarnfe.library

import jr.brian.core.ui.focus.jarnFocusBorderReach
import kotlin.math.abs
import kotlin.math.sign

/** Below this many items the shelf stops at its ends instead of wrapping. */
internal const val SHELF_WRAP_MIN_ITEMS = 4

/** Neighbors shown on each side of the centered item. */
internal const val SHELF_NEIGHBORS = 2

/** One more slot each side stays composed, so an item can slide out instead of vanishing. */
internal const val SHELF_COMPOSED_REACH = SHELF_NEIGHBORS + 1

internal const val SHELF_NEIGHBOR_ALPHA = 0.55f
internal const val SHELF_CENTER_Y = 0.42f
internal const val SHELF_SETTLE_MS = 150L
internal const val SHELF_BADGE_HOLD_MS = 450L

/** Space under the card for one line (systems: name and game count) or two (games), in dp. */
internal const val SHELF_ONE_LINE_CAPTION = 48f
internal const val SHELF_TWO_LINE_CAPTION = 72f

/** Room left under the card with no caption: enough for its focus border, like the 20dp focus edge. */
internal const val SHELF_NO_CAPTION = 20f

/** Kept between the focus border's furthest reach and a neighbor, in dp. */
internal const val SHELF_BORDER_CLEARANCE = 2f

/** Narrow windows get a wider card, so one partial neighbor peeks on each side. */
private const val SHELF_NARROW_GROWTH = 1.8f

/**
 * How a shelf sizes and spaces its cards, as fractions of the shelf width. The centered card is
 * [widthFraction] wide, neighbors are scaled by [neighborScale], [neighborsBefore] and
 * [neighborsAfter] show on each side, and the card sits near [centerY] of the shelf height.
 */
internal data class LibraryShelfGeometry(
    val widthFraction: Float,
    val aspectRatio: Float,
    val neighborScale: Float,
    val gapFraction: Float,
    val captionReserve: Float,
    val neighborsBefore: Int = SHELF_NEIGHBORS,
    val neighborsAfter: Int = SHELF_NEIGHBORS,
    val centerY: Float = SHELF_CENTER_Y,
) {
    fun cardWidthFraction(narrow: Boolean): Float =
        if (narrow) widthFraction * SHELF_NARROW_GROWTH else widthFraction
}

/** Jarn's own shelf geometry, from the approved "Carousel A, Shelf" mockup. */
internal enum class LibraryShelfKind(val geometry: LibraryShelfGeometry) {
    Systems(
        LibraryShelfGeometry(
            widthFraction = 0.30f,
            aspectRatio = 16f / 10f,
            neighborScale = 0.62f,
            gapFraction = 0.02f,
            captionReserve = SHELF_ONE_LINE_CAPTION,
        ),
    ),
    Games(
        LibraryShelfGeometry(
            widthFraction = 0.17f,
            aspectRatio = 3f / 4f,
            neighborScale = 0.70f,
            gapFraction = 0.016f,
            captionReserve = SHELF_TWO_LINE_CAPTION,
        ),
    ),
}

/** Signed slots from [center] to [index], taking the short way round when the shelf wraps. */
internal fun shelfDistance(index: Int, center: Int, count: Int): Int {
    if (count < SHELF_WRAP_MIN_ITEMS) return index - center
    var distance = Math.floorMod(index - center, count)
    if (distance > count / 2) distance -= count
    return distance
}

/** The next centered index. It wraps with enough items, otherwise it stops at the ends. */
internal fun shelfStep(center: Int, delta: Int, count: Int): Int = when {
    count <= 0 -> 0
    count >= SHELF_WRAP_MIN_ITEMS -> Math.floorMod(center + delta, count)
    else -> (center + delta).coerceIn(0, count - 1)
}

/**
 * The indices worth composing around [center], each listed once even on a short wrapped shelf.
 * One slot past [before] and [after] stays composed, so an item can slide out.
 */
internal fun shelfComposedIndices(
    center: Int,
    count: Int,
    before: Int = SHELF_NEIGHBORS,
    after: Int = SHELF_NEIGHBORS,
): List<Int> {
    if (count <= 0) return emptyList()
    return (-(before + 1)..(after + 1))
        .map { offset ->
            if (count >= SHELF_WRAP_MIN_ITEMS) Math.floorMod(center + offset, count) else center + offset
        }
        .filter { it in 0 until count }
        .distinct()
}

/** Horizontal center of the slot [distance] away from a [card] wide center, in the same unit as [shelfWidth]. */
internal fun shelfSlotX(
    distance: Int,
    shelfWidth: Float,
    geometry: LibraryShelfGeometry,
    card: Float,
    minGap: Float = 0f,
): Float {
    if (distance == 0) return 0f
    val side = card * geometry.neighborScale
    val gap = maxOf(geometry.gapFraction * shelfWidth, minGap)
    val steps = abs(distance)
    return distance.sign * (card / 2 + gap + side / 2 + (steps - 1) * (side + gap))
}

internal fun shelfSlotScale(distance: Int, geometry: LibraryShelfGeometry): Float =
    if (distance == 0) 1f else geometry.neighborScale

/** Full for the centered card, dimmed for the [before] and [after] neighbors, hidden past them. */
internal fun shelfSlotAlpha(
    distance: Int,
    before: Int = SHELF_NEIGHBORS,
    after: Int = SHELF_NEIGHBORS,
): Float = when (distance) {
    0 -> 1f
    in -before..after -> SHELF_NEIGHBOR_ALPHA
    else -> 0f
}

/**
 * One animated value drives both: below the neighbor dim the whole slot fades in or out, and
 * above it only the art brightens, so a card's background never changes for focus.
 */
internal fun shelfSlotVisibility(slotAlpha: Float): Float = (slotAlpha / SHELF_NEIGHBOR_ALPHA).coerceAtMost(1f)

internal fun shelfArtAlpha(slotAlpha: Float): Float = maxOf(slotAlpha, SHELF_NEIGHBOR_ALPHA)

/** The gap a centered card [cardWidth] by [cardHeight] needs so its focus border clears a neighbor. */
internal fun shelfMinGap(cardWidth: Float, cardHeight: Float): Float =
    jarnFocusBorderReach(cardWidth, cardHeight).sides + SHELF_BORDER_CLEARANCE

/** A move of more than one slot is a wrap, which snaps instead of sliding across the row. */
internal fun shelfSlotJumps(previous: Int?, next: Int): Boolean =
    previous != null && abs(previous - next) > 1

/** The letter shown while scrolling fast. Anything that isn't a letter reads as "#". */
internal fun shelfBadgeLetter(title: String): String {
    val first = title.trimStart().firstOrNull() ?: return "#"
    return if (first.isLetter()) first.uppercase() else "#"
}

/** Where the centered card sits, in the same unit as the shelf size passed to [shelfFit]. */
internal data class ShelfFit(val cardWidth: Float, val cardHeight: Float, val centerY: Float)

/**
 * Fits the card into whatever height is left, for example under the Continue playing card.
 * [topMargin] leaves room for the focus float, and [captionReserve] keeps the title and its gap
 * inside the shelf. The card keeps [geometry]'s width when there is room and sits near its
 * [LibraryShelfGeometry.centerY], and otherwise shrinks and centers in the space it has.
 */
internal fun shelfFit(
    width: Float,
    height: Float,
    geometry: LibraryShelfGeometry,
    narrow: Boolean,
    topMargin: Float,
    captionReserve: Float,
): ShelfFit {
    val area = (height - topMargin - captionReserve).coerceAtLeast(0f)
    val cardHeight = minOf(width * geometry.cardWidthFraction(narrow) / geometry.aspectRatio, area)
    val highest = topMargin + cardHeight / 2
    val lowest = topMargin + area - cardHeight / 2
    val centerY = (height * geometry.centerY).coerceIn(highest, maxOf(highest, lowest))
    return ShelfFit(cardHeight * geometry.aspectRatio, cardHeight, centerY)
}
