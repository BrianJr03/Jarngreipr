package jr.brian.home.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a custom icon for an app.
 * Maps package names to custom PNG file paths.
 */
@Entity(tableName = "custom_icons")
data class CustomIconEntity(
    @PrimaryKey
    val packageName: String,
    val customIconPath: String // Path to the PNG in internal storage
)
