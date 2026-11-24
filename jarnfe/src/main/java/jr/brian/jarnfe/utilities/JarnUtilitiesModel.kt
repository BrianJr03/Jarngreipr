package jr.brian.jarnfe.utilities

private const val BYTES_PER_GB = 1024f * 1024f * 1024f
private const val MS_PER_MINUTE = 60_000L
private const val MS_PER_HOUR = 3_600_000L
private const val CPU_FIELDS = 8

data class JarnCpuTicks(val total: Long, val idle: Long)

/** Reads the aggregate line of /proc/stat. Null when the line isn't in the expected shape. */
fun jarnParseCpuTicks(line: String?): JarnCpuTicks? {
    val fields = line?.trim()?.split(Regex("\\s+")) ?: return null
    if (fields.firstOrNull() != "cpu" || fields.size <= CPU_FIELDS) return null
    val ticks = fields.subList(1, CPU_FIELDS + 1).map { it.toLongOrNull() ?: return null }
    return JarnCpuTicks(total = ticks.sum(), idle = ticks[3])
}

fun jarnCpuReading(previous: JarnCpuTicks?, current: JarnCpuTicks?): JarnCpuReading {
    if (current == null) return JarnCpuReading.Unavailable
    if (previous == null) return JarnCpuReading.Measuring
    val total = current.total - previous.total
    if (total <= 0) return JarnCpuReading.Measuring
    val busy = total - (current.idle - previous.idle)
    return JarnCpuReading.Percent((busy * 100f / total).coerceIn(0f, 100f))
}

/** Current is negative while discharging. Anything else has no honest estimate. */
fun jarnDischargeRemainingMs(chargeCounterUah: Int, currentUa: Int): Long? {
    if (currentUa >= 0 || chargeCounterUah <= 0) return null
    return (chargeCounterUah.toDouble() / -currentUa.toDouble() * MS_PER_HOUR).toLong().takeIf { it >= MS_PER_MINUTE }
}

/** The platform reports -1 when it has no charge estimate. */
fun jarnChargeRemainingMs(reported: Long): Long? = reported.takeIf { it >= MS_PER_MINUTE }

internal fun jarnGigabytes(bytes: Long): Float = bytes / BYTES_PER_GB

internal data class JarnDuration(val hours: Long, val minutes: Long)

internal fun jarnDuration(ms: Long): JarnDuration =
    JarnDuration(hours = ms / MS_PER_HOUR, minutes = (ms % MS_PER_HOUR) / MS_PER_MINUTE)

internal fun jarnVolumeSteps(max: Int): Int = max.coerceAtLeast(1)

internal fun jarnVolumeLevel(value: Float, max: Int): Int =
    Math.round(value).coerceIn(0, jarnVolumeSteps(max))
