package jr.brian.home.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class WidgetPageAppManagerReorderTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var context: Context
    private lateinit var manager: WidgetPageAppManager

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        manager = WidgetPageAppManager(context)
        clearAll()
    }

    @After
    fun tearDown() = runBlocking {
        clearAll()
        testScope.cancel()
    }

    private suspend fun clearAll() {
        for (pageIndex in 0..9) {
            manager.clearPageData(pageIndex)
        }
    }

    @Test
    fun reorderPages_remapsVisibleAppsAndSectionOrder() = testScope.runTest {
        manager.addVisibleApp(0, "com.page0")
        manager.addVisibleApp(1, "com.page1")
        manager.toggleSectionOrder(0)

        manager.reorderPages(mapOf(0 to 1, 1 to 0))

        assertEquals(setOf("com.page1"), manager.getVisibleApps(0).first())
        assertEquals(setOf("com.page0"), manager.getVisibleApps(1).first())
        assertFalse(manager.getAppsFirstOrder(0).first())
        assertTrue(manager.getAppsFirstOrder(1).first())
    }

    @Test
    fun reorderPages_dropsPagesNotInMap() = testScope.runTest {
        manager.addVisibleApp(0, "com.page0")
        manager.addVisibleApp(1, "com.page1")

        manager.reorderPages(mapOf(0 to 0))

        assertEquals(setOf("com.page0"), manager.getVisibleApps(0).first())
        assertTrue(manager.getVisibleApps(1).first().isEmpty())
    }

    @Test
    fun removePage_shiftsLaterIndicesDown() = testScope.runTest {
        manager.addVisibleApp(0, "com.page0")
        manager.addVisibleApp(1, "com.page1")
        manager.addVisibleApp(2, "com.page2")

        manager.removePage(1)

        assertEquals(setOf("com.page0"), manager.getVisibleApps(0).first())
        assertEquals(setOf("com.page2"), manager.getVisibleApps(1).first())
        assertTrue(manager.getVisibleApps(2).first().isEmpty())
    }

    @Test
    fun removePage_dropsTargetedPageOnly() = testScope.runTest {
        manager.addVisibleApp(0, "com.page0")
        manager.addVisibleApp(2, "com.page2")

        manager.removePage(2)

        assertEquals(setOf("com.page0"), manager.getVisibleApps(0).first())
        assertTrue(manager.getVisibleApps(2).first().isEmpty())
    }
}
