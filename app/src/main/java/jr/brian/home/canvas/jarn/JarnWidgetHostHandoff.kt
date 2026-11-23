package jr.brian.home.canvas.jarn

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import jr.brian.jarnfe.canvas.JarnWidgetLease

/**
 * The system keeps one update callback per host id, and Classic and Jarn FE share an id so saved
 * widgets work in both. Jarn FE takes the callback while its widgets are visible and always hands
 * it back, so Classic's widgets keep updating.
 */
object JarnWidgetHostHandoff {
    private val lock = Any()
    private var classic: AppWidgetHost? = null
    private var jarn: AppWidgetHost? = null
    private var leases = 0

    fun classicStarted(host: AppWidgetHost) = synchronized(lock) {
        classic = host
        jarn?.listenSafely()
        Unit
    }

    fun classicStopped(host: AppWidgetHost) = synchronized(lock) {
        if (classic === host) classic = null
        jarn?.listenSafely()
        Unit
    }

    internal fun acquire(manager: AppWidgetManager, createHost: () -> AppWidgetHost): JarnWidgetLease =
        synchronized(lock) {
            val host = jarn ?: createHost().also {
                jarn = it
                it.listenSafely()
            }
            leases += 1
            Lease(host, manager)
        }

    private fun release() = synchronized(lock) {
        leases -= 1
        if (leases > 0) return@synchronized
        leases = 0
        runCatching { jarn?.stopListening() }
        jarn = null
        classic?.listenSafely()
    }

    private fun AppWidgetHost.listenSafely() {
        runCatching { startListening() }
    }

    private class Lease(
        private val host: AppWidgetHost,
        private val manager: AppWidgetManager,
    ) : JarnWidgetLease {
        private var closed = false

        override fun createView(context: Context, widgetId: Int): View? {
            val info = manager.getAppWidgetInfo(widgetId) ?: return null
            return runCatching { host.createView(context, widgetId, info) }.getOrNull()
        }

        override fun close() {
            val shouldRelease = synchronized(lock) {
                val first = !closed
                closed = true
                first
            }
            if (shouldRelease) release()
        }
    }
}
