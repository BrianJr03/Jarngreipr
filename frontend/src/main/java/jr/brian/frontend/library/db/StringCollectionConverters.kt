package jr.brian.frontend.library.db

import androidx.room.TypeConverter

/**
 * Pipe-delimited string serialization for list/set columns. The pipe is
 * chosen because it does not appear in any ROM extension, system id, or
 * ScreenScraper region code — the three places these converters are
 * actually used. If a future caller stores values that can contain `|`,
 * switch to a different separator before adding the column; TypeConverters
 * are database-wide and changing one mid-schema is a migration.
 *
 * `null` and `""` both round-trip to the empty collection — Room treats
 * a null column the same way an empty collection would represent an
 * uninitialized slot, and this collapses that distinction here so DAOs
 * never have to.
 */
internal class StringCollectionConverters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        value?.joinToString(SEP) ?: ""

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split(SEP)

    @TypeConverter
    fun fromStringSet(value: Set<String>?): String =
        value?.joinToString(SEP) ?: ""

    @TypeConverter
    fun toStringSet(value: String?): Set<String> =
        if (value.isNullOrEmpty()) emptySet() else value.split(SEP).toSet()

    private companion object {
        const val SEP = "|"
    }
}
