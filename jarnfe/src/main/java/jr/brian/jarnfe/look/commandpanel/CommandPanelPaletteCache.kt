package jr.brian.jarnfe.look.commandpanel

import android.util.AtomicFile
import jr.brian.core.model.CommandPanelPalette
import java.io.File

internal interface CommandPanelPaletteStore {
    fun read(): CommandPanelPalette?
    fun write(palette: CommandPanelPalette)
    fun clear()
}

/** Lives in the no-backup directory, so a restored device never starts from another device's colors. */
internal class CommandPanelPaletteCache(noBackupDir: File) : CommandPanelPaletteStore {
    private val file = AtomicFile(File(noBackupDir, FILE_NAME))

    override fun read(): CommandPanelPalette? = runCatching {
        decodeCommandPanelPalette(file.readFully().decodeToString())
    }.getOrNull()

    override fun write(palette: CommandPanelPalette) {
        runCatching {
            val stream = file.startWrite()
            try {
                stream.write(encodeCommandPanelPalette(palette).encodeToByteArray())
                file.finishWrite(stream)
            } catch (e: Exception) {
                file.failWrite(stream)
                throw e
            }
        }
    }

    override fun clear() {
        runCatching { file.delete() }
    }

    private companion object {
        const val FILE_NAME = "command_panel_palette"
    }
}
