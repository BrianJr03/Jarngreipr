package jr.brian.home.esde.data

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class LaunchChoicesRestoreTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    private fun seeded() = ESDEPreferencesManager(context).apply {
        setGameEmulator("snes/a.sfc", "com.retroarch")
        setGameLaunchCommand("snes/a.sfc", "%EMULATOR_RETROARCH%")
        setGameCore("snes/a.sfc", "snes9x")
        setSystemEmulator("psp", "org.ppsspp.ppsspp")
    }

    @Test
    fun `a backup replaces every choice and the result survives a reload`() {
        seeded().restoreLaunchChoices(
            gameEmulators = mapOf("n64/b.z64" to "com.mupen"),
            systemEmulators = mapOf("ps2" to "xyz.aethersx2"),
            commands = emptyMap(),
            cores = mapOf("n64/b.z64" to "mupen64plus"),
        )

        val reloaded = ESDEPreferencesManager(context).state.value
        assertEquals(mapOf("n64/b.z64" to "com.mupen"), reloaded.gameEmulatorMap)
        assertEquals(mapOf("ps2" to "xyz.aethersx2"), reloaded.systemEmulatorMap)
        assertEquals(emptyMap<String, String>(), reloaded.gameCommandMap)
        assertEquals(mapOf("n64/b.z64" to "mupen64plus"), reloaded.gameCoreMap)
    }

    @Test
    fun `a backup without choices keeps the current ones`() {
        val before = seeded().state.value

        ESDEPreferencesManager(context).restoreLaunchChoices(null, null, null, null)

        val reloaded = ESDEPreferencesManager(context).state.value
        assertEquals(before.gameEmulatorMap, reloaded.gameEmulatorMap)
        assertEquals(before.systemEmulatorMap, reloaded.systemEmulatorMap)
        assertEquals(before.gameCommandMap, reloaded.gameCommandMap)
        assertEquals(before.gameCoreMap, reloaded.gameCoreMap)
        assertEquals(mapOf("psp" to "org.ppsspp.ppsspp"), reloaded.systemEmulatorMap)
    }

    @Test
    fun `only the maps a backup carries are replaced`() {
        seeded().restoreLaunchChoices(null, mapOf("gba" to "com.mgba"), null, null)

        val reloaded = ESDEPreferencesManager(context).state.value
        assertEquals(mapOf("gba" to "com.mgba"), reloaded.systemEmulatorMap)
        assertEquals(mapOf("snes/a.sfc" to "com.retroarch"), reloaded.gameEmulatorMap)
        assertEquals(mapOf("snes/a.sfc" to "snes9x"), reloaded.gameCoreMap)
    }
}
