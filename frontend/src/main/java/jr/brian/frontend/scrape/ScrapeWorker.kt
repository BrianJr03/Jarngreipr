package jr.brian.frontend.scrape

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.GameEntity
import kotlinx.coroutines.flow.first

/**
 * WorkManager wrapper around [LibraryScraper] so a scrape survives
 * the frontend being finished by a game launch (plan §3.7
 * "Execution: ScrapeWorker via WorkManager with foreground
 * notification"). Phase 4 skeleton — scope is per-run every game
 * in the DB; a later phase adds "scrape this one game" with input
 * data.
 */
@HiltWorker
class ScrapeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val scraper: LibraryScraper,
    private val gameDao: GameDao,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        setForeground(makeForeground(doneLabel = null))
        // One-shot read of every visible game; the Flow emits
        // immediately with the current DB snapshot.
        val games: List<GameEntity> = gameDao.observeAllVisible().first()
        val summary = scraper.scrape(games = games, request = ScrapeRequest())
        return if (summary.errors > 0 && summary.downloads == 0) Result.failure() else Result.success()
    }

    private fun makeForeground(doneLabel: String?): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Scraping library")
            .setContentText(doneLabel ?: "Fetching art…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= 34) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Library scrape", NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        private const val CHANNEL_ID = "frontend_scrape"
        private const val NOTIFICATION_ID = 1_942
        const val UNIQUE_WORK_NAME = "frontend_scrape_all"

        /** Enqueue the worker. Replaces any already-running scrape. */
        fun enqueueAll(context: Context) {
            val req = OneTimeWorkRequestBuilder<ScrapeWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                req,
            )
        }
    }
}
