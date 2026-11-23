package jr.brian.jarnfe.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnScreenHintBar
import jr.brian.jarnfe.splash.jarnSplashFade

@Composable
internal fun JarnScreenFrame(
    header: @Composable ColumnScope.() -> Unit,
    background: Color = JarnTheme.colors.bg,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(background),
    ) {
        header()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .jarnSplashFade(),
            contentAlignment = Alignment.Center,
        ) { content() }
        JarnScreenHintBar(Modifier.jarnSplashFade())
    }
}
