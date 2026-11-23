package jr.brian.jarnfe.shell

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.delay

private const val NOTICE_DURATION_MS = 5_000L

/** [quantity] is set when [messageRes] is a plurals resource. */
internal data class JarnNotice(
    val messageRes: Int,
    val sequence: Long,
    val quantity: Int? = null,
)

/** Short messages that never take focus or block input. */
internal object JarnNotices {
    var current by mutableStateOf<JarnNotice?>(null)
        private set

    private var sequence = 0L

    fun show(@StringRes messageRes: Int) {
        sequence += 1
        current = JarnNotice(messageRes, sequence)
    }

    fun showCount(@PluralsRes messageRes: Int, quantity: Int) {
        sequence += 1
        current = JarnNotice(messageRes, sequence, quantity)
    }

    fun dismiss(notice: JarnNotice) {
        if (current == notice) current = null
    }
}

@Composable
internal fun BoxScope.JarnNoticeHost() {
    val notice = JarnNotices.current ?: return
    LaunchedEffect(notice) {
        delay(NOTICE_DURATION_MS)
        JarnNotices.dismiss(notice)
    }
    JarnSurface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(JarnTheme.space.xl),
    ) {
        Box(Modifier.padding(horizontal = JarnTheme.space.lg, vertical = JarnTheme.space.md)) {
            JarnText(
                text = notice.quantity?.let { pluralStringResource(notice.messageRes, it, it) }
                    ?: stringResource(notice.messageRes),
                style = JarnTheme.type.body,
                color = JarnTheme.colors.textHigh,
                maxLines = 2,
            )
        }
    }
}
