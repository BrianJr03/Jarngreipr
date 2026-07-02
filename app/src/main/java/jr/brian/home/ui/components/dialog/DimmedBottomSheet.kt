package jr.brian.home.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DimmedBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val wallpaperManager = LocalWallpaperManager.current
    val esdePrefsState by LocalESDEPreferencesManager.current
        .state.collectAsStateWithLifecycle()

    val scrimColor = if (
        wallpaperManager.getWallpaperType() == WallpaperType.ESDE
        && esdePrefsState.dimmingLevelFloat > 0f
    ) {
        Color.Black.copy(alpha = esdePrefsState.dimmingLevelFloat)
    } else {
        BottomSheetDefaults.ScrimColor
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = OledCardColor,
        scrimColor = scrimColor,
        dragHandle = { DimmedBottomSheetDragHandle() },
        modifier = modifier,
        content = content
    )
}

@Composable
private fun DimmedBottomSheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 40.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.3f))
    )
}
