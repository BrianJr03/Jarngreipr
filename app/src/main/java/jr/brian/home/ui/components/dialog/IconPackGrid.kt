package jr.brian.home.ui.components.dialog

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IconPackGrid(
    filteredDrawables: Map<String, Drawable>,
    showKeyboard: Boolean,
    onDrawableSelected: (Drawable) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        LazyVerticalGrid(
            columns = if (showKeyboard) GridCells.Fixed(3) else GridCells.Fixed(6),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredDrawables.entries.toList()) { (name, drawable) ->
                IconPackGridItem(
                    name = name,
                    drawable = drawable,
                    onClick = { onDrawableSelected(drawable) }
                )
            }
        }
    }
}
