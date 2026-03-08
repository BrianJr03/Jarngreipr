package jr.brian.home.data

import jr.brian.ping.PingProfile
import jr.brian.ping.PingService
import javax.inject.Inject
import javax.inject.Singleton

typealias PingListener = (deviceAddress: String, remoteProfile: PingProfile) -> Unit

@Singleton
class PingBroadcastManager @Inject constructor() {
    private val listeners = mutableListOf<PingListener>()

    fun addListener(listener: PingListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: PingListener) {
        listeners.remove(listener)
    }

    init {
        PingService.encounterCooldownMs = 300_000L // 5 Minutes
        PingService.reconnectCooldownMs = 600_000L // 10 Minutes
        PingService.onEncounter = { deviceAddress, remoteProfile ->
            listeners.forEach { it(deviceAddress, remoteProfile) }
        }
    }
}
