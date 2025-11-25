package jr.brian.home.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PowerViewModel @Inject constructor() : ViewModel() {
    private val _isPoweredOff = MutableStateFlow(false)
    val isPoweredOff: StateFlow<Boolean> = _isPoweredOff.asStateFlow()

    fun togglePower() {
        _isPoweredOff.value = !_isPoweredOff.value
    }

    fun powerOn() {
        if (_isPoweredOff.value) {
            _isPoweredOff.value = false
        }
    }
}
