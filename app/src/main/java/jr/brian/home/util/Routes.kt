package jr.brian.home.util

object Routes {
    const val APP_DOCK_SETTINGS = "app_dock_settings"
    const val APP_SEARCH = "app_search"
    const val BACK_BUTTON_SHORTCUT = "back_button_shortcut"
    const val CONTROL_PAD = "control_pad"
    const val CRASH_LOGS = "crash_logs"
    const val CUSTOM_THEME = "custom_theme"
    const val ESDE_SETTINGS = "esde_settings"
    const val ESDE_SYSTEM_APPS = "esde_system_apps"
    const val MARQUEE_PRESS_SHORTCUT = "marquee_press_shortcut"
    const val FAQ = "faq"
    const val LAUNCHER = "launcher"
    const val MONITOR = "monitor"
    const val RECENT_APPS = "recent_apps"
    const val SETTINGS = "settings"
    const val VOLUME_CONTROLS = "volume_controls"
    const val WIDGET_PICKER = "widget_picker/{pageIndex}"

    fun widgetPicker(pageIndex: Int) = "widget_picker/$pageIndex"
}