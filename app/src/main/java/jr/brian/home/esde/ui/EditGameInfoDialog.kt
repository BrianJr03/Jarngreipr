package jr.brian.home.esde.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.scraper.sidecar.UserMetadataOverride
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.ui.components.dialog.AppBottomSheet
import jr.brian.home.ui.theme.ThemeAccentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal const val OVERRIDE_SLOT_COVER = "cover"
internal const val OVERRIDE_SLOT_SCREENSHOT = "screenshot"
internal const val OVERRIDE_SLOT_FANART = "fanart"
internal const val OVERRIDE_SLOT_MARQUEE = "marquee"

@Composable
internal fun EditGameInfoDialog(
    game: GameInfo,
    loadExisting: suspend () -> UserMetadataOverride?,
    copyImage: suspend (slot: String, uri: Uri) -> String?,
    onSave: (UserMetadataOverride) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var displayName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var developer by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var players by remember { mutableStateOf("") }
    var releaseDate by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var coverPath by remember { mutableStateOf<String?>(null) }
    var screenshotPath by remember { mutableStateOf<String?>(null) }
    var fanartPath by remember { mutableStateOf<String?>(null) }
    var marqueePath by remember { mutableStateOf<String?>(null) }
    var ratingError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    LaunchedEffect(game.path) {
        val existing = withContext(Dispatchers.IO) { loadExisting() }
        if (existing != null) {
            displayName = existing.displayName.orEmpty()
            description = existing.description.orEmpty()
            developer = existing.developer.orEmpty()
            publisher = existing.publisher.orEmpty()
            genre = existing.genre.orEmpty()
            players = existing.players.orEmpty()
            releaseDate = existing.releaseDate.orEmpty()
            rating = existing.rating?.toString().orEmpty()
            coverPath = existing.artworkPath
            screenshotPath = existing.screenshotPath
            fanartPath = existing.fanartPath
            marqueePath = existing.marqueeImagePath
        }
    }

    fun pickFor(slot: String, uri: Uri, assign: (String?) -> Unit) {
        scope.launch {
            val copied = withContext(Dispatchers.IO) { copyImage(slot, uri) }
            if (copied != null) assign(copied)
        }
    }

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.rom_edit_info_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.rom_edit_info_subtitle),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OverrideTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = stringResource(R.string.rom_edit_info_field_name)
                )
                OverrideTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.rom_edit_info_field_description),
                    singleLine = false,
                    minHeight = 96.dp
                )
                OverrideTextField(
                    value = developer,
                    onValueChange = { developer = it },
                    label = stringResource(R.string.rom_edit_info_field_developer)
                )
                OverrideTextField(
                    value = publisher,
                    onValueChange = { publisher = it },
                    label = stringResource(R.string.rom_edit_info_field_publisher)
                )
                OverrideTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = stringResource(R.string.rom_edit_info_field_genre)
                )
                OverrideTextField(
                    value = players,
                    onValueChange = { players = it },
                    label = stringResource(R.string.rom_edit_info_field_players)
                )
                OverrideTextField(
                    value = releaseDate,
                    onValueChange = { releaseDate = it },
                    label = stringResource(R.string.rom_edit_info_field_release_date)
                )
                OverrideTextField(
                    value = rating,
                    onValueChange = {
                        rating = it
                        ratingError = false
                    },
                    label = stringResource(R.string.rom_edit_info_field_rating),
                    keyboardType = KeyboardType.Decimal,
                    isError = ratingError,
                    supportingText = if (ratingError) {
                        stringResource(R.string.rom_edit_info_rating_invalid)
                    } else null
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                OverrideImageSlot(
                    label = stringResource(R.string.rom_edit_info_image_cover),
                    path = coverPath,
                    slotId = OVERRIDE_SLOT_COVER,
                    onPicked = { uri -> pickFor(OVERRIDE_SLOT_COVER, uri) { coverPath = it } },
                    onClear = { coverPath = null }
                )
                OverrideImageSlot(
                    label = stringResource(R.string.rom_edit_info_image_screenshot),
                    path = screenshotPath,
                    slotId = OVERRIDE_SLOT_SCREENSHOT,
                    onPicked = { uri -> pickFor(OVERRIDE_SLOT_SCREENSHOT, uri) { screenshotPath = it } },
                    onClear = { screenshotPath = null }
                )
                OverrideImageSlot(
                    label = stringResource(R.string.rom_edit_info_image_fanart),
                    path = fanartPath,
                    slotId = OVERRIDE_SLOT_FANART,
                    onPicked = { uri -> pickFor(OVERRIDE_SLOT_FANART, uri) { fanartPath = it } },
                    onClear = { fanartPath = null }
                )
                OverrideImageSlot(
                    label = stringResource(R.string.rom_edit_info_image_marquee),
                    path = marqueePath,
                    slotId = OVERRIDE_SLOT_MARQUEE,
                    onPicked = { uri -> pickFor(OVERRIDE_SLOT_MARQUEE, uri) { marqueePath = it } },
                    onClear = { marqueePath = null }
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onClear) {
                    Text(
                        text = stringResource(R.string.rom_edit_info_clear_all),
                        color = Color.Red.copy(alpha = 0.75f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.rom_detail_cancel),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Button(
                    onClick = {
                        val ratingValue = rating.trim().takeIf { it.isNotEmpty() }?.toFloatOrNull()
                        if (rating.trim().isNotEmpty() &&
                            (ratingValue == null || ratingValue !in 0f..1f)
                        ) {
                            ratingError = true
                            return@Button
                        }
                        onSave(
                            UserMetadataOverride(
                                displayName = displayName.trim().takeIf { it.isNotEmpty() },
                                description = description.trim().takeIf { it.isNotEmpty() },
                                developer = developer.trim().takeIf { it.isNotEmpty() },
                                publisher = publisher.trim().takeIf { it.isNotEmpty() },
                                genre = genre.trim().takeIf { it.isNotEmpty() },
                                players = players.trim().takeIf { it.isNotEmpty() },
                                releaseDate = releaseDate.trim().takeIf { it.isNotEmpty() },
                                rating = ratingValue,
                                artworkPath = coverPath,
                                screenshotPath = screenshotPath,
                                fanartPath = fanartPath,
                                marqueeImagePath = marqueePath,
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeAccentColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = stringResource(R.string.rom_edit_info_save),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OverrideTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.6f)) },
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText?.let {
            { Text(it, color = Color.Red.copy(alpha = 0.8f), fontSize = 12.sp) }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = ThemeAccentColor,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            cursorColor = ThemeAccentColor,
            focusedLabelColor = ThemeAccentColor,
            errorBorderColor = Color.Red.copy(alpha = 0.8f),
            errorCursorColor = Color.Red.copy(alpha = 0.8f),
            errorLabelColor = Color.Red.copy(alpha = 0.8f),
        )
    )
}

@Composable
private fun OverrideImageSlot(
    label: String,
    path: String?,
    slotId: String,
    onPicked: (Uri) -> Unit,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = LocalESDEImageLoader.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onPicked(uri)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (path != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(path))
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(72.dp)
                )
            } else {
                Text(
                    text = "—",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 20.sp
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = path?.substringAfterLast('/') ?: "",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        TextButton(
            onClick = { picker.launch(arrayOf("image/*")) },
            modifier = Modifier.width(100.dp)
        ) {
            Text(
                text = if (path == null) {
                    stringResource(R.string.rom_edit_info_pick_image)
                } else {
                    stringResource(R.string.rom_edit_info_replace_image)
                },
                color = ThemeAccentColor,
                fontSize = 12.sp
            )
        }
        if (path != null) {
            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(R.string.rom_edit_info_clear_image),
                    color = Color.Red.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
