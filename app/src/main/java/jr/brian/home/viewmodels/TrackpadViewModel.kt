package jr.brian.home.viewmodels

import android.util.DisplayMetrics
import androidx.lifecycle.ViewModel
import jr.brian.home.util.TrackpadInputProcessor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class CursorPosition(val x: Float, val y: Float)

sealed interface TrackpadEvent {
    data class Move(val position: CursorPosition) : TrackpadEvent
    data object PrimaryTap : TrackpadEvent
    data object SecondaryTap : TrackpadEvent
    data class Scroll(val dy: Float) : TrackpadEvent
}

class TrackpadViewModel : ViewModel() {

    private val _cursor = MutableStateFlow(CursorPosition(0.5f, 0.5f))
    val cursor: StateFlow<CursorPosition> = _cursor

    private val _events = MutableSharedFlow<TrackpadEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<TrackpadEvent> = _events

    private var processor: TrackpadInputProcessor? = null

    fun initProcessor(surfaceMetrics: DisplayMetrics, outputMetrics: DisplayMetrics) {
        processor = TrackpadInputProcessor(surfaceMetrics, outputMetrics)
    }

    fun onSample(rawDxPx: Float, rawDyPx: Float, timestampNs: Long) {
        val proc = processor ?: return
        val delta = proc.process(rawDxPx, rawDyPx, timestampNs)
        _cursor.update { pos ->
            CursorPosition(
                x = (pos.x + delta.dx).coerceIn(0f, 1f),
                y = (pos.y + delta.dy).coerceIn(0f, 1f)
            )
        }
        _events.tryEmit(TrackpadEvent.Move(_cursor.value))
    }

    fun onDragEnd() {
        processor?.reset()
    }

    fun onTap() {
        _events.tryEmit(TrackpadEvent.PrimaryTap)
    }

    fun onTwoFingerTap() {
        _events.tryEmit(TrackpadEvent.SecondaryTap)
    }

    fun onTwoFingerScroll(dy: Float) {
        _events.tryEmit(TrackpadEvent.Scroll(dy))
    }
}
