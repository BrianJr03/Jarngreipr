package jr.brian.jarnfe.look.commandpanel

import android.content.BroadcastReceiver
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.res.Configuration
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PatternMatcher
import android.os.UserManager
import androidx.core.content.ContextCompat
import jr.brian.core.model.CommandPanelPalette
import java.security.MessageDigest

internal sealed interface CommandPanelRead {
    data class Fresh(val palette: CommandPanelPalette) : CommandPanelRead

    /** The user hasn't unlocked yet, so nothing can be known about Command Panel. */
    data object Locked : CommandPanelRead

    /** Authentic but force-stopped. Querying would wake it. */
    data object Stopped : CommandPanelRead

    data object Untrusted : CommandPanelRead

    /** Authentic, but its row breaks the contract. */
    data object Rejected : CommandPanelRead

    /** This read failed, possibly before authentication, and a later one may work. Never grounds for observing. */
    data object Unavailable : CommandPanelRead
}

internal interface CommandPanelThemePort {
    fun read(): CommandPanelRead
    fun setObserving(observing: Boolean)
    fun listen(onChange: () -> Unit)
    fun unlisten()
}

internal class AndroidCommandPanelThemePort(private val context: Context) : CommandPanelThemePort {
    private val uri = Uri.Builder()
        .scheme("content")
        .authority(CommandPanelThemeContract.AUTHORITY)
        .path(CommandPanelThemeContract.PATH)
        .build()

    @Volatile
    private var changeListener: (() -> Unit)? = null
    private var isObserving = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            changeListener?.invoke()
        }
    }

    private val configCallback = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            changeListener?.invoke()
        }

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() = Unit
    }

    /** A read during an update looks untrusted, so the finished install triggers a fresh one. */
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            changeListener?.invoke()
        }
    }

    override fun read(): CommandPanelRead = try {
        readAuthenticated()
    } catch (_: Exception) {
        CommandPanelRead.Unavailable
    }

    private fun readAuthenticated(): CommandPanelRead {
        if (!context.isUserUnlocked()) return CommandPanelRead.Locked
        when (decideCommandPanelTrust(resolveIdentity())) {
            CommandPanelTrust.Untrusted -> return CommandPanelRead.Untrusted
            CommandPanelTrust.Stopped -> return CommandPanelRead.Stopped
            CommandPanelTrust.Trusted -> Unit
        }
        val read = queryRow()
        val stillTrusted = decideCommandPanelTrust(resolveIdentity()) != CommandPanelTrust.Untrusted
        return if (stillTrusted) read else CommandPanelRead.Untrusted
    }

    private fun queryRow(): CommandPanelRead {
        val client = context.contentResolver.acquireUnstableContentProviderClient(uri)
            ?: return CommandPanelRead.Unavailable
        return client.use {
            val cursor = it.query(uri, CommandPanelThemeContract.COLUMNS.toTypedArray(), null, null, null)
                ?: return CommandPanelRead.Unavailable
            cursor.use { row ->
                parseCommandPanelRow(row)?.let { palette -> CommandPanelRead.Fresh(palette) }
                    ?: CommandPanelRead.Rejected
            }
        }
    }

    private fun resolveIdentity(): CommandPanelProviderIdentity? = context.commandPanelIdentity()

    /** Registering throws when the provider is missing, so a failure just stays unregistered until the next read. */
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
        context.registerComponentCallbacks(configCallback)
        ContextCompat.registerReceiver(
            context,
            packageReceiver,
            commandPanelPackageFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun unlisten() {
        changeListener = null
        context.unregisterComponentCallbacks(configCallback)
        context.unregisterReceiver(packageReceiver)
    }
}

/** Who answers Command Panel's authority, for [decideCommandPanelTrust]. Shared by the palette and fonts ports. */
internal fun Context.commandPanelIdentity(): CommandPanelProviderIdentity? {
    val provider = packageManager.resolveContentProvider(
        CommandPanelThemeContract.AUTHORITY,
        PackageManager.ComponentInfoFlags.of(0),
    ) ?: return null
    if (provider.packageName != CommandPanelThemeContract.PACKAGE) {
        return CommandPanelProviderIdentity(provider.packageName, emptyList(), isStopped = true)
    }
    val info = packageManager.getPackageInfo(
        provider.packageName,
        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
    )
    val flags = info.applicationInfo?.flags
    return CommandPanelProviderIdentity(
        packageName = provider.packageName,
        signerDigests = info.signingInfo?.apkContentsSigners.orEmpty().map(::signerSha256),
        isStopped = flags == null || flags and ApplicationInfo.FLAG_STOPPED != 0,
    )
}

private fun signerSha256(signature: Signature): String =
    MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
        .joinToString("") { "%02x".format(it) }

internal fun Context.isUserUnlocked(): Boolean = getSystemService(UserManager::class.java)?.isUserUnlocked == true

internal fun commandPanelPackageFilter() = IntentFilter().apply {
    addAction(Intent.ACTION_PACKAGE_ADDED)
    addAction(Intent.ACTION_PACKAGE_REPLACED)
    addAction(Intent.ACTION_PACKAGE_CHANGED)
    addAction(Intent.ACTION_PACKAGE_REMOVED)
    addDataScheme("package")
    addDataSchemeSpecificPart(CommandPanelThemeContract.PACKAGE, PatternMatcher.PATTERN_LITERAL)
}
