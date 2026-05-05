package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.ui.animations.AnimationPresetCard
import jr.brian.home.ui.animations.allAnimationPresets
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager

@Composable
fun TransitionAnimationScreen(
    onDismiss: () -> Unit = {}
) {
    val gridSettingsManager = LocalGridSettingsManager.current
    val selectedName = gridSettingsManager.tabTransitionAnimationName

    BackHandler(onBack = onDismiss)

    Scaffold(
        containerColor = OledBackgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            Column {
                ScreenHeader(onBackClick = onDismiss)

                Text(
                    text = "Tab Transition",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                )

                Text(
                    text = "Choose how pages animate when you swipe between tabs.",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 8.dp)
                )

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(allAnimationPresets) { preset ->
                        val isSelected = when {
                            preset.name == "None" -> selectedName.isEmpty()
                            else -> preset.name == selectedName
                        }
                        AnimationPresetCard(
                            preset = preset,
                            isSelected = isSelected,
                            onSelected = {
                                val nameToSave = if (preset.name == "None") "" else preset.name
                                gridSettingsManager.setTabTransitionAnimationName(nameToSave)
                            }
                        )
                    }
                }
            }
        }
    }
}
