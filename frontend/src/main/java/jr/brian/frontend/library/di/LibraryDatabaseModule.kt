package jr.brian.frontend.library.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.MediaDao
import jr.brian.frontend.library.db.PlayHistoryDao
import jr.brian.frontend.library.db.ScrapeJobDao
import jr.brian.frontend.library.db.SourceDao
import jr.brian.frontend.library.db.SystemDao
import javax.inject.Singleton

/**
 * Provides the library engine's Room graph. Separate module from
 * `:app`'s `AppModule` so :frontend owns the lifecycle of its own
 * tables without the launcher's prefs database coming along for the
 * ride.
 */
@Module
@InstallIn(SingletonComponent::class)
object LibraryDatabaseModule {

    @Provides
    @Singleton
    fun provideLibraryDatabase(
        @ApplicationContext context: Context,
    ): LibraryDatabase = Room.databaseBuilder(
        context,
        LibraryDatabase::class.java,
        LibraryDatabase.DATABASE_NAME,
    ).build()

    @Provides fun provideSystemDao(db: LibraryDatabase): SystemDao = db.systemDao()
    @Provides fun provideSourceDao(db: LibraryDatabase): SourceDao = db.sourceDao()
    @Provides fun provideGameDao(db: LibraryDatabase): GameDao = db.gameDao()
    @Provides fun provideMediaDao(db: LibraryDatabase): MediaDao = db.mediaDao()
    @Provides fun provideScrapeJobDao(db: LibraryDatabase): ScrapeJobDao = db.scrapeJobDao()
    @Provides fun providePlayHistoryDao(db: LibraryDatabase): PlayHistoryDao = db.playHistoryDao()
}
