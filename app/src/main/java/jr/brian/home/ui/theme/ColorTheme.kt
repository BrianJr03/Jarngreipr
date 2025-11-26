package jr.brian.home.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import jr.brian.home.R

data class ColorTheme(
    val id: String,
    @param:StringRes val nameResId: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val lightTextColor: Color,
) {
    companion object {
        val PINK_VIOLET =
            ColorTheme(
                id = "pink_violet",
                nameResId = R.string.theme_pink_violet,
                primaryColor = Color(0xFF8A2BE2),
                secondaryColor = Color(0xFFFF69B4),
                lightTextColor = Color(0xFFFF69B4),
            )

        val BLUE_YELLOW =
            ColorTheme(
                id = "blue_yellow",
                nameResId = R.string.theme_blue_yellow,
                primaryColor = Color(0xFF4169E1),
                secondaryColor = Color(0xFFFFD700),
                lightTextColor = Color(0xFFFFD700),
            )

        val GREEN_CYAN =
            ColorTheme(
                id = "green_cyan",
                nameResId = R.string.theme_green_cyan,
                primaryColor = Color(0xFF008B45),
                secondaryColor = Color(0xFF00CED1),
                lightTextColor = Color(0xFF00CED1),
            )

        val PURPLE_ORANGE =
            ColorTheme(
                id = "purple_orange",
                nameResId = R.string.theme_purple_orange,
                primaryColor = Color(0xFF6A0DAD),
                secondaryColor = Color(0xFFFF8C00),
                lightTextColor = Color(0xFFFF8C00),
            )

        val RED_BLUE =
            ColorTheme(
                id = "red_blue",
                nameResId = R.string.theme_red_blue,
                primaryColor = Color(0xFF4169E1),
                secondaryColor = Color(0xFFE94560),
                lightTextColor = Color(0xFFE94560),
            )

        val MAGENTA_LIME =
            ColorTheme(
                id = "magenta_lime",
                nameResId = R.string.theme_magenta_lime,
                primaryColor = Color(0xFFAA00FF),
                secondaryColor = Color(0xFF00FF00),
                lightTextColor = Color(0xFF00FF00),
            )

        val OLED_BLACK_WHITE =
            ColorTheme(
                id = "oled_black_white",
                nameResId = R.string.theme_light_gray,
                primaryColor = Color.LightGray,
                secondaryColor = Color.LightGray,
                lightTextColor = Color.LightGray,
            )

        val allThemes =
            listOf(
                PINK_VIOLET,
                BLUE_YELLOW,
                GREEN_CYAN,
                PURPLE_ORANGE,
                RED_BLUE,
                MAGENTA_LIME,
                OLED_BLACK_WHITE,
            )

        fun fromId(id: String): ColorTheme = allThemes.find { it.id == id } ?: PINK_VIOLET
    }
}