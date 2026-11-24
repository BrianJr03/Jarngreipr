package jr.brian.home.ui.components.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.home.R
import jr.brian.home.data.FocusLockMode
import jr.brian.home.data.FocusLockPrefsManager
import jr.brian.home.data.ShizukuManager
import jr.brian.home.esde.ui.components.focusableSettingCard
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.util.ThorDetection

/**
 * Extras-section radio-choice for the Focus Lock mode (Off / Top /
 * Bottom / Follow). Below the choice sits a Shizuku-status info card
 * that explains the Shizuku requirement and surfaces install / grant
 * actions when needed.
 *
 * Thor-only — AYN's Focus Lock doesn't exist on other hardware.
 */
@Composable
fun FocusLockSettingItem() {
    if (!ThorDetection.isThor()) return

    val context = LocalContext.current
    val entry = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            FocusLockPrefsEntryPoint::class.java,
        )
    }
    val prefs = entry.focusLockPrefsManager()
    val shizuku = entry.shizukuManager()
    val mode by prefs.mode.collectAsStateWithLifecycle()
    val shizukuAvailable by shizuku.isShizukuAvailable.collectAsStateWithLifecycle()
    val shizukuGranted by shizuku.isShizukuPermissionGranted.collectAsStateWithLifecycle()
    val shizukuInstalled = remember(context, shizukuAvailable) {
        shizuku.isShizukuInstalled(context)
    }

    FocusLockCard(
        currentMode = mode,
        onSelect = { prefs.setMode(it) },
    )

    ShizukuInfoCard(
        installed = shizukuInstalled,
        granted = shizukuGranted,
        showActions = mode != FocusLockMode.Off,
        onInstall = {
            runCatching {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "market://details?id=moe.shizuku.privileged.api".toUri(),
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
            }
        },
        onGrant = { shizuku.requestPermission() },
    )
}

@Composable
private fun FocusLockCard(
    currentMode: FocusLockMode,
    onSelect: (FocusLockMode) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .focusableSettingCard(isFocused)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.focus_lock_mode_title),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.focus_lock_mode_description),
            color = Color.Gray,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FocusLockMode.entries.forEach { entry ->
                FocusLockModeRow(
                    mode = entry,
                    selected = currentMode == entry,
                    onClick = { onSelect(entry) },
                )
            }
        }
    }
}

@Composable
private fun FocusLockModeRow(
    mode: FocusLockMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = ThemePrimaryColor,
                unselectedColor = Color.White.copy(alpha = 0.6f),
            ),
        )
        Column {
            Text(
                text = stringResource(labelRes(mode)),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(detailRes(mode)),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ShizukuInfoCard(
    installed: Boolean,
    granted: Boolean,
    showActions: Boolean,
    onInstall: () -> Unit,
    onGrant: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val statusText = when {
        !installed -> stringResource(R.string.focus_lock_shizuku_not_installed)
        !granted -> stringResource(R.string.focus_lock_shizuku_not_granted)
        else -> stringResource(R.string.focus_lock_shizuku_ready)
    }
    val actionText = when {
        !showActions -> null
        !installed -> stringResource(R.string.focus_lock_shizuku_install_action)
        !granted -> stringResource(R.string.focus_lock_shizuku_grant_action)
        else -> null
    }
    val actionClick: (() -> Unit)? = when {
        !showActions -> null
        !installed -> onInstall
        !granted -> onGrant
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .focusableSettingCard(isFocused)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.focus_lock_shizuku_card_title),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.focus_lock_shizuku_card_description),
            color = Color.Gray,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = statusText,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
        )
        if (actionText != null && actionClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = actionText,
                color = ThemePrimaryColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = actionClick),
            )
        }
    }
}

private fun labelRes(mode: FocusLockMode): Int = when (mode) {
    FocusLockMode.Off -> R.string.focus_lock_mode_off_label
    FocusLockMode.Top -> R.string.focus_lock_mode_top_label
    FocusLockMode.Bottom -> R.string.focus_lock_mode_bottom_label
    FocusLockMode.Follow -> R.string.focus_lock_mode_follow_label
}

private fun detailRes(mode: FocusLockMode): Int = when (mode) {
    FocusLockMode.Off -> R.string.focus_lock_mode_off_detail
    FocusLockMode.Top -> R.string.focus_lock_mode_top_detail
    FocusLockMode.Bottom -> R.string.focus_lock_mode_bottom_detail
    FocusLockMode.Follow -> R.string.focus_lock_mode_follow_detail
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface FocusLockPrefsEntryPoint {
    fun focusLockPrefsManager(): FocusLockPrefsManager
    fun shizukuManager(): ShizukuManager
}
