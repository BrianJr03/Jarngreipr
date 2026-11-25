package jr.brian.home.esde.launch

import android.content.Context
import android.content.pm.PackageInfo
import io.mockk.mockk
import jr.brian.frontend.library.migrate.EsdeAltEmulatorSkip
import jr.brian.frontend.library.migrate.EsdeAltEmulatorSkipReason
import jr.brian.frontend.library.migrate.EsdeAltEmulators
import jr.brian.frontend.library.migrate.EsdeGameAltEmulator
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.util.BundledEsdeSystems
import jr.brian.home.esde.util.EsdeCommandLauncher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

private const val RETROARCH = "com.retroarch.aarch64"
private const val PIZZA_BOY = "it.dbtecno.pizzaboygba"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BundledEsdeSystemsTest {
    private lateinit var context: Context
    private lateinit var prefs: ESDEPreferencesManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        prefs = ESDEPreferencesManager(context)
    }

    private fun install(vararg packages: String) = packages.forEach { name ->
        shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName = name })
    }

    private fun writer() = AppEsdeAltEmulatorWriter(context, prefs, mockk(relaxed = true))

    private fun gba(label: String) = EsdeAltEmulators(games = listOf(EsdeGameAltEmulator("gba/a.gba", "gba", label, "gba")))

    @Test
    fun `the bundled files parse in full`() {
        val systems = BundledEsdeSystems.allCommands(context)
        assertEquals(195, systems.size)
        assertEquals(747, systems.values.sumOf { it.size })
        assertEquals(105, BundledEsdeSystems.rules(context).size)
        assertTrue(systems.getValue("gba").any { it.first == "mGBA" })
    }

    @Test
    fun `the asset is ES-DE's file byte for byte`() {
        val raw = File("src/main/assets/esde/android/es_find_rules.xml").inputStream().use(EsdeCommandLauncher::parseRules)
        assertEquals(raw, BundledEsdeSystems.rules(context))
    }

    @Test
    fun `a GBA RetroArch label saves RetroArch and the mGBA core`() {
        install(RETROARCH)

        val result = writer().write(gba("mGBA"))

        val state = prefs.state.value
        assertEquals(1, result.gamesSet)
        assertEquals(RETROARCH, state.gameEmulatorMap["gba/a.gba"])
        assertEquals("/data/data/$RETROARCH/cores/mgba_libretro_android.so", state.gameCoreMap["gba/a.gba"])
        assertEquals(null, state.gameCommandMap["gba/a.gba"])
    }

    @Test
    fun `a standalone label saves its package and ES-DE's command`() {
        install(PIZZA_BOY)

        writer().write(gba("Pizza Boy GBA (Standalone)"))

        val state = prefs.state.value
        assertEquals(PIZZA_BOY, state.gameEmulatorMap["gba/a.gba"])
        assertTrue(state.gameCommandMap.getValue("gba/a.gba").startsWith("%EMULATOR_PIZZA-BOY-GBA%"))
    }

    @Test
    fun `a standalone emulator that is missing is skipped`() {
        val result = writer().write(gba("Pizza Boy GBA (Standalone)"))
        assertEquals(
            listOf(EsdeAltEmulatorSkip("gba", "Pizza Boy GBA (Standalone)", EsdeAltEmulatorSkipReason.NotInstalled)),
            result.skipped,
        )
    }

    @Test
    fun `a custom es_systems entry wins over the bundled one`() {
        install(RETROARCH)
        File(context.filesDir.parent, "ES-DE/custom_systems").apply { mkdirs() }.resolve("es_systems.xml").writeText(
            """
            <systemList><system><name>gba</name>
            <command label="mGBA">%EMULATOR_RETROARCH% %EXTRA_LIBRETRO%=%INTERNALDATA%/%ANDROIDPACKAGE%/cores/custom_libretro_android.so %EXTRA_ROM%=%ROM%</command>
            </system></systemList>
            """.trimIndent(),
        )

        writer().write(gba("mGBA"))

        assertEquals("/data/data/$RETROARCH/cores/custom_libretro_android.so", prefs.state.value.gameCoreMap["gba/a.gba"])
    }
}
