package jr.brian.home.model

data class CpuInfo(
    val total: Long,
    val idle: Long
) {
    fun calculateUsagePercent(previous: CpuInfo?): Float {
        return previous?.let {
            val totalDiff = this.total - it.total
            val idleDiff = this.idle - it.idle
            if (totalDiff > 0) {
                ((totalDiff - idleDiff).toFloat() / totalDiff.toFloat()) * 100f
            } else {
                0f
            }
        } ?: 0f
    }
}
