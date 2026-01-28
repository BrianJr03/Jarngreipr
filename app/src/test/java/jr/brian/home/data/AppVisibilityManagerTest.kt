package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppVisibilityManagerTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var manager: AppVisibilityManager

    private val prefsData = mutableMapOf<String, Any?>()

    @Before
    fun setup() {
        // Mock SharedPreferences with in-memory storage
        editor = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // Setup SharedPreferences behavior
        every { context.getSharedPreferences("app_visibility_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.apply() } answers { }
        every { editor.putBoolean(any(), any()) } answers {
            prefsData[firstArg()] = secondArg()
            editor
        }
        every { editor.putString(any(), any()) } answers {
            prefsData[firstArg()] = secondArg()
            editor
        }
        every { editor.remove(any()) } answers {
            prefsData.remove(firstArg())
            editor
        }
        every { sharedPreferences.getBoolean(any(), any()) } answers {
            prefsData[firstArg()] as? Boolean ?: secondArg()
        }
        every { sharedPreferences.getString(any(), any()) } answers {
            prefsData[firstArg()] as? String ?: secondArg()
        }

        // Initialize manager
        manager = AppVisibilityManager(context)
    }

    @Test
    fun `initial state loads from SharedPreferences`() = runTest {
        // Given: Pre-populate SharedPreferences
        prefsData["hidden_apps_0"] = "com.app1,com.app2"
        prefsData["hidden_apps_1"] = "com.app3"
        prefsData["show_app_names"] = true
        prefsData["show_folder_names"] = false
        prefsData["new_apps_visible_by_default"] = false

        // When: Create new manager
        val newManager = AppVisibilityManager(context)

        // Then: State is loaded correctly
        assertEquals(setOf("com.app1", "com.app2"), newManager.getHiddenApps(0))
        assertEquals(setOf("com.app3"), newManager.getHiddenApps(1))
        assertTrue(newManager.showAppNames)
        assertFalse(newManager.showFolderNames)
        newManager.newAppsVisibleByDefault.test {
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `initial state has correct defaults when SharedPreferences is empty`() = runTest {
        // When: Manager is created with empty prefs
        // Then: Default values are used
        assertEquals(emptySet<String>(), manager.getHiddenApps(0))
        assertFalse(manager.showAppNames)
        assertTrue(manager.showFolderNames)
        assertTrue(manager.showSettingsBackButton)
        manager.newAppsVisibleByDefault.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `hiddenAppsByPage flow emits initial state`() = runTest {
        // Given: Pre-populate data
        prefsData["hidden_apps_0"] = "com.app1,com.app2"
        prefsData["hidden_apps_2"] = "com.app3"

        // When: Create manager and observe flow
        val newManager = AppVisibilityManager(context)

        // Then: Flow emits correct initial state
        newManager.hiddenAppsByPage.test {
            val state = awaitItem()
            assertEquals(2, state.size)
            assertEquals(setOf("com.app1", "com.app2"), state[0])
            assertEquals(setOf("com.app3"), state[2])
        }
    }

    @Test
    fun `hideApp adds app to hidden list`() = runTest {
        // Given: Empty hidden apps
        assertEquals(emptySet<String>(), manager.getHiddenApps(0))

        // When: Hide an app
        manager.hideApp(0, "com.test.app")

        // Then: App is hidden
        assertEquals(setOf("com.test.app"), manager.getHiddenApps(0))
        assertTrue(manager.isAppHidden(0, "com.test.app"))
    }

    @Test
    fun `hideApp persists to SharedPreferences`() = runTest {
        // When: Hide an app
        manager.hideApp(0, "com.test.app")

        // Then: Verify saved to prefs
        verify { editor.putString("hidden_apps_0", "com.test.app") }
        verify { editor.apply() }
    }

    @Test
    fun `hideApp can hide multiple apps sequentially`() = runTest {
        // When: Hide multiple apps
        manager.hideApp(0, "com.app1")
        manager.hideApp(0, "com.app2")
        manager.hideApp(0, "com.app3")

        // Then: All apps are hidden
        val hiddenApps = manager.getHiddenApps(0)
        assertEquals(3, hiddenApps.size)
        assertTrue(hiddenApps.contains("com.app1"))
        assertTrue(hiddenApps.contains("com.app2"))
        assertTrue(hiddenApps.contains("com.app3"))
    }

    @Test
    fun `hideApp updates hiddenAppsByPage flow`() = runTest {
        manager.hiddenAppsByPage.test {
            // Initial state
            val initial = awaitItem()
            assertEquals(emptyMap<Int, Set<String>>(), initial)

            // When: Hide an app
            manager.hideApp(0, "com.test.app")

            // Then: Flow emits updated state
            val updated = awaitItem()
            assertEquals(setOf("com.test.app"), updated[0])
        }
    }

    @Test
    fun `showApp removes app from hidden list`() = runTest {
        // Given: App is hidden
        manager.hideApp(0, "com.test.app")
        assertTrue(manager.isAppHidden(0, "com.test.app"))

        // When: Show the app
        manager.showApp(0, "com.test.app")

        // Then: App is no longer hidden
        assertFalse(manager.isAppHidden(0, "com.test.app"))
        assertEquals(emptySet<String>(), manager.getHiddenApps(0))
    }

    @Test
    fun `showApp removes key from SharedPreferences when list becomes empty`() = runTest {
        // Given: App is hidden
        manager.hideApp(0, "com.test.app")

        // When: Show the app (list becomes empty)
        manager.showApp(0, "com.test.app")

        // Then: Key is removed from prefs
        verify { editor.remove("hidden_apps_0") }
        verify { editor.apply() }
    }

    @Test
    fun `showApp persists remaining apps when list is not empty`() = runTest {
        // Given: Multiple apps are hidden
        manager.hideApp(0, "com.app1")
        manager.hideApp(0, "com.app2")

        // When: Show one app
        manager.showApp(0, "com.app1")

        // Then: Remaining app is still persisted
        val remaining = manager.getHiddenApps(0)
        assertEquals(setOf("com.app2"), remaining)
    }

    @Test
    fun `showApp updates hiddenAppsByPage flow`() = runTest {
        // Given: App is hidden
        manager.hideApp(0, "com.test.app")

        manager.hiddenAppsByPage.test {
            skipItems(1) // Skip current state

            // When: Show the app
            manager.showApp(0, "com.test.app")

            // Then: Flow emits updated state
            val updated = awaitItem()
            assertFalse(updated.containsKey(0))
        }
    }

    @Test
    fun `hideAllApps adds all apps to hidden list`() = runTest {
        // Given: List of apps
        val apps = listOf("com.app1", "com.app2", "com.app3")

        // When: Hide all apps
        manager.hideAllApps(0, apps)

        // Then: All apps are hidden
        val hiddenApps = manager.getHiddenApps(0)
        assertEquals(3, hiddenApps.size)
        assertTrue(hiddenApps.containsAll(apps))
    }

    @Test
    fun `hideAllApps adds to existing hidden apps`() = runTest {
        // Given: Some apps already hidden
        manager.hideApp(0, "com.existing")

        // When: Hide more apps
        manager.hideAllApps(0, listOf("com.app1", "com.app2"))

        // Then: All apps are hidden
        val hiddenApps = manager.getHiddenApps(0)
        assertEquals(3, hiddenApps.size)
        assertTrue(hiddenApps.contains("com.existing"))
        assertTrue(hiddenApps.contains("com.app1"))
        assertTrue(hiddenApps.contains("com.app2"))
    }

    @Test
    fun `hideAllApps handles empty list`() = runTest {
        // Given: One app is hidden
        manager.hideApp(0, "com.app1")

        // When: Hide empty list
        manager.hideAllApps(0, emptyList())

        // Then: Original app is still hidden
        assertEquals(setOf("com.app1"), manager.getHiddenApps(0))
    }

    @Test
    fun `showAllApps removes all apps from hidden list`() = runTest {
        // Given: Multiple apps are hidden
        manager.hideAllApps(0, listOf("com.app1", "com.app2", "com.app3"))

        // When: Show some apps
        manager.showAllApps(0, listOf("com.app1", "com.app3"))

        // Then: Only app2 remains hidden
        val hiddenApps = manager.getHiddenApps(0)
        assertEquals(setOf("com.app2"), hiddenApps)
    }

    @Test
    fun `showAllApps removes all apps and clears prefs when list becomes empty`() = runTest {
        // Given: Multiple apps are hidden
        val apps = listOf("com.app1", "com.app2")
        manager.hideAllApps(0, apps)

        // When: Show all apps
        manager.showAllApps(0, apps)

        // Then: List is empty and key removed
        assertEquals(emptySet<String>(), manager.getHiddenApps(0))
        verify { editor.remove("hidden_apps_0") }
    }

    @Test
    fun `showAllApps handles empty list`() = runTest {
        // Given: Apps are hidden
        manager.hideAllApps(0, listOf("com.app1", "com.app2"))

        // When: Show empty list
        manager.showAllApps(0, emptyList())

        // Then: All apps still hidden
        assertEquals(setOf("com.app1", "com.app2"), manager.getHiddenApps(0))
    }

    @Test
    fun `apps can be hidden on different pages independently`() = runTest {
        // When: Hide apps on different pages
        manager.hideApp(0, "com.page0.app")
        manager.hideApp(1, "com.page1.app")
        manager.hideApp(2, "com.page2.app")

        // Then: Each page has its own hidden apps
        assertEquals(setOf("com.page0.app"), manager.getHiddenApps(0))
        assertEquals(setOf("com.page1.app"), manager.getHiddenApps(1))
        assertEquals(setOf("com.page2.app"), manager.getHiddenApps(2))
        assertEquals(emptySet<String>(), manager.getHiddenApps(3))
    }

    @Test
    fun `same app can be hidden on multiple pages`() = runTest {
        // When: Hide same app on multiple pages
        manager.hideApp(0, "com.app")
        manager.hideApp(1, "com.app")
        manager.hideApp(2, "com.app")

        // Then: App is hidden on all pages
        assertTrue(manager.isAppHidden(0, "com.app"))
        assertTrue(manager.isAppHidden(1, "com.app"))
        assertTrue(manager.isAppHidden(2, "com.app"))
    }

    @Test
    fun `hiddenAppsByPage flow tracks multiple pages`() = runTest {
        manager.hiddenAppsByPage.test {
            skipItems(1) // Skip initial empty state

            // When: Hide apps on different pages
            manager.hideApp(0, "com.app1")
            manager.hideApp(1, "com.app2")

            // Then: Flow emits state with both pages
            val state = expectMostRecentItem()
            assertEquals(2, state.size)
            assertEquals(setOf("com.app1"), state[0])
            assertEquals(setOf("com.app2"), state[1])
        }
    }

    @Test
    fun `clearing apps from a page removes it from hiddenAppsByPage`() = runTest {
        // Given: Apps on multiple pages
        manager.hideApp(0, "com.app1")
        manager.hideApp(1, "com.app2")

        // When: Clear page 0
        manager.showApp(0, "com.app1")

        // Then: Page 0 is removed from map
        manager.hiddenAppsByPage.test {
            val state = awaitItem()
            assertFalse(state.containsKey(0))
            assertTrue(state.containsKey(1))
        }
    }

    @Test
    fun `isAppHidden returns true for hidden app`() {
        // Given: App is hidden
        manager.hideApp(0, "com.app")

        // Then: Check returns true
        assertTrue(manager.isAppHidden(0, "com.app"))
    }

    @Test
    fun `isAppHidden returns false for visible app`() {
        // Then: Check returns false
        assertFalse(manager.isAppHidden(0, "com.app"))
    }

    @Test
    fun `getHiddenApps returns empty set for page with no hidden apps`() {
        // Then: Empty set returned
        assertEquals(emptySet<String>(), manager.getHiddenApps(5))
    }

    @Test
    fun `getHiddenApps returns correct set for page with hidden apps`() {
        // Given: Multiple apps hidden
        manager.hideAllApps(0, listOf("com.app1", "com.app2", "com.app3"))

        // Then: Correct set returned
        val hiddenApps = manager.getHiddenApps(0)
        assertEquals(3, hiddenApps.size)
        assertTrue(hiddenApps.contains("com.app1"))
        assertTrue(hiddenApps.contains("com.app2"))
        assertTrue(hiddenApps.contains("com.app3"))
    }

    @Test
    fun `toggleShowAppNames toggles state and persists`() {
        // Given: Initial state is false
        assertFalse(manager.showAppNames)

        // When: Toggle
        manager.toggleShowAppNames()

        // Then: State is true and persisted
        assertTrue(manager.showAppNames)
        verify { editor.putBoolean("show_app_names", true) }

        // When: Toggle again
        manager.toggleShowAppNames()

        // Then: State is false
        assertFalse(manager.showAppNames)
        verify { editor.putBoolean("show_app_names", false) }
    }

    @Test
    fun `toggleShowFolderNames toggles state and persists`() {
        // Given: Initial state is true (default)
        assertTrue(manager.showFolderNames)

        // When: Toggle
        manager.toggleShowFolderNames()

        // Then: State is false and persisted
        assertFalse(manager.showFolderNames)
        verify { editor.putBoolean("show_folder_names", false) }

        // When: Toggle again
        manager.toggleShowFolderNames()

        // Then: State is true
        assertTrue(manager.showFolderNames)
        verify { editor.putBoolean("show_folder_names", true) }
    }

    @Test
    fun `toggleShowSettingsBackButton toggles state and persists`() {
        // Given: Initial state is true (default)
        assertTrue(manager.showSettingsBackButton)

        // When: Toggle
        manager.toggleShowSettingsBackButton()

        // Then: State is false and persisted
        assertFalse(manager.showSettingsBackButton)
        verify { editor.putBoolean("show_settings_back_button", false) }

        // When: Toggle again
        manager.toggleShowSettingsBackButton()

        // Then: State is true
        assertTrue(manager.showSettingsBackButton)
        verify { editor.putBoolean("show_settings_back_button", true) }
    }

    @Test
    fun `setNewAppsVisibleByDefault updates flow and persists`() = runTest {
        manager.newAppsVisibleByDefault.test {
            // Initial state is true
            assertEquals(true, awaitItem())

            // When: Set to false
            manager.setNewAppsVisibleByDefault(false)

            // Then: Flow emits false and persisted
            assertEquals(false, awaitItem())
            verify { editor.putBoolean("new_apps_visible_by_default", false) }
        }
    }

    @Test
    fun `setNewAppsVisibleByDefault can be set to same value`() = runTest {
        // When: Set to true (same as default)
        manager.setNewAppsVisibleByDefault(true)

        // Then: Value is persisted
        manager.newAppsVisibleByDefault.test {
            assertEquals(true, awaitItem())
        }
        verify { editor.putBoolean("new_apps_visible_by_default", true) }
    }

    @Test
    fun `hiding duplicate app does not create duplicates in set`() = runTest {
        // When: Hide same app twice
        manager.hideApp(0, "com.app")
        manager.hideApp(0, "com.app")

        // Then: Only one entry exists
        assertEquals(setOf("com.app"), manager.getHiddenApps(0))
    }

    @Test
    fun `showing non-hidden app does not cause errors`() = runTest {
        // When: Show app that isn't hidden
        manager.showApp(0, "com.nonexistent")

        // Then: No errors, empty set returned
        assertEquals(emptySet<String>(), manager.getHiddenApps(0))
    }

    @Test
    fun `persistence format uses comma separator`() = runTest {
        // When: Hide multiple apps
        manager.hideAllApps(0, listOf("com.app1", "com.app2", "com.app3"))

        // Then: Verify format with comma separator
        val stringSlot = slot<String>()
        verify { editor.putString("hidden_apps_0", capture(stringSlot)) }
        
        val saved = stringSlot.captured
        assertTrue(saved.contains(","))
        val parts = saved.split(",")
        assertEquals(3, parts.size)
    }

    @Test
    fun `empty hidden apps list removes SharedPreferences key`() = runTest {
        // Given: Apps are hidden
        manager.hideApp(0, "com.app")

        // When: Show all apps (list becomes empty)
        manager.showApp(0, "com.app")

        // Then: Key is removed
        verify { editor.remove("hidden_apps_0") }
    }

    @Test
    fun `handles app package names with special characters`() = runTest {
        // Given: Package name with dots and underscores
        val packageName = "com.example.app_test.debug"

        // When: Hide the app
        manager.hideApp(0, packageName)

        // Then: App is correctly hidden
        assertTrue(manager.isAppHidden(0, packageName))
        assertEquals(setOf(packageName), manager.getHiddenApps(0))
    }

    @Test
    fun `handles large number of hidden apps on single page`() = runTest {
        // Given: Many apps
        val apps = (1..100).map { "com.app$it" }

        // When: Hide all apps
        manager.hideAllApps(0, apps)

        // Then: All apps are hidden
        val hiddenApps = manager.getHiddenApps(0)
        assertEquals(100, hiddenApps.size)
        assertTrue(hiddenApps.containsAll(apps))
    }

    @Test
    fun `handles maximum page index`() = runTest {
        // When: Hide app on high page index
        manager.hideApp(9, "com.app")

        // Then: App is correctly hidden
        assertTrue(manager.isAppHidden(9, "com.app"))
    }

    @Test
    fun `loads multiple pages on initialization`() = runTest {
        // Given: Data on multiple pages in prefs
        prefsData["hidden_apps_0"] = "com.app1"
        prefsData["hidden_apps_3"] = "com.app2"
        prefsData["hidden_apps_7"] = "com.app3"

        // When: Create new manager
        val newManager = AppVisibilityManager(context)

        // Then: All pages are loaded
        assertEquals(setOf("com.app1"), newManager.getHiddenApps(0))
        assertEquals(setOf("com.app2"), newManager.getHiddenApps(3))
        assertEquals(setOf("com.app3"), newManager.getHiddenApps(7))
        assertEquals(emptySet<String>(), newManager.getHiddenApps(1))
    }
}
