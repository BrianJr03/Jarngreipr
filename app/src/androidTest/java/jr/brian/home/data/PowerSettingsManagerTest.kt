package jr.brian.home.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import jr.brian.home.model.BackButtonShortcut
import jr.brian.home.model.WakeMethod
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PowerSettingsManagerTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var context: Context
    private lateinit var powerSettingsManager: PowerSettingsManager

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        powerSettingsManager = PowerSettingsManager(context)
        clearAllSettings()
    }

    @After
    fun tearDown() = runBlocking {
        clearAllSettings()
        testScope.cancel()
    }

    private fun clearAllSettings() {
        context.getSharedPreferences("power_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testPowerButtonVisibility_defaultValue() = runTest {
        val manager = PowerSettingsManager(context)
        assertFalse(manager.powerButtonVisible.first())
    }

    @Test
    fun testPowerButtonVisibility_setToTrue() = runTest {
        powerSettingsManager.setPowerButtonVisibility(true)
        assertTrue(powerSettingsManager.powerButtonVisible.first())
    }

    @Test
    fun testPowerButtonVisibility_setToFalse() = runTest {
        powerSettingsManager.setPowerButtonVisibility(true)
        powerSettingsManager.setPowerButtonVisibility(false)
        assertFalse(powerSettingsManager.powerButtonVisible.first())
    }

    @Test
    fun testPowerButtonVisibility_flowEmitsUpdates() = runTest {
        powerSettingsManager.powerButtonVisible.test {
            assertEquals(false, awaitItem())
            
            powerSettingsManager.setPowerButtonVisibility(true)
            assertEquals(true, awaitItem())
            
            powerSettingsManager.setPowerButtonVisibility(false)
            assertEquals(false, awaitItem())
            
            cancel()
        }
    }

    @Test
    fun testPowerButtonVisibility_persistsAcrossInstances() = runTest {
        powerSettingsManager.setPowerButtonVisibility(true)
        
        val newManager = PowerSettingsManager(context)
        assertTrue(newManager.powerButtonVisible.first())
    }

    @Test
    fun testQuickDeleteVisibility_defaultValue() = runTest {
        val manager = PowerSettingsManager(context)
        assertFalse(manager.quickDeleteVisible.first())
    }

    @Test
    fun testQuickDeleteVisibility_setToTrue() = runTest {
        powerSettingsManager.setQuickDeleteVisibility(true)
        assertTrue(powerSettingsManager.quickDeleteVisible.first())
    }

    @Test
    fun testQuickDeleteVisibility_flowEmitsUpdates() = runTest {
        powerSettingsManager.quickDeleteVisible.test {
            assertEquals(false, awaitItem())
            
            powerSettingsManager.setQuickDeleteVisibility(true)
            assertEquals(true, awaitItem())
            
            cancel()
        }
    }

    @Test
    fun testQuickDeleteVisibility_persistsAcrossInstances() = runTest {
        powerSettingsManager.setQuickDeleteVisibility(true)
        
        val newManager = PowerSettingsManager(context)
        assertTrue(newManager.quickDeleteVisible.first())
    }

    @Test
    fun testHeaderVisibility_defaultValue() = runTest {
        val manager = PowerSettingsManager(context)
        assertTrue(manager.headerVisible.first())
    }

    @Test
    fun testHeaderVisibility_setToFalse() = runTest {
        powerSettingsManager.setHeaderVisibility(false)
        assertFalse(powerSettingsManager.headerVisible.first())
    }

    @Test
    fun testHeaderVisibility_setToTrue() = runTest {
        powerSettingsManager.setHeaderVisibility(false)
        powerSettingsManager.setHeaderVisibility(true)
        assertTrue(powerSettingsManager.headerVisible.first())
    }

    @Test
    fun testHeaderVisibility_flowEmitsUpdates() = runTest {
        powerSettingsManager.headerVisible.test {
            assertEquals(true, awaitItem())
            
            powerSettingsManager.setHeaderVisibility(false)
            assertEquals(false, awaitItem())
            
            powerSettingsManager.setHeaderVisibility(true)
            assertEquals(true, awaitItem())
            
            cancel()
        }
    }

    @Test
    fun testHeaderVisibility_persistsAcrossInstances() = runTest {
        powerSettingsManager.setHeaderVisibility(false)
        
        val newManager = PowerSettingsManager(context)
        assertFalse(newManager.headerVisible.first())
    }

    @Test
    fun testWakeMethod_defaultValue() = runTest {
        val manager = PowerSettingsManager(context)
        assertEquals(WakeMethod.DOUBLE_TAP, manager.wakeMethod.first())
    }

    @Test
    fun testWakeMethod_setToSingleTap() = runTest {
        powerSettingsManager.setWakeMethod(WakeMethod.SINGLE_TAP)
        assertEquals(WakeMethod.SINGLE_TAP, powerSettingsManager.wakeMethod.first())
    }

    @Test
    fun testWakeMethod_setToLongPress() = runTest {
        powerSettingsManager.setWakeMethod(WakeMethod.LONG_PRESS)
        assertEquals(WakeMethod.LONG_PRESS, powerSettingsManager.wakeMethod.first())
    }

    @Test
    fun testWakeMethod_flowEmitsUpdates() = runTest {
        powerSettingsManager.wakeMethod.test {
            assertEquals(WakeMethod.DOUBLE_TAP, awaitItem())
            
            powerSettingsManager.setWakeMethod(WakeMethod.SINGLE_TAP)
            assertEquals(WakeMethod.SINGLE_TAP, awaitItem())
            
            powerSettingsManager.setWakeMethod(WakeMethod.LONG_PRESS)
            assertEquals(WakeMethod.LONG_PRESS, awaitItem())
            
            cancel()
        }
    }

    @Test
    fun testWakeMethod_persistsAcrossInstances() = runTest {
        powerSettingsManager.setWakeMethod(WakeMethod.LONG_PRESS)
        
        val newManager = PowerSettingsManager(context)
        assertEquals(WakeMethod.LONG_PRESS, newManager.wakeMethod.first())
    }

    @Test
    fun testWakeMethod_handlesInvalidValue() = runTest {
        // Manually set an invalid value in preferences
        context.getSharedPreferences("power_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("wake_method", "INVALID_METHOD")
            .commit()
        
        val manager = PowerSettingsManager(context)
        assertEquals(WakeMethod.DOUBLE_TAP, manager.wakeMethod.first())
    }

    @Test
    fun testBackButtonShortcutEnabled_defaultValue() = runTest {
        val manager = PowerSettingsManager(context)
        assertFalse(manager.backButtonShortcutEnabled.first())
    }

    @Test
    fun testBackButtonShortcutEnabled_setToTrue() = runTest {
        powerSettingsManager.setBackButtonShortcutEnabled(true)
        assertTrue(powerSettingsManager.backButtonShortcutEnabled.first())
    }

    @Test
    fun testBackButtonShortcutEnabled_flowEmitsUpdates() = runTest {
        powerSettingsManager.backButtonShortcutEnabled.test {
            assertEquals(false, awaitItem())
            
            powerSettingsManager.setBackButtonShortcutEnabled(true)
            assertEquals(true, awaitItem())
            
            cancel()
        }
    }

    @Test
    fun testBackButtonShortcutEnabled_persistsAcrossInstances() = runTest {
        powerSettingsManager.setBackButtonShortcutEnabled(true)
        
        val newManager = PowerSettingsManager(context)
        assertTrue(newManager.backButtonShortcutEnabled.first())
    }

    @Test
    fun testBackButtonShortcut_defaultValue() = runTest {
        val manager = PowerSettingsManager(context)
        assertEquals(BackButtonShortcut.NONE, manager.backButtonShortcut.first())
    }

    @Test
    fun testBackButtonShortcut_setToSettings() = runTest {
        powerSettingsManager.setBackButtonShortcut(BackButtonShortcut.SETTINGS)
        assertEquals(BackButtonShortcut.SETTINGS, powerSettingsManager.backButtonShortcut.first())
    }

    @Test
    fun testBackButtonShortcut_setToAppSearch() = runTest {
        powerSettingsManager.setBackButtonShortcut(BackButtonShortcut.APP_SEARCH)
        assertEquals(BackButtonShortcut.APP_SEARCH, powerSettingsManager.backButtonShortcut.first())
    }

    @Test
    fun testBackButtonShortcut_flowEmitsUpdates() = runTest {
        powerSettingsManager.backButtonShortcut.test {
            assertEquals(BackButtonShortcut.NONE, awaitItem())
            
            powerSettingsManager.setBackButtonShortcut(BackButtonShortcut.SETTINGS)
            assertEquals(BackButtonShortcut.SETTINGS, awaitItem())
            
            powerSettingsManager.setBackButtonShortcut(BackButtonShortcut.QUICK_DELETE)
            assertEquals(BackButtonShortcut.QUICK_DELETE, awaitItem())
            
            cancel()
        }
    }

    @Test
    fun testBackButtonShortcut_persistsAcrossInstances() = runTest {
        powerSettingsManager.setBackButtonShortcut(BackButtonShortcut.MONITOR)
        
        val newManager = PowerSettingsManager(context)
        assertEquals(BackButtonShortcut.MONITOR, newManager.backButtonShortcut.first())
    }

    @Test
    fun testBackButtonShortcut_handlesInvalidValue() = runTest {
        // Manually set an invalid value in preferences
        context.getSharedPreferences("power_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("back_button_shortcut", "INVALID_SHORTCUT")
            .commit()
        
        val manager = PowerSettingsManager(context)
        assertEquals(BackButtonShortcut.NONE, manager.backButtonShortcut.first())
    }

    @Test
    fun testBackButtonShortcutAppPackage_defaultValue() = runTest {
        val manager = PowerSettingsManager(context)
        assertNull(manager.backButtonShortcutAppPackage.first())
    }

    @Test
    fun testBackButtonShortcutAppPackage_setPackage() = runTest {
        val packageName = "com.example.app"
        powerSettingsManager.setBackButtonShortcutAppPackage(packageName)
        assertEquals(packageName, powerSettingsManager.backButtonShortcutAppPackage.first())
    }

    @Test
    fun testBackButtonShortcutAppPackage_flowEmitsUpdates() = runTest {
        powerSettingsManager.backButtonShortcutAppPackage.test {
            assertNull(awaitItem())
            
            powerSettingsManager.setBackButtonShortcutAppPackage("com.example.app1")
            assertEquals("com.example.app1", awaitItem())
            
            powerSettingsManager.setBackButtonShortcutAppPackage("com.example.app2")
            assertEquals("com.example.app2", awaitItem())
            
            cancel()
        }
    }

    @Test
    fun testBackButtonShortcutAppPackage_persistsAcrossInstances() = runTest {
        val packageName = "com.example.testapp"
        powerSettingsManager.setBackButtonShortcutAppPackage(packageName)
        
        val newManager = PowerSettingsManager(context)
        assertEquals(packageName, newManager.backButtonShortcutAppPackage.first())
    }

    @Test
    fun testResetBackButtonShortcut_resetsToDefaults() = runTest {
        powerSettingsManager.setBackButtonShortcut(BackButtonShortcut.SETTINGS)
        powerSettingsManager.setBackButtonShortcutAppPackage("com.example.app")
        
        powerSettingsManager.resetBackButtonShortcut()
        
        assertEquals(BackButtonShortcut.NONE, powerSettingsManager.backButtonShortcut.first())
        assertNull(powerSettingsManager.backButtonShortcutAppPackage.first())
    }

    @Test
    fun testResetBackButtonShortcut_flowsEmitUpdates() = runTest {
        powerSettingsManager.setBackButtonShortcut(BackButtonShortcut.APP)
        powerSettingsManager.setBackButtonShortcutAppPackage("com.example.app")
        
        powerSettingsManager.backButtonShortcut.test {
            assertEquals(BackButtonShortcut.APP, awaitItem())
            
            powerSettingsManager.resetBackButtonShortcut()
            assertEquals(BackButtonShortcut.NONE, awaitItem())
            
            cancel()
        }
    }

    @Test
    fun testResetBackButtonShortcut_persistsAcrossInstances() = runTest {
        powerSettingsManager.setBackButtonShortcut(BackButtonShortcut.CONTROL_PAD)
        powerSettingsManager.setBackButtonShortcutAppPackage("com.example.app")
        
        powerSettingsManager.resetBackButtonShortcut()
        
        val newManager = PowerSettingsManager(context)
        assertEquals(BackButtonShortcut.NONE, newManager.backButtonShortcut.first())
        assertNull(newManager.backButtonShortcutAppPackage.first())
    }

    @Test
    fun testMultipleSettings_persistTogether() = runTest {
        powerSettingsManager.setPowerButtonVisibility(true)
        powerSettingsManager.setHeaderVisibility(false)
        powerSettingsManager.setWakeMethod(WakeMethod.LONG_PRESS)
        powerSettingsManager.setBackButtonShortcutEnabled(true)
        powerSettingsManager.setBackButtonShortcut(BackButtonShortcut.SETTINGS)
        
        val newManager = PowerSettingsManager(context)
        assertTrue(newManager.powerButtonVisible.first())
        assertFalse(newManager.headerVisible.first())
        assertEquals(WakeMethod.LONG_PRESS, newManager.wakeMethod.first())
        assertTrue(newManager.backButtonShortcutEnabled.first())
        assertEquals(BackButtonShortcut.SETTINGS, newManager.backButtonShortcut.first())
    }

    @Test
    fun testAllSettings_canBeSetAndRetrieved() = runTest {
        powerSettingsManager.setPowerButtonVisibility(true)
        powerSettingsManager.setQuickDeleteVisibility(true)
        powerSettingsManager.setHeaderVisibility(false)
        powerSettingsManager.setWakeMethod(WakeMethod.SINGLE_TAP)
        powerSettingsManager.setBackButtonShortcutEnabled(true)
        powerSettingsManager.setBackButtonShortcut(BackButtonShortcut.CUSTOM_THEME)
        powerSettingsManager.setBackButtonShortcutAppPackage("com.test.package")
        
        assertTrue(powerSettingsManager.powerButtonVisible.first())
        assertTrue(powerSettingsManager.quickDeleteVisible.first())
        assertFalse(powerSettingsManager.headerVisible.first())
        assertEquals(WakeMethod.SINGLE_TAP, powerSettingsManager.wakeMethod.first())
        assertTrue(powerSettingsManager.backButtonShortcutEnabled.first())
        assertEquals(BackButtonShortcut.CUSTOM_THEME, powerSettingsManager.backButtonShortcut.first())
        assertEquals("com.test.package", powerSettingsManager.backButtonShortcutAppPackage.first())
    }
}
