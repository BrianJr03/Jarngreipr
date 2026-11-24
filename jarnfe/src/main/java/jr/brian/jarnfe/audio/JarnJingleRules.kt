package jr.brian.jarnfe.audio

private val RepoSlug = Regex("^[A-Za-z0-9][A-Za-z0-9-]{0,38}/[A-Za-z0-9._-]{1,100}$")
private val GitHubPrefix = Regex("^https://(?:www\\.)?github\\.com/", RegexOption.IGNORE_CASE)
private val PlatformName = Regex("^[a-z0-9_-]{1,32}$")
private val AudioExtensions = setOf("mp3", "ogg", "flac", "m4a", "wav", "aac", "opus")
private const val MAX_INDEX_ENTRIES = 5_000
private const val MAX_ENTRY_PATH = 512
private const val MAX_PACK_FIELD = 64

/** The owner/repo for a typed slug or GitHub link, or null when it is neither. */
fun jarnJingleRepoSlug(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.contains("://") && !GitHubPrefix.containsMatchIn(trimmed)) return null
    if (trimmed.any { it == '?' || it == '#' }) return null
    val slug = trimmed.replace(GitHubPrefix, "").trimEnd('/').removeSuffix(".git")
    if (!RepoSlug.matches(slug)) return null
    val repo = slug.substringAfter('/')
    return slug.takeUnless { repo == "." || repo == ".." }
}

/** A repository's file path may only name an audio file inside that repository. */
fun jarnJingleEntryPathSafe(path: String): Boolean {
    if (path.isEmpty() || path.length > MAX_ENTRY_PATH) return false
    if (path.startsWith("/") || path.contains('\\') || path.contains(':')) return false
    if (path.any { it.isISOControl() }) return false
    val segments = path.split('/')
    if (segments.any { it.isEmpty() || it == "." || it == ".." }) return false
    return segments.last().substringAfterLast('.', "").lowercase() in AudioExtensions
}

fun jarnJingleIndexSafe(paths: List<String>): Boolean =
    paths.size <= MAX_INDEX_ENTRIES && paths.all(::jarnJingleEntryPathSafe)

/** A pack or game name, which also becomes part of a folder or index entry. */
fun jarnJinglePackFieldValid(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty() || trimmed.length > MAX_PACK_FIELD) return false
    if (trimmed == "." || trimmed == "..") return false
    return trimmed.none { it == '/' || it == '\\' || it.isISOControl() }
}

fun jarnJinglePlatformValid(value: String): Boolean = PlatformName.matches(value.trim().lowercase())

fun jarnJingleFileNameValid(value: String): Boolean {
    if (!jarnJinglePackFieldValid(value)) return false
    val trimmed = value.trim()
    return !trimmed.contains('.') || trimmed.substringAfterLast('.').lowercase() in AudioExtensions
}

/** Keeps the system folder, so system-wide jingles match however the library stores its paths. */
fun jarnJingleGameKey(systemId: String, path: String): String {
    val name = path.replace('\\', '/').trimEnd('/').substringAfterLast('/')
    return "$systemId/$name"
}
