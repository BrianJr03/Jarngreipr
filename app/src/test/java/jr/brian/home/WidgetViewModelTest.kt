package jr.brian.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import jr.brian.home.model.WidgetPage
import jr.brian.home.viewmodels.WidgetUIState
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: WidgetViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = WidgetViewModel()
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
            assertEquals(emptyList<WidgetPage>(), state.widgetPages)
            assertFalse(state.isInitialized)
            assertEquals(0, state.currentPage)
        }
    }

    @Test
    fun `WidgetUIState data class has correct properties`() {
        // Given
        val widgetPages = listOf(
            WidgetPage(index = 0, widgets = emptyList()),
            WidgetPage(index = 1, widgets = emptyList())
        )

        // When
        val state = WidgetUIState(
            widgetPages = widgetPages,
            isInitialized = true,
            currentPage = 1
        )

        // Then
        assertEquals(2, state.widgetPages.size)
        assertEquals(true, state.isInitialized)
        assertEquals(1, state.currentPage)
        assertEquals(0, state.widgetPages[0].index)
        assertEquals(1, state.widgetPages[1].index)
    }

    @Test
    fun `WidgetUIState default values are correct`() {
        // When
        val state = WidgetUIState()

        // Then
        assertEquals(emptyList<WidgetPage>(), state.widgetPages)
        assertEquals(false, state.isInitialized)
        assertEquals(0, state.currentPage)
    }

    @Test
    fun `MAX_WIDGET_PAGES constant has expected value`() {
        // When
        val maxPages = WidgetViewModel.MAX_WIDGET_PAGES

        // Then
        assertEquals(2, maxPages)
    }

    @Test
    fun `allocateAppWidgetId returns -1 when host is not initialized`() {
        // When
        val result = viewModel.allocateAppWidgetId()

        // Then
        assertEquals(-1, result)
    }

    @Test
    fun `getAppWidgetHost returns null when not initialized`() {
        // When
        val result = viewModel.getAppWidgetHost()

        // Then
        assertEquals(null, result)
    }

    @Test
    fun `WidgetUIState can be copied with new values`() {
        // Given
        val originalState = WidgetUIState(
            widgetPages = emptyList(),
            isInitialized = false,
            currentPage = 0
        )

        // When
        val newState = originalState.copy(
            isInitialized = true,
            currentPage = 1
        )

        // Then
        assertEquals(false, originalState.isInitialized)
        assertEquals(0, originalState.currentPage)
        assertEquals(true, newState.isInitialized)
        assertEquals(1, newState.currentPage)
    }

    @Test
    fun `WidgetUIState equality works correctly`() {
        // Given
        val state1 = WidgetUIState(
            widgetPages = emptyList(),
            isInitialized = true,
            currentPage = 0
        )
        val state2 = WidgetUIState(
            widgetPages = emptyList(),
            isInitialized = true,
            currentPage = 0
        )
        val state3 = WidgetUIState(
            widgetPages = emptyList(),
            isInitialized = false,
            currentPage = 0
        )

        // Then
        assertEquals(state1, state2)
        assert(state1 != state3)
    }

    @Test
    fun `WidgetPage data class works correctly`() {
        // Given
        val page = WidgetPage(
            index = 1,
            widgets = emptyList()
        )

        // Then
        assertEquals(1, page.index)
        assertEquals(0, page.widgets.size)
    }

    @Test
    fun `multiple WidgetPages can be created with different indices`() {
        // Given
        val pages = (0 until WidgetViewModel.MAX_WIDGET_PAGES).map { index ->
            WidgetPage(index = index, widgets = emptyList())
        }

        // Then
        assertEquals(WidgetViewModel.MAX_WIDGET_PAGES, pages.size)
        pages.forEachIndexed { expectedIndex, page ->
            assertEquals(expectedIndex, page.index)
        }
    }

    @Test
    fun `ViewModel state flow emits initial state`() = runTest {
        // When/Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.isInitialized)
            assertEquals(0, initialState.currentPage)
        }
    }

    @Test
    fun `WidgetUIState toString contains all properties`() {
        // Given
        val state = WidgetUIState(
            widgetPages = listOf(WidgetPage(0)),
            isInitialized = true,
            currentPage = 0
        )

        // When
        val stringRepresentation = state.toString()

        // Then
        assert(stringRepresentation.contains("widgetPages"))
        assert(stringRepresentation.contains("isInitialized"))
        assert(stringRepresentation.contains("currentPage"))
    }

    @Test
    fun `WidgetUIState hashCode is consistent`() {
        // Given
        val state = WidgetUIState(
            widgetPages = emptyList(),
            isInitialized = true,
            currentPage = 1
        )

        // When
        val hash1 = state.hashCode()
        val hash2 = state.hashCode()

        // Then
        assertEquals(hash1, hash2)
    }

    @Test
    fun `ViewModel can be created multiple times`() {
        // When
        val vm1 = WidgetViewModel()
        val vm2 = WidgetViewModel()

        // Then
        assert(vm1 !== vm2) // Different instances
        assertEquals(vm1.uiState.value, vm2.uiState.value) // Same initial state
    }
}
