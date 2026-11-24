package jr.brian.home.data.config

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val JINGLES_PREFS = "jingles_prefs"
private const val KEY_REPOS = "jingle_repos"
private const val KEY_ENABLED = "jingles_enabled"
private const val MAX_REPOS = 100
private val RepoSlug = Regex("^[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}$")

/** Only an owner/name pair, so a restored repo always resolves to a GitHub repo. */
internal fun isRestorableRepo(slug: String): Boolean =
    RepoSlug.matches(slug) && slug.split('/').none { it == "." || it == ".." }

/** Repos are only ever added, so a restore never drops one this device already uses. */
internal fun mergeJingleRepos(current: Collection<String>, backup: List<String>?): Set<String> {
    if (backup == null) return current.toSet()
    val added = backup.filter(::isRestorableRepo).filter { it !in current }.distinct()
    return current.toSet() + added.take((MAX_REPOS - current.size).coerceAtLeast(0))
}

/**
 * The jingle on/off switch and GitHub repos. Local folders and downloaded packs stay out, because
 * they point at storage this device would need to grant again.
 */
@Singleton
class JingleBackup @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val prefs by lazy { context.getSharedPreferences(JINGLES_PREFS, Context.MODE_PRIVATE) }

    val enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)

    val repos: List<String>
        get() = prefs.getStringSet(KEY_REPOS, emptySet()).orEmpty().sorted()

    fun restore(enabled: Boolean?, repos: List<String>?) {
        val mergedRepos = mergeJingleRepos(this.repos, repos)
        prefs.edit {
            enabled?.let { putBoolean(KEY_ENABLED, it) }
            if (repos != null) putStringSet(KEY_REPOS, mergedRepos)
        }
    }
}
