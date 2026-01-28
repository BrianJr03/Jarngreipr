package jr.brian.home.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.model.IconPack

@Composable
fun IconPackMetadataDisplay(
    iconPack: IconPack,
    filteredIconCount: Int,
    showKeyboard: Boolean,
    isLoadingDrawables: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onToggleKeyboard: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = iconPack.name,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (!isLoadingDrawables) {
                Text(
                    text = "$filteredIconCount icons",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        IconButton(
            onClick = onToggleKeyboard,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (showKeyboard) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = if (showKeyboard) "Hide search" else "Show search",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
