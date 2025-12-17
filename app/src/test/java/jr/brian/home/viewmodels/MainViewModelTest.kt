package jr.brian.home.viewmodels

import android.graphics.drawable.Drawable
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import io.mockk.mockk
import jr.brian.home.data.IconPackManager
import jr.brian.home.model.AppInfo
import jr.brian.home.model.state.AppDrawerUIState
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
class MainViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: MainViewModel
    private lateinit var iconPackManager: IconPackManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        iconPackManager = mockk(relaxed = true)
        viewModel = MainViewModel(iconPackManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has correct default values`() = runTest {
        // When
        viewModel.uiState.test {
            val state = awaitItem()

            // Then
            assertEquals(emptyList<AppInfo>(), state.allApps)
            assertEquals(emptyList<AppInfo>(), state.allAppsUnfiltered)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `AppDrawerUIState has correct default values`() {
        // When
        val state = AppDrawerUIState()

        // Then
        assertEquals(emptyList<AppInfo>(), state.allApps)
        assertEquals(emptyList<AppInfo>(), state.allAppsUnfiltered)
        assertFalse(state.isLoading)
    }

    @Test
    fun `AppDrawerUIState can be copied with new values`() {
        // Given
        val originalState = AppDrawerUIState(
            allApps = emptyList(),
            allAppsUnfiltered = emptyList(),
            isLoading = false
        )

        // When
        val newState = originalState.copy(isLoading = true)

        // Then
        assertFalse(originalState.isLoading)
        assertTrue(newState.isLoading)
    }

    @Test
    fun `AppDrawerUIState equality works correctly`() {
        // Given
        val state1 = AppDrawerUIState(
            allApps = emptyList(),
            allAppsUnfiltered = emptyList(),
            isLoading = false
        )
        val state2 = AppDrawerUIState(
            allApps = emptyList(),
            allAppsUnfiltered = emptyList(),
            isLoading = false
        )
        val state3 = AppDrawerUIState(
            allApps = emptyList(),
            allAppsUnfiltered = emptyList(),
            isLoading = true
        )

        // Then
        assertEquals(state1, state2)
        assertTrue(state1 != state3)
    }

    @Test
    fun `AppDrawerUIState toString contains all properties`() {
        // Given
        val state = AppDrawerUIState(
            allApps = emptyList(),
            allAppsUnfiltered = emptyList(),
            isLoading = true
        )

        // When
        val stringRepresentation = state.toString()

        // Then
        assertTrue(stringRepresentation.contains("allApps"))
        assertTrue(stringRepresentation.contains("allAppsUnfiltered"))
        assertTrue(stringRepresentation.contains("isLoading"))
    }

    @Test
    fun `AppDrawerUIState hashCode is consistent`() {
        // Given
        val state = AppDrawerUIState(
            allApps = emptyList(),
            allAppsUnfiltered = emptyList(),
            isLoading = true
        )

        // When
        val hash1 = state.hashCode()
        val hash2 = state.hashCode()

        // Then
        assertEquals(hash1, hash2)
    }

    @Test
    fun `AppDrawerUIState hashCode is consistent with equals`() {
        // Given
        val state1 = AppDrawerUIState(
            allApps = emptyList(),
            allAppsUnfiltered = emptyList(),
            isLoading = true
        )
        val state2 = AppDrawerUIState(
            allApps = emptyList(),
            allAppsUnfiltered = emptyList(),
            isLoading = true
        )

        // Then
        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())
    }

    @Test
    fun `AppDrawerUIState with different allApps are not equal`() {
        // Given
        val mockIcon = mockk<Drawable>(relaxed = true)
        val app = AppInfo(
            label = "Test",
            packageName = "com.test",
            icon = mockIcon
        )
        val state1 = AppDrawerUIState(allApps = emptyList())
        val state2 = AppDrawerUIState(allApps = listOf(app))

        // Then
        assertTrue(state1 != state2)
    }

    @Test
    fun `AppDrawerUIState with different isLoading values are not equal`() {
        // Given
        val state1 = AppDrawerUIState(isLoading = false)
        val state2 = AppDrawerUIState(isLoading = true)

        // Then
        assertTrue(state1 != state2)
    }

    @Test
    fun `AppDrawerUIState copy preserves unchanged values`() {
        // Given
        val mockIcon = mockk<Drawable>(relaxed = true)
        val apps = listOf(
            AppInfo(
                label = "Test",
                packageName = "com.test",
                icon = mockIcon
            )
        )
        val original = AppDrawerUIState(
            allApps = apps,
            allAppsUnfiltered = apps,
            isLoading = false
        )

        // When
        val copied = original.copy(isLoading = true)

        // Then
        assertEquals(apps, copied.allApps)
        assertEquals(apps, copied.allAppsUnfiltered)
        assertTrue(copied.isLoading)
        assertFalse(original.isLoading)
    }

    @Test
    fun `ViewModel can be instantiated`() {
        // Given/When
        val vm = MainViewModel(iconPackManager)

        // Then
        assertTrue(vm.uiState.value.allApps.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `ViewModel state flow emits initial state`() = runTest {
        // When/Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.isLoading)
            assertTrue(initialState.allApps.isEmpty())
            assertTrue(initialState.allAppsUnfiltered.isEmpty())
        }
    }

    @Test
    fun `ViewModel state flow is cold and emits to multiple collectors`() = runTest {
        // When/Then - First collector
        viewModel.uiState.test {
            val state1 = awaitItem()
            assertFalse(state1.isLoading)
        }

        // When/Then - Second collector gets same state
        viewModel.uiState.test {
            val state2 = awaitItem()
            assertFalse(state2.isLoading)
        }
    }

    @Test
    fun `multiple ViewModels can be created independently`() {
        // Given
        val mockManager1 = mockk<IconPackManager>(relaxed = true)
        val mockManager2 = mockk<IconPackManager>(relaxed = true)

        // When
        val vm1 = MainViewModel(mockManager1)
        val vm2 = MainViewModel(mockManager2)

        // Then
        assertTrue(vm1 !== vm2) // Different instances
        assertEquals(vm1.uiState.value, vm2.uiState.value) // Same initial state
    }

    @Test
    fun `AppDrawerUIState data class has correct properties`() {
        // Given
        val mockIcon = mockk<Drawable>(relaxed = true)
        val apps = listOf(
            AppInfo(
                label = "App One",
                packageName = "com.example.one",
                icon = mockIcon
            ),
            AppInfo(
                label = "App Two",
                packageName = "com.example.two",
                icon = mockIcon
            )
        )

        // When
        val state = AppDrawerUIState(
            allApps = apps,
            allAppsUnfiltered = apps,
            isLoading = true
        )

        // Then
        assertEquals(2, state.allApps.size)
        assertEquals(2, state.allAppsUnfiltered.size)
        assertTrue(state.isLoading)
        assertEquals("App One", state.allApps[0].label)
        assertEquals("App Two", state.allApps[1].label)
    }

    @Test
    fun `initial value is accessible immediately`() {
        // When
        val initialValue = viewModel.uiState.value

        // Then
        assertFalse(initialValue.isLoading)
        assertTrue(initialValue.allApps.isEmpty())
    }

    @Test
    fun `AppDrawerUIState default constructor creates valid empty state`() {
        // When
        val state = AppDrawerUIState()

        // Then
        assertTrue(state.allApps.isEmpty())
        assertTrue(state.allAppsUnfiltered.isEmpty())
        assertFalse(state.isLoading)
    }
}
