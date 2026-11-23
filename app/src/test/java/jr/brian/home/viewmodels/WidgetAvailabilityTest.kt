package jr.brian.home.viewmodels

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.util.Log
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.data.PageTypeManager
import jr.brian.home.data.WidgetPageAppManager
import jr.brian.home.data.WidgetPreferences
import jr.brian.home.model.PageType
import jr.brian.home.model.widget.WidgetConfig
import jr.brian.home.model.widget.WidgetInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetAvailabilityTest {
    private val dispatcher = StandardTestDispatcher()
    private val saved = MutableStateFlow<List<WidgetConfig>>(emptyList())
    private val providers = mutableMapOf<Int, AppWidgetProviderInfo>()
    private val manager = mockk<AppWidgetManager>()
    private val preferences = mockk<WidgetPreferences>()
    private val pageTypes = mockk<PageTypeManager>()
    private lateinit var viewModel: WidgetViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(AppWidgetManager::class, Log::class)
        mockkConstructor(AppWidgetHost::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { AppWidgetManager.getInstance(any()) } returns manager
        every { manager.getAppWidgetInfo(any()) } answers { providers[firstArg()] }
        every { anyConstructed<AppWidgetHost>().startListening() } returns Unit
        every { anyConstructed<AppWidgetHost>().deleteAppWidgetId(any()) } returns Unit
        every { preferences.widgetConfigs } returns saved
        coEvery { preferences.removeWidgetConfig(any()) } answers {
            saved.value = saved.value.filter { it.widgetId != firstArg<Int>() }
        }
        coEvery { preferences.addWidgetConfig(any()) } answers {
            val config = firstArg<WidgetConfig>()
            saved.value = saved.value.filter { it.widgetId != config.widgetId } + config
        }
        every { pageTypes.pageTypes } returns MutableStateFlow(
            listOf(PageType.APPS_AND_WIDGETS_TAB, PageType.APPS_AND_WIDGETS_TAB)
        )
        viewModel = WidgetViewModel(preferences, pageTypes, mockk<WidgetPageAppManager>(relaxed = true))
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun config(id: Int, page: Int = 0, order: Int = id) = WidgetConfig(
        widgetId = id,
        providerClassName = "Provider$id",
        providerPackageName = "pkg",
        pageIndex = page,
        order = order,
    )

    private fun provider(): AppWidgetProviderInfo = AppWidgetProviderInfo().apply {
        provider = mockk<ComponentName>(relaxed = true)
    }

    private fun start(vararg configs: WidgetConfig) {
        saved.value = configs.toList()
        viewModel.initializeWidgetHost(mockk<Context>(relaxed = true))
        dispatcher.scheduler.advanceUntilIdle()
    }

    private fun page(index: Int = 0) = viewModel.uiState.value.widgetPages[index]

    private fun verifyDeleted(id: Int, times: Int = 1) =
        verify(exactly = times) { anyConstructed<AppWidgetHost>().deleteAppWidgetId(id) }

    @Test
    fun `missing provider keeps the id and config without blocking neighbors`() {
        providers[2] = provider()

        start(config(1), config(2))

        verifyDeleted(1, times = 0)
        assertEquals(listOf(1, 2), saved.value.map { it.widgetId })
        assertEquals(listOf(1), page().unavailableWidgets.map { it.widgetId })
        assertEquals(listOf(2), page().widgets.map { it.widgetId })
    }

    @Test
    fun `returning provider restores the widget in its saved position`() {
        providers[1] = provider()
        providers[3] = provider()
        start(config(1), config(2), config(3))

        providers[2] = provider()
        viewModel.refreshUnavailableWidgets()

        assertEquals(listOf(1, 2, 3), page().widgets.map { it.widgetId })
        assertTrue(page().unavailableWidgets.isEmpty())
        verifyDeleted(2, times = 0)
    }

    @Test
    fun `refresh leaves a still missing widget unavailable`() {
        start(config(1))

        viewModel.refreshUnavailableWidgets()

        assertEquals(listOf(1), page().unavailableWidgets.map { it.widgetId })
        assertEquals(listOf(1), saved.value.map { it.widgetId })
    }

    @Test
    fun `user removal deletes an unavailable widget`() {
        start(config(1), config(2))

        viewModel.removeWidgetFromPage(1, 0)
        dispatcher.scheduler.advanceUntilIdle()

        verifyDeleted(1)
        assertEquals(listOf(2), saved.value.map { it.widgetId })
        assertEquals(listOf(2), page().unavailableWidgets.map { it.widgetId })
    }

    @Test
    fun `a new widget never takes the slot of an unavailable one`() {
        providers[1] = provider()
        start(config(1, order = 0), config(2, order = 5))

        val added = WidgetInfo(widgetId = 3, providerInfo = provider())
        viewModel.addWidgetToPage(added, 0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(6, saved.value.first { it.widgetId == 3 }.order)
    }

    @Test
    fun `deleting a page releases its unavailable widgets and shifts later ones`() {
        start(config(1, page = 0), config(2, page = 1))

        viewModel.deletePage(0)
        dispatcher.scheduler.advanceUntilIdle()

        verifyDeleted(1)
        verifyDeleted(2, times = 0)
        assertEquals(listOf(2 to 0), saved.value.map { it.widgetId to it.pageIndex })
    }

    @Test
    fun `canvas removal keeps an id another canvas tile still uses`() {
        start()
        val other = CanvasLayout(items = listOf(CanvasItem.WidgetItem("w2", 42)))

        viewModel.releaseWidgetIdIfUnreferenced(42) { listOf(other) }
        dispatcher.scheduler.advanceUntilIdle()

        verifyDeleted(42, times = 0)
    }

    @Test
    fun `canvas removal keeps an id a widget page still uses`() {
        providers[42] = provider()
        start(config(42))

        viewModel.releaseWidgetIdIfUnreferenced(42) { emptyList() }
        dispatcher.scheduler.advanceUntilIdle()

        verifyDeleted(42, times = 0)
    }

    @Test
    fun `canvas removal deletes the last reference`() {
        start()

        viewModel.releaseWidgetIdIfUnreferenced(42) { listOf(CanvasLayout()) }
        dispatcher.scheduler.advanceUntilIdle()

        verifyDeleted(42)
    }
}
