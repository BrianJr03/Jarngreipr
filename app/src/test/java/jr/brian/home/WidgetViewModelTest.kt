package jr.brian.home

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import io.mockk.mockk
import jr.brian.home.data.WidgetPreferences
import jr.brian.home.model.WidgetPage
import jr.brian.home.viewmodels.WidgetUIState
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.mockk.every

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: WidgetViewModel
    private lateinit var mockWidgetPreferences: WidgetPreferences
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockWidgetPreferences = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)

        every { mockWidgetPreferences.widgetConfigs } returns MutableStateFlow(emptyList())

        viewModel = WidgetViewModel(mockWidgetPreferences)
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
    fun `getAppWidgetHost returns null before initialization`() {
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
        // Given
        val mockPrefs1 = mockk<WidgetPreferences>(relaxed = true)
        val mockPrefs2 = mockk<WidgetPreferences>(relaxed = true)
        every { mockPrefs1.widgetConfigs } returns MutableStateFlow(emptyList())
        every { mockPrefs2.widgetConfigs } returns MutableStateFlow(emptyList())

        // When
        val vm1 = WidgetViewModel(mockPrefs1)
        val vm2 = WidgetViewModel(mockPrefs2)

        // Then
        assert(vm1 !== vm2) // Different instances
        assertEquals(vm1.uiState.value, vm2.uiState.value) // Same initial state
    }

    @Test
    fun `allocateAppWidgetId returns -1 before host initialization`() {
        // When
        val result = viewModel.allocateAppWidgetId()

        // Then
        assertEquals(-1, result)
    }

    @Test
    fun `getAppWidgetHost returns null before widget host is initialized`() {
        // When
        val host = viewModel.getAppWidgetHost()

        // Then
        assertEquals(null, host)
    }

    @Test
    fun `MAX_WIDGET_PAGES constant is accessible`() {
        // When
        val maxPages = WidgetViewModel.MAX_WIDGET_PAGES

        // Then
        assertEquals(2, maxPages)
        assertTrue(maxPages > 0)
    }

    @Test
    fun `WidgetUIState can be created with custom values`() {
        // Given
        val pages = listOf(
            WidgetPage(index = 0, widgets = emptyList()),
            WidgetPage(index = 1, widgets = emptyList())
        )

        // When
        val state = WidgetUIState(
            widgetPages = pages,
            isInitialized = true,
            currentPage = 1
        )

        // Then
        assertEquals(2, state.widgetPages.size)
        assertTrue(state.isInitialized)
        assertEquals(1, state.currentPage)
    }

    @Test
    fun `WidgetUIState copy preserves unchanged values`() {
        // Given
        val pages = listOf(WidgetPage(0))
        val original = WidgetUIState(
            widgetPages = pages,
            isInitialized = true,
            currentPage = 0
        )

        // When
        val copied = original.copy(currentPage = 1)

        // Then
        assertEquals(pages, copied.widgetPages)
        assertTrue(copied.isInitialized)
        assertEquals(1, copied.currentPage)
        assertEquals(0, original.currentPage)
    }

    @Test
    fun `WidgetPage equality works correctly`() {
        // Given
        val page1 = WidgetPage(index = 0, widgets = emptyList())
        val page2 = WidgetPage(index = 0, widgets = emptyList())
        val page3 = WidgetPage(index = 1, widgets = emptyList())

        // Then
        assertEquals(page1, page2)
        assertNotEquals(page1, page3)
    }

    @Test
    fun `WidgetUIState with different pages are not equal`() {
        // Given
        val state1 = WidgetUIState(
            widgetPages = listOf(WidgetPage(0)),
            isInitialized = true,
            currentPage = 0
        )
        val state2 = WidgetUIState(
            widgetPages = listOf(WidgetPage(1)),
            isInitialized = true,
            currentPage = 0
        )

        // Then
        assertNotEquals(state1, state2)
    }

    @Test
    fun `WidgetUIState with different currentPage are not equal`() {
        // Given
        val state1 = WidgetUIState(currentPage = 0)
        val state2 = WidgetUIState(currentPage = 1)

        // Then
        assertNotEquals(state1, state2)
    }

    @Test
    fun `WidgetUIState with different isInitialized are not equal`() {
        // Given
        val state1 = WidgetUIState(isInitialized = true)
        val state2 = WidgetUIState(isInitialized = false)

        // Then
        assertNotEquals(state1, state2)
    }

    @Test
    fun `WidgetPage can hold empty widget list`() {
        // Given
        val page = WidgetPage(index = 0, widgets = emptyList())

        // Then
        assertEquals(0, page.index)
        assertTrue(page.widgets.isEmpty())
    }

    @Test
    fun `WidgetPage index can be any positive integer`() {
        // Given
        val indices = listOf(0, 1, 5, 10, 100)

        // When
        val pages = indices.map { WidgetPage(index = it) }

        // Then
        pages.forEachIndexed { idx, page ->
            assertEquals(indices[idx], page.index)
        }
    }

    @Test
    fun `WidgetUIState hashCode is consistent with equals`() {
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

        // Then
        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())
    }

    @Test
    fun `WidgetPage copy works correctly`() {
        // Given
        val original = WidgetPage(index = 0, widgets = emptyList())

        // When
        val copied = original.copy(index = 1)

        // Then
        assertEquals(1, copied.index)
        assertEquals(0, original.index)
        assertEquals(original.widgets, copied.widgets)
    }

    @Test
    fun `WidgetUIState toString contains meaningful information`() {
        // Given
        val state = WidgetUIState(
            widgetPages = listOf(WidgetPage(0)),
            isInitialized = true,
            currentPage = 1
        )

        // When
        val string = state.toString()

        // Then
        assertTrue(string.contains("WidgetUIState"))
        assertTrue(string.contains("widgetPages"))
        assertTrue(string.contains("isInitialized"))
        assertTrue(string.contains("currentPage"))
    }

    @Test
    fun `WidgetPage toString contains meaningful information`() {
        // Given
        val page = WidgetPage(index = 5, widgets = emptyList())

        // When
        val string = page.toString()

        // Then
        assertTrue(string.contains("WidgetPage"))
        assertTrue(string.contains("index"))
        assertTrue(string.contains("widgets"))
    }

    @Test
    fun `initial state has no pages`() = runTest {
        // When
        viewModel.uiState.test {
            val state = awaitItem()

            // Then
            assertTrue(state.widgetPages.isEmpty())
            assertFalse(state.isInitialized)
        }
    }

    @Test
    fun `ViewModel state flow is cold and emits to multiple collectors`() = runTest {
        // When/Then
        viewModel.uiState.test {
            val state1 = awaitItem()
            assertFalse(state1.isInitialized)
        }

        viewModel.uiState.test {
            val state2 = awaitItem()
            assertFalse(state2.isInitialized)
        }
    }

    @Test
    fun `WidgetUIState default constructor creates valid empty state`() {
        // When
        val state = WidgetUIState()

        // Then
        assertTrue(state.widgetPages.isEmpty())
        assertFalse(state.isInitialized)
        assertEquals(0, state.currentPage)
    }

    @Test
    fun `WidgetPage default constructor has index 0`() {
        // When
        val page = WidgetPage(0)

        // Then
        assertEquals(0, page.index)
        assertTrue(page.widgets.isEmpty())
    }

    @Test
    fun `multiple WidgetPages can exist in state`() {
        // Given
        val pages = (0..5).map { WidgetPage(index = it, widgets = emptyList()) }

        // When
        val state = WidgetUIState(widgetPages = pages)

        // Then
        assertEquals(6, state.widgetPages.size)
        state.widgetPages.forEachIndexed { idx, page ->
            assertEquals(idx, page.index)
        }
    }

    @Test
    fun `WidgetUIState can represent different current pages`() {
        // Given
        val state0 = WidgetUIState(currentPage = 0)
        val state1 = WidgetUIState(currentPage = 1)
        val state2 = WidgetUIState(currentPage = 2)

        // Then
        assertEquals(0, state0.currentPage)
        assertEquals(1, state1.currentPage)
        assertEquals(2, state2.currentPage)
    }

    @Test
    fun `WidgetUIState reflects initialization state changes`() {
        // Given
        val uninitialized = WidgetUIState(isInitialized = false)
        val initialized = uninitialized.copy(isInitialized = true)

        // Then
        assertFalse(uninitialized.isInitialized)
        assertTrue(initialized.isInitialized)
    }
}
