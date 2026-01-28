package jr.brian.home.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun <T> rememberDialogState(
    initialItem: T? = null
): DialogState<T> {
    return remember {
        DialogState(initialItem)
    }
}

class DialogState<T>(
    initialItem: T? = null
) {
    var isVisible by mutableStateOf(false)
        private set
    
    var item by mutableStateOf(initialItem)
        private set
    
    fun show(item: T? = null) {
        this.item = item
        isVisible = true
    }
    
    fun dismiss() {
        isVisible = false
        item = null
    }
}
