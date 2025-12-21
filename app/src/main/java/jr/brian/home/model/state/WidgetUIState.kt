package jr.brian.home.model.state

import jr.brian.home.model.widget.WidgetPage

data class WidgetUIState(
    val widgetPages: List<WidgetPage> = emptyList(),
    val isInitialized: Boolean = false,
    val currentPage: Int = 0,
    val editModeByPage: Map<Int, Boolean> = emptyMap()
)