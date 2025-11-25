package jr.brian.home.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PowerViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: PowerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PowerViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is powered on`() = runTest {
        // When
        viewModel.isPoweredOff.test {
            val state = awaitItem()

            // Then
            assertFalse(state)
        }
    }

    @Test
    fun `togglePower changes state from off to on`() = runTest {
        // Given
        viewModel.togglePower() // Turn off first

        // When
        viewModel.togglePower()

        // Then
        viewModel.isPoweredOff.test {
            val state = awaitItem()
            assertFalse(state)
        }
    }

    @Test
    fun `togglePower changes state from on to off`() = runTest {
        // Given - initial state is on

        // When
        viewModel.togglePower()

        // Then
        viewModel.isPoweredOff.test {
            val state = awaitItem()
            assertTrue(state)
        }
    }

    @Test
    fun `togglePower can be called multiple times`() = runTest {
        // When
        viewModel.togglePower() // off
        viewModel.togglePower() // on
        viewModel.togglePower() // off
        viewModel.togglePower() // on

        // Then
        viewModel.isPoweredOff.test {
            val state = awaitItem()
            assertFalse(state)
        }
    }

    @Test
    fun `powerOn turns device on when powered off`() = runTest {
        // Given
        viewModel.togglePower() // Turn off first

        viewModel.isPoweredOff.test {
            assertTrue(awaitItem())

            // When
            viewModel.powerOn()

            // Then
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `powerOn does nothing when already powered on`() = runTest {
        // Given - initial state is on
        val initialState = viewModel.isPoweredOff.value

        // When
        viewModel.powerOn()

        // Then
        assertEquals(initialState, viewModel.isPoweredOff.value)
        assertFalse(viewModel.isPoweredOff.value)
    }

    @Test
    fun `powerOn can be called multiple times safely`() = runTest {
        // Given
        viewModel.togglePower() // Turn off

        // When
        viewModel.powerOn()
        viewModel.powerOn()
        viewModel.powerOn()

        // Then
        viewModel.isPoweredOff.test {
            val state = awaitItem()
            assertFalse(state)
        }
    }

    @Test
    fun `state flow emits initial value`() = runTest {
        // When/Then
        viewModel.isPoweredOff.test {
            val initialState = awaitItem()
            assertFalse(initialState)
        }
    }

    @Test
    fun `state flow emits updates when toggled`() = runTest {
        viewModel.isPoweredOff.test {
            // Initial state
            assertFalse(awaitItem())

            // Toggle to off
            viewModel.togglePower()
            assertTrue(awaitItem())

            // Toggle to on
            viewModel.togglePower()
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `state flow emits updates when powerOn is called`() = runTest {
        viewModel.isPoweredOff.test {
            // Initial state
            assertFalse(awaitItem())

            // Toggle to off
            viewModel.togglePower()
            assertTrue(awaitItem())

            // Power on
            viewModel.powerOn()
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `multiple ViewModels have independent state`() = runTest {
        // Given
        val viewModel2 = PowerViewModel()

        // When
        viewModel.togglePower() // Turn off first VM

        // Then
        viewModel.isPoweredOff.test {
            assertTrue(awaitItem())
        }

        viewModel2.isPoweredOff.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `togglePower is idempotent for state transitions`() = runTest {
        // When
        viewModel.togglePower() // off
        val stateAfterFirstToggle = viewModel.isPoweredOff.value

        viewModel.togglePower() // on
        val stateAfterSecondToggle = viewModel.isPoweredOff.value

        // Then
        assertTrue(stateAfterFirstToggle)
        assertFalse(stateAfterSecondToggle)
    }

    @Test
    fun `powerOn only changes state when device is off`() = runTest {
        // Given - device is on
        viewModel.isPoweredOff.test {
            skipItems(1) // Skip initial emission

            // When - call powerOn while already on
            viewModel.powerOn()

            // Then - no new emission should occur
            expectNoEvents()
        }
    }

    @Test
    fun `state transitions are immediate`() = runTest {
        // Given
        assertFalse(viewModel.isPoweredOff.value)

        // When
        viewModel.togglePower()

        // Then
        assertTrue(viewModel.isPoweredOff.value)

        // When
        viewModel.togglePower()

        // Then
        assertFalse(viewModel.isPoweredOff.value)
    }

    @Test
    fun `StateFlow is cold and emits to multiple collectors`() = runTest {
        // Given
        viewModel.togglePower() // Turn off

        // When/Then - First collector
        viewModel.isPoweredOff.test {
            assertTrue(awaitItem())
        }

        // When/Then - Second collector gets same state
        viewModel.isPoweredOff.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `togglePower alternates state correctly over many iterations`() = runTest {
        // Given
        val iterations = 10

        // When/Then
        for (i in 1..iterations) {
            viewModel.togglePower()
            val expectedState = i % 2 == 1 // Odd iterations should be off
            assertEquals(expectedState, viewModel.isPoweredOff.value)
        }
    }

    @Test
    fun `powerOn after multiple toggles works correctly`() = runTest {
        // Given
        viewModel.togglePower() // off
        viewModel.togglePower() // on
        viewModel.togglePower() // off
        viewModel.togglePower() // on
        viewModel.togglePower() // off

        // When
        viewModel.powerOn()

        // Then
        viewModel.isPoweredOff.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `ViewModel can be instantiated multiple times independently`() = runTest {
        // Given
        val vm1 = PowerViewModel()
        val vm2 = PowerViewModel()
        val vm3 = PowerViewModel()

        // When
        vm1.togglePower()
        vm2.togglePower()
        // vm3 remains unchanged

        // Then
        assertTrue(vm1.isPoweredOff.value)
        assertTrue(vm2.isPoweredOff.value)
        assertFalse(vm3.isPoweredOff.value)
    }

    @Test
    fun `state is consistent across multiple rapid toggles`() = runTest {
        viewModel.isPoweredOff.test {
            assertFalse(awaitItem()) // Initial state

            // Rapid toggles
            viewModel.togglePower()
            assertTrue(awaitItem())

            viewModel.togglePower()
            assertFalse(awaitItem())

            viewModel.togglePower()
            assertTrue(awaitItem())

            viewModel.togglePower()
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `powerOn does not affect subsequent togglePower behavior`() = runTest {
        // Given
        viewModel.togglePower() // off
        viewModel.powerOn() // on

        // When
        viewModel.togglePower() // should turn off

        // Then
        viewModel.isPoweredOff.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `initial value is accessible immediately`() {
        // When
        val initialValue = viewModel.isPoweredOff.value

        // Then
        assertFalse(initialValue)
    }

    @Test
    fun `value updates are synchronous`() {
        // Given
        assertFalse(viewModel.isPoweredOff.value)

        // When
        viewModel.togglePower()

        // Then - value is immediately updated
        assertTrue(viewModel.isPoweredOff.value)
    }
}
