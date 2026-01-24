package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import jr.brian.home.model.ControlPadItem
import jr.brian.home.model.PhysicalButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ControlPadManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _controlPadItems = MutableStateFlow(loadControlPadItems())
    val controlPadItems: StateFlow<List<ControlPadItem>> = _controlPadItems.asStateFlow()

    private fun loadControlPadItems(): List<ControlPadItem> {
        return (0 until CONTROL_PAD_COUNT).map { index ->
            val label = prefs.getString("${KEY_ITEM_LABEL}_$index", "${index + 1}") ?: "${index + 1}"
            val buttonName = prefs.getString("${KEY_ITEM_BUTTON}_$index", null)
            val mappedButton = buttonName?.let {
                try {
                    PhysicalButton.valueOf(it)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            ControlPadItem(label = label, mappedButton = mappedButton)
        }
    }

    fun setControlPadItem(index: Int, item: ControlPadItem) {
        if (index !in 0 until CONTROL_PAD_COUNT) return

        val updatedItems = _controlPadItems.value.toMutableList()
        updatedItems[index] = item
        _controlPadItems.value = updatedItems

        prefs.edit().apply {
            putString("${KEY_ITEM_LABEL}_$index", item.label)
            putString("${KEY_ITEM_BUTTON}_$index", item.mappedButton?.name)
            apply()
        }
    }

    fun resetAllMappings() {
        val defaultItems = (0 until CONTROL_PAD_COUNT).map { index ->
            ControlPadItem(label = "${index + 1}", mappedButton = null)
        }
        _controlPadItems.value = defaultItems

        prefs.edit().apply {
            for (index in 0 until CONTROL_PAD_COUNT) {
                putString("${KEY_ITEM_LABEL}_$index", "${index + 1}")
                remove("${KEY_ITEM_BUTTON}_$index")
            }
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "control_pad_prefs"
        private const val KEY_ITEM_LABEL = "item_label"
        private const val KEY_ITEM_BUTTON = "item_button"
        private const val CONTROL_PAD_COUNT = 6
    }
}
