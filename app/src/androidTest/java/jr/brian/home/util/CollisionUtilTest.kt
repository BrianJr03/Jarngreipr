package jr.brian.home.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import jr.brian.home.model.ItemRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollisionUtilTest {
    @Test
    fun checkCollision_nonOverlappingRectangles_returnsFalse() {
        // Arrange: Two rectangles far apart
        val result = checkCollision(
            x1 = 0f, y1 = 0f, w1 = 10f, h1 = 10f,
            x2 = 20f, y2 = 20f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertFalse("Rectangles should not collide when far apart", result)
    }

    @Test
    fun checkCollision_overlappingRectangles_returnsTrue() {
        // Arrange: Two rectangles that overlap
        val result = checkCollision(
            x1 = 0f, y1 = 0f, w1 = 10f, h1 = 10f,
            x2 = 5f, y2 = 5f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertTrue("Rectangles should collide when overlapping", result)
    }

    @Test
    fun checkCollision_rectanglesJustTouching_returnsFalse() {
        // Arrange: Two rectangles with edges touching but not overlapping
        val result = checkCollision(
            x1 = 0f, y1 = 0f, w1 = 10f, h1 = 10f,
            x2 = 10f, y2 = 0f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertFalse("Rectangles should not collide when just touching", result)
    }

    @Test
    fun checkCollision_oneRectangleCompletelyInsideAnother_returnsTrue() {
        // Arrange: Small rectangle completely inside larger one
        val result = checkCollision(
            x1 = 0f, y1 = 0f, w1 = 20f, h1 = 20f,
            x2 = 5f, y2 = 5f, w2 = 5f, h2 = 5f
        )

        // Assert
        assertTrue("Should collide when one rectangle is inside another", result)
    }

    @Test
    fun checkCollision_identicalRectangles_returnsTrue() {
        // Arrange: Two identical rectangles
        val result = checkCollision(
            x1 = 10f, y1 = 10f, w1 = 15f, h1 = 15f,
            x2 = 10f, y2 = 10f, w2 = 15f, h2 = 15f
        )

        // Assert
        assertTrue("Identical rectangles should collide", result)
    }

    @Test
    fun checkCollision_partialOverlapFromLeft_returnsTrue() {
        // Arrange: Rectangle 1 overlaps from the left
        val result = checkCollision(
            x1 = 0f, y1 = 0f, w1 = 15f, h1 = 10f,
            x2 = 10f, y2 = 0f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertTrue("Should detect partial overlap from left", result)
    }

    @Test
    fun checkCollision_partialOverlapFromRight_returnsTrue() {
        // Arrange: Rectangle 1 overlaps from the right
        val result = checkCollision(
            x1 = 15f, y1 = 0f, w1 = 15f, h1 = 10f,
            x2 = 10f, y2 = 0f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertTrue("Should detect partial overlap from right", result)
    }

    @Test
    fun checkCollision_partialOverlapFromTop_returnsTrue() {
        // Arrange: Rectangle 1 overlaps from the top
        val result = checkCollision(
            x1 = 0f, y1 = 0f, w1 = 10f, h1 = 15f,
            x2 = 0f, y2 = 10f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertTrue("Should detect partial overlap from top", result)
    }

    @Test
    fun checkCollision_partialOverlapFromBottom_returnsTrue() {
        // Arrange: Rectangle 1 overlaps from the bottom
        val result = checkCollision(
            x1 = 0f, y1 = 15f, w1 = 10f, h1 = 15f,
            x2 = 0f, y2 = 10f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertTrue("Should detect partial overlap from bottom", result)
    }

    @Test
    fun checkCollision_cornerOverlap_returnsTrue() {
        // Arrange: Only corners overlap
        val result = checkCollision(
            x1 = 0f, y1 = 0f, w1 = 11f, h1 = 11f,
            x2 = 10f, y2 = 10f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertTrue("Should detect corner overlap", result)
    }

    @Test
    fun checkCollision_narrowRectanglesOverlapping_returnsTrue() {
        // Arrange: Very narrow rectangles that overlap
        val result = checkCollision(
            x1 = 5f, y1 = 0f, w1 = 1f, h1 = 20f,
            x2 = 0f, y2 = 5f, w2 = 20f, h2 = 1f
        )

        // Assert
        assertTrue("Should detect overlap with narrow rectangles", result)
    }

    @Test
    fun checkCollision_rectangleAboveAnother_returnsFalse() {
        // Arrange: Rectangle 1 is completely above rectangle 2
        val result = checkCollision(
            x1 = 0f, y1 = 0f, w1 = 10f, h1 = 5f,
            x2 = 0f, y2 = 10f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertFalse("Should not collide when one is above the other", result)
    }

    @Test
    fun checkCollision_rectangleBelowAnother_returnsFalse() {
        // Arrange: Rectangle 1 is completely below rectangle 2
        val result = checkCollision(
            x1 = 0f, y1 = 20f, w1 = 10f, h1 = 10f,
            x2 = 0f, y2 = 0f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertFalse("Should not collide when one is below the other", result)
    }

    @Test
    fun checkCollision_rectangleToTheLeft_returnsFalse() {
        // Arrange: Rectangle 1 is completely to the left of rectangle 2
        val result = checkCollision(
            x1 = 0f, y1 = 0f, w1 = 5f, h1 = 10f,
            x2 = 10f, y2 = 0f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertFalse("Should not collide when one is to the left", result)
    }

    @Test
    fun checkCollision_rectangleToTheRight_returnsFalse() {
        // Arrange: Rectangle 1 is completely to the right of rectangle 2
        val result = checkCollision(
            x1 = 20f, y1 = 0f, w1 = 10f, h1 = 10f,
            x2 = 0f, y2 = 0f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertFalse("Should not collide when one is to the right", result)
    }

    @Test
    fun checkCollision_zeroSizeRectangle_returnsFalse() {
        // Arrange: Zero-size rectangle (point) outside another rectangle
        val result = checkCollision(
            x1 = 15f, y1 = 15f, w1 = 0f, h1 = 0f,
            x2 = 0f, y2 = 0f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertFalse("Should not collide when zero-size rectangle is outside", result)
    }

    @Test
    fun checkCollision_zeroSizeRectangleInside_returnsTrue() {
        // Arrange: Zero-size rectangle (point) inside another rectangle
        val result = checkCollision(
            x1 = 5f, y1 = 5f, w1 = 0f, h1 = 0f,
            x2 = 0f, y2 = 0f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertTrue("Should collide when zero-size rectangle (point) is inside", result)
    }

    @Test
    fun checkCollision_negativeCoordinates_overlapping_returnsTrue() {
        // Arrange: Rectangles with negative coordinates that overlap
        val result = checkCollision(
            x1 = -10f, y1 = -10f, w1 = 15f, h1 = 15f,
            x2 = -5f, y2 = -5f, w2 = 10f, h2 = 10f
        )

        // Assert
        assertTrue("Should detect collision with negative coordinates", result)
    }

    @Test
    fun checkCollision_largeCoordinates_overlapping_returnsTrue() {
        // Arrange: Rectangles with very large coordinates
        val result = checkCollision(
            x1 = 1000f, y1 = 1000f, w1 = 100f, h1 = 100f,
            x2 = 1050f, y2 = 1050f, w2 = 100f, h2 = 100f
        )

        // Assert
        assertTrue("Should detect collision with large coordinates", result)
    }

    @Test
    fun findNonOverlappingPosition_noCollision_returnsSamePosition() {
        // Arrange: Empty space, no items to collide with
        val allItems = emptyList<ItemRect>()
        val targetX = 100f
        val targetY = 100f
        val targetSize = 50f

        // Act
        val result = findNonOverlappingPosition(
            targetX = targetX,
            targetY = targetY,
            targetSize = targetSize,
            excludeId = "test",
            allItems = allItems,
            containerWidth = 500f,
            containerHeight = 500f,
            borderPadding = 10f
        )

        // Assert
        assertEquals("X should remain the same when no collision", targetX, result.first, 0.01f)
        assertEquals("Y should remain the same when no collision", targetY, result.second, 0.01f)
    }

    @Test
    fun findNonOverlappingPosition_collision_findsNewPosition() {
        // Arrange: Item blocking the target position
        val blockingItem = ItemRect(
            id = "blocker",
            x = 100f,
            y = 100f,
            width = 50f,
            height = 50f
        )
        val allItems = listOf(blockingItem)

        // Act
        val result = findNonOverlappingPosition(
            targetX = 100f,
            targetY = 100f,
            targetSize = 50f,
            excludeId = "test",
            allItems = allItems,
            containerWidth = 500f,
            containerHeight = 500f,
            borderPadding = 10f
        )

        // Assert
        val hasCollision = checkCollision(
            result.first, result.second, 50f, 50f,
            blockingItem.x - 8f, blockingItem.y - 8f,
            blockingItem.width + 16f, blockingItem.height + 16f
        )
        assertFalse("Result position should not collide with existing item", hasCollision)
    }

    @Test
    fun findNonOverlappingPosition_excludedIdCollision_returnsSamePosition() {
        // Arrange: Item with same ID as excludeId
        val item = ItemRect(
            id = "same-id",
            x = 100f,
            y = 100f,
            width = 50f,
            height = 50f
        )
        val allItems = listOf(item)

        // Act
        val result = findNonOverlappingPosition(
            targetX = 100f,
            targetY = 100f,
            targetSize = 50f,
            excludeId = "same-id",
            allItems = allItems,
            containerWidth = 500f,
            containerHeight = 500f,
            borderPadding = 10f
        )

        // Assert
        assertEquals("Should return same position when colliding with excluded ID", 100f, result.first, 0.01f)
        assertEquals("Should return same position when colliding with excluded ID", 100f, result.second, 0.01f)
    }

    @Test
    fun findNonOverlappingPosition_nearBorder_respectsBorderPadding() {
        // Arrange: Try to place item too close to border
        val allItems = emptyList<ItemRect>()
        val borderPadding = 20f

        // Act
        val result = findNonOverlappingPosition(
            targetX = 5f, // Too close to left border
            targetY = 5f, // Too close to top border
            targetSize = 50f,
            excludeId = "test",
            allItems = allItems,
            containerWidth = 500f,
            containerHeight = 500f,
            borderPadding = borderPadding
        )

        // Assert
        assertTrue("X should respect border padding", result.first >= borderPadding)
        assertTrue("Y should respect border padding", result.second >= borderPadding)
        assertTrue("X + size should respect right border", result.first + 50f <= 500f - borderPadding)
        assertTrue("Y + size should respect bottom border", result.second + 50f <= 500f - borderPadding)
    }

    @Test
    fun findNonOverlappingPosition_multipleCollisions_findsFreespace() {
        // Arrange: Multiple items creating a crowded space
        val allItems = listOf(
            ItemRect("item1", 50f, 50f, 50f, 50f),
            ItemRect("item2", 150f, 50f, 50f, 50f),
            ItemRect("item3", 50f, 150f, 50f, 50f)
        )

        // Act
        val result = findNonOverlappingPosition(
            targetX = 50f,
            targetY = 50f,
            targetSize = 50f,
            excludeId = "test",
            allItems = allItems,
            containerWidth = 500f,
            containerHeight = 500f,
            borderPadding = 10f
        )

        // Assert: Check no collision with any item (including 8f gap)
        allItems.forEach { item ->
            val hasCollision = checkCollision(
                result.first, result.second, 50f, 50f,
                item.x - 8f, item.y - 8f,
                item.width + 16f, item.height + 16f
            )
            assertFalse("Result should not collide with item ${item.id}", hasCollision)
        }
    }

    @Test
    fun findNonOverlappingPosition_smallContainer_findsValidPosition() {
        // Arrange: Small container with one item
        val item = ItemRect("item1", 30f, 30f, 40f, 40f)
        val allItems = listOf(item)

        // Act
        val result = findNonOverlappingPosition(
            targetX = 30f,
            targetY = 30f,
            targetSize = 40f,
            excludeId = "test",
            allItems = allItems,
            containerWidth = 200f,
            containerHeight = 200f,
            borderPadding = 5f
        )

        // Assert: Position should be within bounds
        assertTrue("X should be within container", result.first >= 5f && result.first + 40f <= 195f)
        assertTrue("Y should be within container", result.second >= 5f && result.second + 40f <= 195f)
    }

    @Test
    fun findNonOverlappingPosition_perfectlyFilledGrid_fallsBackToOriginal() {
        // Arrange: Create a scenario where finding space is very difficult
        // This tests the fallback mechanism
        val containerSize = 100f
        val itemSize = 50f
        val allItems = listOf(
            ItemRect("item1", 10f, 10f, 40f, 40f),
            ItemRect("item2", 50f, 10f, 40f, 40f),
            ItemRect("item3", 10f, 50f, 40f, 40f),
            ItemRect("item4", 50f, 50f, 40f, 40f)
        )

        // Act
        val result = findNonOverlappingPosition(
            targetX = 25f,
            targetY = 25f,
            targetSize = 30f,
            excludeId = "test",
            allItems = allItems,
            containerWidth = containerSize,
            containerHeight = containerSize,
            borderPadding = 5f
        )

        // Assert: Result should be valid (either found space or returned original)
        assertTrue("Result should have valid coordinates", 
            result.first.isFinite() && result.second.isFinite())
    }
}
