package jr.brian.home.data

import jr.brian.home.model.app.RecentAppInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for caching recent apps to avoid reloading on every screen open.
 * Cache expires after 30 seconds to ensure fresh data on revisit.
 */
@Singleton
class RecentAppsCacheManager @Inject constructor() {
    
    private var cachedApps: List<RecentAppInfo> = emptyList()
    private var cacheTimestamp: Long = 0
    
    companion object {
        private const val CACHE_DURATION_MS = 30_000L // 30 seconds
    }
    
    /**
     * Get cached apps if still valid, or null if cache is expired/empty.
     */
    fun get(): List<RecentAppInfo>? {
        val now = System.currentTimeMillis()
        return if (cachedApps.isNotEmpty() && (now - cacheTimestamp) < CACHE_DURATION_MS) {
            cachedApps
        } else {
            null
        }
    }
    
    /**
     * Cache the list of recent apps.
     */
    fun set(apps: List<RecentAppInfo>) {
        cachedApps = apps
        cacheTimestamp = System.currentTimeMillis()
    }
    
    /**
     * Clear the cache.
     */
    fun clear() {
        cachedApps = emptyList()
        cacheTimestamp = 0
    }
    
    /**
     * Check if cache has valid data.
     */
    fun hasValidCache(): Boolean {
        val now = System.currentTimeMillis()
        return cachedApps.isNotEmpty() && (now - cacheTimestamp) < CACHE_DURATION_MS
    }
}
