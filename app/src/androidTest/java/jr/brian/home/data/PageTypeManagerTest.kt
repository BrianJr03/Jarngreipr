package jr.brian.home.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PageTypeManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var context: Context
    private lateinit var pageTypeManager: PageTypeManager

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        clearPageTypeData()
        pageTypeManager = PageTypeManager(context)
    }

    @After
    fun tearDown() = runBlocking {
        clearPageTypeData()
        testScope.cancel()
    }

    private fun clearPageTypeData() {
        context.getSharedPreferences("page_type_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun initialLoad_withNoData_createsDefaultPage() = testScope.runTest {
        // When - PageTypeManager is initialized (in setup)

        // Then
        val pageTypes = pageTypeManager.pageTypes.first()
        assertEquals(1, pageTypes.size)
        assertEquals(PageType.APPS_TAB, pageTypes[0])
    }

    @Test
    fun initialLoad_withExistingData_loadsCorrectly() = testScope.runTest {
        // Given - manually set some data in prefs
        context.getSharedPreferences("page_type_prefs", Context.MODE_PRIVATE)
            .edit()
            .apply {
                putInt("page_count", 3)
                putString("page_type_0", PageType.APPS_TAB.name)
                putString("page_type_1", PageType.APPS_AND_WIDGETS_TAB.name)
                putString("page_type_2", PageType.APP_DRAWER_TAB.name)
            }
            .commit()

        // When
        val newManager = PageTypeManager(context)

        // Then
        val pageTypes = newManager.pageTypes.first()
        assertEquals(3, pageTypes.size)
        assertEquals(PageType.APPS_TAB, pageTypes[0])
        assertEquals(PageType.APPS_AND_WIDGETS_TAB, pageTypes[1])
        assertEquals(PageType.APP_DRAWER_TAB, pageTypes[2])
    }

    @Test
    fun initialLoad_withInvalidPageType_defaultsToAppsTab() = testScope.runTest {
        // Given - manually set invalid data
        context.getSharedPreferences("page_type_prefs", Context.MODE_PRIVATE)
            .edit()
            .apply {
                putInt("page_count", 2)
                putString("page_type_0", "INVALID_TYPE")
                putString("page_type_1", PageType.APPS_AND_WIDGETS_TAB.name)
            }
            .commit()

        // When
        val newManager = PageTypeManager(context)

        // Then
        val pageTypes = newManager.pageTypes.first()
        assertEquals(2, pageTypes.size)
        assertEquals(PageType.APPS_TAB, pageTypes[0]) // Invalid type defaults to APPS_TAB
        assertEquals(PageType.APPS_AND_WIDGETS_TAB, pageTypes[1])
    }

    @Test
    fun initialLoad_withMissingPageType_defaultsToAppsTab() = testScope.runTest {
        // Given - count says 2 but only 1 entry exists
        context.getSharedPreferences("page_type_prefs", Context.MODE_PRIVATE)
            .edit()
            .apply {
                putInt("page_count", 2)
                putString("page_type_0", PageType.APP_DRAWER_TAB.name)
                // page_type_1 is missing
            }
            .commit()

        // When
        val newManager = PageTypeManager(context)

        // Then
        val pageTypes = newManager.pageTypes.first()
        assertEquals(2, pageTypes.size)
        assertEquals(PageType.APP_DRAWER_TAB, pageTypes[0])
        assertEquals(PageType.APPS_TAB, pageTypes[1]) // Missing entry defaults to APPS_TAB
    }

    @Test
    fun addPage_addsPageToEnd() = testScope.runTest {
        // Given - start with default page
        val initialPageTypes = pageTypeManager.pageTypes.first()
        assertEquals(1, initialPageTypes.size)

        // When
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)

        // Then
        val pageTypes = pageTypeManager.pageTypes.first()
        assertEquals(2, pageTypes.size)
        assertEquals(PageType.APPS_TAB, pageTypes[0])
        assertEquals(PageType.APPS_AND_WIDGETS_TAB, pageTypes[1])
    }

    @Test
    fun addPage_multiplePages_addsInCorrectOrder() = testScope.runTest {
        // When
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.addPage(PageType.APP_DRAWER_TAB)
        pageTypeManager.addPage(PageType.APPS_TAB)

        // Then
        val pageTypes = pageTypeManager.pageTypes.first()
        assertEquals(4, pageTypes.size) // 1 default + 3 added
        assertEquals(PageType.APPS_TAB, pageTypes[0])
        assertEquals(PageType.APPS_AND_WIDGETS_TAB, pageTypes[1])
        assertEquals(PageType.APP_DRAWER_TAB, pageTypes[2])
        assertEquals(PageType.APPS_TAB, pageTypes[3])
    }

    @Test
    fun addPage_persistsToSharedPreferences() = testScope.runTest {
        // When
        pageTypeManager.addPage(PageType.APP_DRAWER_TAB)

        // Then - verify it's persisted by creating a new manager
        val newManager = PageTypeManager(context)
        val pageTypes = newManager.pageTypes.first()
        assertEquals(2, pageTypes.size)
        assertEquals(PageType.APPS_TAB, pageTypes[0])
        assertEquals(PageType.APP_DRAWER_TAB, pageTypes[1])
    }

    @Test
    fun addPage_emitsNewStateToFlow() = testScope.runTest {
        // When/Then
        pageTypeManager.pageTypes.test {
            // Initial state
            val initial = awaitItem()
            assertEquals(1, initial.size)

            // Add page
            pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
            val updated = awaitItem()
            assertEquals(2, updated.size)
            assertEquals(PageType.APPS_AND_WIDGETS_TAB, updated[1])
        }
    }

    @Test
    fun removePage_removesPageAtIndex() = testScope.runTest {
        // Given - add some pages
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.addPage(PageType.APP_DRAWER_TAB)
        val beforeRemove = pageTypeManager.pageTypes.first()
        assertEquals(3, beforeRemove.size)

        // When - remove middle page
        pageTypeManager.removePage(1)

        // Then
        val pageTypes = pageTypeManager.pageTypes.first()
        assertEquals(2, pageTypes.size)
        assertEquals(PageType.APPS_TAB, pageTypes[0])
        assertEquals(PageType.APP_DRAWER_TAB, pageTypes[1])
    }

    @Test
    fun removePage_removesFirstPage() = testScope.runTest {
        // Given
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.addPage(PageType.APP_DRAWER_TAB)

        // When
        pageTypeManager.removePage(0)

        // Then
        val pageTypes = pageTypeManager.pageTypes.first()
        assertEquals(2, pageTypes.size)
        assertEquals(PageType.APPS_AND_WIDGETS_TAB, pageTypes[0])
        assertEquals(PageType.APP_DRAWER_TAB, pageTypes[1])
    }

    @Test
    fun removePage_removesLastPage() = testScope.runTest {
        // Given
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.addPage(PageType.APP_DRAWER_TAB)

        // When
        pageTypeManager.removePage(2)

        // Then
        val pageTypes = pageTypeManager.pageTypes.first()
        assertEquals(2, pageTypes.size)
        assertEquals(PageType.APPS_TAB, pageTypes[0])
        assertEquals(PageType.APPS_AND_WIDGETS_TAB, pageTypes[1])
    }

    @Test
    fun removePage_withInvalidIndex_doesNothing() = testScope.runTest {
        // Given
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        val beforeRemove = pageTypeManager.pageTypes.first()
        assertEquals(2, beforeRemove.size)

        // When - try to remove invalid indices
        pageTypeManager.removePage(-1)
        pageTypeManager.removePage(5)

        // Then - no changes
        val pageTypes = pageTypeManager.pageTypes.first()
        assertEquals(2, pageTypes.size)
        assertEquals(PageType.APPS_TAB, pageTypes[0])
        assertEquals(PageType.APPS_AND_WIDGETS_TAB, pageTypes[1])
    }

    @Test
    fun removePage_persistsToSharedPreferences() = testScope.runTest {
        // Given
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.addPage(PageType.APP_DRAWER_TAB)

        // When
        pageTypeManager.removePage(1)

        // Then - verify persistence by creating new manager
        val newManager = PageTypeManager(context)
        val pageTypes = newManager.pageTypes.first()
        assertEquals(2, pageTypes.size)
        assertEquals(PageType.APPS_TAB, pageTypes[0])
        assertEquals(PageType.APP_DRAWER_TAB, pageTypes[1])
    }

    @Test
    fun removePage_cleansUpOrphanedPreferenceKeys() = testScope.runTest {
        // Given
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.addPage(PageType.APP_DRAWER_TAB)
        val prefs = context.getSharedPreferences("page_type_prefs", Context.MODE_PRIVATE)
        
        // Verify all keys exist before removal
        assertTrue(prefs.contains("page_type_0"))
        assertTrue(prefs.contains("page_type_1"))
        assertTrue(prefs.contains("page_type_2"))

        // When
        pageTypeManager.removePage(2)

        // Then - orphaned key should be removed
        assertTrue(prefs.contains("page_type_0"))
        assertTrue(prefs.contains("page_type_1"))
        assertTrue(!prefs.contains("page_type_2"))
    }

    @Test
    fun removePage_emitsNewStateToFlow() = testScope.runTest {
        // Given
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.addPage(PageType.APP_DRAWER_TAB)

        // When/Then
        pageTypeManager.pageTypes.test {
            // Skip to current state (3 pages)
            assertEquals(3, awaitItem().size)

            // Remove page
            pageTypeManager.removePage(1)
            val updated = awaitItem()
            assertEquals(2, updated.size)
            assertEquals(PageType.APPS_TAB, updated[0])
            assertEquals(PageType.APP_DRAWER_TAB, updated[1])
        }
    }

    @Test
    fun removePage_reindexesCorrectly() = testScope.runTest {
        // Given - create specific pattern
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.addPage(PageType.APP_DRAWER_TAB)
        pageTypeManager.addPage(PageType.APPS_TAB)

        // When - remove middle page
        pageTypeManager.removePage(1)

        // Then - verify re-indexing in SharedPreferences
        val prefs = context.getSharedPreferences("page_type_prefs", Context.MODE_PRIVATE)
        assertEquals(3, prefs.getInt("page_count", 0))
        assertEquals(PageType.APPS_TAB.name, prefs.getString("page_type_0", null))
        assertEquals(PageType.APP_DRAWER_TAB.name, prefs.getString("page_type_1", null))
        assertEquals(PageType.APPS_TAB.name, prefs.getString("page_type_2", null))
    }

    @Test
    fun pageTypes_isReadOnlyFlow() = testScope.runTest {
        // Given/When
        val pageTypesFlow = pageTypeManager.pageTypes

        // Then - verify it's a StateFlow (not MutableStateFlow)
        assertTrue(pageTypesFlow is kotlinx.coroutines.flow.StateFlow)
    }

    @Test
    fun multipleOperations_maintainConsistency() = testScope.runTest {
        // When - perform series of operations
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.addPage(PageType.APP_DRAWER_TAB)
        pageTypeManager.addPage(PageType.APPS_TAB)
        pageTypeManager.removePage(0)
        pageTypeManager.addPage(PageType.APPS_AND_WIDGETS_TAB)
        pageTypeManager.removePage(2)

        // Then - verify final state
        val pageTypes = pageTypeManager.pageTypes.first()
        assertEquals(3, pageTypes.size)
        assertEquals(PageType.APPS_AND_WIDGETS_TAB, pageTypes[0])
        assertEquals(PageType.APP_DRAWER_TAB, pageTypes[1])
        assertEquals(PageType.APPS_AND_WIDGETS_TAB, pageTypes[2])

        // Verify persistence
        val newManager = PageTypeManager(context)
        val persistedTypes = newManager.pageTypes.first()
        assertEquals(pageTypes, persistedTypes)
    }
}
