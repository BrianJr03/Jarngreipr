package jr.brian.home.esde.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.ui.components.quick_delete.FolderPathItem

@Composable
fun ExtrasSectionContent(
    folderPaths: List<String>,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onDeleteEmptyFolders: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ToggleSetting(
            title = stringResource(R.string.esde_settings_add_cleanup_folder),
            description = stringResource(R.string.esde_settings_add_cleanup_folder_description),
            checked = false,
            showToggle = false,
            onClick = onAddFolder
        )
        
        if (folderPaths.isNotEmpty()) {
            Text(
                text = stringResource(R.string.esde_settings_selected_folders),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            folderPaths.forEach { path ->
                FolderPathItem(
                    path = path,
                    onRemove = { onRemoveFolder(path) },
                    onClick = { }
                )
            }
        }
        
        ToggleSetting(
            title = stringResource(R.string.esde_settings_delete_empty_folders),
            description = stringResource(R.string.esde_settings_delete_empty_folders_description),
            checked = false,
            showToggle = false,
            onClick = onDeleteEmptyFolders
        )
    }
}
