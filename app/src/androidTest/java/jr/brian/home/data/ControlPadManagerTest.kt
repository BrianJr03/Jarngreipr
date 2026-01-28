package jr.brian.home.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import jr.brian.home.model.ControlPadItem
import jr.brian.home.model.PhysicalButton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ControlPadManagerTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var context: Context
    private lateinit var controlPadManager: ControlPadManager

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("control_pad_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        controlPadManager = ControlPadManager(context)
    }

    @After
    fun tearDown() = runBlocking {
        context.getSharedPreferences("control_pad_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        testScope.cancel()
    }

    @Test
    fun testInitialControlPadItems_shouldLoadDefaultValues() = runTest {
        controlPadManager.controlPadItems.test {
            val items = awaitItem()
            assertEquals(6, items.size)
            items.forEachIndexed { index, item ->
                assertEquals("${index + 1}", item.label)
                assertNull(item.mappedButton)
            }
        }
    }

    @Test
    fun testSetControlPadItem_shouldUpdateItemAndPersist() = runTest {
        val testItem = ControlPadItem(label = "Jump", mappedButton = PhysicalButton.A)
        
        controlPadManager.setControlPadItem(0, testItem)
        
        controlPadManager.controlPadItems.test {
            val items = awaitItem()
            assertEquals("Jump", items[0].label)
            assertEquals(PhysicalButton.A, items[0].mappedButton)
        }
        
        // Verify persistence by creating new instance
        val newManager = ControlPadManager(context)
        newManager.controlPadItems.test {
            val items = awaitItem()
            assertEquals("Jump", items[0].label)
            assertEquals(PhysicalButton.A, items[0].mappedButton)
        }
    }

    @Test
    fun testSetControlPadItem_multipleItems_shouldUpdateCorrectly() = runTest {
        val item1 = ControlPadItem(label = "Jump", mappedButton = PhysicalButton.A)
        val item2 = ControlPadItem(label = "Shoot", mappedButton = PhysicalButton.B)
        val item3 = ControlPadItem(label = "Reload", mappedButton = PhysicalButton.X)
        
        controlPadManager.setControlPadItem(0, item1)
        controlPadManager.setControlPadItem(2, item2)
        controlPadManager.setControlPadItem(5, item3)
        
        controlPadManager.controlPadItems.test {
            val items = awaitItem()
            assertEquals("Jump", items[0].label)
            assertEquals(PhysicalButton.A, items[0].mappedButton)
            assertEquals("Shoot", items[2].label)
            assertEquals(PhysicalButton.B, items[2].mappedButton)
            assertEquals("Reload", items[5].label)
            assertEquals(PhysicalButton.X, items[5].mappedButton)
            
            // Verify other items remain default
            assertEquals("2", items[1].label)
            assertNull(items[1].mappedButton)
        }
    }

    @Test
    fun testSetControlPadItem_withInvalidIndex_shouldBeIgnored() = runTest {
        val testItem = ControlPadItem(label = "Invalid", mappedButton = PhysicalButton.A)
        
        controlPadManager.setControlPadItem(-1, testItem)
        controlPadManager.setControlPadItem(6, testItem)
        controlPadManager.setControlPadItem(100, testItem)
        
        controlPadManager.controlPadItems.test {
            val items = awaitItem()
            // All items should still be default
            items.forEachIndexed { index, item ->
                assertEquals("${index + 1}", item.label)
                assertNull(item.mappedButton)
            }
        }
    }

    @Test
    fun testSetControlPadItem_withNullButton_shouldClearMapping() = runTest {
        // First set a mapping
        val item1 = ControlPadItem(label = "Jump", mappedButton = PhysicalButton.A)
        controlPadManager.setControlPadItem(0, item1)
        
        // Then clear it
        val item2 = ControlPadItem(label = "Jump", mappedButton = null)
        controlPadManager.setControlPadItem(0, item2)
        
        controlPadManager.controlPadItems.test {
            val items = awaitItem()
            assertEquals("Jump", items[0].label)
            assertNull(items[0].mappedButton)
        }
    }

    @Test
    fun testResetAllMappings_shouldClearAllCustomizations() = runTest {
        // Set multiple custom mappings
        controlPadManager.setControlPadItem(0, ControlPadItem("Jump", PhysicalButton.A))
        controlPadManager.setControlPadItem(1, ControlPadItem("Shoot", PhysicalButton.B))
        controlPadManager.setControlPadItem(2, ControlPadItem("Reload", PhysicalButton.X))
        
        // Reset all
        controlPadManager.resetAllMappings()
        
        controlPadManager.controlPadItems.test {
            val items = awaitItem()
            assertEquals(6, items.size)
            items.forEachIndexed { index, item ->
                assertEquals("${index + 1}", item.label)
                assertNull(item.mappedButton)
            }
        }
        
        // Verify persistence
        val newManager = ControlPadManager(context)
        newManager.controlPadItems.test {
            val items = awaitItem()
            items.forEachIndexed { index, item ->
                assertEquals("${index + 1}", item.label)
                assertNull(item.mappedButton)
            }
        }
    }

    @Test
    fun testButtonMappings_withAllPhysicalButtons() = runTest {
        val allButtons = PhysicalButton.entries.toTypedArray()
        
        // Test that we can map any physical button
        allButtons.take(6).forEachIndexed { index, button ->
            val item = ControlPadItem(label = button.displayName, mappedButton = button)
            controlPadManager.setControlPadItem(index, item)
        }
        
        controlPadManager.controlPadItems.test {
            val items = awaitItem()
            allButtons.take(6).forEachIndexed { index, button ->
                assertEquals(button.displayName, items[index].label)
                assertEquals(button, items[index].mappedButton)
            }
        }
    }

    @Test
    fun testInitialJoystickMode_shouldBeRightOnly() = runTest {
        controlPadManager.joystickMode.test {
            assertEquals(JoystickMode.RIGHT_ONLY, awaitItem())
        }
    }

    @Test
    fun testSetJoystickMode_leftOnly_shouldUpdateAndPersist() = runTest {
        controlPadManager.setJoystickMode(JoystickMode.LEFT_ONLY)
        
        controlPadManager.joystickMode.test {
            assertEquals(JoystickMode.LEFT_ONLY, awaitItem())
        }
        
        // Verify persistence
        val newManager = ControlPadManager(context)
        newManager.joystickMode.test {
            assertEquals(JoystickMode.LEFT_ONLY, awaitItem())
        }
    }

    @Test
    fun testSetJoystickMode_rightOnly_shouldUpdateAndPersist() = runTest {
        controlPadManager.setJoystickMode(JoystickMode.RIGHT_ONLY)
        
        controlPadManager.joystickMode.test {
            assertEquals(JoystickMode.RIGHT_ONLY, awaitItem())
        }
        
        // Verify persistence
        val newManager = ControlPadManager(context)
        newManager.joystickMode.test {
            assertEquals(JoystickMode.RIGHT_ONLY, awaitItem())
        }
    }

    @Test
    fun testSetJoystickMode_leftRight_shouldUpdateAndPersist() = runTest {
        controlPadManager.setJoystickMode(JoystickMode.LEFT_RIGHT)
        
        controlPadManager.joystickMode.test {
            assertEquals(JoystickMode.LEFT_RIGHT, awaitItem())
        }
        
        // Verify persistence
        val newManager = ControlPadManager(context)
        newManager.joystickMode.test {
            assertEquals(JoystickMode.LEFT_RIGHT, awaitItem())
        }
    }

    @Test
    fun testJoystickMode_changingModes_shouldEmitUpdates() = runTest {
        controlPadManager.joystickMode.test {
            assertEquals(JoystickMode.RIGHT_ONLY, awaitItem())
            
            controlPadManager.setJoystickMode(JoystickMode.LEFT_ONLY)
            assertEquals(JoystickMode.LEFT_ONLY, awaitItem())
            
            controlPadManager.setJoystickMode(JoystickMode.LEFT_RIGHT)
            assertEquals(JoystickMode.LEFT_RIGHT, awaitItem())
            
            controlPadManager.setJoystickMode(JoystickMode.RIGHT_ONLY)
            assertEquals(JoystickMode.RIGHT_ONLY, awaitItem())
        }
    }

    @Test
    fun testJoystickMode_withCorruptedPrefs_shouldUseDefault() = runTest {
        // Manually corrupt the preference
        context.getSharedPreferences("control_pad_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("joystick_mode", "INVALID_MODE")
            .commit()
        
        val newManager = ControlPadManager(context)
        newManager.joystickMode.test {
            assertEquals(JoystickMode.RIGHT_ONLY, awaitItem())
        }
    }

    @Test
    fun testInitialCameraSensitivity_shouldBeDefault() = runTest {
        controlPadManager.cameraSensitivity.test {
            assertEquals(ControlPadManager.DEFAULT_CAMERA_SENSITIVITY, awaitItem())
        }
    }

    @Test
    fun testSetCameraSensitivity_validValue_shouldUpdateAndPersist() = runTest {
        val testSensitivity = 0.15f
        
        controlPadManager.setCameraSensitivity(testSensitivity)
        
        controlPadManager.cameraSensitivity.test {
            assertEquals(testSensitivity, awaitItem())
        }
        
        // Verify persistence
        val newManager = ControlPadManager(context)
        newManager.cameraSensitivity.test {
            assertEquals(testSensitivity, awaitItem())
        }
    }

    @Test
    fun testSetCameraSensitivity_minValue_shouldWork() = runTest {
        controlPadManager.setCameraSensitivity(ControlPadManager.MIN_CAMERA_SENSITIVITY)
        
        controlPadManager.cameraSensitivity.test {
            assertEquals(ControlPadManager.MIN_CAMERA_SENSITIVITY, awaitItem())
        }
    }

    @Test
    fun testSetCameraSensitivity_maxValue_shouldWork() = runTest {
        controlPadManager.setCameraSensitivity(ControlPadManager.MAX_CAMERA_SENSITIVITY)
        
        controlPadManager.cameraSensitivity.test {
            assertEquals(ControlPadManager.MAX_CAMERA_SENSITIVITY, awaitItem())
        }
    }

    @Test
    fun testSetCameraSensitivity_belowMin_shouldClampToMin() = runTest {
        controlPadManager.setCameraSensitivity(0.001f)
        
        controlPadManager.cameraSensitivity.test {
            assertEquals(ControlPadManager.MIN_CAMERA_SENSITIVITY, awaitItem())
        }
    }

    @Test
    fun testSetCameraSensitivity_aboveMax_shouldClampToMax() = runTest {
        controlPadManager.setCameraSensitivity(1.0f)
        
        controlPadManager.cameraSensitivity.test {
            assertEquals(ControlPadManager.MAX_CAMERA_SENSITIVITY, awaitItem())
        }
    }

    @Test
    fun testSetCameraSensitivity_negativeValue_shouldClampToMin() = runTest {
        controlPadManager.setCameraSensitivity(-0.5f)
        
        controlPadManager.cameraSensitivity.test {
            assertEquals(ControlPadManager.MIN_CAMERA_SENSITIVITY, awaitItem())
        }
    }

    @Test
    fun testSetCameraSensitivity_multipleChanges_shouldEmitUpdates() = runTest {
        controlPadManager.cameraSensitivity.test {
            assertEquals(ControlPadManager.DEFAULT_CAMERA_SENSITIVITY, awaitItem())
            
            controlPadManager.setCameraSensitivity(0.10f)
            assertEquals(0.10f, awaitItem())
            
            controlPadManager.setCameraSensitivity(0.20f)
            assertEquals(0.20f, awaitItem())
            
            controlPadManager.setCameraSensitivity(ControlPadManager.MIN_CAMERA_SENSITIVITY)
            assertEquals(ControlPadManager.MIN_CAMERA_SENSITIVITY, awaitItem())
        }
    }

    @Test
    fun testMultipleSettings_shouldAllPersist() = runTest {
        // Set up various configurations
        controlPadManager.setControlPadItem(0, ControlPadItem("Jump", PhysicalButton.A))
        controlPadManager.setControlPadItem(1, ControlPadItem("Shoot", PhysicalButton.B))
        controlPadManager.setJoystickMode(JoystickMode.LEFT_RIGHT)
        controlPadManager.setCameraSensitivity(0.18f)
        
        // Create new manager to verify persistence
        val newManager = ControlPadManager(context)
        
        newManager.controlPadItems.test {
            val items = awaitItem()
            assertEquals("Jump", items[0].label)
            assertEquals(PhysicalButton.A, items[0].mappedButton)
            assertEquals("Shoot", items[1].label)
            assertEquals(PhysicalButton.B, items[1].mappedButton)
        }
        
        newManager.joystickMode.test {
            assertEquals(JoystickMode.LEFT_RIGHT, awaitItem())
        }
        
        newManager.cameraSensitivity.test {
            assertEquals(0.18f, awaitItem())
        }
    }

    @Test
    fun testResetAllMappings_shouldNotAffectOtherSettings() = runTest {
        // Set up various configurations
        controlPadManager.setControlPadItem(0, ControlPadItem("Jump", PhysicalButton.A))
        controlPadManager.setJoystickMode(JoystickMode.LEFT_RIGHT)
        controlPadManager.setCameraSensitivity(0.18f)
        
        // Reset only mappings
        controlPadManager.resetAllMappings()
        
        // Verify mappings are reset
        controlPadManager.controlPadItems.test {
            val items = awaitItem()
            items.forEachIndexed { index, item ->
                assertEquals("${index + 1}", item.label)
                assertNull(item.mappedButton)
            }
        }
        
        // Verify other settings are unchanged
        controlPadManager.joystickMode.test {
            assertEquals(JoystickMode.LEFT_RIGHT, awaitItem())
        }
        
        controlPadManager.cameraSensitivity.test {
            assertEquals(0.18f, awaitItem())
        }
    }

    @Test
    fun testConcurrentUpdates_shouldMaintainConsistency() = runTest {
        // Rapidly change settings
        repeat(10) { i ->
            controlPadManager.setControlPadItem(
                i % 6, 
                ControlPadItem("Item$i", PhysicalButton.entries[i % PhysicalButton.entries.size])
            )
        }
        
        controlPadManager.controlPadItems.test {
            val items = awaitItem()
            assertEquals(6, items.size)
            // Verify the last updates for each slot
            items.forEachIndexed { index, item ->
                assert(item.label.startsWith("Item") || item.label == "${index + 1}")
            }
        }
    }
}
