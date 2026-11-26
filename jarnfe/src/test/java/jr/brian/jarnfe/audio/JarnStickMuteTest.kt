package jr.brian.jarnfe.audio

import android.app.Application
import jr.brian.jarnfe.R
import jr.brian.jarnfe.shell.JarnCornerToast
import jr.brian.jarnfe.shell.JarnCornerToasts
import jr.brian.jarnfe.shell.JarnToastIcon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnStickMuteTest {
    private val audio = FakeAudio()

    @Before
    fun setUp() = JarnCornerToasts.clear()

    @Test
    fun `L3 flips the jingles mute switch both ways and says so`() = runTest {
        assertTrue(JarnStickMute.toggleJingles(audio))
        assertTrue(audio.jinglesMuted.value)
        assertEquals(jinglesToast(muted = true), JarnCornerToasts.current?.toast)

        assertFalse(JarnStickMute.toggleJingles(audio))
        assertFalse(audio.jinglesMuted.value)
        assertEquals(jinglesToast(muted = false), JarnCornerToasts.current?.toast)
        assertEquals(emptyList<JarnCornerToast>(), JarnCornerToasts.waiting)
    }

    @Test
    fun `R3 flips the music mute switch both ways and says so`() {
        assertTrue(JarnStickMute.toggleMusic(audio))
        assertTrue(audio.musicOff)
        assertEquals(musicToast(muted = true), JarnCornerToasts.current?.toast)

        assertFalse(JarnStickMute.toggleMusic(audio))
        assertFalse(audio.musicOff)
        assertEquals(musicToast(muted = false), JarnCornerToasts.current?.toast)
    }

    @Test
    fun `each state has its own text, icon and button`() {
        assertEquals(R.string.jarnfe_audio_toast_jingles_muted, jinglesToast(true).title)
        assertEquals(R.string.jarnfe_audio_toast_jingles_on, jinglesToast(false).title)
        assertEquals(R.string.jarnfe_audio_toast_music_muted, musicToast(true).title)
        assertEquals(R.string.jarnfe_audio_toast_music_on, musicToast(false).title)
        assertEquals(JarnToastIcon.Res(R.drawable.ic_jarn_bell_off), jinglesToast(true).icon)
        assertEquals(JarnToastIcon.Res(R.drawable.ic_jarn_bell), jinglesToast(false).icon)
        assertEquals(JarnToastIcon.Res(R.drawable.ic_jarn_music_off), musicToast(true).icon)
        assertEquals(JarnToastIcon.Res(R.drawable.ic_jarn_music), musicToast(false).icon)
        assertEquals(R.string.jarnfe_audio_toast_l3, jinglesToast(true).subtitle)
        assertEquals(R.string.jarnfe_audio_toast_r3, musicToast(true).subtitle)
        assertEquals("mute_jingles", jinglesToast(true).key)
        assertEquals("mute_music", musicToast(true).key)
    }

    @Test
    fun `jingles and music toasts queue rather than replace each other`() = runTest {
        JarnStickMute.toggleJingles(audio)
        JarnStickMute.toggleMusic(audio)
        assertEquals(jinglesToast(muted = true), JarnCornerToasts.current?.toast)
        assertEquals(listOf(musicToast(muted = true)), JarnCornerToasts.waiting)
    }

    private class FakeAudio : JarnAudioData {
        val jinglesMuted = MutableStateFlow(false)
        var musicOff = false

        override val music: JarnMusicSettings
            get() = JarnMusicSettings(JarnMusicSource.File, null, null, 1f, musicOff)
        override val jingles: Flow<JarnJingleSettings> = jinglesMuted.map { JarnJingleSettings(muted = it) }
        override val repos: Flow<List<JarnJingleRepo>> = emptyFlow()
        override val folders: Flow<List<JarnJingleFolder>> = emptyFlow()
        override val download: StateFlow<JarnJingleDownload?> = MutableStateFlow(null)
        override val otherMediaPlaying: Flow<Boolean> = emptyFlow()

        override fun setMusicMuted(muted: Boolean) {
            musicOff = muted
        }

        override fun setJinglesMuted(muted: Boolean) {
            jinglesMuted.value = muted
        }

        override fun allowPlayback() = Unit
        override fun holdPlayback() = Unit
        override suspend fun playJingle(gameKey: String) = false
        override fun stopJingle() = Unit
        override fun setMusicFolder(uri: String) = Unit
        override fun setMusicFile(uri: String) = Unit
        override fun setMusicVolume(volume: Float) = Unit
        override fun setJinglesEnabled(enabled: Boolean) = Unit
        override fun setJingleVolume(volume: Float) = Unit
        override fun setRegexPriority(enabled: Boolean) = Unit
        override fun setNormalize(enabled: Boolean) = Unit
        override fun addRepo(slug: String) = false
        override fun removeRepo(slug: String) = Unit
        override suspend fun searchRepos(query: String): List<JarnJingleSearchResult>? = null
        override fun startDownload(slug: String, onDone: (JarnJingleDownloadResult) -> Unit) = Unit
        override fun cancelDownload() = Unit
        override fun addFolder(uri: String) = JarnJingleFolderResult.Invalid
        override fun removeFolder(path: String) = Unit
        override fun refreshFolders() = Unit
        override fun packParent(): String? = null
        override fun setPackParent(uri: String) = Unit
        override suspend fun savePackEntry(parentUri: String, draft: JarnJinglePackDraft) = false
        override suspend fun gameKey(libraryId: String): String? = null
    }
}
