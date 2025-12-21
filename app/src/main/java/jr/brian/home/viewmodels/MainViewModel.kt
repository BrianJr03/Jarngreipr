package jr.brian.home.viewmodels

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.data.IconPackManager
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.state.AppDrawerUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val iconPackManager: IconPackManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppDrawerUIState())
    val uiState = _uiState.asStateFlow()

    fun loadAllApps(
        context: Context,
        includeSystemApps: Boolean = true,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val pm: PackageManager = context.packageManager

            val mainIntent =
                Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

            val resolveInfos =
                pm.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
                )

            val allAppInfos =
                resolveInfos
                    .mapNotNull { resolveInfo ->
                        val appInfo = resolveInfo.activityInfo.applicationInfo
                        val packageName = resolveInfo.activityInfo.packageName

                        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val isUpdatedSystemApp =
                            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                        if (isSystemApp && !isUpdatedSystemApp && !includeSystemApps) {
                            return@mapNotNull null
                        }

                        val category = appInfo.category
                        val label = resolveInfo.loadLabel(pm).toString()
                        val activityName = resolveInfo.activityInfo.name

                        val defaultIcon = resolveInfo.loadIcon(pm)

                        val icon = iconPackManager.getIconForApp(packageName, activityName)
                            ?: iconPackManager.applyIconMask(defaultIcon)
                            ?: defaultIcon

                        AppInfo(
                            label = label,
                            packageName = packageName,
                            icon = icon,
                            category = category,
                            activityName = activityName,
                        )
                    }.distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }

            _uiState.value =
                _uiState.value.copy(
                    allApps = allAppInfos,
                    allAppsUnfiltered = allAppInfos,
                    isLoading = false,
                )
        }
    }
}