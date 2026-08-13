package jr.brian.home.esde.ui.frontend

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.esde.util.LocalESDEImageLoader
import kotlinx.coroutines.delay

private const val CROSSFADE_MS = 300
private const val FOCUS_DEBOUNCE_MS = 150L

/**
 * Full-screen backdrop that follows focus. Pass [focusedUri] on every focus
 * change; the composable debounces internally so rapid D-pad scrolling only
 * decodes the item the user lands on. [fallbackColor] paints the null branch
 * when the feature is on but the focused item has no art — callers pass
 * OledBackgroundColor there so the grid can stay transparent without exposing
 * a black flash to whatever is behind.
 */
@Composable
fun FrontendFocusBackground(
    focusedUri: String?,
    dimAlpha: Float,
    modifier: Modifier = Modifier,
    fallbackColor: Color = Color.Transparent
) {
    var debouncedUri by remember { mutableStateOf(focusedUri) }
    LaunchedEffect(focusedUri) {
        if (focusedUri == null) {
            debouncedUri = null
        } else if (focusedUri != debouncedUri) {
            // Debounce protects rapid D-pad scrolling from decoding every item
            // passed over. There is nothing to protect when we haven't shown
            // anything yet — seed the first non-null value immediately.
            if (debouncedUri != null) delay(FOCUS_DEBOUNCE_MS)
            debouncedUri = focusedUri
        }
    }

    Crossfade(
        targetState = debouncedUri,
        animationSpec = tween(CROSSFADE_MS),
        label = "frontendFocusBackground",
        modifier = modifier.fillMaxSize()
    ) { currentUri ->
        if (currentUri == null) {
            Box(modifier = Modifier.fillMaxSize().background(fallbackColor))
        } else {
            FocusBackgroundImage(uri = currentUri, dimAlpha = dimAlpha)
        }
    }
}

@Composable
private fun FocusBackgroundImage(uri: String, dimAlpha: Float) {
    val context = LocalContext.current
    val imageLoader = LocalESDEImageLoader.current
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(uri).build(),
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha.coerceIn(0f, 1f)))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bottomFadeBrush())
        )
    }
}

private fun bottomFadeBrush(): Brush = Brush.verticalGradient(
    0f to Color.Transparent,
    0.6f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.5f)
)
