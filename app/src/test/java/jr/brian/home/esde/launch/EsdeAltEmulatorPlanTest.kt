package jr.brian.home.esde.launch

import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.migrate.EsdeAltEmulatorSkip
import jr.brian.frontend.library.migrate.EsdeAltEmulatorSkipReason
import jr.brian.frontend.library.migrate.EsdeAltEmulators
import jr.brian.frontend.library.migrate.EsdeGameAltEmulator
import jr.brian.frontend.library.migrate.EsdeSystemAltEmulator
import jr.brian.frontend.library.migrate.EsdeSystemGame
import jr.brian.frontend.library.migrate.launchChoiceKey
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.gameKey
import jr.brian.jarnfe.launch.JarnEmulatorChoice
import org.junit.Assert.assertEquals
import org.junit.Test

private const val PS2_COMMAND = "%EMULATOR_AETHERSX2% %ACTION%=android.intent.action.MAIN %EXTRA_bootPath%=%ROMSAF%"
private const val RA_COMMAND =
    "%EMULATOR_RETROARCH% %EXTRA_CONFIGFILE%=/x/retroarch.cfg %EXTRA_LIBRETRO%=snes9x_libretro_android.so %EXTRA_ROM%=%ROMSAF%"

class EsdeAltEmulatorPlanTest {
    private val commands = listOf("AetherSX2" to PS2_COMMAND, "Snes9x - Current" to RA_COMMAND, "Odd" to "no token")
    private val rules = mapOf(
        "AETHERSX2" to listOf("xyz.aethersx2.android/xyz.aethersx2.android.EmulationActivity"),
        "RETROARCH" to listOf("com.retroarch.aarch64/a", "com.retroarch/b"),
    )

    private fun match(label: String, installed: Set<String> = setOf("xyz.aethersx2.android", "com.retroarch")) =
        matchEsdeLabel(label, commands, { rules[it].orEmpty() }, { it in installed })

    private val matcher = { _: String, label: String -> match(label) }

    @Test
    fun `a known label maps to its command and installed package`() {
        assertEquals(EsdeLabelMatch.Found(JarnEmulatorChoice("xyz.aethersx2.android", PS2_COMMAND)), match("AetherSX2"))
        assertEquals(EsdeLabelMatch.Found(JarnEmulatorChoice("xyz.aethersx2.android", PS2_COMMAND)), match(" aethersx2 "))
    }

    @Test
    fun `an unknown label or a command without an emulator is unknown`() {
        assertEquals(EsdeLabelMatch.Unknown, match("Something Else"))
        assertEquals(EsdeLabelMatch.Unknown, match("Odd"))
    }

    @Test
    fun `a label whose emulator is missing is not installed`() {
        assertEquals(EsdeLabelMatch.NotInstalled, match("AetherSX2", installed = emptySet()))
    }

    @Test
    fun `a RetroArch label saves the installed RetroArch's core and no command`() {
        assertEquals(
            EsdeLabelMatch.Found(
                JarnEmulatorChoice("com.retroarch", corePath = "/data/data/com.retroarch/cores/snes9x_libretro_android.so"),
            ),
            match("Snes9x - Current"),
        )
        assertEquals(
            JarnEmulatorChoice("com.retroarch", "%EMULATOR_RETROARCH% %EXTRA_ROM%=%ROMSAF%"),
            esdeChoice("com.retroarch", "%EMULATOR_RETROARCH% %EXTRA_ROM%=%ROMSAF%"),
        )
    }

    @Test
    fun `a command with a token the launcher can't fill is unsupported`() {
        val dos = listOf("DOSBox" to "%EMULATOR_AETHERSX2% %EXTRA_path%=%ROMPATHRAW%")
        assertEquals(EsdeLabelMatch.Unsupported, matchEsdeLabel("DOSBox", dos, { rules[it].orEmpty() }, { true }))
    }

    @Test
    fun `ES-DE's placeholder core path becomes RetroArch's cores folder`() {
        val command = "%EMULATOR_RETROARCH% %EXTRA_LIBRETRO%=%INTERNALDATA%/%ANDROIDPACKAGE%/cores/mgba_libretro_android.so"
        assertEquals(
            JarnEmulatorChoice("com.retroarch", corePath = "/data/data/com.retroarch/cores/mgba_libretro_android.so"),
            esdeChoice("com.retroarch", command),
        )
    }

    @Test
    fun `rules are tried in launch order`() {
        val entries = esdeRules("RETROARCH", mapOf("RETROARCH" to listOf("custom/a")), mapOf("RETROARCH" to listOf("bundled/b")))
        assertEquals("custom/a", entries.first())
        assertEquals("bundled/b", entries.last())
    }

    @Test
    fun `game choices are saved and existing ones are kept`() {
        val found = EsdeAltEmulators(
            games = listOf(
                EsdeGameAltEmulator("ps2/a.iso", "ps2", "AetherSX2", "iso"),
                EsdeGameAltEmulator("ps2/b.iso", "ps2", "AetherSX2", "iso"),
                EsdeGameAltEmulator("ps2/c.iso", "ps2", "Nope", "iso"),
            ),
        )
        val saved = LaunchChoices(GameEmulatorMaps(cores = mapOf("ps2/b.iso" to "mine")))

        val plan = planEsdeAltEmulators(found, saved, { listOf(it) }, matcher)

        assertEquals(mapOf("ps2/a.iso" to "xyz.aethersx2.android"), plan.choices.games.emulators)
        assertEquals(mapOf("ps2/a.iso" to PS2_COMMAND), plan.choices.games.commands)
        assertEquals(mapOf("ps2/b.iso" to "mine"), plan.choices.games.cores)
        assertEquals(1, plan.result.gamesSet)
        assertEquals(1, plan.result.keptExisting)
        assertEquals(listOf(EsdeAltEmulatorSkip("ps2", "Nope", EsdeAltEmulatorSkipReason.UnknownLabel)), plan.result.skipped)
    }

    @Test
    fun `a system choice fills the system and every game without its own choice`() {
        val found = EsdeAltEmulators(
            systems = listOf(
                EsdeSystemAltEmulator(
                    systemId = "snes",
                    esdeSystem = "snes",
                    label = "Snes9x - Current",
                    games = listOf(EsdeSystemGame("snes/a.sfc", null), EsdeSystemGame("snes/b.sfc", null)),
                ),
            ),
        )
        val saved = LaunchChoices(GameEmulatorMaps(emulators = mapOf("snes/b.sfc" to "com.snes9x.ex")))

        val plan = planEsdeAltEmulators(found, saved, { listOf(it, "sfc") }, matcher)

        assertEquals(mapOf("snes" to "com.retroarch", "sfc" to "com.retroarch"), plan.choices.systems)
        assertEquals(mapOf("snes/a.sfc" to "/data/data/com.retroarch/cores/snes9x_libretro_android.so"), plan.choices.games.cores)
        assertEquals(mapOf("snes/b.sfc" to "com.snes9x.ex"), plan.choices.games.emulators)
        assertEquals(1, plan.result.systemsSet)
    }

    @Test
    fun `a system that already has a choice keeps it`() {
        val found = EsdeAltEmulators(
            systems = listOf(EsdeSystemAltEmulator("ps2", "ps2", "AetherSX2", listOf(EsdeSystemGame("ps2/a.iso", null)))),
        )
        val saved = LaunchChoices(systems = mapOf("ps2" to "xyz.nethersx2"))

        val plan = planEsdeAltEmulators(found, saved, { listOf(it) }, matcher)

        assertEquals(saved, plan.choices)
        assertEquals(1, plan.result.keptExisting)
        assertEquals(0, plan.result.systemsSet)
    }

    @Test
    fun `a game's own ES-DE choice wins over its system's`() {
        val found = EsdeAltEmulators(
            systems = listOf(EsdeSystemAltEmulator("ps2", "ps2", "AetherSX2", listOf(EsdeSystemGame("ps2/a.iso", null)))),
            games = listOf(EsdeGameAltEmulator("ps2/a.iso", "ps2", "Snes9x - Current", "iso")),
        )

        val plan = planEsdeAltEmulators(found, LaunchChoices(), { listOf(it) }, matcher)

        assertEquals("com.retroarch", plan.choices.games.emulators["ps2/a.iso"])
        assertEquals(emptyMap<String, String>(), plan.choices.games.commands)
    }

    @Test
    fun `the import key is the launch path's key`() {
        val withPath = GameEntity(sourceId = 1, systemId = "snes", relativePath = "a.sfc", absolutePath = "/s/snes/a.sfc", displayName = "a")
        val fromFolder = withPath.copy(absolutePath = null)
        listOf(withPath, fromFolder).forEach { entity ->
            val launched = GameInfo(path = entity.absolutePath ?: entity.relativePath, name = "a", systemName = entity.systemId)
            assertEquals(gameKey(launched), entity.launchChoiceKey())
        }
    }
}
