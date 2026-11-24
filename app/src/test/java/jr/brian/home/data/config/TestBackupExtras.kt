package jr.brian.home.data.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.mockk
import jr.brian.home.data.database.RssFeedDao

/** Real extras over Robolectric storage, with an empty feed list unless [dao] says otherwise. */
internal fun testBackupExtras(
    dao: RssFeedDao = mockk(relaxed = true) { coEvery { getAllFeedsSnapshot() } returns emptyList() },
): SettingsBackupExtras {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return SettingsBackupExtras(
        context = context,
        appearance = AppearanceBackup(context),
        rssFeeds = RssFeedBackup(dao),
        jingles = JingleBackup(context),
    )
}
