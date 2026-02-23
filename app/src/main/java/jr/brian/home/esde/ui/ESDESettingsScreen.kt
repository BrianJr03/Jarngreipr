package jr.brian.home.esde.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import jr.brian.home.R
import jr.brian.home.esde.viewmodel.ESDEViewModel
import jr.brian.home.model.state.DeleteResult
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.components.settings.sections.ESDESettingsContent
import jr.brian.home.ui.theme.OledBackgroundColor

@Composable
fun ESDESettingsScreen(
    viewModel: ESDEViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onRunSetupWizard: () -> Unit,
    onNavigateToMarqueePressShortcut: () -> Unit = {},
    onNavigateToSystemApps: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(viewModel.deleteEmptyFoldersResult.value) {
        viewModel.deleteEmptyFoldersResult.value?.let { result ->
            val message = when (result) {
                is DeleteResult.Success -> "Successfully deleted ${result.deletedCount} empty folder(s)"
                is DeleteResult.Failure -> result.message
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearDeleteResult()
        }
    }

    Scaffold(
        containerColor = OledBackgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            Column {
                ScreenHeader(onBackClick = onNavigateBack)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.esde_settings_title),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp)
                ) {
                    ESDESettingsContent(
                        onRunSetupWizard = onRunSetupWizard,
                        onNavigateToMarqueePressShortcut = onNavigateToMarqueePressShortcut,
                        onNavigateToSystemApps = onNavigateToSystemApps
                    )
                }
            }
        }
    }
}
