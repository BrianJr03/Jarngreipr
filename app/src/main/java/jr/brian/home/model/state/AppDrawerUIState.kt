package jr.brian.home.model.state

import jr.brian.home.model.app.AppInfo

data class AppDrawerUIState(
    val allApps: List<AppInfo> = emptyList(),
    val allAppsUnfiltered: List<AppInfo> = emptyList(),
    val isLoading: Boolean = false,
)