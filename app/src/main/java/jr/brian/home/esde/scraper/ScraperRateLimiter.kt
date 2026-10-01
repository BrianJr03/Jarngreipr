package jr.brian.home.esde.scraper

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.system.measureTimeMillis

/**
 * Bounds concurrent requests + spaces them out so a single backend doesn't
 * burn its per-minute quota. `threads` gates parallelism; `minIntervalMs`
 * enforces a floor between successive request starts globally across the
 * limiter. Threads default to 1 for both clients and can be raised at
 * runtime once we know the account's actual thread quota.
 */
class ScraperRateLimiter(
    initialThreads: Int = 1,
    initialMinIntervalMs: Long = 1200L
) {
    @Volatile private var threads: Int = initialThreads.coerceAtLeast(1)
    @Volatile private var minIntervalMs: Long = initialMinIntervalMs.coerceAtLeast(0)
    private var semaphore = Semaphore(threads)
    private val gate = Mutex()
    @Volatile private var lastStartMs: Long = 0L

    fun updateLimits(threads: Int, minIntervalMs: Long) {
        val t = threads.coerceAtLeast(1)
        this.threads = t
        this.minIntervalMs = minIntervalMs.coerceAtLeast(0)
        semaphore = Semaphore(t)
    }

    suspend fun <T> withPermit(block: suspend () -> T): T = semaphore.withPermit {
        gate.lock()
        try {
            val now = System.currentTimeMillis()
            val wait = (lastStartMs + minIntervalMs) - now
            if (wait > 0) delay(wait)
            lastStartMs = System.currentTimeMillis()
        } finally {
            gate.unlock()
        }
        block()
    }

    @Suppress("unused") // exposed for tests / diagnostic logging
    inline fun <T> measure(block: () -> T): Pair<T, Long> {
        var result: T
        val ms = measureTimeMillis { result = block() }
        @Suppress("UNCHECKED_CAST")
        return (result as T) to ms
    }
}
