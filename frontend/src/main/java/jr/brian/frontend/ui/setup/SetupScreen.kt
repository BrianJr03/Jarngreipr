package jr.brian.frontend.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import jr.brian.frontend.ui.input.GamepadAction
import jr.brian.frontend.ui.input.handleGamepad

/**
 * First-run wizard surface. Guides the user through:
 *  - a welcome / permissions prompt;
 *  - adding a ROMs folder;
 *  - running an initial scan;
 *  - a done screen.
 *
 * Minimum-viable Phase 3g — the actual permission request + SAF
 * folder picker ask will be wired via Activity contracts when the
 * launcher-side rewire in Phase 3h lands; for now the UI accepts a
 * path string so the flow is end-to-end testable.
 */
@Composable
fun SetupScreen(
    onDone: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    SetupContent(
        state = state,
        onNext = viewModel::next,
        onAddSource = { path -> viewModel.addSource(path) },
        onScan = viewModel::scan,
        onDone = onDone,
    )
}

@Composable
fun SetupContent(
    state: SetupState,
    onNext: () -> Unit,
    onAddSource: (String) -> Unit,
    onScan: () -> Unit,
    onDone: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .handleGamepad { action ->
                if (action == GamepadAction.Back && state is SetupState.Done) {
                    onDone(); true
                } else false
            },
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            when (state) {
                SetupState.Welcome -> WelcomeStep(onNext)
                SetupState.AddSource -> AddSourceStep(onAddSource)
                SetupState.Scan -> ScanStep(onScan)
                SetupState.Done -> DoneStep(onDone)
                is SetupState.Working -> WorkingStep(state.message)
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Welcome", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            "Point Jarngreipr at your ROM folder. We'll scan it in the background.",
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onNext) { Text("Get started") }
    }
}

@Composable
private fun AddSourceStep(onAddSource: (String) -> Unit) {
    var path by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Add a ROMs folder", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            "Enter the folder path. SAF pickers wire up when the launcher side migrates.",
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(value = path, onValueChange = { path = it }, label = { Text("Path") })
        Button(
            onClick = { if (path.isNotBlank()) onAddSource(path.trim()) },
            enabled = path.isNotBlank(),
        ) { Text("Add") }
    }
}

@Composable
private fun ScanStep(onScan: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Scan now?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            "A first scan can take a minute on large libraries.",
            color = MaterialTheme.colorScheme.onSurface,
        )
        Button(onClick = onScan) { Text("Scan") }
    }
}

@Composable
private fun DoneStep(onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("All set", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            "You can rescan or add more folders from Settings.",
            color = MaterialTheme.colorScheme.onSurface,
        )
        Button(onClick = onDone) { Text("Open Home") }
    }
}

@Composable
private fun WorkingStep(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(message, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
