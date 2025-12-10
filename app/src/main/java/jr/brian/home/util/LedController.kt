package jr.brian.home.util

import android.os.IBinder
import android.os.Parcel
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class LedController {
    private val pServerBinder: IBinder? = try {
        val serviceManager = Class.forName("android.os.ServiceManager")
        val getService = serviceManager.getDeclaredMethod("getService", String::class.java)
        getService.invoke(serviceManager, "PServerBinder") as? IBinder
    } catch (e: Exception) {
        null
    }
    private val commandExecutor = Executors.newSingleThreadExecutor()
    private val commandQueue = LinkedBlockingQueue<String>(10)

    fun setLedColor(
        red: Float,
        green: Float,
        blue: Float,
        brightness: Int = 255,
        leftTop: Boolean = true,
        leftBottom: Boolean = true,
        rightTop: Boolean = true,
        rightBottom: Boolean = true
    ) {
        val commands = mutableListOf<String>()

        if (leftTop) {
            commands.add("echo 1-$red:$green:$blue:$brightness > /sys/class/sn3112l/led/brightness")
        }
        if (leftBottom) {
            commands.add("echo 2-$red:$green:$blue:$brightness > /sys/class/sn3112l/led/brightness")
        }
        if (rightTop) {
            commands.add("echo 1-$red:$green:$blue:$brightness > /sys/class/sn3112r/led/brightness")
        }
        if (rightBottom) {
            commands.add("echo 2-$red:$green:$blue:$brightness > /sys/class/sn3112r/led/brightness")
        }

        if (commands.isNotEmpty()) {
            val command = commands.joinToString(" && ")
            if (!commandQueue.offer(command)) {
                commandQueue.poll()
                commandQueue.offer(command)
            }
            executeCommandAsync()
        }
    }

    fun setBrightness(brightness: Int) {
        val b = brightness.coerceIn(0, 255)
        val commands = listOf(
            "echo 1-0:0:0:$b > /sys/class/sn3112l/led/brightness",
            "echo 2-0:0:0:$b > /sys/class/sn3112l/led/brightness",
            "echo 1-0:0:0:$b > /sys/class/sn3112r/led/brightness",
            "echo 2-0:0:0:$b > /sys/class/sn3112r/led/brightness"
        )
        val command = commands.joinToString(" && ")
        if (!commandQueue.offer(command)) {
            commandQueue.poll()
            commandQueue.offer(command)
        }
        executeCommandAsync()
    }

    private fun executeCommandAsync() {
        commandExecutor.execute {
            commandQueue.poll()?.let { command ->
                executeCommand(command)
            }
        }
    }

    private fun executeCommand(command: String) {
        pServerBinder?.let { binder ->
            val data = Parcel.obtain()
            val reply = Parcel.obtain()

            try {
                data.writeStringArray(arrayOf(command, "1"))
                binder.transact(0, data, reply, IBinder.FLAG_ONEWAY)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                data.recycle()
                reply.recycle()
            }
        }
    }

    fun shutdown() {
        commandExecutor.shutdown()
        try {
            commandExecutor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            commandExecutor.shutdownNow()
        }
    }
}