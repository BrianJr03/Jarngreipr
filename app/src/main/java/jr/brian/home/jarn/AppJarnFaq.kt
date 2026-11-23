package jr.brian.home.jarn

import android.content.Context
import androidx.annotation.StringRes
import jr.brian.home.R
import jr.brian.jarnfe.utilities.JarnFaqEntry
import jr.brian.jarnfe.utilities.JarnFaqSection
import jr.brian.jarnfe.utilities.JarnFaqSnippet

/** Reads Classic's FAQ strings so both frontends share one copy of the answers. */
internal fun jarnFaqSections(context: Context): List<JarnFaqSection> {
    fun entry(@StringRes question: Int, @StringRes answer: Int, vararg snippets: JarnFaqSnippet) =
        JarnFaqEntry(context.getString(question), context.getString(answer), snippets.toList())

    fun snippet(@StringRes label: Int, @StringRes value: Int) =
        JarnFaqSnippet(context.getString(label), context.getString(value))

    fun section(@StringRes title: Int, vararg entries: JarnFaqEntry) =
        JarnFaqSection(context.getString(title), entries.toList())

    return listOf(
        section(
            R.string.faq_section_general,
            entry(R.string.faq_keyboard_question, R.string.faq_keyboard_answer),
            entry(R.string.faq_app_close_question, R.string.faq_app_close_answer),
        ),
        section(
            R.string.faq_section_jingles,
            entry(R.string.faq_jingles_what_question, R.string.faq_jingles_what_answer),
            entry(R.string.faq_jingles_offline_question, R.string.faq_jingles_offline_answer),
            entry(R.string.faq_jingles_matching_question, R.string.faq_jingles_matching_answer),
            entry(
                R.string.faq_jingles_format_question,
                R.string.faq_jingles_format_answer,
                snippet(R.string.jarn_faq_example, R.string.faq_jingles_format_code),
            ),
            entry(R.string.faq_jingles_troubleshoot_question, R.string.faq_jingles_troubleshoot_answer),
            entry(
                R.string.faq_jingles_defaults_question,
                R.string.faq_jingles_defaults_answer,
                snippet(R.string.jarn_faq_example, R.string.faq_jingles_defaults_flat_code),
                snippet(R.string.jarn_faq_grouped_format, R.string.faq_jingles_defaults_grouped_code),
            ),
            entry(R.string.jingles_faq_inconsistent_q, R.string.jingles_faq_inconsistent_a),
        ),
        section(
            R.string.faq_section_rom_search,
            entry(R.string.faq_rom_search_what_question, R.string.faq_rom_search_what_answer),
            entry(R.string.faq_rom_search_emulator_question, R.string.faq_rom_search_emulator_answer),
        ),
        section(
            R.string.faq_section_rss,
            entry(R.string.faq_rss_nowplaying_question, R.string.faq_rss_nowplaying_answer),
            entry(
                R.string.faq_rss_adb_question,
                R.string.faq_rss_adb_answer,
                snippet(R.string.jarn_faq_grant_command, R.string.faq_rss_adb_command),
            ),
        ),
        section(
            R.string.faq_section_theming,
            entry(R.string.faq_theme_sharing_question, R.string.faq_theme_sharing_answer),
            entry(R.string.faq_wallpaper_sharing_question, R.string.faq_wallpaper_sharing_answer),
            entry(
                R.string.faq_wallpaper_automation_question,
                R.string.faq_wallpaper_automation_answer,
                snippet(R.string.jarn_faq_esde_wallpaper_action, R.string.jarn_faq_esde_wallpaper_value),
                snippet(R.string.jarn_faq_standard_wallpaper_action, R.string.jarn_faq_standard_wallpaper_value),
                JarnFaqSnippet(context.getString(R.string.jarn_faq_package), context.packageName),
                snippet(R.string.jarn_faq_class, R.string.jarn_faq_class_value),
            ),
        ),
    )
}
