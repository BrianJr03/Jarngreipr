package jr.brian.home.jarn

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.MotionEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.data.PowerSettingsManager
import jr.brian.home.model.BackButtonShortcut
import jr.brian.home.util.shizuku.ShizukuInputManager
import jr.brian.jarnfe.tools.JarnBackShortcut
import jr.brian.jarnfe.tools.JarnBackShortcuts
import jr.brian.jarnfe.tools.JarnBackSlot
import jr.brian.jarnfe.tools.JarnInputTools
import jr.brian.jarnfe.tools.JarnPointerBounds
import jr.brian.jarnfe.tools.JarnPointerCommand
import jr.brian.jarnfe.tools.JarnPointerStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/** Uses the settings Classic already stores and backs up, so nothing new is persisted here. */
@Singleton
class AppJarnInputTools @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val power: PowerSettingsManager,
) : JarnInputTools {
    private val connected = AtomicBoolean(false)

    override val pointerStatus: Flow<JarnPointerStatus> = combine(
        ShizukuInputManager.isShizukuAvailable,
        ShizukuInputManager.isShizukuPermissionGranted,
        ShizukuInputManager.isServiceConnected,
        ::JarnPointerStatus,
    )

    override val backShortcuts: Flow<JarnBackShortcuts> = combine(
        power.backButtonShortcutEnabled,
        power.backButtonShortcut,
        power.backButtonShortcutAppPackage,
        power.secondaryBackButtonShortcut,
        power.secondaryBackButtonShortcutAppPackage,
    ) { enabled, primary, primaryApp, secondary, secondaryApp ->
        JarnBackShortcuts(enabled, primary.toJarn(), primaryApp, secondary.toJarn(), secondaryApp)
    }

    override fun connectPointer() {
        if (connected.compareAndSet(false, true)) {
            ShizukuInputManager.initialize()
        } else {
            ShizukuInputManager.checkPermission()
        }
    }

    override fun requestPointerPermission() = ShizukuInputManager.requestPermission()

    /** The input service injects on the default display, whichever screen hosts the trackpad. */
    override fun pointerBounds(): JarnPointerBounds {
        val displays = context.getSystemService(DisplayManager::class.java)
        val size = Point()
        @Suppress("DEPRECATION")
        displays.getDisplay(Display.DEFAULT_DISPLAY).getRealSize(size)
        return JarnPointerBounds(size.x.toFloat(), size.y.toFloat())
    }

    override fun sendPointer(command: JarnPointerCommand) {
        when (command) {
            is JarnPointerCommand.Move -> ShizukuInputManager.injectMouseMove(command.x, command.y)
            is JarnPointerCommand.Scroll ->
                ShizukuInputManager.injectMouseScroll(command.x, command.y, command.amount)
            is JarnPointerCommand.Click -> {
                val button = if (command.secondary) MotionEvent.BUTTON_SECONDARY else MotionEvent.BUTTON_PRIMARY
                ShizukuInputManager.injectMouseClick(command.x, command.y, button, MotionEvent.ACTION_DOWN)
                ShizukuInputManager.injectMouseClick(command.x, command.y, button, MotionEvent.ACTION_UP)
            }
        }
    }

    override fun setBackShortcutsEnabled(enabled: Boolean) {
        power.setBackButtonShortcutEnabled(enabled)
        if (!enabled) power.resetBackButtonShortcut()
    }

    override fun setBackShortcut(slot: JarnBackSlot, shortcut: JarnBackShortcut) {
        val stored = shortcut.toStored()
        if (slot == JarnBackSlot.Primary) {
            power.setBackButtonShortcut(stored)
        } else {
            power.setSecondaryBackButtonShortcut(stored)
        }
    }

    override fun setBackShortcutApp(slot: JarnBackSlot, packageName: String) {
        if (slot == JarnBackSlot.Primary) {
            power.setBackButtonShortcutAppPackage(packageName)
        } else {
            power.setSecondaryBackButtonShortcutAppPackage(packageName)
        }
    }
}

internal fun BackButtonShortcut.toJarn(): JarnBackShortcut = JarnBackShortcut.valueOf(name)

internal fun JarnBackShortcut.toStored(): BackButtonShortcut = BackButtonShortcut.valueOf(name)
