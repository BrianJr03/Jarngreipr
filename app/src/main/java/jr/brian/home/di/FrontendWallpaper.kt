package jr.brian.home.di

import javax.inject.Qualifier

/**
 * Dagger qualifier for the WallpaperManager scoped to the frontend
 * (bottom) display, backed by its own SharedPreferences file so the
 * user can pick a wallpaper for FrontEndActivity independent of the
 * main launcher side.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FrontendWallpaper
