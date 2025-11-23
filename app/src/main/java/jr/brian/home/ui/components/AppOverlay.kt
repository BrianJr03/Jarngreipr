package jr.brian.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as GraphicsColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.util.OverlayInfoUtil

@Composable
fun AppOverlay(onDismissOverlay: () -> Unit) {
    val randomMessage = remember { OverlayInfoUtil.getRandomFact() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GraphicsColor.Black.copy(alpha = 0.8f))
            .pointerInput(Unit) {
                detectTapGestures { }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.welcome_overlay_title),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = GraphicsColor.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(randomMessage),
                fontSize = 20.sp,
                color = GraphicsColor.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onDismissOverlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemePrimaryColor
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(horizontal = 64.dp)
                    .height(64.dp)
            ) {
                Text(
                    text = stringResource(R.string.welcome_overlay_button),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GraphicsColor.White
                )
            }
        }
    }
}