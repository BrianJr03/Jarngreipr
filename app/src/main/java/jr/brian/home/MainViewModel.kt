package jr.brian.home

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jr.brian.home.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppDrawerUIState())
    val uiState = _uiState.asStateFlow()

    fun loadAllApps(
        context: Context,
        includeSystemApps: Boolean = false,
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

            val appInfos =
                resolveInfos
                    .mapNotNull { resolveInfo ->
                        val appInfo = resolveInfo.activityInfo.applicationInfo
                        val packageName = resolveInfo.activityInfo.packageName

                        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                        if (isSystemApp && !includeSystemApps) {
                            return@mapNotNull null
                        }

                        val category = appInfo.category
                        val label = resolveInfo.loadLabel(pm).toString()

                        AppInfo(
                            label = label,
                            packageName = packageName,
                            icon = resolveInfo.loadIcon(pm),
                            category = category,
                        )
                    }.distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase() }


            _uiState.value =
                _uiState.value.copy(
                    allApps = appInfos,
                    isLoading = false,
                )

            Log.d("AppDrawer", "Loaded ${appInfos.size} total apps")
        }
    }
}

data class AppDrawerUIState(
    val allApps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = false,
)