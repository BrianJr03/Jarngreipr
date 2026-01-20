package jr.brian.home.util

import jr.brian.home.model.ItemRect

fun checkCollision(
    x1: Float, y1: Float, w1: Float, h1: Float,
    x2: Float, y2: Float, w2: Float, h2: Float
): Boolean {
    return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2
}

fun findNonOverlappingPosition(
    targetX: Float,
    targetY: Float,
    targetSize: Float,
    excludeId: String,
    allItems: List<ItemRect>,
    containerWidth: Float,
    containerHeight: Float,
    borderPadding: Float
): Pair<Float, Float> {
    val minGap = 8f // Minimum gap between items

    // Helper to check if position is valid (no collision)
    fun isPositionFree(x: Float, y: Float): Boolean {
        if (x < borderPadding || x + targetSize > containerWidth - borderPadding ||
            y < borderPadding || y + targetSize > containerHeight - borderPadding
        ) {
            return false
        }
        return allItems.none { item ->
            item.id != excludeId && checkCollision(
                x, y, targetSize, targetSize,
                item.x - minGap, item.y - minGap,
                item.width + minGap * 2, item.height + minGap * 2
            )
        }
    }

    // Check if current position has no collision
    if (isPositionFree(targetX, targetY)) {
        return Pair(targetX, targetY)
    }

    // Try pushing in each direction from the collision
    allItems.forEach { item ->
        if (item.id != excludeId && checkCollision(
                targetX, targetY, targetSize, targetSize,
                item.x - minGap, item.y - minGap,
                item.width + minGap * 2, item.height + minGap * 2
            )
        ) {
            // Try positions around this item
            val positions = listOf(
                Pair(item.x + item.width + minGap, targetY),  // Right of item
                Pair(item.x - targetSize - minGap, targetY),  // Left of item
                Pair(targetX, item.y + item.height + minGap), // Below item
                Pair(targetX, item.y - targetSize - minGap)   // Above item
            )

            for ((testX, testY) in positions) {
                if (isPositionFree(testX, testY)) {
                    return Pair(testX, testY)
                }
            }
        }
    }

    // Grid search for a free spot
    val gridStep = targetSize + minGap
    val maxColumns = ((containerWidth - borderPadding * 2) / gridStep).toInt()
    val maxRows = ((containerHeight - borderPadding * 2) / gridStep).toInt()

    for (row in 0 until maxRows) {
        for (col in 0 until maxColumns) {
            val testX = borderPadding + col * gridStep
            val testY = borderPadding + row * gridStep

            if (isPositionFree(testX, testY)) {
                return Pair(testX, testY)
            }
        }
    }

    // If no free spot found in grid, try to find any available space
    // by scanning with smaller increments
    val fineStep = targetSize / 2
    var testY = borderPadding
    while (testY + targetSize <= containerHeight - borderPadding) {
        var testX = borderPadding
        while (testX + targetSize <= containerWidth - borderPadding) {
            if (isPositionFree(testX, testY)) {
                return Pair(testX, testY)
            }
            testX += fineStep
        }
        testY += fineStep
    }

    // Fallback: return original position (will overlap, but better than nothing)
    return Pair(targetX, targetY)
}
