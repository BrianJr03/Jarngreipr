package jr.brian.home.data

import android.content.Context
import androidx.datastore.preferences.core.edit
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WidgetPageAppManagerTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var context: Context
    private lateinit var widgetPageAppManager: WidgetPageAppManager

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        widgetPageAppManager = WidgetPageAppManager(context)
        clearAllPageData()
    }

    @After
    fun tearDown() = runBlocking {
        clearAllPageData()
        testScope.cancel()
    }

    private suspend fun clearAllPageData() {
        for (pageIndex in 0..20) {
            widgetPageAppManager.clearPageData(pageIndex)
        }
    }

    @Test
    fun getVisibleApps_returnsEmptySetInitially() = testScope.runTest {
        // When
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()

        // Then
        assertTrue(visibleApps.isEmpty())
    }

    @Test
    fun getVisibleApps_returnsAddedApp() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.example.app1")

        // When
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()

        // Then
        assertEquals(1, visibleApps.size)
        assertTrue(visibleApps.contains("com.example.app1"))
    }

    @Test
    fun getVisibleApps_returnsMultipleApps() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.example.app1")
        widgetPageAppManager.addVisibleApp(0, "com.example.app2")
        widgetPageAppManager.addVisibleApp(0, "com.example.app3")

        // When
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()

        // Then
        assertEquals(3, visibleApps.size)
        assertTrue(visibleApps.contains("com.example.app1"))
        assertTrue(visibleApps.contains("com.example.app2"))
        assertTrue(visibleApps.contains("com.example.app3"))
    }

    @Test
    fun getVisibleApps_differentPages_returnIndependentData() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.page0.app")
        widgetPageAppManager.addVisibleApp(1, "com.page1.app")
        widgetPageAppManager.addVisibleApp(2, "com.page2.app")

        // When
        val page0Apps = widgetPageAppManager.getVisibleApps(0).first()
        val page1Apps = widgetPageAppManager.getVisibleApps(1).first()
        val page2Apps = widgetPageAppManager.getVisibleApps(2).first()

        // Then
        assertEquals(1, page0Apps.size)
        assertEquals(1, page1Apps.size)
        assertEquals(1, page2Apps.size)
        assertTrue(page0Apps.contains("com.page0.app"))
        assertTrue(page1Apps.contains("com.page1.app"))
        assertTrue(page2Apps.contains("com.page2.app"))
    }

    @Test
    fun getAppsFirstOrder_returnsFalseInitially() = testScope.runTest {
        // When
        val appsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()

        // Then
        assertFalse(appsFirst)
    }

    @Test
    fun getAppsFirstOrder_returnsTrueAfterToggle() = testScope.runTest {
        // Given
        widgetPageAppManager.toggleSectionOrder(0)

        // When
        val appsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()

        // Then
        assertTrue(appsFirst)
    }

    @Test
    fun getAppsFirstOrder_returnsFalseAfterDoubleToggle() = testScope.runTest {
        // Given
        widgetPageAppManager.toggleSectionOrder(0)
        widgetPageAppManager.toggleSectionOrder(0)

        // When
        val appsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()

        // Then
        assertFalse(appsFirst)
    }

    @Test
    fun getAppsFirstOrder_differentPages_returnIndependentData() = testScope.runTest {
        // Given
        widgetPageAppManager.toggleSectionOrder(0)
        widgetPageAppManager.toggleSectionOrder(2)

        // When
        val page0AppsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()
        val page1AppsFirst = widgetPageAppManager.getAppsFirstOrder(1).first()
        val page2AppsFirst = widgetPageAppManager.getAppsFirstOrder(2).first()

        // Then
        assertTrue(page0AppsFirst)
        assertFalse(page1AppsFirst)
        assertTrue(page2AppsFirst)
    }

    @Test
    fun addVisibleApp_addsNewAppToEmptySet() = testScope.runTest {
        // When
        widgetPageAppManager.addVisibleApp(0, "com.example.app")

        // Then
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()
        assertEquals(1, visibleApps.size)
        assertTrue(visibleApps.contains("com.example.app"))
    }

    @Test
    fun addVisibleApp_addsMultipleApps() = testScope.runTest {
        // When
        widgetPageAppManager.addVisibleApp(0, "com.app1")
        widgetPageAppManager.addVisibleApp(0, "com.app2")
        widgetPageAppManager.addVisibleApp(0, "com.app3")

        // Then
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()
        assertEquals(3, visibleApps.size)
    }

    @Test
    fun addVisibleApp_preventsDuplicates() = testScope.runTest {
        // When
        widgetPageAppManager.addVisibleApp(0, "com.example.app")
        widgetPageAppManager.addVisibleApp(0, "com.example.app")
        widgetPageAppManager.addVisibleApp(0, "com.example.app")

        // Then
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()
        assertEquals(1, visibleApps.size)
    }

    @Test
    fun addVisibleApp_emitsFlowUpdates() = testScope.runTest {
        // When/Then
        widgetPageAppManager.getVisibleApps(0).test {
            // Initial empty state
            assertEquals(0, awaitItem().size)

            // Add first app
            widgetPageAppManager.addVisibleApp(0, "com.app1")
            assertEquals(1, awaitItem().size)

            // Add second app
            widgetPageAppManager.addVisibleApp(0, "com.app2")
            assertEquals(2, awaitItem().size)

            cancel()
        }
    }

    @Test
    fun removeVisibleApp_removesExistingApp() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.app1")
        widgetPageAppManager.addVisibleApp(0, "com.app2")
        widgetPageAppManager.addVisibleApp(0, "com.app3")

        // When
        widgetPageAppManager.removeVisibleApp(0, "com.app2")

        // Then
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()
        assertEquals(2, visibleApps.size)
        assertFalse(visibleApps.contains("com.app2"))
        assertTrue(visibleApps.contains("com.app1"))
        assertTrue(visibleApps.contains("com.app3"))
    }

    @Test
    fun removeVisibleApp_removesAllApps() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.app1")
        widgetPageAppManager.addVisibleApp(0, "com.app2")

        // When
        widgetPageAppManager.removeVisibleApp(0, "com.app1")
        widgetPageAppManager.removeVisibleApp(0, "com.app2")

        // Then
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()
        assertTrue(visibleApps.isEmpty())
    }

    @Test
    fun removeVisibleApp_handlesNonExistentApp() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.app1")

        // When
        widgetPageAppManager.removeVisibleApp(0, "com.nonexistent")

        // Then
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()
        assertEquals(1, visibleApps.size)
        assertTrue(visibleApps.contains("com.app1"))
    }

    @Test
    fun removeVisibleApp_handlesEmptySet() = testScope.runTest {
        // When
        widgetPageAppManager.removeVisibleApp(0, "com.app1")

        // Then
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()
        assertTrue(visibleApps.isEmpty())
    }

    @Test
    fun removeVisibleApp_emitsFlowUpdates() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.app1")
        widgetPageAppManager.addVisibleApp(0, "com.app2")

        // When/Then
        widgetPageAppManager.getVisibleApps(0).test {
            // Current state with 2 apps
            assertEquals(2, awaitItem().size)

            // Remove first app
            widgetPageAppManager.removeVisibleApp(0, "com.app1")
            assertEquals(1, awaitItem().size)

            // Remove second app
            widgetPageAppManager.removeVisibleApp(0, "com.app2")
            assertEquals(0, awaitItem().size)

            cancel()
        }
    }

    @Test
    fun toggleSectionOrder_togglesFromFalseToTrue() = testScope.runTest {
        // When
        widgetPageAppManager.toggleSectionOrder(0)

        // Then
        val appsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()
        assertTrue(appsFirst)
    }

    @Test
    fun toggleSectionOrder_togglesFromTrueToFalse() = testScope.runTest {
        // Given
        widgetPageAppManager.toggleSectionOrder(0)

        // When
        widgetPageAppManager.toggleSectionOrder(0)

        // Then
        val appsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()
        assertFalse(appsFirst)
    }

    @Test
    fun toggleSectionOrder_multipleToggles() = testScope.runTest {
        // When
        widgetPageAppManager.toggleSectionOrder(0)
        widgetPageAppManager.toggleSectionOrder(0)
        widgetPageAppManager.toggleSectionOrder(0)

        // Then
        val appsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()
        assertTrue(appsFirst)
    }

    @Test
    fun toggleSectionOrder_emitsFlowUpdates() = testScope.runTest {
        // When/Then
        widgetPageAppManager.getAppsFirstOrder(0).test {
            // Initial false state
            assertFalse(awaitItem())

            // Toggle to true
            widgetPageAppManager.toggleSectionOrder(0)
            assertTrue(awaitItem())

            // Toggle back to false
            widgetPageAppManager.toggleSectionOrder(0)
            assertFalse(awaitItem())

            cancel()
        }
    }

    @Test
    fun clearPageData_removesAllVisibleApps() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.app1")
        widgetPageAppManager.addVisibleApp(0, "com.app2")
        widgetPageAppManager.addVisibleApp(0, "com.app3")

        // When
        widgetPageAppManager.clearPageData(0)

        // Then
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()
        assertTrue(visibleApps.isEmpty())
    }

    @Test
    fun clearPageData_resetsSectionOrder() = testScope.runTest {
        // Given
        widgetPageAppManager.toggleSectionOrder(0)

        // When
        widgetPageAppManager.clearPageData(0)

        // Then
        val appsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()
        assertFalse(appsFirst)
    }

    @Test
    fun clearPageData_removesBothVisibleAppsAndSectionOrder() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.app1")
        widgetPageAppManager.addVisibleApp(0, "com.app2")
        widgetPageAppManager.toggleSectionOrder(0)

        // When
        widgetPageAppManager.clearPageData(0)

        // Then
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()
        val appsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()
        assertTrue(visibleApps.isEmpty())
        assertFalse(appsFirst)
    }

    @Test
    fun clearPageData_onlyAffectsSpecifiedPage() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.page0.app")
        widgetPageAppManager.addVisibleApp(1, "com.page1.app")
        widgetPageAppManager.toggleSectionOrder(0)
        widgetPageAppManager.toggleSectionOrder(1)

        // When
        widgetPageAppManager.clearPageData(0)

        // Then
        val page0Apps = widgetPageAppManager.getVisibleApps(0).first()
        val page1Apps = widgetPageAppManager.getVisibleApps(1).first()
        val page0AppsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()
        val page1AppsFirst = widgetPageAppManager.getAppsFirstOrder(1).first()

        assertTrue(page0Apps.isEmpty())
        assertEquals(1, page1Apps.size)
        assertFalse(page0AppsFirst)
        assertTrue(page1AppsFirst)
    }

    @Test
    fun clearPageData_onEmptyPage_doesNotThrow() = testScope.runTest {
        // When/Then - Should not throw
        widgetPageAppManager.clearPageData(0)

        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()
        val appsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()
        assertTrue(visibleApps.isEmpty())
        assertFalse(appsFirst)
    }

    @Test
    fun reindexPages_deletesPageAtIndex() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.page0.app")
        widgetPageAppManager.addVisibleApp(1, "com.page1.app")
        widgetPageAppManager.addVisibleApp(2, "com.page2.app")

        // When - Delete page 1 (total pages after deletion: 2)
        widgetPageAppManager.reindexPages(deletedPageIndex = 1, totalPagesAfterDeletion = 2)

        // Then
        val page0Apps = widgetPageAppManager.getVisibleApps(0).first()
        val page1Apps = widgetPageAppManager.getVisibleApps(1).first()
        val page2Apps = widgetPageAppManager.getVisibleApps(2).first()

        // Page 0 stays at 0
        assertTrue(page0Apps.contains("com.page0.app"))
        // Old page 2 moved to page 1
        assertTrue(page1Apps.contains("com.page2.app"))
        // Page 2 should be empty now
        assertTrue(page2Apps.isEmpty())
    }

    @Test
    fun reindexPages_deletesFirstPage() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.page0.app")
        widgetPageAppManager.addVisibleApp(1, "com.page1.app")
        widgetPageAppManager.addVisibleApp(2, "com.page2.app")

        // When - Delete page 0
        widgetPageAppManager.reindexPages(deletedPageIndex = 0, totalPagesAfterDeletion = 2)

        // Then
        val page0Apps = widgetPageAppManager.getVisibleApps(0).first()
        val page1Apps = widgetPageAppManager.getVisibleApps(1).first()
        val page2Apps = widgetPageAppManager.getVisibleApps(2).first()

        // Old page 1 moved to page 0
        assertTrue(page0Apps.contains("com.page1.app"))
        // Old page 2 moved to page 1
        assertTrue(page1Apps.contains("com.page2.app"))
        // Page 2 should be empty
        assertTrue(page2Apps.isEmpty())
    }

    @Test
    fun reindexPages_deletesLastPage() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.page0.app")
        widgetPageAppManager.addVisibleApp(1, "com.page1.app")
        widgetPageAppManager.addVisibleApp(2, "com.page2.app")

        // When - Delete page 2
        widgetPageAppManager.reindexPages(deletedPageIndex = 2, totalPagesAfterDeletion = 2)

        // Then
        val page0Apps = widgetPageAppManager.getVisibleApps(0).first()
        val page1Apps = widgetPageAppManager.getVisibleApps(1).first()
        val page2Apps = widgetPageAppManager.getVisibleApps(2).first()

        // Pages 0 and 1 stay the same
        assertTrue(page0Apps.contains("com.page0.app"))
        assertTrue(page1Apps.contains("com.page1.app"))
        // Page 2 is deleted
        assertTrue(page2Apps.isEmpty())
    }

    @Test
    fun reindexPages_preservesSectionOrderData() = testScope.runTest {
        // Given
        widgetPageAppManager.toggleSectionOrder(0)
        widgetPageAppManager.toggleSectionOrder(2)

        // When - Delete page 1
        widgetPageAppManager.reindexPages(deletedPageIndex = 1, totalPagesAfterDeletion = 2)

        // Then
        val page0AppsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()
        val page1AppsFirst = widgetPageAppManager.getAppsFirstOrder(1).first()
        val page2AppsFirst = widgetPageAppManager.getAppsFirstOrder(2).first()

        assertTrue(page0AppsFirst)
        // Old page 2's true value moved to page 1
        assertTrue(page1AppsFirst)
        // Page 2 should be default false
        assertFalse(page2AppsFirst)
    }

    @Test
    fun reindexPages_preservesBothVisibleAppsAndSectionOrder() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.page0.app")
        widgetPageAppManager.toggleSectionOrder(0)
        
        widgetPageAppManager.addVisibleApp(1, "com.page1.app")
        // Page 1 has default false for appsFirst
        
        widgetPageAppManager.addVisibleApp(2, "com.page2.app")
        widgetPageAppManager.toggleSectionOrder(2)

        // When - Delete page 1
        widgetPageAppManager.reindexPages(deletedPageIndex = 1, totalPagesAfterDeletion = 2)

        // Then
        val page0Apps = widgetPageAppManager.getVisibleApps(0).first()
        val page0AppsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()
        val page1Apps = widgetPageAppManager.getVisibleApps(1).first()
        val page1AppsFirst = widgetPageAppManager.getAppsFirstOrder(1).first()

        // Page 0 unchanged
        assertTrue(page0Apps.contains("com.page0.app"))
        assertTrue(page0AppsFirst)

        // Old page 2 data moved to page 1
        assertTrue(page1Apps.contains("com.page2.app"))
        assertTrue(page1AppsFirst)
    }

    @Test
    fun reindexPages_withGapsInPages() = testScope.runTest {
        // Given - Only pages 0, 2, and 4 have data
        widgetPageAppManager.addVisibleApp(0, "com.page0.app")
        widgetPageAppManager.addVisibleApp(2, "com.page2.app")
        widgetPageAppManager.addVisibleApp(4, "com.page4.app")

        // When - Delete page 1 (which was empty)
        widgetPageAppManager.reindexPages(deletedPageIndex = 1, totalPagesAfterDeletion = 4)

        // Then
        val page0Apps = widgetPageAppManager.getVisibleApps(0).first()
        val page1Apps = widgetPageAppManager.getVisibleApps(1).first()
        val page2Apps = widgetPageAppManager.getVisibleApps(2).first()
        val page3Apps = widgetPageAppManager.getVisibleApps(3).first()

        // Page 0 stays at 0
        assertTrue(page0Apps.contains("com.page0.app"))
        // Old page 2 moved to page 1
        assertTrue(page1Apps.contains("com.page2.app"))
        // Page 2 should be empty
        assertTrue(page2Apps.isEmpty())
        // Old page 4 moved to page 3
        assertTrue(page3Apps.contains("com.page4.app"))
    }

    @Test
    fun reindexPages_deletesPageWithData() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.page0.app")
        widgetPageAppManager.addVisibleApp(1, "com.page1.app")
        widgetPageAppManager.addVisibleApp(2, "com.page2.app")
        widgetPageAppManager.toggleSectionOrder(1)

        // When - Delete page 1 which has data
        widgetPageAppManager.reindexPages(deletedPageIndex = 1, totalPagesAfterDeletion = 2)

        // Then - Page 1's data should be gone
        val page0Apps = widgetPageAppManager.getVisibleApps(0).first()
        val page1Apps = widgetPageAppManager.getVisibleApps(1).first()
        val page1AppsFirst = widgetPageAppManager.getAppsFirstOrder(1).first()

        assertTrue(page0Apps.contains("com.page0.app"))
        // Old page 2 moved to page 1
        assertTrue(page1Apps.contains("com.page2.app"))
        // Old page 1's section order should be gone
        assertFalse(page1AppsFirst)
    }

    @Test
    fun reindexPages_withOnlyOnePage() = testScope.runTest {
        // Given
        widgetPageAppManager.addVisibleApp(0, "com.page0.app")

        // When - Delete page 0
        widgetPageAppManager.reindexPages(deletedPageIndex = 0, totalPagesAfterDeletion = 0)

        // Then
        val page0Apps = widgetPageAppManager.getVisibleApps(0).first()
        assertTrue(page0Apps.isEmpty())
    }

    @Test
    fun addRemoveApp_multipleTimesInSequence() = testScope.runTest {
        // When/Then
        widgetPageAppManager.addVisibleApp(0, "com.app1")
        assertEquals(1, widgetPageAppManager.getVisibleApps(0).first().size)

        widgetPageAppManager.removeVisibleApp(0, "com.app1")
        assertTrue(widgetPageAppManager.getVisibleApps(0).first().isEmpty())

        widgetPageAppManager.addVisibleApp(0, "com.app1")
        assertEquals(1, widgetPageAppManager.getVisibleApps(0).first().size)

        widgetPageAppManager.addVisibleApp(0, "com.app2")
        assertEquals(2, widgetPageAppManager.getVisibleApps(0).first().size)
    }

    @Test
    fun multiplePages_fullWorkflow() = testScope.runTest {
        // Given - Setup 3 pages with data
        for (i in 0..2) {
            widgetPageAppManager.addVisibleApp(i, "com.page$i.app1")
            widgetPageAppManager.addVisibleApp(i, "com.page$i.app2")
            if (i % 2 == 0) {
                widgetPageAppManager.toggleSectionOrder(i)
            }
        }

        // When - Modify pages
        widgetPageAppManager.removeVisibleApp(1, "com.page1.app1")
        widgetPageAppManager.toggleSectionOrder(1)

        // Then
        val page0Apps = widgetPageAppManager.getVisibleApps(0).first()
        val page1Apps = widgetPageAppManager.getVisibleApps(1).first()
        val page2Apps = widgetPageAppManager.getVisibleApps(2).first()
        val page0AppsFirst = widgetPageAppManager.getAppsFirstOrder(0).first()
        val page1AppsFirst = widgetPageAppManager.getAppsFirstOrder(1).first()
        val page2AppsFirst = widgetPageAppManager.getAppsFirstOrder(2).first()

        assertEquals(2, page0Apps.size)
        assertEquals(1, page1Apps.size)
        assertEquals(2, page2Apps.size)
        assertTrue(page0AppsFirst)
        assertTrue(page1AppsFirst)
        assertTrue(page2AppsFirst)
    }

    @Test
    fun appNames_withSpecialCharacters() = testScope.runTest {
        // Given
        val specialApps = listOf(
            "com.app.with-dash",
            "com.app.with_underscore",
            "com.app.with.multiple.dots",
            "com.app123.with.numbers456"
        )

        // When
        specialApps.forEach { widgetPageAppManager.addVisibleApp(0, it) }

        // Then
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()
        assertEquals(specialApps.size, visibleApps.size)
        specialApps.forEach { app ->
            assertTrue(visibleApps.contains(app))
        }
    }

    @Test
    fun largeNumberOfApps_performanceTest() = testScope.runTest {
        // Given - Add 100 apps
        val apps = (1..100).map { "com.example.app$it" }

        // When
        apps.forEach { widgetPageAppManager.addVisibleApp(0, it) }

        // Then
        val visibleApps = widgetPageAppManager.getVisibleApps(0).first()
        assertEquals(100, visibleApps.size)

        // When - Remove half of them
        apps.take(50).forEach { widgetPageAppManager.removeVisibleApp(0, it) }

        // Then
        val remainingApps = widgetPageAppManager.getVisibleApps(0).first()
        assertEquals(50, remainingApps.size)
    }
}
