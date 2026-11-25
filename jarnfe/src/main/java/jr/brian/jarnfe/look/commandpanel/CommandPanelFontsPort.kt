package jr.brian.jarnfe.look.commandpanel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import java.io.InputStream

internal sealed interface CommandPanelFontsRead {
    data class Fresh(val fonts: CommandPanelFonts) : CommandPanelFontsRead
    data object Locked : CommandPanelFontsRead
    data object Stopped : CommandPanelFontsRead
    data object Untrusted : CommandPanelFontsRead

    /** Authentic, but too old to share fonts (no `/fonts` row). */
    data object Unsupported : CommandPanelFontsRead
    data object Rejected : CommandPanelFontsRead
    data object Unavailable : CommandPanelFontsRead
}

internal interface CommandPanelFontsPort {
    fun read(): CommandPanelFontsRead

    /** Command Panel's copy of imported font [id], or null. Authenticates first, like [read]. */
    fun openFont(id: String): InputStream?
    fun setObserving(observing: Boolean)
    fun listen(onChange: () -> Unit)
    fun unlisten()
}

private fun commandPanelUri(path: String): Uri = Uri.Builder()
    .scheme("content")
    .authority(CommandPanelThemeContract.AUTHORITY)
    .path(path)
    .build()

/** Same trust rules as [AndroidCommandPanelThemePort]: authenticate before and after every read. */
internal class AndroidCommandPanelFontsPort(private val context: Context) : CommandPanelFontsPort {
    private val uri = commandPanelUri(CommandPanelFontsContract.PATH)

    @Volatile
    private var changeListener: (() -> Unit)? = null
    private var isObserving = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            changeListener?.invoke()
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            changeListener?.invoke()
        }
    }

    override fun read(): CommandPanelFontsRead = try {
        when (val gate = gate()) {
            null -> queryRow().takeIf { isTrusted() } ?: CommandPanelFontsRead.Untrusted
            else -> gate
        }
    } catch (_: Exception) {
        CommandPanelFontsRead.Unavailable
    }

    override fun openFont(id: String): InputStream? = runCatching {
        if (gate() != null) return null
        val fileUri = commandPanelUri("${CommandPanelFontsContract.FILE_PATH}/$id")
        val descriptor = context.contentResolver.openFileDescriptor(fileUri, "r") ?: return null
        if (!isTrusted()) {
            descriptor.close()
            return null
        }
        ParcelFileDescriptor.AutoCloseInputStream(descriptor)
    }.getOrNull()

    /** Null when Command Panel is trusted and running; otherwise why it can't be read. */
    private fun gate(): CommandPanelFontsRead? {
        if (!context.isUserUnlocked()) return CommandPanelFontsRead.Locked
        return when (decideCommandPanelTrust(context.commandPanelIdentity())) {
            CommandPanelTrust.Untrusted -> CommandPanelFontsRead.Untrusted
            CommandPanelTrust.Stopped -> CommandPanelFontsRead.Stopped
            CommandPanelTrust.Trusted -> null
        }
    }

    private fun isTrusted(): Boolean =
        decideCommandPanelTrust(context.commandPanelIdentity()) != CommandPanelTrust.Untrusted

    private fun queryRow(): CommandPanelFontsRead {
        val client = context.contentResolver.acquireUnstableContentProviderClient(uri)
            ?: return CommandPanelFontsRead.Unavailable
        return client.use {
            val cursor = it.query(uri, CommandPanelFontsContract.COLUMNS.toTypedArray(), null, null, null)
                ?: return CommandPanelFontsRead.Unsupported
            cursor.use { row ->
                parseCommandPanelFontsRow(row)?.let(CommandPanelFontsRead::Fresh) ?: CommandPanelFontsRead.Rejected
            }
        }
    }

    override fun setObserving(observing: Boolean) {
        if (observing == isObserving) return
        runCatching {
            if (observing) {
                context.contentResolver.registerContentObserver(uri, false, observer)
            } else {
                context.contentResolver.unregisterContentObserver(observer)
            }
            isObserving = observing
        }
    }

    override fun listen(onChange: () -> Unit) {
        changeListener = onChange
        ContextCompat.registerReceiver(
            context,
            packageReceiver,
            commandPanelPackageFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun unlisten() {
        changeListener = null
        context.unregisterReceiver(packageReceiver)
    }
}
