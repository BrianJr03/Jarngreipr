package jr.brian.frontend.scrape

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Bounds concurrent requests and spaces them out so a single
 * backend doesn't burn its per-minute quota.
 *
 * [threads] gates parallelism; [minIntervalMs] enforces a floor
 * between successive request starts across the whole limiter. Both
 * are updateable so the ScreenScraper client can lift threads to
 * `ssuserInfos.maxthreads` after it reads them from the account.
 *
 * Ported from `:app`'s original with the same semantics so Phase 4's
 * end-to-end tests validate the same timing invariants.
 */
class ScraperRateLimiter(
    initialThreads: Int = 1,
    initialMinIntervalMs: Long = 1200L,
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
}
