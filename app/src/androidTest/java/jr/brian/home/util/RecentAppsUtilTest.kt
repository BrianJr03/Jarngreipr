package jr.brian.home.util

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentAppsUtilTest {
    private lateinit var context: Context
    private lateinit var mockContext: Context
    private lateinit var mockAppOpsManager: AppOpsManager
    private lateinit var mockUsageStatsManager: UsageStatsManager
    private lateinit var mockPackageManager: PackageManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        mockContext = mockk(relaxed = true)
        mockAppOpsManager = mockk(relaxed = true)
        mockUsageStatsManager = mockk(relaxed = true)
        mockPackageManager = mockk(relaxed = true)
        
        every { mockContext.getSystemService(Context.APP_OPS_SERVICE) } returns mockAppOpsManager
        every { mockContext.getSystemService(Context.USAGE_STATS_SERVICE) } returns mockUsageStatsManager
        every { mockContext.packageManager } returns mockPackageManager
        every { mockContext.packageName } returns "jr.brian.home"
        
        mockkStatic(Process::class)
        every { Process.myUid() } returns 10000
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun testHasUsageStatsPermission_whenPermissionGranted_returnsTrue() {
        every { 
            mockAppOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                any(),
                any()
            ) 
        } returns AppOpsManager.MODE_ALLOWED
        
        val result = RecentAppsUtil.hasUsageStatsPermission(mockContext)
        
        assertTrue(result)
    }
    
    @Test
    fun testHasUsageStatsPermission_whenPermissionDenied_returnsFalse() {
        every { 
            mockAppOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                any(),
                any()
            ) 
        } returns AppOpsManager.MODE_IGNORED
        
        val result = RecentAppsUtil.hasUsageStatsPermission(mockContext)
        
        assertFalse(result)
    }
    
    @Test
    fun testHasUsageStatsPermission_whenPermissionDefault_returnsFalse() {
        every { 
            mockAppOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                any(),
                any()
            ) 
        } returns AppOpsManager.MODE_DEFAULT
        
        val result = RecentAppsUtil.hasUsageStatsPermission(mockContext)
        
        assertFalse(result)
    }
    
    @Test
    fun testHasUsageStatsPermission_usesCorrectUidAndPackage() {
        val testUid = 12345
        val testPackage = "com.test.app"
        
        every { Process.myUid() } returns testUid
        every { mockContext.packageName } returns testPackage
        every { 
            mockAppOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                testUid,
                testPackage
            ) 
        } returns AppOpsManager.MODE_ALLOWED
        
        val result = RecentAppsUtil.hasUsageStatsPermission(mockContext)
        
        assertTrue(result)
    }
    
    @Test
    fun testGetRecentApps_returnsEmptyList_whenNoUsageStats() {
        every { 
            mockUsageStatsManager.queryUsageStats(
                any(),
                any(),
                any()
            ) 
        } returns emptyList()
        
        val result = RecentAppsUtil.getRecentApps(mockContext)
        
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun testGetRecentApps_filtersOutAppsWithZeroForegroundTime() {
        val currentTime = System.currentTimeMillis()
        val recentTime = currentTime - (5 * 60 * 1000) // 5 minutes ago
        
        val usageStats1 = createMockUsageStats("com.app1", recentTime, 0) // No foreground time
        val usageStats2 = createMockUsageStats("com.app2", recentTime, 10000) // Has foreground time
        
        setupMockPackageManager("com.app2", "App 2")
        
        every { 
            mockUsageStatsManager.queryUsageStats(
                any(),
                any(),
                any()
            ) 
        } returns listOf(usageStats1, usageStats2)
        
        val result = RecentAppsUtil.getRecentApps(mockContext)
        
        assertEquals(1, result.size)
        assertEquals("com.app2", result[0].packageName)
    }
    
    @Test
    fun testGetRecentApps_filtersOutAppsOlderThan10Minutes() {
        val currentTime = System.currentTimeMillis()
        val recentTime = currentTime - (5 * 60 * 1000) // 5 minutes ago
        val oldTime = currentTime - (15 * 60 * 1000) // 15 minutes ago
        
        val usageStats1 = createMockUsageStats("com.app1", recentTime, 10000)
        val usageStats2 = createMockUsageStats("com.app2", oldTime, 10000)
        
        setupMockPackageManager("com.app1", "App 1")
        setupMockPackageManager("com.app2", "App 2")
        
        every { 
            mockUsageStatsManager.queryUsageStats(
                any(),
                any(),
                any()
            ) 
        } returns listOf(usageStats1, usageStats2)
        
        val result = RecentAppsUtil.getRecentApps(mockContext)
        
        assertEquals(1, result.size)
        assertEquals("com.app1", result[0].packageName)
    }
    
    @Test
    fun testGetRecentApps_filtersOutLauncherPackage() {
        val currentTime = System.currentTimeMillis()
        val recentTime = currentTime - (5 * 60 * 1000)
        
        val usageStats1 = createMockUsageStats("jr.brian.home", recentTime, 10000)
        val usageStats2 = createMockUsageStats("com.app1", recentTime, 10000)
        
        setupMockPackageManager("com.app1", "App 1")
        
        every { 
            mockUsageStatsManager.queryUsageStats(
                any(),
                any(),
                any()
            ) 
        } returns listOf(usageStats1, usageStats2)
        
        val result = RecentAppsUtil.getRecentApps(mockContext)
        
        assertEquals(1, result.size)
        assertEquals("com.app1", result[0].packageName)
    }
    
    @Test
    fun testGetRecentApps_filtersOutAppsWithoutLaunchIntent() {
        val currentTime = System.currentTimeMillis()
        val recentTime = currentTime - (5 * 60 * 1000)
        
        val usageStats1 = createMockUsageStats("com.app1", recentTime, 10000)
        val usageStats2 = createMockUsageStats("com.app2", recentTime, 10000)
        
        setupMockPackageManager("com.app1", "App 1", hasLaunchIntent = true)
        setupMockPackageManager("com.app2", "App 2", hasLaunchIntent = false)
        
        every { 
            mockUsageStatsManager.queryUsageStats(
                any(),
                any(),
                any()
            ) 
        } returns listOf(usageStats1, usageStats2)
        
        val result = RecentAppsUtil.getRecentApps(mockContext)
        
        assertEquals(1, result.size)
        assertEquals("com.app1", result[0].packageName)
    }
    
    @Test
    fun testGetRecentApps_sortsAppsByLastUsedTime() {
        val currentTime = System.currentTimeMillis()
        val time1 = currentTime - (3 * 60 * 1000) // 3 minutes ago
        val time2 = currentTime - (5 * 60 * 1000) // 5 minutes ago
        val time3 = currentTime - (7 * 60 * 1000) // 7 minutes ago
        
        val usageStats1 = createMockUsageStats("com.app1", time2, 10000)
        val usageStats2 = createMockUsageStats("com.app2", time1, 10000) // Most recent
        val usageStats3 = createMockUsageStats("com.app3", time3, 10000)
        
        setupMockPackageManager("com.app1", "App 1")
        setupMockPackageManager("com.app2", "App 2")
        setupMockPackageManager("com.app3", "App 3")
        
        every { 
            mockUsageStatsManager.queryUsageStats(
                any(),
                any(),
                any()
            ) 
        } returns listOf(usageStats1, usageStats2, usageStats3)
        
        val result = RecentAppsUtil.getRecentApps(mockContext)
        
        assertEquals(3, result.size)
        assertEquals("com.app2", result[0].packageName) // Most recent first
        assertEquals("com.app1", result[1].packageName)
        assertEquals("com.app3", result[2].packageName)
    }
    
    @Test
    fun testGetRecentApps_removesDuplicatePackages() {
        val currentTime = System.currentTimeMillis()
        val recentTime = currentTime - (5 * 60 * 1000)
        
        // Same package, different times
        val usageStats1 = createMockUsageStats("com.app1", recentTime - 60000, 10000)
        val usageStats2 = createMockUsageStats("com.app1", recentTime, 10000)
        
        setupMockPackageManager("com.app1", "App 1")
        
        every { 
            mockUsageStatsManager.queryUsageStats(
                any(),
                any(),
                any()
            ) 
        } returns listOf(usageStats1, usageStats2)
        
        val result = RecentAppsUtil.getRecentApps(mockContext)
        
        assertEquals(1, result.size)
        assertEquals("com.app1", result[0].packageName)
    }
    
    @Test
    fun testGetRecentApps_limitsResultsTo20Apps() {
        val currentTime = System.currentTimeMillis()
        val usageStatsList = mutableListOf<UsageStats>()
        
        // Create 25 apps
        for (i in 1..25) {
            val time = currentTime - (i * 10 * 1000) // Each app used progressively earlier
            usageStatsList.add(createMockUsageStats("com.app$i", time, 10000))
            setupMockPackageManager("com.app$i", "App $i")
        }
        
        every { 
            mockUsageStatsManager.queryUsageStats(
                any(),
                any(),
                any()
            ) 
        } returns usageStatsList
        
        val result = RecentAppsUtil.getRecentApps(mockContext)
        
        assertEquals(20, result.size)
    }
    
    @Test
    fun testGetRecentApps_handlesPackageNotFound() {
        val currentTime = System.currentTimeMillis()
        val recentTime = currentTime - (5 * 60 * 1000)
        
        val usageStats1 = createMockUsageStats("com.app1", recentTime, 10000)
        val usageStats2 = createMockUsageStats("com.app2", recentTime, 10000)
        
        setupMockPackageManager("com.app1", "App 1")
        every { 
            mockPackageManager.getApplicationInfo("com.app2", any<PackageManager.ApplicationInfoFlags>())
        } throws PackageManager.NameNotFoundException()
        
        every { 
            mockUsageStatsManager.queryUsageStats(
                any(),
                any(),
                any()
            ) 
        } returns listOf(usageStats1, usageStats2)
        
        val result = RecentAppsUtil.getRecentApps(mockContext)
        
        assertEquals(1, result.size)
        assertEquals("com.app1", result[0].packageName)
    }
    
    @Test
    fun testGetRecentApps_populatesRecentAppInfoCorrectly() {
        val currentTime = System.currentTimeMillis()
        val recentTime = currentTime - (5 * 60 * 1000)
        val usageTime = 300000L // 5 minutes in ms
        
        val usageStats = createMockUsageStats("com.app1", recentTime, usageTime)
        
        setupMockPackageManager("com.app1", "Test App")
        
        every { 
            mockUsageStatsManager.queryUsageStats(
                any(),
                any(),
                any()
            ) 
        } returns listOf(usageStats)
        
        val result = RecentAppsUtil.getRecentApps(mockContext)
        
        assertEquals(1, result.size)
        assertEquals("com.app1", result[0].packageName)
        assertEquals("Test App", result[0].label)
        assertEquals(usageTime, result[0].usageTimeMs)
    }
    
    @Test
    fun testFormatUsageDuration_lessThanOneMinute_returnsLessThan1m() {
        val result = RecentAppsUtil.formatUsageDuration(0)
        assertEquals("<1m", result)
        
        val result2 = RecentAppsUtil.formatUsageDuration(30000) // 30 seconds
        assertEquals("<1m", result2)
        
        val result3 = RecentAppsUtil.formatUsageDuration(59999) // Just under 1 minute
        assertEquals("<1m", result3)
    }
    
    @Test
    fun testFormatUsageDuration_negativeValue_returnsLessThan1m() {
        val result = RecentAppsUtil.formatUsageDuration(-100)
        assertEquals("<1m", result)
    }
    
    @Test
    fun testFormatUsageDuration_exactlyOneMinute_returns1m() {
        val result = RecentAppsUtil.formatUsageDuration(60000) // 1 minute
        assertEquals("1m", result)
    }
    
    @Test
    fun testFormatUsageDuration_multipleMinutes_returnsMinutesOnly() {
        val result = RecentAppsUtil.formatUsageDuration(180000) // 3 minutes
        assertEquals("3m", result)
        
        val result2 = RecentAppsUtil.formatUsageDuration(1800000) // 30 minutes
        assertEquals("30m", result2)
        
        val result3 = RecentAppsUtil.formatUsageDuration(3540000) // 59 minutes
        assertEquals("59m", result3)
    }
    
    @Test
    fun testFormatUsageDuration_exactlyOneHour_returns1h() {
        val result = RecentAppsUtil.formatUsageDuration(3600000) // 1 hour
        assertEquals("1h", result)
    }
    
    @Test
    fun testFormatUsageDuration_multipleHoursNoMinutes_returnsHoursOnly() {
        val result = RecentAppsUtil.formatUsageDuration(7200000) // 2 hours
        assertEquals("2h", result)
        
        val result2 = RecentAppsUtil.formatUsageDuration(36000000) // 10 hours
        assertEquals("10h", result2)
    }
    
    @Test
    fun testFormatUsageDuration_hoursAndMinutes_returnsBothFormatted() {
        val result = RecentAppsUtil.formatUsageDuration(3660000) // 1 hour 1 minute
        assertEquals("1h 1m", result)
        
        val result2 = RecentAppsUtil.formatUsageDuration(5400000) // 1 hour 30 minutes
        assertEquals("1h 30m", result2)
        
        val result3 = RecentAppsUtil.formatUsageDuration(9000000) // 2 hours 30 minutes
        assertEquals("2h 30m", result3)
    }
    
    @Test
    fun testFormatUsageDuration_roundsDownMinutes() {
        val result = RecentAppsUtil.formatUsageDuration(119999) // 1 minute 59 seconds
        assertEquals("1m", result)
        
        val result2 = RecentAppsUtil.formatUsageDuration(3719999) // 1 hour 1 minute 59 seconds
        assertEquals("1h 1m", result2)
    }
    
    @Test
    fun testFormatUsageDuration_variousLargeDurations() {
        val result = RecentAppsUtil.formatUsageDuration(43200000) // 12 hours
        assertEquals("12h", result)
        
        val result2 = RecentAppsUtil.formatUsageDuration(86400000) // 24 hours
        assertEquals("24h", result2)
        
        val result3 = RecentAppsUtil.formatUsageDuration(90060000) // 25 hours 1 minute
        assertEquals("25h 1m", result3)
    }
    
    private fun createMockUsageStats(
        packageName: String,
        lastTimeUsed: Long,
        totalTimeInForeground: Long
    ): UsageStats {
        val usageStats = mockk<UsageStats>(relaxed = true)
        every { usageStats.packageName } returns packageName
        every { usageStats.lastTimeUsed } returns lastTimeUsed
        every { usageStats.totalTimeInForeground } returns totalTimeInForeground
        return usageStats
    }
    
    private fun setupMockPackageManager(
        packageName: String,
        label: String,
        hasLaunchIntent: Boolean = true
    ) {
        val appInfo = mockk<ApplicationInfo>(relaxed = true)
        val drawable = ColorDrawable(0xFF0000FF.toInt())
        
        every { 
            mockPackageManager.getApplicationInfo(
                packageName,
                any<PackageManager.ApplicationInfoFlags>()
            )
        } returns appInfo
        
        every { mockPackageManager.getApplicationLabel(appInfo) } returns label
        every { mockPackageManager.getApplicationIcon(appInfo) } returns drawable
        
        if (hasLaunchIntent) {
            val intent = mockk<Intent>(relaxed = true)
            every { mockPackageManager.getLaunchIntentForPackage(packageName) } returns intent
        } else {
            every { mockPackageManager.getLaunchIntentForPackage(packageName) } returns null
        }
    }
}
