package jr.brian.home.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.focus.FocusRequester

/**
 * Creates a FocusRequester that automatically requests focus when mounted.
 * 
 * Use this for components that should gain focus immediately when displayed.
 *
 * @return A FocusRequester that will request focus on composition
 */
@Composable
fun rememberAutoFocus(): FocusRequester {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    return focusRequester
}

/**
 * Creates a FocusRequester that conditionally requests focus.
 * 
 * Use this when focus should be requested based on a condition.
 *
 * @param shouldFocus Whether focus should be requested
 * @return A FocusRequester
 */
@Composable
fun rememberConditionalFocus(shouldFocus: Boolean): FocusRequester {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(shouldFocus) {
        if (shouldFocus) {
            focusRequester.requestFocus()
        }
    }
    
    return focusRequester
}

/**
 * Creates a map of FocusRequesters for managing focus in lists or grids.
 * 
 * Use this for components that need to manage focus across multiple items.
 *
 * @return A SnapshotStateMap that can store FocusRequesters by index
 */
@Composable
fun rememberFocusRequesterMap(): SnapshotStateMap<Int, FocusRequester> {
    return remember { androidx.compose.runtime.mutableStateMapOf() }
}

/**
 * Creates a FocusManager for managing multiple focus requesters.
 * 
 * Use this when you have a known number of focusable items.
 *
 * @param itemCount The number of items to manage focus for
 * @return A FocusManager instance
 */
@Composable
fun rememberFocusManager(itemCount: Int): FocusManager {
    return remember(itemCount) {
        FocusManager(itemCount)
    }
}

/**
 * Manages focus for a collection of items.
 */
class FocusManager(itemCount: Int) {
    private val requesters = List(itemCount) { FocusRequester() }
    
    /**
     * Gets the FocusRequester at the specified index.
     */
    fun getRequester(index: Int): FocusRequester? = requesters.getOrNull(index)
    
    /**
     * Requests focus for the item at the specified index.
     */
    fun requestFocus(index: Int) {
        requesters.getOrNull(index)?.requestFocus()
    }
    
    /**
     * Gets all focus requesters.
     */
    fun getAllRequesters(): List<FocusRequester> = requesters
}
