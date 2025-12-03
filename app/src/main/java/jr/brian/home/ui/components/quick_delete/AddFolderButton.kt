package jr.brian.home.ui.components.quick_delete

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.home.R

@Composable
fun AddFolderButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ActionButton(
        text = stringResource(R.string.clean_folders_add_folder),
        icon = Icons.Default.Add,
        onClick = onClick,
        modifier = modifier,
        isPrimary = true
    )
}
