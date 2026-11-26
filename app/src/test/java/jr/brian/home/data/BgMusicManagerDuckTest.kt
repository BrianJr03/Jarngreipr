package jr.brian.home.data

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * A focus grant we ask for ourselves comes back synchronously, so the listener's
 * AUDIOFOCUS_GAIN branch (the old only path to [BgMusicManager.unDuck]) never runs.
 * Guards #516: music stayed at the duck volume after a hold that started while ducked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BgMusicManagerDuckTest {

    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var manager: BgMusicManager
    private lateinit var player: MediaPlayer
    private val volumes = mutableListOf<Float>()

    @Before
    fun setUp() {
        manager = BgMusicManager(context)
        manager.setVolume(VOLUME)
        player = mockk(relaxed = true)
        every { player.isPlaying } returns false
        every { player.setVolume(any(), any()) } answers { volumes += firstArg<Float>() }
        setField("mediaPlayer", player)
        setField("isIntendedToPlay", true)
    }

    @Test
    fun `release after a hold that started while ducked plays at full volume`() {
        manager.duck()
        manager.holdPlayback()
        manager.releaseHold()

        verify { player.start() }
        assertEquals(VOLUME, volumes.last(), 0.0001f)
    }

    @Test
    fun `a refused focus request keeps the duck`() {
        manager.duck()
        manager.holdPlayback()
        shadowOf(audioManager()).setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        manager.releaseHold()

        verify(exactly = 0) { player.start() }
        assertEquals(VOLUME * DUCK_FACTOR, volumes.last(), 0.0001f)
    }

    @Test
    fun `focus listener still ducks and restores`() {
        val listener = getField<AudioManager.OnAudioFocusChangeListener>("audioFocusListener")

        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        assertEquals(VOLUME * DUCK_FACTOR, volumes.last(), 0.0001f)

        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(VOLUME, volumes.last(), 0.0001f)
    }

    @Test
    fun `release while muted stays silent`() {
        manager.setMuted(true)
        manager.duck()
        manager.holdPlayback()
        manager.releaseHold()

        verify(exactly = 0) { player.start() }
        assertEquals(emptyList<Float>(), volumes)
        assertEquals(true, manager.muted)
    }

    private fun audioManager() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun setField(name: String, value: Any?) {
        BgMusicManager::class.java.getDeclaredField(name).apply { isAccessible = true }.set(manager, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getField(name: String): T =
        BgMusicManager::class.java.getDeclaredField(name).apply { isAccessible = true }.get(manager) as T

    private companion object {
        const val VOLUME = 0.8f
        const val DUCK_FACTOR = 0.2f
    }
}
