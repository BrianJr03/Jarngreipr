package jr.brian.home.esde.ui.frontend.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.revealWhenFocused(focused: Boolean): Modifier {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(focused) {
        if (focused) {
            withFrameNanos { }
            runCatching { requester.bringIntoView() }
        }
    }
    return this.bringIntoViewRequester(requester)
}
