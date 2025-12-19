package jr.brian.home.util

import jr.brian.home.model.CpuInfo
import java.io.RandomAccessFile

class CpuMonitor {
    private var lastCpuInfo: CpuInfo? = null

    fun getCurrentUsage(): Float {
        val currentInfo = readCpuStats() ?: return 0f
        val usage = currentInfo.calculateUsagePercent(lastCpuInfo)
        lastCpuInfo = currentInfo
        return usage.coerceIn(0f, 100f)
    }

    private fun readCpuStats(): CpuInfo? {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()

            val toks = load.split(" +".toRegex())
            val total = toks[1].toLong() + toks[2].toLong() + toks[3].toLong() +
                    toks[4].toLong() + toks[5].toLong() + toks[6].toLong() +
                    toks[7].toLong() + toks[8].toLong()
            val idle = toks[4].toLong()

            CpuInfo(total, idle)
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("unused")
    fun reset() {
        lastCpuInfo = null
    }
}
