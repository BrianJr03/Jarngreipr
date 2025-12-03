package jr.brian.home.ui.components.quick_delete

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun FolderPathItem(
    path: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val displayName = remember(path) {
        extractDisplayName(path)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = OledCardColor,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = ThemePrimaryColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = ThemePrimaryColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun extractDisplayName(uriString: String): String {
    return try {
        val decodedUri = java.net.URLDecoder.decode(uriString, "UTF-8")

        when {
            decodedUri.contains("/tree/primary:") -> {
                val path = decodedUri.substringAfter("/tree/primary:")
                if (path.isNotEmpty()) "Internal/$path" else "Internal Storage"
            }

            decodedUri.contains("/tree/") -> {
                val afterTree = decodedUri.substringAfter("/tree/")
                val parts = afterTree.split(":")
                when {
                    parts.size >= 2 -> {
                        val volume = parts[0]
                        val path = parts.drop(1).joinToString(":")
                        if (path.isNotEmpty()) "$volume/$path" else volume
                    }

                    else -> afterTree
                }
            }

            decodedUri.contains("primary") -> "Internal Storage"
            else -> {
                val lastPart = decodedUri.substringAfterLast("/")
                if (lastPart.isNotEmpty()) lastPart else "Folder"
            }
        }
    } catch (e: Exception) {
        uriString.takeLast(30)
    }
}
