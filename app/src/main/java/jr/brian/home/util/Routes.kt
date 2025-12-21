package jr.brian.home.util

object Routes {
    const val LAUNCHER = "launcher"
    const val SETTINGS = "settings"
    const val FAQ = "faq"
    const val CUSTOM_THEME = "custom_theme"
    const val APP_SEARCH = "app_search"
    const val BACK_BUTTON_SHORTCUT = "back_button_shortcut"
    const val MONITOR = "monitor"
    const val WIDGET_PICKER = "widget_picker/{pageIndex}"

    fun widgetPicker(pageIndex: Int) = "widget_picker/$pageIndex"
}