package jr.brian.home.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun <T> rememberDialogState(
    initialItem: T? = null
): DialogState<T> {
    val hapticFeedback = LocalHapticFeedback.current
    return remember {
        DialogState(initialItem, hapticFeedback)
    }
}

class DialogState<T>(
    initialItem: T? = null,
    private val hapticFeedback: HapticFeedback? = null
) {
    var isVisible by mutableStateOf(false)
        private set
    
    var item by mutableStateOf(initialItem)
        private set
    
    fun show(item: T? = null) {
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        this.item = item
        isVisible = true
    }
    
    fun dismiss() {
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        isVisible = false
        item = null
    }
}
