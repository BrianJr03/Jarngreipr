package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GridSettingsManagerTest {
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var gridSettingsManager: GridSettingsManager

    // Storage for mocked SharedPreferences
    private val prefsStorage = mutableMapOf<String, Any>()

    @Before
    fun setup() {
        // Clear storage before each test
        prefsStorage.clear()

        // Mock Context
        context = mockk(relaxed = true)

        // Mock SharedPreferences with storage
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        // Setup SharedPreferences behavior
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putInt(any(), any()) } answers {
            prefsStorage[firstArg()] = secondArg<Int>()
            editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            prefsStorage[firstArg()] = secondArg<Boolean>()
            editor
        }
        every { editor.apply() } returns Unit
        every { sharedPreferences.getInt(any(), any()) } answers {
            prefsStorage[firstArg()] as? Int ?: secondArg()
        }
        every { sharedPreferences.getBoolean(any(), any()) } answers {
            prefsStorage[firstArg()] as? Boolean ?: secondArg()
        }
    }

    @Test
    fun `initial state loads default values when no prefs exist`() {
        // When
        gridSettingsManager = GridSettingsManager(context)

        // Then
        assertEquals(GridSettingsManager.DEFAULT_COLUMN_COUNT, gridSettingsManager.columnCount)
        assertTrue(gridSettingsManager.unlimitedMode) // Default is unlimited mode
    }

    @Test
    fun `initial state loads saved column count from preferences`() {
        // Given
        prefsStorage["column_count"] = 5

        // When
        gridSettingsManager = GridSettingsManager(context)

        // Then
        assertEquals(5, gridSettingsManager.columnCount)
    }

    @Test
    fun `initial state loads saved unlimited mode from preferences`() {
        // Given
        prefsStorage["unlimited_mode"] = false

        // When
        gridSettingsManager = GridSettingsManager(context)

        // Then
        assertFalse(gridSettingsManager.unlimitedMode)
    }

    @Test
    fun `initial state loads saved row count from preferences`() {
        // Given
        prefsStorage["row_count"] = 10

        // When
        gridSettingsManager = GridSettingsManager(context)

        // Then
        assertEquals(10, gridSettingsManager.rowCount)
    }

    @Test
    fun `initial row count defaults to ABSOLUTE_MAX_ROWS when no saved value and no apps`() {
        // Given
        prefsStorage.remove("row_count")

        // When
        gridSettingsManager = GridSettingsManager(context)

        // Then
        assertEquals(GridSettingsManager.ABSOLUTE_MAX_ROWS, gridSettingsManager.rowCount)
    }

    @Test
    fun `updateColumnCount updates column count within valid range`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.updateColumnCount(5)

        // Then
        assertEquals(5, gridSettingsManager.columnCount)
        verify { editor.putInt("column_count", 5) }
    }

    @Test
    fun `updateColumnCount accepts minimum column count`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.updateColumnCount(GridSettingsManager.MIN_COLUMNS)

        // Then
        assertEquals(GridSettingsManager.MIN_COLUMNS, gridSettingsManager.columnCount)
    }

    @Test
    fun `updateColumnCount accepts maximum column count`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.updateColumnCount(GridSettingsManager.MAX_COLUMNS)

        // Then
        assertEquals(GridSettingsManager.MAX_COLUMNS, gridSettingsManager.columnCount)
    }

    @Test
    fun `updateColumnCount rejects value below minimum`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        val originalCount = gridSettingsManager.columnCount

        // When
        gridSettingsManager.updateColumnCount(0)

        // Then
        assertEquals(originalCount, gridSettingsManager.columnCount)
    }

    @Test
    fun `updateColumnCount rejects value above maximum`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        val originalCount = gridSettingsManager.columnCount

        // When
        gridSettingsManager.updateColumnCount(8)

        // Then
        assertEquals(originalCount, gridSettingsManager.columnCount)
    }

    @Test
    fun `updateColumnCount disables unlimited mode`() {
        // Given
        prefsStorage["unlimited_mode"] = true
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.updateColumnCount(5)

        // Then
        assertFalse(gridSettingsManager.unlimitedMode)
        verify { editor.putBoolean("unlimited_mode", false) }
    }

    @Test
    fun `updateColumnCount persists to SharedPreferences`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.updateColumnCount(6)

        // Then
        verify { editor.putInt("column_count", 6) }
        verify { editor.apply() }
    }

    @Test
    fun `updateRowCount updates row count within valid range`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(100)

        // When
        gridSettingsManager.updateRowCount(10)

        // Then
        assertEquals(10, gridSettingsManager.rowCount)
        verify { editor.putInt("row_count", 10) }
    }

    @Test
    fun `updateRowCount accepts minimum row count`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(10)

        // When
        gridSettingsManager.updateRowCount(GridSettingsManager.MIN_ROWS)

        // Then
        assertEquals(GridSettingsManager.MIN_ROWS, gridSettingsManager.rowCount)
    }

    @Test
    fun `updateRowCount accepts maximum rows based on getMaxRows`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(100)
        val maxRows = gridSettingsManager.getMaxRows()

        // When
        gridSettingsManager.updateRowCount(maxRows)

        // Then
        assertEquals(maxRows, gridSettingsManager.rowCount)
    }

    @Test
    fun `updateRowCount rejects value below minimum`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        val originalCount = gridSettingsManager.rowCount

        // When
        gridSettingsManager.updateRowCount(0)

        // Then
        assertEquals(originalCount, gridSettingsManager.rowCount)
    }

    @Test
    fun `updateRowCount rejects value above maximum`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(20) // Limited rows
        val maxRows = gridSettingsManager.getMaxRows()

        // When
        gridSettingsManager.updateRowCount(maxRows + 10)

        // Then
        assertTrue(gridSettingsManager.rowCount <= maxRows)
    }

    @Test
    fun `updateRowCount disables unlimited mode`() {
        // Given
        prefsStorage["unlimited_mode"] = true
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(100)

        // When
        gridSettingsManager.updateRowCount(10)

        // Then
        assertFalse(gridSettingsManager.unlimitedMode)
        verify { editor.putBoolean("unlimited_mode", false) }
    }

    @Test
    fun `updateRowCount persists to SharedPreferences`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(100)

        // When
        gridSettingsManager.updateRowCount(15)

        // Then
        verify { editor.putInt("row_count", 15) }
        verify { editor.apply() }
    }

    @Test
    fun `setUnlimitedMode enables unlimited mode`() {
        // Given
        prefsStorage["unlimited_mode"] = false
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.setUnlimitedMode(true)

        // Then
        assertTrue(gridSettingsManager.unlimitedMode)
    }

    @Test
    fun `setUnlimitedMode disables unlimited mode`() {
        // Given
        prefsStorage["unlimited_mode"] = true
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.setUnlimitedMode(false)

        // Then
        assertFalse(gridSettingsManager.unlimitedMode)
    }

    @Test
    fun `setUnlimitedMode persists to SharedPreferences`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.setUnlimitedMode(false)

        // Then
        verify { editor.putBoolean("unlimited_mode", false) }
        verify { editor.apply() }
    }

    @Test
    fun `unlimited mode defaults to true when not previously set`() {
        // Given
        prefsStorage.remove("unlimited_mode")

        // When
        gridSettingsManager = GridSettingsManager(context)

        // Then
        assertTrue(gridSettingsManager.unlimitedMode)
    }

    @Test
    fun `getMaxRows returns Int MAX_VALUE when unlimited mode is enabled`() {
        // Given
        prefsStorage["unlimited_mode"] = true
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(50)

        // When
        val maxRows = gridSettingsManager.getMaxRows()

        // Then
        assertEquals(Int.MAX_VALUE, maxRows)
    }

    @Test
    fun `getMaxRows returns ABSOLUTE_MAX_ROWS when total apps is zero`() {
        // Given
        prefsStorage["unlimited_mode"] = false
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(0)

        // When
        val maxRows = gridSettingsManager.getMaxRows()

        // Then
        assertEquals(GridSettingsManager.ABSOLUTE_MAX_ROWS, maxRows)
    }

    @Test
    fun `getMaxRows calculates required rows based on app count`() {
        // Given
        prefsStorage["unlimited_mode"] = false
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(20) // 20 apps with 4 columns = 5 rows

        // When
        val maxRows = gridSettingsManager.getMaxRows()

        // Then
        val expectedRows = (20 + GridSettingsManager.DEFAULT_COLUMN_COUNT - 1) / 
                          GridSettingsManager.DEFAULT_COLUMN_COUNT
        assertEquals(expectedRows, maxRows)
    }

    @Test
    fun `getMaxRows caps at ABSOLUTE_MAX_ROWS`() {
        // Given
        prefsStorage["unlimited_mode"] = false
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(300) // Exceeds max rows

        // When
        val maxRows = gridSettingsManager.getMaxRows()

        // Then
        assertEquals(GridSettingsManager.ABSOLUTE_MAX_ROWS, maxRows)
    }

    @Test
    fun `getMaxRows handles edge case with exactly one row of apps`() {
        // Given
        prefsStorage["unlimited_mode"] = false
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(4) // Exactly 1 row with 4 columns

        // When
        val maxRows = gridSettingsManager.getMaxRows()

        // Then
        assertEquals(1, maxRows)
    }

    @Test
    fun `getMaxRows rounds up for partial rows`() {
        // Given
        prefsStorage["unlimited_mode"] = false
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(5) // 5 apps with 4 columns = 2 rows (round up)

        // When
        val maxRows = gridSettingsManager.getMaxRows()

        // Then
        assertEquals(2, maxRows)
    }

    @Test
    fun `resetToDefault restores default column count`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.updateColumnCount(6)

        // When
        gridSettingsManager.resetToDefault(20)

        // Then
        assertEquals(GridSettingsManager.DEFAULT_COLUMN_COUNT, gridSettingsManager.columnCount)
    }

    @Test
    fun `resetToDefault calculates rows based on app count`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.resetToDefault(20) // 20 apps = 5 rows with 4 columns

        // Then
        val expectedRows = (20 + GridSettingsManager.DEFAULT_COLUMN_COUNT - 1) / 
                          GridSettingsManager.DEFAULT_COLUMN_COUNT
        assertEquals(expectedRows, gridSettingsManager.rowCount)
    }

    @Test
    fun `resetToDefault enables unlimited mode`() {
        // Given
        prefsStorage["unlimited_mode"] = false
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.resetToDefault(20)

        // Then
        assertTrue(gridSettingsManager.unlimitedMode)
    }

    @Test
    fun `resetToDefault persists all values to SharedPreferences`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.resetToDefault(20)

        // Then
        verify { editor.putInt("column_count", GridSettingsManager.DEFAULT_COLUMN_COUNT) }
        verify { editor.putBoolean("unlimited_mode", true) }
        verify { editor.apply() }
    }

    @Test
    fun `resetToDefault handles minimum app count`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.resetToDefault(1)

        // Then
        assertTrue(gridSettingsManager.rowCount >= GridSettingsManager.MIN_ROWS)
    }

    @Test
    fun `resetToDefault caps rows at ABSOLUTE_MAX_ROWS`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.resetToDefault(300) // Excessive app count

        // Then
        assertEquals(GridSettingsManager.ABSOLUTE_MAX_ROWS, gridSettingsManager.rowCount)
    }

    @Test
    fun `initializeDefaultRows sets rows when not previously set`() {
        // Given
        prefsStorage.remove("row_count")
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.initializeDefaultRows(20)

        // Then
        val expectedRows = (20 + GridSettingsManager.DEFAULT_COLUMN_COUNT - 1) / 
                          GridSettingsManager.DEFAULT_COLUMN_COUNT
        assertEquals(expectedRows, gridSettingsManager.rowCount)
    }

    @Test
    fun `initializeDefaultRows does nothing when rows already set`() {
        // Given
        prefsStorage["row_count"] = 10
        gridSettingsManager = GridSettingsManager(context)
        val originalRowCount = gridSettingsManager.rowCount

        // When
        gridSettingsManager.initializeDefaultRows(20)

        // Then
        assertEquals(originalRowCount, gridSettingsManager.rowCount)
    }

    @Test
    fun `initializeDefaultRows does nothing when app count is zero`() {
        // Given
        prefsStorage.remove("row_count")
        gridSettingsManager = GridSettingsManager(context)
        val originalRowCount = gridSettingsManager.rowCount

        // When
        gridSettingsManager.initializeDefaultRows(0)

        // Then
        assertEquals(originalRowCount, gridSettingsManager.rowCount)
    }

    @Test
    fun `initializeDefaultRows persists value to SharedPreferences`() {
        // Given
        prefsStorage.remove("row_count")
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.initializeDefaultRows(20)

        // Then
        val expectedRows = (20 + GridSettingsManager.DEFAULT_COLUMN_COUNT - 1) / 
                          GridSettingsManager.DEFAULT_COLUMN_COUNT
        verify { editor.putInt("row_count", expectedRows) }
        verify { editor.apply() }
    }

    @Test
    fun `initializeDefaultRows respects minimum rows constraint`() {
        // Given
        prefsStorage.remove("row_count")
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.initializeDefaultRows(1)

        // Then
        assertTrue(gridSettingsManager.rowCount >= GridSettingsManager.MIN_ROWS)
    }

    @Test
    fun `initializeDefaultRows respects maximum rows constraint`() {
        // Given
        prefsStorage.remove("row_count")
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.initializeDefaultRows(300)

        // Then
        assertTrue(gridSettingsManager.rowCount <= GridSettingsManager.ABSOLUTE_MAX_ROWS)
    }

    @Test
    fun `setTotalAppsCount updates internal count`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.setTotalAppsCount(50)

        // Then - verify it affects getMaxRows calculation
        prefsStorage["unlimited_mode"] = false
        gridSettingsManager.setUnlimitedMode(false)
        val maxRows = gridSettingsManager.getMaxRows()
        val expectedRows = (50 + GridSettingsManager.DEFAULT_COLUMN_COUNT - 1) / 
                          GridSettingsManager.DEFAULT_COLUMN_COUNT
        assertEquals(expectedRows, maxRows)
    }

    @Test
    fun `setTotalAppsCount with zero does not affect unlimited mode behavior`() {
        // Given
        prefsStorage["unlimited_mode"] = true
        gridSettingsManager = GridSettingsManager(context)

        // When
        gridSettingsManager.setTotalAppsCount(0)

        // Then
        assertEquals(Int.MAX_VALUE, gridSettingsManager.getMaxRows())
    }

    @Test
    fun `column and row configuration determine grid capacity`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.updateColumnCount(5)
        gridSettingsManager.updateRowCount(10)

        // When/Then
        val capacity = gridSettingsManager.columnCount * gridSettingsManager.rowCount
        assertEquals(50, capacity)
    }

    @Test
    fun `changing columns affects maximum grid capacity`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        val initialColumns = gridSettingsManager.columnCount

        // When
        gridSettingsManager.updateColumnCount(GridSettingsManager.MAX_COLUMNS)

        // Then
        assertTrue(gridSettingsManager.columnCount > initialColumns)
    }

    @Test
    fun `unlimited mode allows maximum flexibility`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setUnlimitedMode(true)

        // When
        val maxRows = gridSettingsManager.getMaxRows()

        // Then
        assertEquals(Int.MAX_VALUE, maxRows) // Unlimited
    }

    @Test
    fun `limited mode constrains layout based on app count`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.setTotalAppsCount(16) // 4x4 = 16 apps
        gridSettingsManager.setUnlimitedMode(false)

        // When
        val maxRows = gridSettingsManager.getMaxRows()

        // Then
        assertEquals(4, maxRows) // Only 4 rows needed
    }

    @Test
    fun `settings persist across multiple manager instances`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.updateColumnCount(6)
        gridSettingsManager.updateRowCount(15)
        gridSettingsManager.setUnlimitedMode(false)

        // When - Create new manager instance
        val newManager = GridSettingsManager(context)

        // Then
        assertEquals(6, newManager.columnCount)
        assertEquals(15, newManager.rowCount)
        assertFalse(newManager.unlimitedMode)
    }

    @Test
    fun `manual adjustments preserve settings until reset`() {
        // Given
        gridSettingsManager = GridSettingsManager(context)
        gridSettingsManager.updateColumnCount(5)
        gridSettingsManager.updateRowCount(10)

        // When
        val columnsBeforeReset = gridSettingsManager.columnCount
        val rowsBeforeReset = gridSettingsManager.rowCount
        gridSettingsManager.resetToDefault(20)

        // Then
        assertTrue(gridSettingsManager.columnCount != columnsBeforeReset || 
                  columnsBeforeReset == GridSettingsManager.DEFAULT_COLUMN_COUNT)
        assertEquals(GridSettingsManager.DEFAULT_COLUMN_COUNT, gridSettingsManager.columnCount)
    }

    @Test
    fun `constants are correctly defined`() {
        assertEquals(4, GridSettingsManager.DEFAULT_COLUMN_COUNT)
        assertEquals(1, GridSettingsManager.MIN_COLUMNS)
        assertEquals(7, GridSettingsManager.MAX_COLUMNS)
        assertEquals(1, GridSettingsManager.MIN_ROWS)
        assertEquals(50, GridSettingsManager.ABSOLUTE_MAX_ROWS)
    }

    @Test
    fun `SharedPreferences uses correct preference name`() {
        // When
        GridSettingsManager(context)

        // Then
        verify { context.getSharedPreferences("grid_settings_prefs", Context.MODE_PRIVATE) }
    }
}
