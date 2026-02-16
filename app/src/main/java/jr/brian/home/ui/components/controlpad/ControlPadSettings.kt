package jr.brian.home.ui.components.controlpad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.data.ControlPadManager
import jr.brian.home.data.JoystickMode
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun EditModeSettingsPanel(
    joystickMode: JoystickMode,
    cameraSensitivity: Float,
    onJoystickModeSelected: (JoystickMode) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.55f)
            .background(
                color = OledCardColor.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                brush = borderBrush(
                    isFocused = true,
                    colors = listOf(
                        ThemePrimaryColor.copy(alpha = 0.6f),
                        ThemeSecondaryColor.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        JoystickModeSelector(
            joystickMode = joystickMode,
            onModeSelected = onJoystickModeSelected
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )

        SensitivitySlider(
            cameraSensitivity = cameraSensitivity,
            onSensitivityChange = onSensitivityChange
        )
    }
}

@Composable
private fun JoystickModeSelector(
    joystickMode: JoystickMode,
    onModeSelected: (JoystickMode) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.control_pad_joystick),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JoystickModeOption(
                text = stringResource(R.string.control_pad_joystick_l),
                isSelected = joystickMode == JoystickMode.LEFT_ONLY,
                onClick = { onModeSelected(JoystickMode.LEFT_ONLY) }
            )
            JoystickModeOption(
                text = stringResource(R.string.control_pad_joystick_lr),
                isSelected = joystickMode == JoystickMode.LEFT_RIGHT,
                onClick = { onModeSelected(JoystickMode.LEFT_RIGHT) }
            )
            JoystickModeOption(
                text = stringResource(R.string.control_pad_joystick_r),
                isSelected = joystickMode == JoystickMode.RIGHT_ONLY,
                onClick = { onModeSelected(JoystickMode.RIGHT_ONLY) }
            )
        }
    }
}

@Composable
private fun JoystickModeOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(
                if (isSelected) ThemePrimaryColor.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(
                    width = 2.dp,
                    color = if (isSelected) ThemePrimaryColor else Color.White.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .padding(3.dp)
                .background(
                    if (isSelected) ThemePrimaryColor else Color.Transparent,
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SensitivitySlider(
    cameraSensitivity: Float,
    onSensitivityChange: (Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.control_pad_camera_sensitivity),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Slider(
            value = cameraSensitivity,
            onValueChange = onSensitivityChange,
            valueRange = ControlPadManager.MIN_CAMERA_SENSITIVITY..ControlPadManager.MAX_CAMERA_SENSITIVITY,
            colors = SliderDefaults.colors(
                thumbColor = ThemePrimaryColor,
                activeTrackColor = ThemePrimaryColor,
                inactiveTrackColor = ThemeSecondaryColor.copy(alpha = 0.3f)
            )
        )
        Text(
            text = "%.3f".format(cameraSensitivity),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}
