package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DockManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _dockApps = MutableStateFlow(loadDockApps())
    val dockApps: StateFlow<List<String>> = _dockApps.asStateFlow()

    private fun loadDockApps(): List<String> {
        val slotCount = prefs.getInt(KEY_DOCK_SLOT_COUNT, 0)
        if (slotCount == 0) return emptyList()
        
        return (0 until slotCount).map { dockIndex ->
            prefs.getString("${KEY_DOCK_APP_PREFIX}$dockIndex", null) ?: ""
        }
    }

    fun addEmptySlot(position: Int) {
        if (position !in 0 until MAX_DOCK_APPS) return
        
        val currentDockApps = _dockApps.value.toMutableList()
        
        while (currentDockApps.size <= position) {
            currentDockApps.add("")
        }
        
        _dockApps.value = currentDockApps
        saveDockApps(currentDockApps)
    }

    fun addAppToDock(packageName: String, position: Int) {
        if (position !in 0 until MAX_DOCK_APPS) return
        
        val currentDockApps = _dockApps.value.toMutableList()
        
        while (currentDockApps.size <= position) {
            currentDockApps.add("")
        }
        
        if (currentDockApps.count { it.isNotEmpty() } >= MAX_DOCK_APPS) return
        if (currentDockApps.contains(packageName)) return
        
        currentDockApps[position] = packageName
        _dockApps.value = currentDockApps
        saveDockApps(currentDockApps)
    }

    fun removeAppFromDock(packageName: String) {
        val currentDockApps = _dockApps.value.toMutableList()
        
        val index = currentDockApps.indexOf(packageName)
        if (index == -1) return
        
        currentDockApps[index] = ""
        _dockApps.value = currentDockApps
        saveDockApps(currentDockApps)
    }

    fun removeEmptySlot(position: Int) {
        val currentDockApps = _dockApps.value.toMutableList()
        
        if (position !in currentDockApps.indices) return
        if (currentDockApps[position].isNotEmpty()) return
        
        currentDockApps.removeAt(position)
        _dockApps.value = currentDockApps
        
        if (currentDockApps.isEmpty()) {
            prefs.edit().apply {
                for (i in 0 until MAX_DOCK_APPS) {
                    remove("${KEY_DOCK_APP_PREFIX}$i")
                }
                remove(KEY_DOCK_SLOT_COUNT)
                apply()
            }
        } else {
            saveDockApps(currentDockApps)
        }
    }

    fun isAppInDock(packageName: String): Boolean {
        return _dockApps.value.contains(packageName)
    }

    private fun saveDockApps(apps: List<String>) {
        prefs.edit().apply {
            putInt(KEY_DOCK_SLOT_COUNT, apps.size)
            
            for (i in 0 until MAX_DOCK_APPS) {
                remove("${KEY_DOCK_APP_PREFIX}$i")
            }
            
            apps.forEachIndexed { index, packageName ->
                if (packageName.isNotEmpty()) {
                    putString("${KEY_DOCK_APP_PREFIX}$index", packageName)
                }
            }
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "dock_prefs"
        private const val KEY_DOCK_APP_PREFIX = "dock_app_"
        private const val KEY_DOCK_SLOT_COUNT = "dock_slot_count"
        const val MAX_DOCK_APPS = 5
    }
}
