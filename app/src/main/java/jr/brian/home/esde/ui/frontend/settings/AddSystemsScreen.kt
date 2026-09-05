package jr.brian.home.esde.ui.frontend.settings

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.addSystemFolderMapping
import jr.brian.home.esde.data.removeSystemFolderMapping
import jr.brian.home.esde.model.SystemFolderMapping
import jr.brian.home.esde.util.RomScanner
import jr.brian.home.esde.util.documentIdToStoragePath
import jr.brian.home.esde.util.persistSafTreeForSystem
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.extensions.clickWithHaptic
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen editor for [SystemFolderMapping] entries. Distinct from the
 * per-system storage row on [SystemCustomizationScreen] because a mapping's
 * folder can be named anything and can live on an SD card; the row there is
 * for the common case where a system already exists under a configured ROMs
 * root.
 *
 * @param knownSystemSuggestions systems the app already knows about (scanned
 *   library + emulator preferences + prior mappings) — offered as a shortcut
 *   list in the "which system?" step so a user with a `ps2` folder doesn't
 *   have to type. Free text is always allowed too since the point of the
 *   feature is folders that don't follow the naming convention.
 */
@Composable
fun AddSystemsScreen(
    esdePrefs: ESDEPreferencesManager,
    knownSystemSuggestions: List<String>,
    onDismiss: () -> Unit,
) {
    val prefsState by esdePrefs.state.collectAsStateWithLifecycle()
    val mappings = prefsState.systemFolderMappings

    var pendingTreeUri by remember { mutableStateOf<Uri?>(null) }
    var validationBySystem by remember { mutableStateOf<Map<String, ValidationResult>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) pendingTreeUri = treeUri
    }

    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }

    Surface(
        color = OledBackgroundColor,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BUTTON_B -> {
                        onDismiss(); true
                    }
                    else -> false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader()
            AddMappingRow(onClick = {
                picker.launch(null)
            })
            if (mappings.isEmpty()) {
                Text(
                    text = stringResource(R.string.add_systems_no_mappings),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                )
            } else {
                mappings.forEach { mapping ->
                    MappingRow(
                        mapping = mapping,
                        validation = validationBySystem[mapping.systemName + mapping.treeUri],
                        onRemove = {
                            esdePrefs.removeSystemFolderMapping(mapping)
                            validationBySystem = validationBySystem -
                                (mapping.systemName + mapping.treeUri)
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            CloseButton(onClick = onDismiss)
        }
    }

    val treeUriForDialog = pendingTreeUri
    if (treeUriForDialog != null) {
        PickSystemDialog(
            treeUri = treeUriForDialog,
            suggestions = knownSystemSuggestions,
            onConfirm = { systemName ->
                pendingTreeUri = null
                val displayPath = readableDisplayPath(treeUriForDialog)
                    ?: treeUriForDialog.toString()
                val mapping = SystemFolderMapping(
                    systemName = systemName,
                    treeUri = treeUriForDialog.toString(),
                    displayPath = displayPath,
                )
                persistSafTreeForSystem(context, esdePrefs, systemName, treeUriForDialog)
                esdePrefs.addSystemFolderMapping(mapping)

                val key = mapping.systemName + mapping.treeUri
                validationBySystem = validationBySystem + (key to ValidationResult.Running)
                scope.launch {
                    val outcome = withContext(Dispatchers.IO) {
                        validateMapping(context, mapping)
                    }
                    validationBySystem = validationBySystem + (key to outcome)
                }
            },
            onDismiss = { pendingTreeUri = null }
        )
    }
}

@Composable
private fun ScreenHeader() {
    Column {
        Text(
            text = stringResource(R.string.add_systems_screen_title),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.add_systems_screen_summary),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun AddMappingRow(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(focused))
            .background(subtleCardGradient(focused), RoundedCornerShape(16.dp))
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) ThemePrimaryColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickWithHaptic(haptic) { onClick() }
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode ==
                    KeyEvent.KEYCODE_BUTTON_A
                ) {
                    onClick(); true
                } else false
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = ThemePrimaryColor,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.add_systems_add_action_title),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.add_systems_add_action_description),
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun MappingRow(
    mapping: SystemFolderMapping,
    validation: ValidationResult?,
    onRemove: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedFocusedScale(focused))
            .background(subtleCardGradient(focused), RoundedCornerShape(16.dp))
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) ThemePrimaryColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickWithHaptic(haptic) { onRemove() }
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode ==
                    KeyEvent.KEYCODE_BUTTON_A
                ) {
                    onRemove(); true
                } else false
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mapping.systemName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = mapping.displayPath,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp
            )
            ValidationText(validation)
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.add_systems_remove),
            tint = ThemeAccentColor,
        )
    }
}

@Composable
private fun ValidationText(validation: ValidationResult?) {
    if (validation == null) return
    val text = when (validation) {
        ValidationResult.Running -> stringResource(R.string.add_systems_validating)
        is ValidationResult.Ok -> stringResource(R.string.add_systems_validated_ok, validation.count)
        ValidationResult.Zero -> stringResource(R.string.add_systems_validated_zero)
    }
    Spacer(Modifier.height(4.dp))
    Text(text = text, color = ThemePrimaryColor, fontSize = 13.sp)
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = stringResource(R.string.add_systems_close),
            color = ThemeAccentColor
        )
    }
}

@Composable
private fun PickSystemDialog(
    treeUri: Uri,
    suggestions: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }
    val effective = (selected ?: typed).trim()
    val canConfirm = effective.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledBackgroundColor,
        title = {
            Text(
                text = stringResource(R.string.add_systems_pick_system_title),
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = readableDisplayPath(treeUri) ?: treeUri.toString(),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                if (suggestions.isNotEmpty()) {
                    suggestions.forEach { name ->
                        SuggestionChip(
                            name = name,
                            selected = selected == name,
                            onClick = {
                                selected = if (selected == name) null else name
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = typed,
                    onValueChange = {
                        typed = it
                        selected = null
                    },
                    label = {
                        Text(text = stringResource(R.string.add_systems_pick_system_free_text))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = { if (canConfirm) onConfirm(effective) }
            ) {
                Text(text = stringResource(R.string.add_systems_pick_system_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.add_systems_pick_system_cancel))
            }
        }
    )
}

@Composable
private fun SuggestionChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) ThemePrimaryColor.copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) ThemePrimaryColor.copy(alpha = 0.6f)
                else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickWithHaptic(haptic) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Extract a human-readable /storage path from a tree URI. Uses the shared
 * document-ID → storage-path helper so this stays consistent with what the
 * launcher will actually resolve.
 */
internal fun readableDisplayPath(treeUri: Uri): String? {
    val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
        ?: return null
    return documentIdToStoragePath(docId)
}

private sealed interface ValidationResult {
    data object Running : ValidationResult
    data class Ok(val count: Int) : ValidationResult
    data object Zero : ValidationResult
}

/**
 * Walks [mapping] with the same extension rules the real scan uses and
 * reports the count. Runs on IO; safe to call from a coroutine launched by
 * the caller. Not the full [RomScanner.scan] — we don't need dedup, metadata,
 * or cache stamps, just "does this folder contain ROMs?"
 */
private fun validateMapping(
    context: Context,
    mapping: SystemFolderMapping,
): ValidationResult {
    val treeUri = runCatching { mapping.treeUri.toUri() }.getOrNull()
        ?: return ValidationResult.Zero
    val root = DocumentFile.fromTreeUri(context, treeUri) ?: return ValidationResult.Zero
    val extensions = RomScanner.extensionsFor(mapping.systemName, emptyMap())
    val count = countRoms(root, extensions, depthRemaining = 6)
    return if (count == 0) ValidationResult.Zero else ValidationResult.Ok(count)
}

private fun countRoms(
    dir: DocumentFile,
    extensions: Set<String>,
    depthRemaining: Int,
): Int {
    if (depthRemaining < 0) return 0
    var count = 0
    val children = runCatching { dir.listFiles() }.getOrDefault(emptyArray())
    for (child in children) {
        val name = child.name ?: continue
        if (name.startsWith(".")) continue
        val ext = name.substringAfterLast('.', "").lowercase()
        if (child.isDirectory) {
            if (ext.isNotEmpty() && ext in extensions) {
                count++
            } else {
                count += countRoms(child, extensions, depthRemaining - 1)
            }
        } else if (child.isFile) {
            if (ext.isNotEmpty() && ext in extensions) count++
        }
    }
    return count
}

